package fiveavian.proxvc.vc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.block.Block;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class StreamingAudioSource implements AutoCloseable {
    private static final int NUM_BUFFERS = 8;

    public final int source;
    private final IntBuffer buffers = BufferUtils.createIntBuffer(NUM_BUFFERS);
    private int bufferIndex = 0;
    private int numBuffersAvailable = NUM_BUFFERS;
    public Integer reverbSlot;
    public Integer reverbEffect;
    public boolean reverbEnabled = true;

    public Integer lowpassFilter = null;
    public float lowpassIntensity = 0.5f;

    private static final Vec3[] directions = {
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
    };

    public StreamingAudioSource() {
        try {
            source = AL10.alGenSources();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate OpenAL source. Is OpenAL initialized? Check if your volume is set to 0!", e);
        }

        setupLowpass();
        setupReverb();
        AL10.alGenBuffers(buffers);
        AL10.alDistanceModel(AL11.AL_LINEAR_DISTANCE);
        AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 32f);
        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 16f);

        //mouth to world
        AL11.alSourcef(source, AL11.AL_CONE_INNER_ANGLE, 100f);
        AL11.alSourcef(source, AL11.AL_CONE_OUTER_ANGLE, 220f);
        AL11.alSourcef(source, EXTEfx.AL_CONE_OUTER_GAINHF, 0.6f); // Disable high frequency attenuation in the cone
        AL11.alSourcef(source, AL11.AL_CONE_OUTER_GAIN, 0.6f);

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
        //System.out.println("Set lowpass intensity to " + lowpassIntensity);
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

    public void calculateMuffleIntensity(Minecraft client, Player entity, float intensity) {
        if (intensity == 0) {
            setLowpassIntensity(0.5f, client.timer.partialTicks);
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
            setLowpassIntensity(0.5f, client.timer.partialTicks);
            return;
        }

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

        boolean isInRoom = averageDistance < 10f && numRays > 0 && escapedRays < numRays / 2;
        boolean isInRoomFromEars = averageDistanceFromEars < 10f && numRaysFromEars > 0 && escapedRaysFromEars < numRaysFromEars / 2;

        if (!isInRoom && !isInRoomFromEars) {
            setLowpassIntensity(0.5f, client.timer.partialTicks);
            return;
        }

        double thickness = hitFromEars.location.distanceTo(hitFromSource.location);
        //block != null && block.getMaterial().isSolid()
        Block<?> blockAtEars = client.currentWorld.getBlock(hitFromEars.x, hitFromEars.y, hitFromEars.z);
        Block<?> blockAtSource = client.currentWorld.getBlock(hitFromSource.x, hitFromSource.y, hitFromSource.z);
        float resistance = 0f;
        resistance += blockAtEars.blastResistance + blockAtSource.blastResistance;

        // get resistance between the two blocks
        for (int i = 0; i < thickness; i++) {
            Vec3 dir = hitFromSource.location.add(-hitFromSource.x, -hitFromSource.y, -hitFromSource.z)
                    .normalize().scale(i);

            Vec3 pos = hitFromEars.location.add(dir.x, dir.y, dir.z);
            Block<?> block = client.currentWorld.getBlock((int) pos.x, (int) pos.y, (int) pos.z);
            if (block != null) {
                resistance += block.blastResistance;
                if (resistance > 12f) {
                    break; // stop if resistance is too high
                }
            }
        }

        assert blockAtEars != null;
        assert blockAtSource != null;


        resistance /= 12f;

        float baseIntensityFormula = (float) MathHelper.lerp(0.5, 0.0,
                MathHelper.clamp(thickness / 5f, 0f, 1f));

        baseIntensityFormula = MathHelper.lerp(0.5f, baseIntensityFormula, resistance);


        baseIntensityFormula = MathHelper.clamp(baseIntensityFormula, 0f, 1f);
        float intensityFromPlayer = MathHelper.clamp(intensity, 0f, 1f);

        int mouthalInfluence = 0;
        if (escapedMouthRays > 0) {
            mouthalInfluence = (int) MathHelper.clamp(escapedMouthRays / (float) numRays, 0f, 1f);
        }
        System.out.println("Mouthal influence: " + mouthalInfluence + " | Escaped rays: " + escapedMouthRays + " | Num rays: " + numRays);
        float finalIntensity = MathHelper.lerp(baseIntensityFormula, 1f, 1 - intensityFromPlayer);
        finalIntensity = MathHelper.lerp(finalIntensity, 0.5f, mouthalInfluence * 0.5f);
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
                    pos.add(dir.x * maxDistance, dir.y * maxDistance, dir.z * maxDistance),
                    false);
            Block<?> block = null;
            if (hit != null && hit.hitType == HitResult.HitType.TILE) {
                block = client.currentWorld.getBlock(hit.x, hit.y, hit.z);
            }
            if (block != null && block.getMaterial().isSolid()) {
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
            totalDistance = maxDistance * directions.length;
            numRays = directions.length;
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

    public boolean queueSamples(ByteBuffer samples) {
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
