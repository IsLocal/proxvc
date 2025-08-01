package fiveavian.proxvc.vc;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

public class AudioInputDevice implements AutoCloseable {
    private static final int NUM_DEVICE_BUFFERS = 8;

    private final ByteBuffer samples = BufferUtils.createByteBuffer(VCProtocol.BUFFER_SIZE);
    private final IntBuffer ints = BufferUtils.createIntBuffer(1);
    private Long device = null;
    private boolean isTalking = false;

    public static String[] getSpecifiers() {
        List<String> result = null;
        try {
            result = ALUtil.getStringList(0, ALC11.ALC_CAPTURE_DEVICE_SPECIFIER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result == null ? new String[0] : result.toArray(new String[0]);
    }

    public synchronized void open(String deviceName) {
        close();
        if (deviceName == null || deviceName.isEmpty()) {
            device = null;
        } else {
            try {
                device = ALC11.alcCaptureOpenDevice(
                        deviceName,
                        VCProtocol.SAMPLE_RATE,
                        AL10.AL_FORMAT_MONO16,
                        VCProtocol.SAMPLE_COUNT * NUM_DEVICE_BUFFERS
                );
                ALC11.alcCaptureStart(device);
            } catch (Exception ex) {
                ex.printStackTrace();
                device = null;
            }
        }
    }

    public synchronized boolean isClosed() {
        return device == null;
    }

    public synchronized ByteBuffer pollSamples() {
        if (isClosed()) {
            return null;
        }
        ints.rewind();
        ALC11.alcGetIntegerv(device, ALC11.ALC_CAPTURE_SAMPLES, ints);
        if (ints.get(0) < VCProtocol.SAMPLE_COUNT) {
            return null;
        }
        samples.rewind();
        ALC11.alcCaptureSamples(device, samples, VCProtocol.SAMPLE_COUNT);
        isTalking = !isSilent(samples);
        return samples;
    }

    public synchronized boolean isTalking() {
        if (isClosed()) {
            return false;
        }
        return isTalking;
    }

    private boolean isSilent(ByteBuffer samples) {
        if (samples.remaining() < 2) { // Assuming 16-bit samples (2 bytes)
            return true; // Not enough data to determine silence
        }
        for (int i = 0; i < samples.remaining(); i += 2) {
            short sample = samples.getShort(i); // Read 16-bit sample
            if (Math.abs(sample) > 1) {
                return false; // Found a sample above the silence threshold
            }
        }
        return true; // All samples are below the threshold
    }

    @Override
    public synchronized void close() {
        if (isClosed())
            return;
        ALC11.alcCaptureStop(device);
        ALC11.alcCaptureCloseDevice(device);
    }
}
