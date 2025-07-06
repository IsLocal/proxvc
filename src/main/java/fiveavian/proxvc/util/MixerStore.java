package fiveavian.proxvc.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MixerStore {

    public Path configPath = FabricLoader.getInstance().getConfigDir().resolve("proxvc_mixer.properties");
    public Map<Integer, Float> mixerData = new HashMap<>();


    public void load() {
        try {
            if (!Files.exists(configPath)) {
                return;
            }
            Properties properties = new Properties();
            properties.load(Files.newInputStream(configPath));

            for (String key : properties.stringPropertyNames()) {
                try {
                    int entityId = Integer.parseInt(key);
                    float volume = Float.parseFloat(properties.getProperty(key));
                    mixerData.put(entityId, volume);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid entry in mixer properties: " + key + " = " + properties.getProperty(key));
                }
            }
        } catch (IOException ex) {
            System.out.println("Failed to load options.");
            ex.printStackTrace();
        }
    }

    public float getMixerProperty(int entityId) {
        if (mixerData.containsKey(entityId)) {
            return mixerData.get(entityId);
        }
        return 1.0f; // default volume
    }

    public void setMixerProperty(int entityId, float volume) {
        if (volume == 1.0f) {
            mixerData.remove(entityId);
            return;
        }
        mixerData.put(entityId, volume);
    }

    public void save() {
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



