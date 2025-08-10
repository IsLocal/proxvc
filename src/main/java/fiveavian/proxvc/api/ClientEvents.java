package fiveavian.proxvc.api;

import fiveavian.proxvc.ProxVCClient;
import fiveavian.proxvc.vc.StreamingAudioSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.core.net.packet.PacketLogin;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientEvents {
    public static final List<Consumer<Minecraft>> START = new ArrayList<>();
    public static final List<Consumer<Minecraft>> STOP = new ArrayList<>();
    public static final List<Consumer<Minecraft>> TICK = new ArrayList<>();
    public static final List<BiConsumer<Minecraft, WorldRenderer>> RENDER = new ArrayList<>();

    public static final List<BiConsumer<Minecraft, PacketLogin>> LOGIN = new ArrayList<>();
    public static final List<Consumer<Minecraft>> DISCONNECT = new ArrayList<>();
    
    public static final List<Consumer<ProxVCClient>> CLIENT_START = new ArrayList<>();
    public static final List<Consumer<ProxVCClient>> CLIENT_STOP = new ArrayList<>();

    public static final List<BiConsumer<Integer, StreamingAudioSource>> SETUP_SOURCE_AL_CONTEXT = new ArrayList<>();

    public static final List<BiConsumer<StreamingAudioSource, ByteBuffer>> SOURCE_QUEUE_AUDIO = new ArrayList<>();

    public static final List<Consumer<StreamingAudioSource>> SOURCE_CLOSE = new ArrayList<>();
    public static final List<Consumer<ProxVCClient>> PROXVC_STOP = new ArrayList<>();


}
