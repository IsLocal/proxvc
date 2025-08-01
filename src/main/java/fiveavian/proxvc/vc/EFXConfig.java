package fiveavian.proxvc.vc;

import net.minecraft.core.block.Blocks;

public class EFXConfig {
    static float NOEFFECT_LOWPASS = 1f; // Default gain for high frequencies

    // Distance where full muffle effect is applied
    static float REFERENCE_DISTANCE = 2f;

    //Reference used to calculate the base resistance that gives full muffle effect
    static float TWO_BLOCK_RESISTANCE_REF = Blocks.PLANKS_OAK.blastResistance + Blocks.PLANKS_OAK.blastResistance ;
}
