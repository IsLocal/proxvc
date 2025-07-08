package fiveavian.proxvc.vc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.block.Block;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class StreamingAudioSource implements AutoCloseable {
    private static final int NUM_BUFFERS = 8;

    public final int source;
    private final IntBuffer buffers = BufferUtils.createIntBuffer(NUM_BUFFERS);
    private int bufferIndex = 0;
    private int numBuffersAvailable = NUM_BUFFERS;
    public final int entityId;
    public final String playerName;
    public float volume = 1.0f;
    public long lastHeard = System.currentTimeMillis();

    public Integer reverbSlot;
    public Integer reverbEffect;
    public boolean reverbEnabled = true;

    public Integer lowpassFilter = null;
    public float lowpassIntensity = 1f;

    public StreamingAudioSource(int entityId, String playerName) {
        this.entityId = entityId;
        this.playerName = playerName;

        try {
            source = AL10.alGenSources();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate OpenAL source. Is OpenAL initialized? Check if your volume is set to 0!", e);
        }

        // Experimental effects - may not work on all systems

        setupLowpass();
        setupReverb();


        AL10.alGenBuffers(buffers);
        AL10.alDistanceModel(AL11.AL_LINEAR_DISTANCE);
        AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 32f);
        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 16f);
    }

    public void setupLowpass() {
        // test for EFX support

        int filter = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);

        // Set the cutoff frequency (in Hz)
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, 1f); // Adjust gain as needed
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, lowpassIntensity); // Attenuate high frequencies

        // Attach the filter to the source
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);

        lowpassFilter = filter;
    }

    public void setLowpassIntensity(float intensity, float partialTick) {
        if (lowpassFilter == null)
            return;
        intensity = Math.max(0f, Math.min(1f, intensity));
        if (Math.abs(intensity - lowpassIntensity) < 0.01f)
            return;

        lowpassIntensity = MathHelper.lerp(lowpassIntensity, intensity, 0.13f * partialTick * 20f);
        lowpassIntensity = MathHelper.clamp(lowpassIntensity, 0f, 1f);

        EXTEfx.alFilterf(lowpassFilter, EXTEfx.AL_LOWPASS_GAINHF, lowpassIntensity); // Attenuate high frequencies
        System.out.println("Set lowpass intensity to " + lowpassIntensity);
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, lowpassFilter);

    }

    public void setupReverb() {
        int slot = EXTEfx.alGenAuxiliaryEffectSlots();
        int effect = EXTEfx.alGenEffects();

        EXTEfx.alEffecti(effect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);

        EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, effect);

        EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_GAIN, 0.0f);

        reverbSlot = slot;
        reverbEffect = effect;
        reverbEnabled = true;

        AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, reverbEffect, 0, EXTEfx.AL_DIRECT_FILTER);
    }

    public void calculateMuffleIntensity(Minecraft client, Player entity) {
        HitResult hitFromEars = client.currentWorld.checkBlockCollisionBetweenPoints(
                client.thePlayer.getPosition(0, true),
                entity.getPosition(0, true),
                false);
        Block<?> block = null;

        HitResult hitFromSource = client.currentWorld.checkBlockCollisionBetweenPoints(
                entity.getPosition(0, true),
                client.thePlayer.getPosition(0, true),
                false);


        if (hitFromEars == null || hitFromSource == null || hitFromEars.hitType != HitResult.HitType.TILE || hitFromSource.hitType != HitResult.HitType.TILE) {
            setLowpassIntensity(0.5f, client.timer.partialTicks);
            return;
        }
        double thickness = hitFromEars.location.distanceToSquared(hitFromSource.location);
        //block != null && block.getMaterial().isSolid()
        setLowpassIntensity(((5f - (float) thickness) * 0.02f), client.timer.partialTicks);

        //new implementation idea:
        // 1. raycast to player, count number of solid blocks in the way
        // 2. set lowpass gain hf based on that number (e.g. 0.0f for 3 or more blocks, 0.5f for 2 blocks, 0.75f for 1 block, 1.0f for 0 blocks)

    }

    public void calculateRoomDescription(Minecraft client, Player entity) {
        //measure the size of the room the player is in by raycasting in 6 directions and measuring distance to nearest solid block
        Vec3 pos = entity.getPosition(0, true);
        float maxDistance = 20f;
        float totalDistance = 0f;
        int numRays = 0;
        Vec3[] directions = {
                Vec3.getPermanentVec3(1, 0, 0),    // Right
                Vec3.getPermanentVec3(-1, 0, 0),   // Left
                Vec3.getPermanentVec3(0, 1, 0),    // Up
                Vec3.getPermanentVec3(0, -1, 0),   // Down
                Vec3.getPermanentVec3(0, 0, 1),    // Forward
                Vec3.getPermanentVec3(0, 0, -1),   // Backward
                Vec3.getPermanentVec3(1, 1, 0),    // Up-Right
                Vec3.getPermanentVec3(1, -1, 0),   // Down-Right
                Vec3.getPermanentVec3(-1, 1, 0),   // Up-Left
                Vec3.getPermanentVec3(-1, -1, 0),  // Down-Left
                Vec3.getPermanentVec3(1, 0, 1),    // Right-Forward
                Vec3.getPermanentVec3(1, 0, -1),   // Right-Backward
                Vec3.getPermanentVec3(-1, 0, 1),   // Left-Forward
                Vec3.getPermanentVec3(-1, 0, -1),  // Left-Backward
                Vec3.getPermanentVec3(0, 1, 1),    // Up-Forward
                Vec3.getPermanentVec3(0, 1, -1),   // Up-Backward
                Vec3.getPermanentVec3(0, -1, 1),   // Down-Forward
                Vec3.getPermanentVec3(0, -1, -1)   // Down-Backward
        };


        for (Vec3 dir : directions) {
            HitResult hit = client.currentWorld.checkBlockCollisionBetweenPoints(
                    pos,
                    pos.add(dir.x * maxDistance, dir.y * maxDistance, dir.z * maxDistance),
                    false);
            if (hit != null && hit.hitType == HitResult.HitType.TILE) {
                Block<?> block = client.currentWorld.getBlock(hit.x, hit.y, hit.z);
                if (block == null || !block.getMaterial().isSolid()) {
                    totalDistance += maxDistance;
                    numRays++;
                    continue;
                }
                double distance = pos.distanceTo(hit.location);
                totalDistance += (float) Math.min(distance, maxDistance);
                numRays++;
            } else {
                totalDistance += maxDistance;
                numRays++;
            }
        }
        if (numRays == 0)
            return;

        float avgDistance = totalDistance / numRays;
        // 0.0f at 2 blocks, 1.0 at 20 blocks
        float reverbIntensity = (avgDistance - 2f) / 8f; // 0.0f at 2 blocks, 1.0f at 10 blocks
        reverbIntensity = Math.max(0f, Math.min(1f, reverbIntensity));

        //enable or disable reverb based on intensity
        if (reverbIntensity == 0f && reverbEnabled) {
            reverbEnabled = false;
            AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, 0, EXTEfx.AL_FILTER_NULL);
        } else if (reverbIntensity > 0f && !reverbEnabled) {
            reverbEnabled = true;
            AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, this.reverbEffect, 0, EXTEfx.AL_FILTER_NULL);
        }
        //slot


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

    public boolean queueSamples(ByteBuffer samples) {
        lastHeard = System.currentTimeMillis();
        int numBuffersToUnqueue = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
        numBuffersAvailable += numBuffersToUnqueue;
        for (int i = 0; i < numBuffersToUnqueue; i++) {
            AL10.alSourceUnqueueBuffers(source);
        }
        if (numBuffersAvailable == 0) {
            return false;
        }
        AL10.alBufferData(buffers.get(bufferIndex), AL10.AL_FORMAT_MONO16, samples, VCProtocol.SAMPLE_RATE);
        AL10.alSourceQueueBuffers(source, buffers.get(bufferIndex));
        int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
        if (state != AL10.AL_PLAYING) {
            AL10.alSourcePlay(source);
        }
        numBuffersAvailable -= 1;
        bufferIndex += 1;
        bufferIndex %= NUM_BUFFERS;
        return true;
    }

    @Override
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

        AL10.alDeleteSources(source);
        AL10.alDeleteBuffers(buffers);
    }
}
