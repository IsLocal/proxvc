package fiveavian.proxvc.vc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.block.Block;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EFX {
    private final int source;
    public Integer reverbSlot;
    public Integer reverbEffect;
    public boolean reverbEnabled = true;
    public Integer lowpassFilter = null;
    public float lowpassIntensity = 0.5f;

    private static final List<Vec3> directions = Collections.unmodifiableList(Arrays.asList(
            Vec3.getPermanentVec3(1, 0, 0),
            Vec3.getPermanentVec3(-1, 0, 0),
            Vec3.getPermanentVec3(0, 1, 0),
            Vec3.getPermanentVec3(0, -1, 0),
            Vec3.getPermanentVec3(0, 0, 1),
            Vec3.getPermanentVec3(0, 0, -1),
            Vec3.getPermanentVec3(1, 1, 0).normalize(),
            Vec3.getPermanentVec3(1, -1, 0).normalize(),
            Vec3.getPermanentVec3(-1, 1, 0).normalize(),
            Vec3.getPermanentVec3(-1, -1, 0).normalize(),
            Vec3.getPermanentVec3(1, 0, 1).normalize(),
            Vec3.getPermanentVec3(1, 0, -1).normalize(),
            Vec3.getPermanentVec3(-1, 0, 1).normalize(),
            Vec3.getPermanentVec3(-1, 0, -1).normalize(),
            Vec3.getPermanentVec3(0, 1, 1).normalize(),
            Vec3.getPermanentVec3(0, 1, -1).normalize(),
            Vec3.getPermanentVec3(0, -1, 1).normalize(),
            Vec3.getPermanentVec3(0, -1, -1).normalize()
    ));

    public EFX(int source) {
        this.source = source;
        setupLowpass();
        setupReverb();
    }


    public void setupLowpass() {
        int filter = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);

        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, 1f); // Adjust gain as needed
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, lowpassIntensity); // Attenuate high frequencies

        // Attach the filter to the source
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);

        lowpassFilter = filter;
    }

    public void setLowpassIntensity(float intensity, float partialTick) {
        if (lowpassFilter == null)
            return;
        intensity = MathHelper.clamp(intensity, 0f, 0.5f);
        if (Math.abs(intensity - lowpassIntensity) < 0.01f)
            return;

        lowpassIntensity = MathHelper.lerp(lowpassIntensity, intensity, 0.13f * partialTick * 20f);
        lowpassIntensity = MathHelper.clamp(lowpassIntensity, 0f, 1f);

        EXTEfx.alFilterf(lowpassFilter, EXTEfx.AL_LOWPASS_GAINHF, lowpassIntensity); // Attenuate high frequencies
        EXTEfx.alEffectf(lowpassFilter, EXTEfx.AL_LOWPASS_GAIN, 0.1f); // Set the gain for the lowpass filter
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, lowpassFilter);

    }

    public void setupReverb() {
        int slot = EXTEfx.alGenAuxiliaryEffectSlots();
        int effect = EXTEfx.alGenEffects();

        EXTEfx.alEffecti(effect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);

        EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, effect);

        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAIN, 0.0f);

        reverbSlot = slot;
        reverbEffect = effect;
        reverbEnabled = true;

        AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, reverbEffect, 0, EXTEfx.AL_DIRECT_FILTER);
    }

    public void calculateMuffleIntensity(Minecraft client, Player entity, float intensityOptionFloatValue) {
        if (intensityOptionFloatValue == 0) {
            setLowpassIntensity(EFXConfig.NOEFFECT_LOWPASS, client.timer.partialTicks);
            return;
        }
        HitResult hitFromEars = client.currentWorld.checkBlockCollisionBetweenPoints(
                client.thePlayer.getPosition(0, true),
                entity.getPosition(0, true),
                false);

        HitResult hitFromSource = client.currentWorld.checkBlockCollisionBetweenPoints(
                entity.getPosition(0, true),
                client.thePlayer.getPosition(0, true),
                false);

        if (hitFromEars == null || hitFromSource == null || hitFromEars.hitType != HitResult.HitType.TILE || hitFromSource.hitType != HitResult.HitType.TILE) {
            setLowpassIntensity(EFXConfig.NOEFFECT_LOWPASS, client.timer.partialTicks);
            return;
        }
        // Shoot rays in 16 directions from the player and the source to get description of the room

        Object[] roomDescription = calculateRoomDescription(client, entity);
        float averageDistance = (float) roomDescription[0];
        int numRays = (int) roomDescription[1];
        int escapedRays = (int) roomDescription[2];
        int escapedMouthRays = (int) roomDescription[3];

        Object[] roomDescriptionFromEars = calculateRoomDescription(client, client.thePlayer);
        float averageDistanceFromEars = (float) roomDescriptionFromEars[0];
        int numRaysFromEars = (int) roomDescriptionFromEars[1];
        int escapedRaysFromEars = (int) roomDescriptionFromEars[2];
        int escapedMouthRaysFromEars = (int) roomDescriptionFromEars[3];

        // Check if the player is in a room based on the number of rays that hit solid blocks
        //If more rays escaped than hit, we assume the player is outside or in a large open space
        boolean isInRoom = numRays > 0 && escapedRays < numRays / 2;
        boolean isInRoomFromEars = numRaysFromEars > 0 && escapedRaysFromEars < numRaysFromEars / 2;

        // If the player is not in a room, we set the lowpass intensity to the default value
        // Additional implements for reverb and other effects can be added here
        if (!isInRoom && !isInRoomFromEars) {
            setLowpassIntensity(EFXConfig.NOEFFECT_LOWPASS, client.timer.partialTicks);
            return;
        }

        double thickness = hitFromEars.location.distanceTo(hitFromSource.location);

        Block<?> blockAtEars = client.currentWorld.getBlock(hitFromEars.x, hitFromEars.y, hitFromEars.z);
        Block<?> blockAtSource = client.currentWorld.getBlock(hitFromSource.x, hitFromSource.y, hitFromSource.z);

        assert blockAtEars != null;
        assert blockAtSource != null;

        // Calculate the cumulative blast resistance of the blocks in the path
        float resistance = 0f;
        resistance += blockAtEars.blastResistance + blockAtSource.blastResistance;

        for (int i = 0; i < thickness; i++) {
            if (resistance > EFXConfig.TWO_BLOCK_RESISTANCE_REF) {
                break;
            }
            Vec3 dir = hitFromSource.location.add(-hitFromSource.x, -hitFromSource.y, -hitFromSource.z)
                    .normalize().scale(i);

            Vec3 pos = hitFromEars.location.add(dir.x, dir.y, dir.z);
            Block<?> block = client.currentWorld.getBlock((int) pos.x, (int) pos.y, (int) pos.z);
            if (block != null) {
                resistance += block.blastResistance;
            }
        }

        resistance /= (EFXConfig.TWO_BLOCK_RESISTANCE_REF);

        float baseIntensityFormula = (float) MathHelper.lerp(EFXConfig.NOEFFECT_LOWPASS, 0.0,
                MathHelper.clamp(thickness / EFXConfig.REFERENCE_DISTANCE, 0f, 1f));

        baseIntensityFormula = MathHelper.lerp(EFXConfig.NOEFFECT_LOWPASS, baseIntensityFormula, resistance);

        baseIntensityFormula = MathHelper.clamp(baseIntensityFormula, 0f, 1f);

        float intensityFromPlayer = MathHelper.clamp(intensityOptionFloatValue, 0f, 1f);

        // Calculate the influence of the rays that escaped from the mouth
        int mouthalInfluence = 0;
        if (escapedMouthRays > 0) {
            mouthalInfluence = (int) MathHelper.clamp(escapedMouthRays / (float) numRays, 0f, 1f);
        }

        float finalIntensity = MathHelper.lerp(baseIntensityFormula, 1f, 1 - intensityFromPlayer);
        //finalIntensity = MathHelper.lerp(finalIntensity, EFXConfig.NOEFFECT_LOWPASS, mouthalInfluence * 0.5f);
        setLowpassIntensity(finalIntensity, client.timer.partialTicks);
    }

    public Object[] calculateRoomDescription(Minecraft client, Player entity) {
        Vec3 pos = entity.getPosition(client.timer.partialTicks, true);
        float maxDistance = 20f;
        float totalDistance = 0f;
        int escapedRays = 0;
        int numRays = 0;
        int escapedMouthRays = 0;


        for (Vec3 dir : directions) {
            HitResult hit = client.currentWorld.checkBlockCollisionBetweenPoints(
                    pos,
                    Vec3.getTempVec3(pos.x, pos.y, pos.z).add(dir.x * maxDistance, dir.y * maxDistance, dir.z * maxDistance),
                    true);
            Block<?> block = null;
            if (hit != null && hit.hitType == HitResult.HitType.TILE) {
                block = client.currentWorld.getBlock(hit.x, hit.y, hit.z);
            }

            if (block != null && (block.getMaterial().isSolid() || block.getMaterial().isLiquid())) {
                float distance = (float) hit.location.distanceTo(pos);
                totalDistance += distance;
                numRays++;
            } else {

                escapedRays++;
                if (dir.dotProduct(entity.getLookAngle()) >= 0.5f) {
                    escapedMouthRays++;
                }
            }

        }
        if (numRays == 0) {
            // no solid blocks found, assume a large room
            totalDistance = maxDistance * directions.size();
            numRays = directions.size();
        }
        float averageDistance = totalDistance / numRays;

        return new Object[]{
                averageDistance, // average distance to solid blocks
                numRays, // number of rays that hit solid blocks
                escapedRays, // number of rays that did not hit solid blocks
                escapedMouthRays // number of rays that did not hit solid blocks and were directed away from the mouth
        };
    }

    public void setReverbEnabled(boolean enable) {
        if (reverbSlot == null || reverbEffect == null)
            return;
        if (enable && !reverbEnabled) {
            AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, this.reverbEffect, 0, EXTEfx.AL_FILTER_NULL);
        } else if (!enable && reverbEnabled) {
            AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, 0, EXTEfx.AL_FILTER_NULL);
        }
    }

    public void close() {
        if (lowpassFilter != null) {
            EXTEfx.alDeleteFilters(lowpassFilter);
            lowpassFilter = null;
        }
        if (reverbEffect != null) {
            EXTEfx.alDeleteEffects(reverbEffect);
            reverbEffect = null;
        }
        if (reverbSlot != null) {
            EXTEfx.alDeleteAuxiliaryEffectSlots(reverbSlot);
            reverbSlot = null;
        }
    }
}
