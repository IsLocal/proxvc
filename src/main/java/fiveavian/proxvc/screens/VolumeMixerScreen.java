package fiveavian.proxvc.screens;

import fiveavian.proxvc.vc.StreamingAudioSource;
import fiveavian.proxvc.widgets.PlayerVolumeElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ListLayout;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.gui.SliderElement;

import java.util.Map;

public class VolumeMixerScreen extends Screen {

    public final Map<Integer, StreamingAudioSource> sources;
    public final ListLayout layout;

    public VolumeMixerScreen(Map<Integer, StreamingAudioSource> sources) {
        super(null);
        this.sources = sources;
        this.layout = new ListLayout(this);
    }

    public void init() {
        //grid
        int x = width / 2 - 100;
        int y = height / 2 - (sources.size() * 24) / 2;

        int i = 0;

        for (StreamingAudioSource source : sources.values()) {
            if (source.lastHeard < System.currentTimeMillis() - 10000) {
                continue; // skip sources that haven't been heard from in the last 10 seconds
            }
            SliderElement mixer = new PlayerVolumeElement(source, x, y + i * 24, sources);

            this.add(mixer);
            i++;
        }


        super.init();
    }


    @Override
    public void render(int mx, int my, float partialTick) {
        if (this.sources.isEmpty() || this.buttons.isEmpty()) {
            this.drawStringCentered(this.font, "No players in range.", width / 2, height / 2 - 20, 0xFFFFFF);

        }
        super.render(mx, my, partialTick);
    }

    @Override
    public void opened(Minecraft mc, int width, int height) {

        super.opened(mc, width, height);

    }

    @Override
    public void keyPressed(char eventCharacter, int eventKey, int mx, int my) {

        super.keyPressed(eventCharacter, eventKey, mx, my);
    }
}
