package fiveavian.proxvc.gui;

import fiveavian.proxvc.vc.AudioInputDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.hud.HudIngame;
import net.minecraft.client.gui.hud.component.HudComponentMovable;
import net.minecraft.client.gui.hud.component.layout.Layout;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.OptionBoolean;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.Texture;
import org.lwjgl.opengl.GL11;

public class HudComponentStatus extends HudComponentMovable {
    Minecraft mc = Minecraft.getMinecraft();
    private Texture statusIconTexture;
    private OptionBoolean usePushToTalk;
    private OptionBoolean isMuted;
    private KeyBinding keyPushToTalk;
    private AudioInputDevice device;
    private OptionBoolean showMicStatus;


    public HudComponentStatus(String key, Layout layout) {
        super(key, 24, 24, layout);
    }


    @Override
    public boolean isVisible(Minecraft minecraft) {
        return minecraft.gameSettings.immersiveMode.drawOverlays() && showMicStatus.value;
    }

    public void setStatusData(OptionBoolean usePushToTalk, OptionBoolean isMuted, OptionBoolean showMicStatus, KeyBinding keyPushToTalk, AudioInputDevice device) {
        statusIconTexture = mc.textureManager.loadTexture("/gui/proxvc.png");
        this.usePushToTalk = usePushToTalk;
        this.isMuted = isMuted;
        this.keyPushToTalk = keyPushToTalk;
        this.showMicStatus = showMicStatus;
        this.device = device;
    }

    @Override
    public void render(Minecraft mc, HudIngame hud, int xSizeScreen, int ySizeScreen, float partialTick) {
        int x = this.getLayout().getComponentX(mc, this, xSizeScreen);
        int y = this.getLayout().getComponentY(mc, this, ySizeScreen);

        statusIconTexture.bind();
        GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
        double u = 0.0;
        if (isMuted.value) {
            u = 0.2;
        } else if (device.isClosed()) {
            u = 0.4;
        } else if (usePushToTalk.value && !keyPushToTalk.isPressed()) {
            u = 0.6;
        } else if (device.isClosed()) {
            u = 0.2;
        } else if (device.isTalking()) {
            u = 0.8;
        }
        Tessellator.instance.startDrawingQuads();
        Tessellator.instance.setColorRGBA_F(1f, 1f, 1f, 0.5f);
        Tessellator.instance.drawRectangleWithUV(x, y, 24, 24, u, 0.0, 0.20, 1.0);
        Tessellator.instance.draw();
    }

    @Override
    public void renderPreview(Minecraft mc, Gui gui, Layout layout, int xSizeScreen, int ySizeScreen) {
        int x = this.getLayout().getComponentX(mc, this, xSizeScreen);
        int y = this.getLayout().getComponentY(mc, this, ySizeScreen);

        statusIconTexture.bind();
        GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
        double u = 0.0;

        Tessellator.instance.startDrawingQuads();
        Tessellator.instance.setColorRGBA_F(1f, 1f, 1f, 0.5f);
        Tessellator.instance.drawRectangleWithUV(x, y, 24, 24, u, 0.0, 0.20, 1.0);
        Tessellator.instance.draw();
    }
}
