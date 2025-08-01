package fiveavian.proxvc.mixin.client;

import fiveavian.proxvc.api.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.net.packet.PacketDisconnect;
import net.minecraft.core.net.packet.PacketLogin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(value = PacketHandlerClient.class, remap = false)
public class PacketHandlerClientMixin {
    @Shadow
    @Final
    private Minecraft mc;
    @Shadow
    private boolean disconnected;

    @Inject(method = "handleLogin", at = @At("TAIL"))
    public void handleLogin(PacketLogin packet, CallbackInfo ci) {
        for (BiConsumer<Minecraft, PacketLogin> listener : ClientEvents.LOGIN) {
            listener.accept(mc, packet);
        }
    }
    @Inject(method = "sendFinalPacket", at = @At("HEAD"))
    public void sendFinalPacket(CallbackInfo ci) {
        for (Consumer<Minecraft> listener : ClientEvents.DISCONNECT) {
            listener.accept(mc);
        }
    }


    @Inject(method = "handleKickDisconnect", at = @At("HEAD"))
    public void handleKickDisconnect(PacketDisconnect packet, CallbackInfo ci) {
        for (Consumer<Minecraft> listener : ClientEvents.DISCONNECT) {
            listener.accept(mc);
        }
    }

    @Inject(method = "handleErrorMessage", at = @At("HEAD"))
    public void handleErrorMessage(String s, Object[] errorLines, CallbackInfo ci) {
        if (!disconnected) {
            for (Consumer<Minecraft> listener : ClientEvents.DISCONNECT) {
                listener.accept(mc);
            }
        }
    }
}
