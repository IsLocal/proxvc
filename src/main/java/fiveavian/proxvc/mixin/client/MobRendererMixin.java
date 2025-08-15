package fiveavian.proxvc.mixin.client;

import fiveavian.proxvc.ProxVCClient;
import fiveavian.proxvc.util.Waveforms;
import fiveavian.proxvc.vc.StreamingAudioSource;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MobRenderer.class, remap = false)
public abstract class MobRendererMixin<T extends Mob> extends EntityRenderer<T>{


    @Inject(method = "renderLivingLabel", at = @At("HEAD"))
    public void renderLivingLabel(Tessellator tessellator, T entity, String s, double d, double d1, double d2, int maxDistance, boolean depthTest, CallbackInfo ci) {
        if (ProxVCClient.instance.waveformType.value == Waveforms.types.OFF) return;
        if (!(entity instanceof Player)) return;

        float f = (float) ((EntityRenderer<?>) (Object) this).renderDispatcher.camera.distanceTo(entity);
        if (f > (float) maxDistance) return;

        StreamingAudioSource source = ProxVCClient.instance.sources.get(entity.id);
        if (source == null) return;

        // Use the same transformations as the name
        GL11.glPushMatrix();
        GL11.glTranslatef((float) d + 0.0F, (float) d1 + entity.getHeadHeight() + 0.8F, (float) d2);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-((EntityRenderer) (Object) this).renderDispatcher.viewLerpYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(((EntityRenderer) (Object) this).renderDispatcher.viewLerpPitch, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-0.026666671F, -0.026666671F, 0.026666671F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float width = 50;
        float height = 8.0f;
        float xOffset = -width / 2;
        float yOffset = -12;


//        // Waveform with depth testing (dimmed)
        int[] audioDataArray =  source.lastWaveformPoints;
//        renderDotWaveform(audioDataArray, xOffset, yOffset, width, height, 0.25f); // Dimmed version
//        renderGlowWaveform(audioDataArray, xOffset, yOffset, width, height, 0.25f); // Full brightness version

        GL11.glDisable(GL11.GL_DEPTH_TEST);

        Waveforms.renderWaveformStyle(null, audioDataArray, xOffset, yOffset, width, height, 0.15f, true, (float)this.renderDispatcher.camera.distanceTo(entity));

        // Second pass - without depth testing for visible parts
        if (!depthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        Waveforms.renderWaveformStyle(null, audioDataArray, xOffset, yOffset, width, height, 1f, true, (float)this.renderDispatcher.camera.distanceTo(entity)); // Full brightness version

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



}




