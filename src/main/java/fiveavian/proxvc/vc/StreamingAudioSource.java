package fiveavian.proxvc.vc;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class StreamingAudioSource implements AutoCloseable {
    private static final int NUM_BUFFERS = 8;

    public final int source ;
    private final IntBuffer buffers = BufferUtils.createIntBuffer(NUM_BUFFERS);
    private int bufferIndex = 0;
    private int numBuffersAvailable = NUM_BUFFERS;

    public StreamingAudioSource() {
        try {
            source = AL10.alGenSources();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate OpenAL source. Is OpenAL initialized? Check if your volume is set to 0!", e);
        }
        AL10.alGenBuffers(buffers);
        AL10.alDistanceModel(AL11.AL_LINEAR_DISTANCE);
        AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 32f);
        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 16f);
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
        AL10.alDeleteSources(source);
        AL10.alDeleteBuffers(buffers);
    }
}
