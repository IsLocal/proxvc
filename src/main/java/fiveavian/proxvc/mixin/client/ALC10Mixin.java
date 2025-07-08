package fiveavian.proxvc.mixin.client;

import org.lwjgl.openal.ALC10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import java.nio.charset.*;

/**
 * Ensures that Minecraft doesn't crash on certain devices.
 */
@Pseudo
@Mixin(value = ALC10.class, remap = false)

public class ALC10Mixin {
    @Unique
    private static final CharsetDecoder DECODER = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);

    @Redirect(method = "alcGetString", at = @At(value = "INVOKE", target = "Lorg/lwjgl/MemoryUtil;decodeUTF8(Ljava/nio/ByteBuffer;)Ljava/lang/String;"), require = 0)
    private static String decodeUTF8(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        try {
            return DECODER.decode(buffer).toString();
        } catch (CharacterCodingException ex) {
            // This should not happen! The decoder has been configured to not throw these exceptions.
            throw new RuntimeException(ex);
        }
    }
}