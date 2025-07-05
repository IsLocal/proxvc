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

    public static String[] getSpecifiers() {
        List<String> result = null;
        try {
            result = ALUtil.getStringList(0, ALC11.ALC_CAPTURE_DEVICE_SPECIFIER);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("Raw device specifiers: " + (result == null ? "dunno" : result)); // Debugging output

        return result == null ? new String[0] : result.toArray(new String[0]);
    }

    public synchronized void open(String deviceName) {
        close();
        System.out.println("Opening audio input device: " + deviceName); // Debugging output


        if (deviceName == null || deviceName.isEmpty()) {
            device = null;
        } else {
            device = ALC11.alcCaptureOpenDevice(
                    deviceName,
                    VCProtocol.SAMPLE_RATE,
                    AL10.AL_FORMAT_MONO16,
                    VCProtocol.SAMPLE_COUNT * NUM_DEVICE_BUFFERS
            );
            ALC11.alcCaptureStart(device);
            System.out.println("Opened audio input device: " + deviceName); // Debugging output
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
        ALC10.alcGetInteger(device, ALC11.ALC_CAPTURE_SAMPLES);
        if (ints.get(0) < VCProtocol.SAMPLE_COUNT) {
            return null;
        }
        samples.rewind();
        ALC11.alcCaptureSamples(device, samples, VCProtocol.SAMPLE_COUNT);
        return samples;
    }

    @Override
    public synchronized void close() {
        if (device == null) {
            return;
        }
        if (isClosed())
            return;
        ALC11.alcCaptureStop(device);
        ALC11.alcCaptureCloseDevice(device);
    }
}
