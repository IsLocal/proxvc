package fiveavian.proxvc.gui;

import fiveavian.proxvc.util.MixerStore;
import fiveavian.proxvc.vc.StreamingAudioSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.options.components.OptionsComponent;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.sound.SoundCategory;
import net.minecraft.core.util.helper.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Alternatively could be done with modified OptionsCategory and or FloatOptionComponents
 * but found this to be more flexible and easier to implement.
 */

public class VolumeMixerComponent implements OptionsComponent, Listener<ButtonElement> {
    private final float AMP_FACTOR = 10.0F; // Factor to convert slider value to volume
    private static final int BUTTON_HEIGHT = 20;

    private final Minecraft mc = Minecraft.getMinecraft();


    private final Map<Integer, StreamingAudioSource> sources;
    private final ListLayout slidersLayout;

    private final Map<Integer, SliderElement> entityIdToSlider = new HashMap<>();
    private final Map<Integer, String> entityIdToName = new HashMap<>();
    private final Map<SliderElement, ButtonElement> sliderToResetButton = new HashMap<>();

    private int absoluteMouseX = 0;
    private int absoluteMouseY = 0;

    private  int relativeMouseX = 0;
    private  int relativeMouseY = 0;

    private  boolean renderTooltip = false;
    private final TooltipElement tooltipElement = new TooltipElement(mc);

    public VolumeMixerComponent(Map<Integer, StreamingAudioSource> sources) {
        this.sources = sources;


        slidersLayout = (new ListLayout(mc.currentScreen))
                .setAlign(0, 0)
                .setVertical(true)
                .setElementSize(150, BUTTON_HEIGHT)
                .setMargin(1);

        MixerStore.load();

    }

    @Override
    public int getHeight() {
        if (renderTooltip) {
            return 85 + relativeMouseY;
        }

        if (sources.isEmpty()) {
            return 10; // If no sources, just return the height of the button
        }
        //calculate the height based on the number of sliders
        return slidersLayout.elements.size() * BUTTON_HEIGHT + (slidersLayout.elements.size() - 1) * slidersLayout.margin; // 15 for the header text
    }

    @Override
    public void render(int x, int y, int width, int relativeMouseX, int relativeMouseY) {
        absoluteMouseX = x + relativeMouseX;
        absoluteMouseY = y + relativeMouseY;
        this.relativeMouseX = relativeMouseX;
        this.relativeMouseY = relativeMouseY;

        if (sources.isEmpty() && mc.currentWorld != null && relativeMouseY > 0 && relativeMouseY < 10 && relativeMouseX > 0 ) {
            tooltipElement.zLevel = 2F;

            tooltipElement.render(
                    "§cIf you think this shouldn't be \n§chappening check the following\n\n" +
                    "• There are players with the mod in render distance.\n" +
                    "• The ProxVC mod is installed on the server.\n"
                    + "\n§7If you are still having issues, \n§7please report it on the ProxVC GitHub page.",
                    absoluteMouseX, absoluteMouseY, 8,8);
            this.renderTooltip = true;
        } else this.renderTooltip = false;

        if (mc.currentWorld == null) {
            mc.currentScreen.drawString(mc.font, "Load into a server first!", x + 5, y, 0xFFFFFF);
            return; // If the current screen is null, do not render
        }
        if (sources.isEmpty()) {//gui\sprites\screen\creative\clear.png
            mc.textureManager.loadTexture("/assets/minecraft/textures/gui/sprites/screen/creative/clear.png").bind();
            Tessellator.instance.startDrawingQuads();
            Tessellator.instance.setColorRGBA_F(1f, 1f, 1f, 0.5f);
            Tessellator.instance.drawRectangleWithUV(
                    x - 10, y,
                    (int) 9.6,9,
                    1, 0, 1, 1);
            Tessellator.instance.draw();

            mc.currentScreen.drawString(mc.font, (renderTooltip ? TextFormatting.YELLOW.toString() : "") + "No audio sources found.", x + 5 , y, 0xFFFFFF);
            return;
        }

        slidersLayout.setOffset(x, y);
        slidersLayout.updateElementPositions();

        for (GuiElement element : slidersLayout.elements) {
            SliderElement slider = (SliderElement) element;
            slider.drawButton(mc, x + relativeMouseX, y + relativeMouseY);
            ButtonElement resetButton = sliderToResetButton.get(slider);
            resetButton.xPosition = x + slider.width + 3; // Position reset button to the right of the slider
            resetButton.yPosition = slider.yPosition;

            resetButton.drawButton(mc, absoluteMouseX, absoluteMouseY);
        }
    }

    @Override
    public void onMouseClick(int mouseButton, int x, int y, int width, int relativeMouseX, int relativeMouseY) {
        // Use rendered mouse coordinates because the one that this gives is stupid
        for (GuiElement element : slidersLayout.elements) {
            SliderElement slider = (SliderElement) element;
            if (slider.mouseClicked(mc, absoluteMouseX, absoluteMouseY)) {
                this.listen(slider);
                mc.sndManager.playSound("random.click", SoundCategory.GUI_SOUNDS, 1.0F, 1.0F);
            }

            ButtonElement resetButton = sliderToResetButton.get(slider);
            if (resetButton.mouseClicked(mc, absoluteMouseX, absoluteMouseY)) {
                slider.sliderValue = 1 / AMP_FACTOR; // Reset to default value
                mc.sndManager.playSound("random.click", SoundCategory.GUI_SOUNDS, 1.0F, 1.0F);
                this.listen(slider); // Update the source volume
            }
        }
    }

    @Override
    public void onMouseMove(int x, int y, int width, int relativeMouseX, int relativeMouseY) {
        for (GuiElement element : slidersLayout.elements) {
            SliderElement slider = (SliderElement) element;
            slider.mouseDragged(mc, absoluteMouseX, absoluteMouseY);
            if (slider.dragging) {
                this.listen(slider);
            }
        }

    }

    @Override
    public void onMouseRelease(int i, int j, int k, int l, int m, int n) {
        for (GuiElement element : slidersLayout.elements) {
            SliderElement slider = (SliderElement) element;
            slider.mouseReleased(absoluteMouseX, absoluteMouseY);
        }
    }

    @Override
    public void tick() {
        updateSources();
    }

    @Override
    public boolean matchesSearchTerm(String string) {
        return false;
    }

    @Override
    public void init(Minecraft mc) {
        updateSources();
    }


    void updateSources() {
        for (GuiElement element : new ArrayList<>(slidersLayout.elements)) {
            SliderElement slider = (SliderElement) element;

            if (sources.containsKey(slider.id))
                continue;

            slidersLayout.elements.remove(element);
            entityIdToSlider.remove(slider.id);
            entityIdToName.remove(slider.id);
            sliderToResetButton.remove(slider);
        }

        for (Map.Entry<Integer, StreamingAudioSource> entry : new HashMap<>(sources).entrySet()) {

            int entityId = entry.getKey();
            StreamingAudioSource source = entry.getValue();

            if (entityIdToSlider.containsKey(entityId))
                continue;
            String entityName = null;

            for (Entity player : mc.currentWorld.loadedEntityList) {
                if (player.id == entityId) {
                    entityName = ((Player)player).username;
                    break;
                }
            }
            assert entityName != null;
            SliderElement slider = new SliderElement(entityId,
                    0, 0, 150, BUTTON_HEIGHT,
                    "",
                    (source.volume / AMP_FACTOR));
            updateSliderDisplayString(slider, entityName);
            slider.enabled = true;
            ButtonElement resetButton = (new ButtonElement(0, 0, 0, 20, 20, ""))
                    .setTextures("minecraft:gui/misc/icon_reset",
                            "minecraft:gui/misc/icon_reset_highlighted",
                            "minecraft:gui/misc/icon_reset");

            entityIdToSlider.put(entityId, slider);
            slidersLayout.addElement(slider);
            entityIdToName.put(entityId, entityName);
            sliderToResetButton.put(slider, resetButton);
        }
    }


    @Override
    public void listen(ButtonElement object) {
        SliderElement slider = (SliderElement) object;
        StreamingAudioSource source = sources.get(slider.id);
        if (source != null) {
            String entityName = entityIdToName.get(slider.id);
            updateSliderDisplayString(slider, entityName);
            source.volume = (float) (slider.sliderValue * AMP_FACTOR); // Assuming the slider value is between 0 and 1
            MixerStore.setMixerProperty(slider.id, (float) (slider.sliderValue * AMP_FACTOR));
        } else {

            slidersLayout.elements.remove(object);
            entityIdToSlider.remove(slider.id);
            entityIdToName.remove(slider.id);
            sliderToResetButton.remove(slider);
        }
    }

    public void updateSliderDisplayString(SliderElement slider, String entityName) {
        slider.displayString = "Volume: " + (entityName.length() > 8 ? entityName.substring(0, 7) + "..." : entityName) + " (" + (int) (slider.sliderValue * AMP_FACTOR * 100) + "%)";
    }

    @Override
    public void onClose() {}
    @Override
    public void onKeyPress(int i, char c) {}
}
