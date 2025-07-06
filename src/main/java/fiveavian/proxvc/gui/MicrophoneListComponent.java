package fiveavian.proxvc.gui;

import fiveavian.proxvc.vc.AudioInputDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.options.components.OptionsComponent;
import net.minecraft.client.option.OptionString;
import net.minecraft.client.render.Font;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.lang.I18n;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

public class MicrophoneListComponent implements OptionsComponent {
    private static final int MARGIN = 3;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_HEIGHT_WITH_MARGIN = BUTTON_HEIGHT + MARGIN;

    private final AudioInputDevice device;
    private final OptionString specifierOption;
    private Minecraft mc;
    private String[] specifiers;
    private int updateTickCount = 0;

    public MicrophoneListComponent(AudioInputDevice device, OptionString specifierOption) {
        this.device = device;
        this.specifierOption = specifierOption;
    }

    @Override
    public void init(Minecraft mc) {
        this.mc = mc;
        updateSpecifiers();
    }

    @Override
    public void tick() {
        updateTickCount += 1;
        if (updateTickCount >= 40) {
            updateTickCount = 0;
            updateSpecifiers();
        }
    }

    private void updateSpecifiers() {
        specifiers = AudioInputDevice.getSpecifiers();
        if (specifierOption.value == null)
            return;
        for (String specifier : specifiers) {
            if (Objects.equals(specifier, specifierOption.value))
                return;
        }
        // specifier not found
        selectSpecifier(null);
    }

    private void selectSpecifier(String specifier) {
        specifierOption.value = specifier;
        device.open(specifier);
    }

    @Override
    public int getHeight() {
        if (specifiers.length == 0) {
            return 20;
        } else {
            return MARGIN + specifiers.length * BUTTON_HEIGHT_WITH_MARGIN;
        }
    }

    @Override
    public void render(int x, int y, int width, int relativeMouseX, int relativeMouseY) {
        Tessellator tessellator = Tessellator.instance;
        Font fontRenderer = mc.font;
        I18n i18n = I18n.getInstance();
        if (specifiers.length == 0) {
            fontRenderer.drawCenteredString(i18n.translateKey("gui.options.page.proxvc.label.no_devices"), x + width / 2, y + 4, 0x5F7F7F7F);
            return;
        }
        y += MARGIN;
        for (String specifier : specifiers) {
            if (Objects.equals(specifier, specifierOption.value)) {
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                tessellator.startDrawingQuads();
                // Border
                tessellator.setColorOpaque_I(0x808080);
                tessellator.addVertexWithUV(x - 2, y + BUTTON_HEIGHT + 2, 0.0, 0.0, 1.0);
                tessellator.addVertexWithUV(x + width + 2, y + BUTTON_HEIGHT + 2, 0.0, 1.0, 1.0);
                tessellator.addVertexWithUV(x + width + 2, y - 2, 0.0, 1.0, 1.0);
                tessellator.addVertexWithUV(x - 2, y - 2, 0.0, 0.0, 0.0);
                // Fill
                tessellator.setColorOpaque_I(0x000000);
                tessellator.addVertexWithUV(x - 1, y + BUTTON_HEIGHT + 1, 0.0, 0.0, 1.0);
                tessellator.addVertexWithUV(x + width + 1, y + BUTTON_HEIGHT + 1, 0.0, 1.0, 1.0);
                tessellator.addVertexWithUV(x + width + 1, y - 1, 0.0, 1.0, 1.0);
                tessellator.addVertexWithUV(x - 1, y - 1, 0.0, 0.0, 0.0);
                tessellator.draw();
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
            fontRenderer.drawString(specifier.replace("OpenAL Soft on", ""), x + 1, y + 4, 0xFFFFFF);
            y += BUTTON_HEIGHT_WITH_MARGIN;
        }
    }

    @Override
    public void onMouseClick(int mouseButton, int x, int y, int width, int relativeMouseX, int relativeMouseY) {
        if (relativeMouseX < 0 || relativeMouseX >= width)
            return;
        y = MARGIN;
        for (String specifier : specifiers) {
            if (relativeMouseY >= y && relativeMouseY < y + BUTTON_HEIGHT) {
                if (Objects.equals(specifier, specifierOption.value)) {
                    selectSpecifier(null);
                } else {
                    selectSpecifier(specifier);
                }
                break;
            }
            y += BUTTON_HEIGHT_WITH_MARGIN;
        }
    }

    @Override
    public void onMouseMove(int x, int y, int width, int relativeMouseX, int relativeMouseY) {
    }

    @Override
    public void onMouseRelease(int mouseButton, int x, int y, int width, int relativeMouseX, int relativeMouseY) {
    }

    @Override
    public void onKeyPress(int keyCode, char character) {
    }

    @Override
    public boolean matchesSearchTerm(String string) {
        return false;
    }
}
