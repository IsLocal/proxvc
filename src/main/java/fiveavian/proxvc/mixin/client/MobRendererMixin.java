package fiveavian.proxvc.mixin.client;

import fiveavian.proxvc.ProxVCClient;
import fiveavian.proxvc.vc.AudioInputDevice;
import fiveavian.proxvc.vc.StreamingAudioSource;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.Font;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(value = MobRenderer.class, remap = false)
public class MobRendererMixin<T extends Mob> {
    @Unique
    private static ProxVCClient proxVC;


    @Inject(method = "renderLivingLabel", at = @At("HEAD"))
    public void renderLivingLabel(Tessellator tessellator, T entity, String s, double d, double d1, double d2, int maxDistance, boolean depthTest, CallbackInfo ci) {
        if (!(entity instanceof Player)) return;

        float f = (float) ((EntityRenderer<?>) (Object) this).renderDispatcher.camera.distanceTo(entity);
        if (f > (float) maxDistance) return;

        if (proxVC == null) {
            proxVC = (ProxVCClient) FabricLoader.getInstance()
                    .getEntrypoints("client", ClientModInitializer.class)
                    .stream()
                    .filter(e -> e instanceof ProxVCClient)
                    .findFirst()
                    .get();
        }

        StreamingAudioSource source = proxVC.sources.get(entity.id);
        if (source == null || source.lastSamples == null) return;

        // Use the same transformations as the name
        GL11.glPushMatrix();
        GL11.glTranslatef((float) d + 0.0F, (float) d1 + entity.getHeadHeight() + 0.8F, (float) d2);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-((EntityRenderer) (Object) this).renderDispatcher.viewLerpYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(((EntityRenderer) (Object) this).renderDispatcher.viewLerpPitch, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-0.026666671F, -0.026666671F, 0.026666671F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);

        // Draw twice - once for depth testing, once without
        // First pass - with depth testing for occluded parts
        if (!depthTest) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float width = 50;
        float height = 8.0f;
        float xOffset = -width / 2;
        float yOffset = -12;

        // Background with depth testing (semi-transparent)
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
        tessellator.addVertex(xOffset - 1, yOffset - 1, 0.0);
        tessellator.addVertex(xOffset - 1, yOffset + height + 1, 0.0);
        tessellator.addVertex(xOffset + width + 1, yOffset + height + 1, 0.0);
        tessellator.addVertex(xOffset + width + 1, yOffset - 1, 0.0);
        tessellator.draw();

        // Waveform with depth testing (dimmed)
        int[] audioDataArray = getWaveformPoints(source.lastSamples, 20);
        renderWaveform(audioDataArray, xOffset, yOffset, width, height, 0.25f); // Dimmed version

        // Second pass - without depth testing for visible parts
        if (!depthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        // Background without depth testing
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.75F);
        tessellator.addVertex(xOffset - 1, yOffset - 1, 0.0);
        tessellator.addVertex(xOffset - 1, yOffset + height + 1, 0.0);
        tessellator.addVertex(xOffset + width + 1, yOffset + height + 1, 0.0);
        tessellator.addVertex(xOffset + width + 1, yOffset - 1, 0.0);
        tessellator.draw();

        // Waveform without depth testing (full brightness)
        renderWaveform(audioDataArray, xOffset, yOffset, width, height, 1.0f); // Full brightness version

        // Restore GL state
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (!depthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    @Unique
    public int[] getWaveformPoints(ByteBuffer samples, int numPoints) {
        return AudioInputDevice.getWaveformPoints(samples, numPoints);
    }

    @Unique
    private void renderWaveform(int[] points, float x, float y, float width, float height, float alpha) {
        int n = points.length;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(2.0f); // Make the line a bit thicker
        GL11.glBegin(GL11.GL_LINE_STRIP);

        for (int i = 0; i < n; i++) {
            float px = x + (i * width) / (n - 1);
            float norm = Math.abs(points[i]) / 32768.0f; // 0 (low) to 1 (high)
            float r = norm;
            float g = 1.0f - norm;
            GL11.glColor4f(0, 0, 1f, alpha);
            float py = y + height / 2 - (points[i] / (32768.0f * 3)) * ((float) height / 2);
            GL11.glVertex2f(px, py);
        }

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}




