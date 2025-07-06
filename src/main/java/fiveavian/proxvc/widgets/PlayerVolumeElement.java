package fiveavian.proxvc.widgets;

import fiveavian.proxvc.vc.StreamingAudioSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.SliderElement;

import java.util.Map;

public class PlayerVolumeElement extends SliderElement {
    public final StreamingAudioSource source;
    public final Map<Integer, StreamingAudioSource> sources;


    public PlayerVolumeElement(StreamingAudioSource source, int x, int y, Map<Integer, StreamingAudioSource> sources) {

        super(source.entityId, x, y, 200, 20, "Player " + source.playerName + " Volume: ", source.volume/2.5f);
        this.source = source;
        this.sources = sources;
    }

    @Override
    public void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        super.mouseDragged(mc, mouseX, mouseY);
        float volume = (float) this.sliderValue * 2.5f;

        this.displayString = "Player " + source.playerName + " Volume: " + (int) ((volume/2.5) * 250) + "%";

        if (!sources.containsKey(source.entityId)) {
            // player disconnected while the volume mixer was open
            this.visible = false;

            return;
        }
        source.volume = volume;

    }
}
