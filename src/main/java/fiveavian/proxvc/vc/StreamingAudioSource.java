package fiveavian.proxvc.vc;

import fiveavian.proxvc.util.Waveforms;
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
    public float volume = 1.0f;

    public final EFX efx;

    public StreamingAudioSource() {
        try {
            source = AL10.alGenSources();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate OpenAL source. Is OpenAL initialized? Check if your volume is set to 0!", e);
        }
        efx = new EFX(source);

        AL10.alGenBuffers(buffers);
        AL10.alDistanceModel(AL11.AL_LINEAR_DISTANCE);
        AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 32f);
        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 16f);

        //mouth to world
        AL11.alSourcef(source, AL11.AL_CONE_INNER_ANGLE, 100f);
        AL11.alSourcef(source, AL11.AL_CONE_OUTER_ANGLE, 220f);
        AL11.alSourcef(source, EXTEfx.AL_CONE_OUTER_GAINHF, 0.5f); // Disable high frequency attenuation in the cone
        AL11.alSourcef(source, AL11.AL_CONE_OUTER_GAIN, 0.8f);


    }
    public int[] lastWaveformPoints = Waveforms.getWaveformPoints(null, 20);

    public void queueSamples(ByteBuffer samples) {
        int numBuffersToUnqueue = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
        numBuffersAvailable += numBuffersToUnqueue;
        for (int i = 0; i < numBuffersToUnqueue; i++) {
            AL10.alSourceUnqueueBuffers(source);
        }
        if (numBuffersAvailable == 0) {
            return;
        }
        AL10.alBufferData(buffers.get(bufferIndex), AL10.AL_FORMAT_MONO16, samples, VCProtocol.SAMPLE_RATE);
        AL10.alSourceQueueBuffers(source, buffers.get(bufferIndex));
        //save copt
        if (samples.remaining() > 0) {
            lastWaveformPoints = Waveforms.getWaveformPoints(samples, 20);
        }

        int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
        if (state != AL10.AL_PLAYING) {
            AL10.alSourcePlay(source);
        }
        numBuffersAvailable -= 1;
        bufferIndex += 1;
        bufferIndex %= NUM_BUFFERS;
    }


    public void setAttenuationProfile(AttenuationProfile profile) {
        switch (profile) {
            case REALISTIC:
                AL10.alDistanceModel(AL11.AL_EXPONENT_DISTANCE);
                AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 4f);
                AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 32f);
                AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 16f);
                break;
            case VOICE_CLARITY:
                AL10.alDistanceModel(AL11.AL_LINEAR_DISTANCE);
                AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1);
                AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 32f);
                AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 16f);
                break;
        }
    }

    @Override
    public void close() {
        efx.close();
        AL10.alDeleteSources(source);
        AL10.alDeleteBuffers(buffers);
    }


}

