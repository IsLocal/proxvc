package fiveavian.proxvc.vc.client;

import fiveavian.proxvc.ProxVCClient;
import fiveavian.proxvc.util.BufferAES;
import fiveavian.proxvc.util.DatagramPacketWrapper;
import fiveavian.proxvc.vc.AudioInputDevice;
import fiveavian.proxvc.vc.StreamingAudioSource;
import fiveavian.proxvc.vc.VCProtocol;
import net.minecraft.client.Minecraft;
import net.minecraft.core.util.helper.AES;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Map;

public class VCInputClient implements Runnable {
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[VCProtocol.BUFFER_SIZE]);
    private static final int TICKS_UNTIL_NOOP_PACKET = 25;

    private final ProxVCClient vcClient;
    private final Minecraft client;
    private final DatagramSocket socket;
    private final DatagramPacketWrapper packet;
    private final AudioInputDevice device;
    private final Map<Integer, StreamingAudioSource> sources;
    private int ticksUntilNoopPacket = 25;

    public VCInputClient(ProxVCClient vcClient) {
        this.vcClient = vcClient;
        client = vcClient.client;
        socket = vcClient.socket;
        packet = new DatagramPacketWrapper(socket, 8 + VCProtocol.BUFFER_SIZE + 16);
        device = vcClient.device;
        sources = vcClient.sources;
    }

    @Override
    public void run() {
        while (!socket.isClosed()) {
            try {
                Thread.sleep(10);
                if (vcClient.isDisconnected())
                    continue;
                sendNextPacket();
            } catch (SocketException ignored) {
            } catch (Exception ex) {
                System.out.println("Caught an exception during the input client loop.");
                ex.printStackTrace();
            }
        }
        System.out.println("Exited input client loop.");
    }

    private void sendNextPacket() throws Exception {
        if (sources.isEmpty()) {
            return;
        }
        if (client.thePlayer == null) {
            return;
        }
        ByteBuffer samples = device.pollSamples();
        if (samples == null) {
            return;
        }
        packet.buffer.rewind();
        packet.buffer.putInt(client.thePlayer.id);
        if (vcClient.isMuted.value || (vcClient.usePushToTalk.value && !vcClient.keyPushToTalk.isPressed())) {
            ticksUntilNoopPacket -= 1;
            if (ticksUntilNoopPacket > 0) {
                return;
            }
            ticksUntilNoopPacket = TICKS_UNTIL_NOOP_PACKET;
            samples = EMPTY_BUFFER;
        } else {
            ticksUntilNoopPacket = TICKS_UNTIL_NOOP_PACKET;
        }
        samples.rewind();
        BufferAES.encrypt(AES.clientKeyChain, samples, packet.buffer);
        packet.send(vcClient.serverAddress);
    }
}
