package fiveavian.proxvc.mixin.client;

import fiveavian.proxvc.gui.HudComponentStatus;
import net.minecraft.client.gui.hud.component.ComponentAnchor;
import net.minecraft.client.gui.hud.component.HudComponents;
import net.minecraft.client.gui.hud.component.layout.LayoutAbsolute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HudComponents.class, remap = false)
public class HudComponentsMixin {
    @Inject(method = "<clinit>", at = @At(value = "TAIL"))
    private static void init(CallbackInfo ci) {
        HudComponents.register(new HudComponentStatus("mic_status",
                new LayoutAbsolute(0.0F, 1F, ComponentAnchor.BOTTOM_LEFT)));
    }
}
