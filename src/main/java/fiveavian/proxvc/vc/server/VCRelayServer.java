package fiveavian.proxvc.vc.server;

import fiveavian.proxvc.ProxVCServer;
import fiveavian.proxvc.util.BufferAES;
import fiveavian.proxvc.util.DatagramPacketWrapper;
import fiveavian.proxvc.vc.VCProtocol;
import net.minecraft.core.util.helper.AES;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.player.PlayerServer;

import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class VCRelayServer implements Runnable {
    private final MinecraftServer server;
    private final DatagramSocket socket;
    private final DatagramPacketWrapper packet;

    private final HashMap<SocketAddress, PlayerServer> connections = new HashMap<>();
    private final ByteBuffer samples = ByteBuffer.allocate(VCProtocol.BUFFER_SIZE + 16);

    public VCRelayServer(ProxVCServer vcServer) {
        server = vcServer.server;
        socket = vcServer.socket;
        packet = new DatagramPacketWrapper(socket, 4 + VCProtocol.BUFFER_SIZE + 16);
    }

    @Override
    public void run() {
        while (!socket.isClosed()) {
            try {
                handleNextPacket();
            } catch (SocketException ignored) {
            } catch (Exception ex) {
                System.out.println("Caught an exception during the relay server loop.");
                ex.printStackTrace();
            }
        }
        System.out.println("Exited relay server loop.");
    }

    private void handleNextPacket() throws Exception {
        packet.receive();
        SocketAddress address = packet.packet.getSocketAddress();
        int entityId = packet.buffer.getInt();
        PlayerServer player = getPlayerById(entityId);
        if (player == null) {
            return;
        }
        samples.rewind();
        samples.limit(VCProtocol.BUFFER_SIZE + 16); // add room for AES padding
        BufferAES.decrypt(AES.keyChain.get(player.username), packet.buffer, samples);
        samples.limit(VCProtocol.BUFFER_SIZE);

        connections.put(packet.packet.getSocketAddress(), player);
        connections.entrySet().removeIf(this::isConnectionOffline);
        for (SocketAddress key : connections.keySet()) {
            shareSamples(address, player, key, connections.get(key));
        }
    }

    private PlayerServer getPlayerById(int id) {
        for (PlayerServer player : server.playerList.playerEntities) {
            if (player.id == id) {
                return player;
            }
        }
        return null;
    }

    private boolean isConnectionOffline(Map.Entry<SocketAddress, PlayerServer> entry) {
        return getPlayerById(entry.getValue().id) == null;
    }

    private void shareSamples(SocketAddress sourceAddress, PlayerServer sourcePlayer, SocketAddress address, PlayerServer player) throws Exception {
        if (sourceAddress.equals(address) || sourcePlayer.id == player.id || sourcePlayer.distanceTo(player) > 32f) {
            return;
        }
        samples.rewind();
        packet.buffer.position(4);
        BufferAES.encrypt(AES.keyChain.get(player.username), samples, packet.buffer);
        packet.send(address);
    }
}
