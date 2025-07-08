package fiveavian.proxvc;

import fiveavian.proxvc.api.ServerEvents;
import fiveavian.proxvc.vc.server.VCRelayServer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.minecraft.server.MinecraftServer;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ProxVCServer implements DedicatedServerModInitializer {
    public MinecraftServer server;
    public DatagramSocket socket;
    public Thread relayThread;

    @Override
    public void onInitializeServer() {
        ServerEvents.START.add(this::start);
        ServerEvents.STOP.add(this::stop);
    }

    private void start(MinecraftServer server) {
        this.server = server;

        try {
            String ip = server.propertyManager.getStringProperty("server-ip", "");
            int port = server.propertyManager.getIntProperty("server-port", 25565);
            socket = new DatagramSocket(port, ip.isEmpty() ? null : InetAddress.getByName(ip));
            relayThread = new Thread(new VCRelayServer(this));
            relayThread.start();
        } catch (SocketException | UnknownHostException ex) {
            System.out.println("Failed to start the ProxVC server because of an exception.");
            System.out.println("Continuing without ProxVC.");
            ex.printStackTrace();
        }
    }

    private void stop(MinecraftServer server) {
        try {
            if (socket != null) {
                socket.close();
            }
            if (relayThread != null) {
                relayThread.join();
            }
        } catch (InterruptedException ex) {
            System.out.println("Failed to stop the ProxVC server because of an exception.");
            ex.printStackTrace();
        }
    }
}
