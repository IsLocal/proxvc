package fiveavian.proxvc.mixin.client;

import fiveavian.proxvc.physics.SoundPhysics;
import net.minecraft.client.option.GameSettings;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.client.sound.SoundEntry;
import net.minecraft.core.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ensures that Minecraft doesn't crash
 * when starting the game with no volume.
 */
@Mixin(value = SoundEngine.class, remap = false)
public class SoundEngineMixin {
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundCategoryHelper;isAnyEnabled(Lnet/minecraft/client/option/GameSettings;)Z"))
    public boolean isAnyEnabled(GameSettings category) {
        return true;
    }

    @Inject(method = "playSound(Lnet/minecraft/client/sound/SoundEntry;Lnet/minecraft/core/sound/SoundCategory;FF)V", at = @At("HEAD"))
    public void playSound(SoundEntry entry, SoundCategory category, float volume, float pitch, CallbackInfo ci) {
        SoundPhysics.lastSoundCategory = category;
    }
    @Inject(method = "playSoundAt(Lnet/minecraft/client/sound/SoundEntry;Lnet/minecraft/core/sound/SoundCategory;FFFFF)V", at = @At("HEAD"))
    public void playSoundAt(SoundEntry entry, SoundCategory category, float x, float y, float z, float volume, float pitch, CallbackInfo ci) {
        SoundPhysics.lastSoundCategory = category;
    }
}