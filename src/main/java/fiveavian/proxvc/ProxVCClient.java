package fiveavian.proxvc;

import fiveavian.proxvc.api.ClientEvents;
import fiveavian.proxvc.gui.HudComponentStatus;
import fiveavian.proxvc.gui.HudComponentWaveForm;
import fiveavian.proxvc.gui.MicrophoneListComponent;
import fiveavian.proxvc.gui.VolumeMixerComponent;
import fiveavian.proxvc.util.EnvironmentDescriptor;
import fiveavian.proxvc.util.MixerStore;
import fiveavian.proxvc.util.OptionStore;
import fiveavian.proxvc.util.Waveforms;
import fiveavian.proxvc.vc.AttenuationProfile;
import fiveavian.proxvc.vc.AudioInputDevice;
import fiveavian.proxvc.vc.StreamingAudioSource;
import fiveavian.proxvc.vc.client.VCInputClient;
import fiveavian.proxvc.vc.client.VCOutputClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.component.HudComponents;
import net.minecraft.client.gui.options.ScreenOptions;
import net.minecraft.client.gui.options.components.*;
import net.minecraft.client.gui.options.data.OptionsPage;
import net.minecraft.client.gui.options.data.OptionsPages;
import net.minecraft.client.input.InputDevice;
import net.minecraft.client.option.*;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.texture.Texture;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.Items;
import net.minecraft.core.net.packet.PacketLogin;
import net.minecraft.core.util.phys.Vec3;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.openal.AL10;

import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
#TODO Fix bug with output logic.
*/

public class ProxVCClient implements ClientModInitializer {
    public static ProxVCClient instance;
    public Minecraft client;
    public DatagramSocket socket;
    public AudioInputDevice device;
    public final Map<Integer, StreamingAudioSource> sources = new HashMap<>();
    public SocketAddress serverAddress;
    private Thread inputThread;
    private Thread outputThread;
    public Texture statusIconTexture;

    public final KeyBinding keyMute = new KeyBinding("key.mute").setDefault(InputDevice.keyboard, Keyboard.KEY_M);
    private boolean isMutePressed = false;
    public final KeyBinding keyPushToTalk = new KeyBinding("key.push_to_talk").setDefault(InputDevice.keyboard, Keyboard.KEY_V);
    public final KeyBinding keyOpenMenu = new KeyBinding("key.open_menu").setDefault(InputDevice.keyboard, Keyboard.KEY_N);

    public OptionsPage optionsPage;
    public final KeyBinding[] keyBindings = {keyMute, keyPushToTalk, keyOpenMenu};
    public OptionFloat voiceChatVolume;
    public OptionBoolean isMuted;
    public OptionBoolean usePushToTalk;
    public OptionBoolean showWaveform;
    public OptionBoolean showMicStatus;
    public OptionString selectedInputDevice;
    public OptionFloat muffleIntensity;
    public OptionEnum<AttenuationProfile> attenuationProfile;
    public OptionEnum<Waveforms.types> waveformType;

    public Option<?>[] options;
    public Path optionFilePath;
    private boolean attenuationProfileChanged = false;

    public EnvironmentDescriptor descriptor;

    public boolean isDisconnected() {
        return !client.isMultiplayerWorld() || serverAddress == null;
    }

    @Override
    public void onInitializeClient() {
        ClientEvents.START.add(this::start);
        ClientEvents.STOP.add(this::stop);
        ClientEvents.TICK.add(this::tick);
        ClientEvents.RENDER.add(this::render);
        ClientEvents.LOGIN.add(this::login);
        ClientEvents.DISCONNECT.add(this::disconnect);
    }

    private void start(Minecraft client) {
        instance = this;
        this.client = client;
        voiceChatVolume = new OptionFloat(client.gameSettings, "sound.voice_chat", 1.0f);
        isMuted = new OptionBoolean(client.gameSettings, "is_muted", false);
        usePushToTalk = new OptionBoolean(client.gameSettings, "use_push_to_talk", false);
        showWaveform = new OptionBoolean(client.gameSettings, "show_waveform", true);
        showMicStatus = new OptionBoolean(client.gameSettings, "show_mic_status", true);
        selectedInputDevice = new OptionString(client.gameSettings, "selected_input_device", null);
        muffleIntensity = new OptionFloat(client.gameSettings, "muffle_intensity", 1f);
        attenuationProfile = new OptionEnum<>(client.gameSettings, "attenuation_profile", AttenuationProfile.class, AttenuationProfile.VOICE_CLARITY);
        attenuationProfile.addCallback(value -> {
            attenuationProfileChanged = true;
        });
        waveformType = new OptionEnum<>(client.gameSettings, "waveform_type", Waveforms.types.class, Waveforms.types.BASIC);


        options = new Option[]{voiceChatVolume, isMuted, usePushToTalk, selectedInputDevice, muffleIntensity, showWaveform, showMicStatus, attenuationProfile,waveformType};
        optionFilePath = FabricLoader.getInstance().getConfigDir().resolve("proxvc_client.properties");
        OptionStore.loadOptions(optionFilePath, options, keyBindings);
        OptionStore.saveOptions(optionFilePath, options, keyBindings);
        try {
            socket = new DatagramSocket();
            device = new AudioInputDevice();
            serverAddress = null;
            inputThread = new Thread(new VCInputClient(this));
            outputThread = new Thread(new VCOutputClient(this));
            inputThread.start();
            outputThread.start();

            OptionsCategory generalCategory = new OptionsCategory("gui.options.page.proxvc.category.general", Items.RECORD_CAT.getDefaultStack())
                    .withComponent(new FloatOptionComponent(voiceChatVolume))
                    .withComponent(new ToggleableOptionComponent<>(attenuationProfile))
                    .withComponent(new BooleanOptionComponent(isMuted))
                    .withComponent(new BooleanOptionComponent(usePushToTalk));
            OptionsCategory devicesCategory = new OptionsCategory("gui.options.page.proxvc.category.devices", Items.REPEATER.getDefaultStack())
                    .withComponent(new MicrophoneListComponent(device, selectedInputDevice));
            OptionsCategory controlsCategory = new OptionsCategory("gui.options.page.proxvc.category.controls",Items.AMMO_ARROW.getDefaultStack())
                    .withComponent(new KeyBindingComponent(keyMute))
                    .withComponent(new KeyBindingComponent(keyPushToTalk))
                    .withComponent(new KeyBindingComponent(keyOpenMenu));
            OptionsCategory hudCategory = new OptionsCategory("gui.options.page.proxvc.category.hud", Blocks.GLASS.getDefaultStack())
                    .withComponent(new ToggleableOptionComponent<>(waveformType))
                    .withComponent(new BooleanOptionComponent(showWaveform))
                    .withComponent(new BooleanOptionComponent(showMicStatus));
            OptionsCategory mixerCategory = new OptionsCategory("gui.options.page.proxvc.category.mixer", Items.DUST_REDSTONE.getDefaultStack())
                    .withComponent(new VolumeMixerComponent(sources));
            mixerCategory.collapsed = true;
            OptionsCategory effectsCategory = new OptionsCategory("gui.options.page.proxvc.category.effects",Items.AMMO_FIREBALL.getDefaultStack())
                    .withComponent(new FloatOptionComponent(muffleIntensity));

            optionsPage = OptionsPages.register(new OptionsPage("gui.options.page.proxvc.title", Blocks.NOTEBLOCK.getDefaultStack()))
                    .withComponent(generalCategory)
                    .withComponent(mixerCategory)
                    .withComponent(devicesCategory)
                    .withComponent(controlsCategory)
                    .withComponent(effectsCategory)
                    .withComponent(hudCategory);
            ((HudComponentStatus) HudComponents.INSTANCE.getComponent("mic_status"))
                    .setStatusData(usePushToTalk, isMuted, showMicStatus, keyPushToTalk, device);
            ((HudComponentWaveForm) HudComponents.INSTANCE.getComponent("waveform"))
                    .setWaveformData(showWaveform, device);

            device.open(selectedInputDevice.value);
            MixerStore.load();
            descriptor = new EnvironmentDescriptor(client, sources);
            System.out.println("ProxVC successfully started.");
        } catch (SocketException ex) {
            System.out.println("Failed to start the ProxVC client because of an exception.");
            System.out.println("Continuing without ProxVC.");
            ex.printStackTrace();
        }
    }

    private void stop(Minecraft client) {
        if (optionFilePath != null)
            OptionStore.saveOptions(optionFilePath, options, keyBindings);
        try {
            if (socket != null) {
                socket.close();
            }
            if (inputThread != null) {
                inputThread.join();
            }
            if (outputThread != null) {
                outputThread.join();
            }
            if (device != null) {
                device.close();
            }
            MixerStore.save();
        } catch (InterruptedException ex) {
            System.out.println("Failed to stop the ProxVC client because of an exception.");
            ex.printStackTrace();
        }
    }

    private void tick(Minecraft client) {


        if (isDisconnected())
            return;

        Set<Integer> toRemove = new HashSet<>(sources.keySet());
        Set<Integer> toAdd = new HashSet<>();
        for (Entity entity : client.currentWorld.loadedEntityList) {
            if (entity instanceof Player && entity.id != client.thePlayer.id) {
                toRemove.remove(entity.id);
                toAdd.add(entity.id);
            }
        }
        for (int entityId : toRemove) {
            sources.get(entityId).close();
            sources.remove(entityId);
        }
        for (int entityId : toAdd) {
            if (!sources.containsKey(entityId)) {
                StreamingAudioSource source = new StreamingAudioSource();
                source.volume = MixerStore.getMixerProperty(entityId);
                source.setAttenuationProfile(attenuationProfile.value);
                sources.put(entityId, source);
            }
        }

        if (client.currentScreen == null) {
            if (keyMute.isPressed()) {
                if (!isMutePressed) {
                    isMutePressed = true;
                    isMuted.value = !isMuted.value;
                }
            } else {
                isMutePressed = false;
            }
        }
        if (keyOpenMenu.isPressed() && client.currentScreen == null) {
            client.displayScreen(new ScreenOptions(null, optionsPage));
        }

        for (Entity entity : client.currentWorld.loadedEntityList) {
            StreamingAudioSource source = sources.get(entity.id);
            if (source == null) {
                continue;
            }
            source.efx.calculateMuffleIntensity(client, (Player) entity, muffleIntensity.value);

            Vec3 headPos = ((Player) entity).getPosition(client.timer.partialTicks, true);
            Vec3 look = entity.getLookAngle();
            AL10.alSource3f(source.source, AL10.AL_POSITION, (float) entity.x, (float) entity.y, (float) entity.z);
            AL10.alSource3f(source.source, AL10.AL_DIRECTION, (float) look.x, (float) look.y, (float) look.z);
            AL10.alSource3f(source.source, AL10.AL_VELOCITY, (float) entity.xd, (float) entity.yd, (float) entity.zd);
            AL10.alSourcef(source.source, AL10.AL_GAIN, voiceChatVolume.value * source.volume);
            if (attenuationProfileChanged) {
                source.setAttenuationProfile(attenuationProfile.value);
            }
        }
        if (attenuationProfileChanged) {
            attenuationProfileChanged = false;
        }
    }

    private void render(Minecraft client, WorldRenderer renderer) {


    }

    private void login(Minecraft client, PacketLogin packet) {
        Socket socket = (Socket) client.getSendQueue().netManager.socket;
        serverAddress = socket.getRemoteSocketAddress();
    }

    private void disconnect(Minecraft client) {
        serverAddress = null;
        for (StreamingAudioSource source : sources.values()) {
            source.close();
        }
        sources.clear();
    }
}
