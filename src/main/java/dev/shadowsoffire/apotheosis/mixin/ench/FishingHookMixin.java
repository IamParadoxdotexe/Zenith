package dev.shadowsoffire.apotheosis.mixin.ench;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.ench.EnchModule;
import dev.shadowsoffire.apotheosis.mixin.accessors.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.FishingHook;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow @Final private int lureSpeed;

    @Shadow private int timeUntilLured;

    /**
     * Calculates the delay for catching a fish. Ensures that the value never returns <= 0, so that it doesn't get infinitely locked.
     */
    //todo what the fuck is this
    @Inject(method = "catchingFish",
            slice = @Slice(from = @At(value = "INVOKE",
            target = "net/minecraft/util/Mth.nextInt (Lnet/minecraft/util/RandomSource;II)I",  shift = At.Shift.AFTER, ordinal = 2)),
            at = @At(value = "FIELD", target = "net/minecraft/world/entity/projectile/FishingHook.timeUntilLured : I",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void pain(BlockPos pos, CallbackInfo ci){
        if (!Apotheosis.enableEnch) return;
        if (Apotheosis.enableDebug) EnchModule.LOGGER.error("Modifying catching fish time");
        int lowBound = Math.max(1, 100 - this.lureSpeed * 10);
        int highBound = Math.max(lowBound, 600 - this.lureSpeed * 60);
        this.timeUntilLured = Mth.nextInt(((EntityAccessor)((FishingHook)(Object)this)).getRandom(), lowBound, highBound);
    }
}
