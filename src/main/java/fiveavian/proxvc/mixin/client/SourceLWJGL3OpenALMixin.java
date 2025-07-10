package fiveavian.proxvc.mixin.client;

import com.mojang.logging.LogUtils;
import fiveavian.proxvc.physics.SoundPhysics;
import net.betterthanadventure.sound.ChannelLWJGL3OpenAL;
import net.betterthanadventure.sound.SourceLWJGL3OpenAL;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.block.Block;
import net.minecraft.core.sound.SoundCategory;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.FilenameURL;
import paulscode.sound.SoundBuffer;
import paulscode.sound.Source;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Pseudo
@Mixin(value = SourceLWJGL3OpenAL.class, remap = false)
public class SourceLWJGL3OpenALMixin extends Source {
    @Shadow private ChannelLWJGL3OpenAL channelOpenAL;
    @Unique
    final Minecraft mc = Minecraft.getMinecraft();


    public SourceLWJGL3OpenALMixin(boolean priority, boolean toStream, boolean toLoop, String sourcename, FilenameURL filenameURL, SoundBuffer soundBuffer, float x, float y, float z, int attModel, float distOrRoll, boolean temporary) {
        super(priority, toStream, toLoop, sourcename, filenameURL, soundBuffer, x, y, z, attModel, distOrRoll, temporary);
    }

    Integer lowpassFilter = null;

    @Redirect(method = "play", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/AL10;alSourcefv(IILjava/nio/FloatBuffer;)V", remap = false),require = 0)
    private void alSourcefv(int source, int param, FloatBuffer values) {
        if (SoundPhysics.lastSoundCategory != null && SoundPhysics.lastSoundCategory != SoundCategory.ENTITY_SOUNDS && SoundPhysics.lastSoundCategory != SoundCategory.WORLD_SOUNDS) {
            AL10.alSourcefv(source, param, values);
            return;
        }
        if (param != AL10.AL_POSITION || mc.currentWorld == null || mc.thePlayer == null) {
            AL10.alSourcefv(source, param, values);
            return;
        }

        lowpassFilter = this.setupLowpass(source);
        this.calculateMuffleIntensity( source, values.get(0), values.get(1), values.get(2));

        AL10.alSource3f(source, AL10.AL_POSITION, values.get(0), values.get(1), values.get(2));
    }


    @Inject(method = "cleanup", at = @At("HEAD"), require = 0)
    private void cleanup(CallbackInfo ci) {
        if (lowpassFilter != null) {

            EXTEfx.alDeleteFilters(lowpassFilter);

            lowpassFilter = null;
        }
    }

    @Unique
    public void calculateMuffleIntensity(int source, float x, float y, float z) {
        Vec3 pos = Vec3.getTempVec3(x, y, z);

        if (lowpassFilter == null) {
            throw new IllegalStateException("Lowpass filter is not initialized for source: " + source);
        }

        Minecraft client = Minecraft.getMinecraft();
        HitResult hitFromEars = client.currentWorld.checkBlockCollisionBetweenPoints(
                client.thePlayer.getPosition(0, true),
                pos,
                false);
        Block<?> block = null;

        HitResult hitFromSource = client.currentWorld.checkBlockCollisionBetweenPoints(
                pos,
                client.thePlayer.getPosition(0, true),
                false);

        if (hitFromEars == null || hitFromSource == null || hitFromEars.hitType != HitResult.HitType.TILE || hitFromSource.hitType != HitResult.HitType.TILE) {
            setLowpassIntensity(source, 0.5f);
            return;
        }

        double thickness = Math.floor( hitFromEars.location.distanceToSquared(hitFromSource.location));
        // 0.0 at 10 blocks, 0.5 at 5 blocks
        setLowpassIntensity(source, (float) (1.5 - (thickness * 0.1f)));

    }

    @Unique
    public void setLowpassIntensity(int source, float intensity) {

        intensity = MathHelper.clamp(intensity, 0.05f, 1.0f);
        System.out.println("Setting lowpass intensity to " + intensity + " for source " + source);

        EXTEfx.alFilterf(lowpassFilter, EXTEfx.AL_LOWPASS_GAINHF,  intensity); // Set the gain for high frequencies based on intensity
        //System.out.println("Set lowpass intensity to " + lowpassIntensity);
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, lowpassFilter);
    }

    @Unique
    public int setupLowpass(int source) {
        int newFilter = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(newFilter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);

        EXTEfx.alFilterf(newFilter, EXTEfx.AL_LOWPASS_GAIN, 1.0f); // Set the gain for low frequencies
        EXTEfx.alFilterf(newFilter, EXTEfx.AL_LOWPASS_GAINHF, 0.0f); // Set the gain for high frequencies to 0


        // Attach the filter to the source
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, newFilter);

        return newFilter;
    }
}
