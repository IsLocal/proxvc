package fiveavian.proxvc.mixin.client;

import fiveavian.proxvc.gui.HudComponentStatus;
import fiveavian.proxvc.gui.HudComponentWaveForm;
import net.minecraft.client.gui.hud.component.ComponentAnchor;
import net.minecraft.client.gui.hud.component.HudComponent;
import net.minecraft.client.gui.hud.component.HudComponents;
import net.minecraft.client.gui.hud.component.layout.LayoutAbsolute;
import net.minecraft.client.gui.hud.component.layout.LayoutSnap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HudComponents.class, remap = false)
public class HudComponentsMixin {
    @Inject(method = "<clinit>", at = @At(value = "TAIL"))
    private static void init(CallbackInfo ci) {
        HudComponent hudMicIcon = HudComponents.register(new HudComponentStatus("mic_status",
                new LayoutAbsolute(0.0F, 1F, ComponentAnchor.BOTTOM_LEFT)));

        HudComponents.register(new HudComponentWaveForm("waveform",
                new LayoutSnap(hudMicIcon, ComponentAnchor.CENTER_RIGHT, ComponentAnchor.CENTER_LEFT)));
    }
}
