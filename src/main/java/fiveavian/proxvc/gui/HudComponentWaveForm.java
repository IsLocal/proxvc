package fiveavian.proxvc.gui;

import fiveavian.proxvc.vc.AudioInputDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.hud.HudIngame;
import net.minecraft.client.gui.hud.component.HudComponentMovable;
import net.minecraft.client.gui.hud.component.layout.Layout;
import net.minecraft.client.option.OptionBoolean;
import net.minecraft.client.option.enums.TooltipStyle;
import net.minecraft.client.render.tessellator.Tessellator;
import org.lwjgl.opengl.GL11;

public class HudComponentWaveForm extends HudComponentMovable {
    private OptionBoolean showWaveform;
    private AudioInputDevice device;
    private Minecraft mc;

    private int[] blankPoints = new int[20];
    public HudComponentWaveForm(String key, Layout layout) {
        super(key, 100, 15, layout);

    }

    public void setWaveformData(OptionBoolean showWaveform, AudioInputDevice device) {
        this.showWaveform = showWaveform;
        this.device = device;
    }

    @Override
    public boolean isVisible(Minecraft minecraft) {
        return minecraft.gameSettings.immersiveMode.drawOverlays() && showWaveform.value;
    }
    @Override
    public void render(Minecraft mc, HudIngame hud, int xSizeScreen, int ySizeScreen, float partialTick) {
        if (this.mc == null) {
            this.mc = mc;
        }
        int x = this.getLayout().getComponentX(mc, this, xSizeScreen);
        int y = this.getLayout().getComponentY(mc, this, ySizeScreen);
        //hud.drawRect(x, y, x + 100, y + 15, 0x80000000);

        drawBackground(x, y, x + 100, y + 15);

        renderWaveform(device.points != null ? device.points : blankPoints, x+2,y, 96, 15);
    }

    @Override
    public void renderPreview(Minecraft mc, Gui gui, Layout layout, int xSizeScreen, int ySizeScreen) {
        int x = this.getLayout().getComponentX(mc, this, xSizeScreen);
        int y = this.getLayout().getComponentY(mc, this, ySizeScreen);

        gui.drawRect(x, y, x + 100, y + 15, 0x80000000);

        //drawBackground(x, y, x + 90, y + 15);
        renderWaveform(blankPoints, x+2,y, 96, 15);
    }

    public void renderWaveform(int[] points, int x, int y, int width, int height) {
        int n = points.length;
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < n; i++) {
            float px = x + (i * width) / (n - 1);
            float norm = Math.abs(points[i]) / 32768.0f; // 0 (low) to 1 (high)
            float r = norm;
            float g = 1.0f - norm;
            GL11.glColor3f(r, g, 0.0f);
            float py = y + height / 2 - (points[i] / 32768.0f) * ((float) height / 2);
            GL11.glVertex2f(px, py);
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);

    }

    private int[] drawBackground(int minX, int minY, int maxX, int maxY) {
        if (this.mc.gameSettings.tooltipStyle.value == TooltipStyle.MODERN) {
            return this.drawBackgroundModern(minX, minY, maxX, maxY);
        } else {
            int bottomLeftCornerY = maxY - 7;
            int topRightCornerX = maxX - 7;

            int bottomRightCornerX = maxX - 7;
            int bottomRightCornerY = maxY - 7;
            int horWidth = maxX - minX - 14;
            int vertHeight = maxY - minY - 14;
            int horWidth2 = maxX - minX - 6;
            int vertHeight2 = maxY - minY - 6;
            Tessellator tl = Tessellator.instance;
            this.mc.textureManager.bindTexture(this.mc.textureManager.loadTexture(((TooltipStyle)TooltipStyle.CRT).getFilePath()));
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            tl.startDrawingQuads();
            tl.drawRectangleWithUV(minX, minY, 7, 7, (double)0.0F, (double)0.0F, (double)0.21875F, (double)0.21875F);
            tl.drawRectangleWithUV(minX, bottomLeftCornerY, 7, 7, (double)0.0F, (double)0.21875F, (double)0.21875F, (double)0.21875F);
            tl.drawRectangleWithUV(topRightCornerX, minY, 7, 7, (double)0.21875F, (double)0.0F, (double)0.21875F, (double)0.21875F);
            tl.drawRectangleWithUV(bottomRightCornerX, bottomRightCornerY, 7, 7, (double)0.21875F, (double)0.21875F, (double)0.21875F, (double)0.21875F);

            for(int x = minX + 7; x < minX + 7 + horWidth / 11 * 11; x += 11) {
                tl.drawRectangleWithUV(x, minY, 11, 3, (double)0.4375F, (double)0.0F, (double)0.34375F, (double)0.09375F);
            }

            int finalWidth = horWidth - horWidth / 11 * 11;
            tl.drawRectangleWithUV(topRightCornerX - finalWidth, minY, finalWidth, 3, (double)0.4375F, (double)0.0F, (double)((float)finalWidth / 32.0F), (double)0.09375F);

            for(int x = minX + 7; x < minX + 7 + horWidth / 11 * 11; x += 11) {
                tl.drawRectangleWithUV(x, maxY - 3, 11, 3, (double)0.4375F, (double)0.34375F, (double)0.34375F, (double)0.09375F);
            }

            tl.drawRectangleWithUV(bottomRightCornerX - finalWidth, maxY - 3, finalWidth, 3, (double)0.4375F, (double)0.34375F, (double)((float)finalWidth / 32.0F), (double)0.09375F);

            for(int y = minY + 7; y < minY + 7 + vertHeight / 11 * 11; y += 11) {
                tl.drawRectangleWithUV(minX, y, 3, 11, (double)0.0F, (double)0.4375F, (double)0.09375F, (double)0.34375F);
            }

            int finalHeight = vertHeight - vertHeight / 11 * 11;
            tl.drawRectangleWithUV(minX, bottomLeftCornerY - finalHeight, 3, finalHeight, (double)0.0F, (double)0.4375F, (double)0.09375F, (double)((float)finalHeight / 32.0F));

            for(int y = minY + 7; y < minY + 7 + vertHeight / 11 * 11; y += 11) {
                tl.drawRectangleWithUV(maxX - 3, y, 3, 11, (double)0.34375F, (double)0.4375F, (double)0.09375F, (double)0.34375F);
            }

            tl.drawRectangleWithUV(maxX - 3, bottomRightCornerY - finalHeight, 3, finalHeight, (double)0.34375F, (double)0.4375F, (double)0.09375F, (double)((float)finalHeight / 32.0F));

            for(int x = minX + 3; x < minX + 3 + horWidth2 / 8 * 8; x += 8) {
                for(int y = minY + 3; y < minY + 3 + vertHeight2 / 8 * 8; y += 8) {
                    tl.drawRectangleWithUV(x, y, 8, 8, (double)0.4375F, (double)0.4375F, (double)0.25F, (double)0.25F);
                }
            }

            int finalHeight2 = vertHeight2 - vertHeight2 / 8 * 8;
            int finalWidth2 = horWidth2 - horWidth2 / 8 * 8;

            for(int x = minX + 3; x < minX + 3 + horWidth2 / 8 * 8; x += 8) {
                tl.drawRectangleWithUV(x, maxY - 3 - finalHeight2, 8, finalHeight2, (double)0.4375F, (double)0.4375F, (double)0.25F, (double)((float)finalHeight2 / 32.0F));
            }

            for(int y = minY + 3; y < minY + 3 + vertHeight2 / 8 * 8; y += 8) {
                tl.drawRectangleWithUV(maxX - 3 - finalWidth2, y, finalWidth2, 8, (double)0.4375F, (double)0.4375F, (double)((float)finalWidth2 / 32.0F), (double)0.25F);
            }

            tl.drawRectangleWithUV(maxX - 3 - finalWidth2, maxY - 3 - finalHeight2, finalWidth2, finalHeight2, (double)0.4375F, (double)0.4375F, (double)((float)finalWidth / 32.0F), (double)((float)finalWidth2 / 32.0F));
            tl.draw();
            GL11.glDisable(3042);
            return new int[]{minX, minY};
        }
    }
    private int[] drawBackgroundModern(int minX, int minY, int maxX, int maxY) {
        int x = minX + 4;
        int y = minY + 4;
        int width = maxX - minX - 8;
        int height = maxY - minY - 8;
        int bgColor = -267386864;
        int lineColorTop = 1347420415;
        int lineColorBottom = (lineColorTop & 16711422) >> 1 | lineColorTop & -16777216;
        this.drawGradientRect(x - 3, y - 4, x + width + 3, y - 3, bgColor, bgColor);
        this.drawGradientRect(x - 3, y + height + 3, x + width + 3, y + height + 4, bgColor, bgColor);
        this.drawGradientRect(x - 3, y - 3, x + width + 3, y + height + 3, bgColor, bgColor);
        this.drawGradientRect(x - 4, y - 3, x - 3, y + height + 3, bgColor, bgColor);
        this.drawGradientRect(x + width + 3, y - 3, x + width + 4, y + height + 3, bgColor, bgColor);
        this.drawGradientRect(x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, lineColorTop, lineColorBottom);
        this.drawGradientRect(x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, lineColorTop, lineColorBottom);
        this.drawGradientRect(x - 3, y - 3, x + width + 3, y - 3 + 1, lineColorTop, lineColorTop);
        this.drawGradientRect(x - 3, y + height + 2, x + width + 3, y + height + 3, lineColorBottom, lineColorBottom);
        return new int[]{x - 3, y - 4};
    }
    public void drawGradientRect(int minX, int minY, int maxX, int maxY, int argb1, int argb2) {
        float a1 = (float)(argb1 >> 24 & 255) / 255.0F;
        float r1 = (float)(argb1 >> 16 & 255) / 255.0F;
        float g1 = (float)(argb1 >> 8 & 255) / 255.0F;
        float b1 = (float)(argb1 & 255) / 255.0F;
        float a2 = (float)(argb2 >> 24 & 255) / 255.0F;
        float r2 = (float)(argb2 >> 16 & 255) / 255.0F;
        float g2 = (float)(argb2 >> 8 & 255) / 255.0F;
        float b2 = (float)(argb2 & 255) / 255.0F;
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glDisable(3008);
        GL11.glBlendFunc(770, 771);
        GL11.glShadeModel(7425);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(r1, g1, b1, a1);
        tessellator.addVertex((double)maxX, (double)minY, (double)0.0F);
        tessellator.addVertex((double)minX, (double)minY, (double)0.0F);
        tessellator.setColorRGBA_F(r2, g2, b2, a2);
        tessellator.addVertex((double)minX, (double)maxY, (double)0.0F);
        tessellator.addVertex((double)maxX, (double)maxY, (double)0.0F);
        tessellator.draw();
        GL11.glShadeModel(7424);
        GL11.glDisable(3042);
        GL11.glEnable(3008);
        GL11.glEnable(3553);
    }
}
