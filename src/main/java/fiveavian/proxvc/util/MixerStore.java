package fiveavian.proxvc.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.entity.player.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class MixerStore {
    private static boolean loaded = false;
    public static Path configPath = FabricLoader.getInstance().getConfigDir().resolve("proxvc_mixer.properties");
    public static Map<Integer, Float> mixerData = new HashMap<>();

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void load() {
        try {
            if (!Files.exists(configPath)) {
                return;
            }
            if (loaded) {
                return;
            }
            Properties properties = new Properties();
            properties.load(Files.newInputStream(configPath));

            for (String key : properties.stringPropertyNames()) {
                try {

                    int u = Integer.parseInt(key);
                    float volume = Float.parseFloat(properties.getProperty(key));
                    mixerData.put(u, volume);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid entry in mixer properties: " + key + " = " + properties.getProperty(key));
                }
            }
            loaded = true;
        } catch (IOException ex) {
            System.out.println("Failed to load options.");
            ex.printStackTrace();
        }
    }

    public static UUID getUUID(int entityId) {
        for (Player player : mc.currentWorld.players) {
            if (player.id == entityId) {
                return player.uuid;
            }
        }
        return null;
    }

    public static float getMixerProperty(int entityId) {
        UUID uuid = getUUID(entityId);
        if (uuid == null) {
            return 1.0f;
        }

        if (mixerData.containsKey(uuid.hashCode())) {
            return mixerData.get(uuid.hashCode());
        }
        return 1.0f;
    }

    public static void setMixerProperty(int entityId, float volume) {
        UUID uuid = getUUID(entityId);
        if (uuid == null) {
            return;
        }

        if (volume == 1.0f) {
            mixerData.remove(uuid.hashCode());
            return;
        }
        mixerData.put(uuid.hashCode(), volume);
    }

    public static void save() {
        try {
            if (!Files.exists(configPath)) {
                Files.createFile(configPath);
            }

            Properties properties = new Properties();

            for (Map.Entry<Integer, Float> entry : mixerData.entrySet()) {
                properties.setProperty(entry.getKey().toString(), entry.getValue().toString());
            }

            properties.store(Files.newOutputStream(configPath), null);
        } catch (IOException ex) {
            System.out.println("Failed to save options.");
            ex.printStackTrace();
        }
    }
}


