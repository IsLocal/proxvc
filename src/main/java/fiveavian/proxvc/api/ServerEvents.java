package fiveavian.proxvc.api;

import fiveavian.proxvc.ProxVCServer;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ServerEvents {
    public static final List<Consumer<MinecraftServer>> START = new ArrayList<>();
    public static final List<Consumer<MinecraftServer>> STOP = new ArrayList<>();

    public static final List<Consumer<ProxVCServer>> SHARE_SAMPLESET = new ArrayList<>();
}
