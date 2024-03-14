package dev.shadowsoffire.apotheosis.mixin.ench;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.ench.EnchModule;
import dev.shadowsoffire.apotheosis.ench.table.IEnchantableItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin implements IEnchantableItem {

    /**
     * @author Shadows
     * @reason Enables all items to be enchantable by default.
     */
    @Inject(method = "getEnchantmentValue", at = @At(value = "HEAD"), cancellable = true)
    public void zenith_getEnchantmentValue(CallbackInfoReturnable<Integer> cir) {
        if (Apotheosis.enableEnch) cir.setReturnValue(1);
    }

    @Inject(method = "isEnchantable",at = @At("HEAD"), cancellable = true)
    private void zenith_addCustomEnchantableItems(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Apotheosis.enableEnch && EnchModule.isVanillaAnvil(stack)) cir.setReturnValue(true);
    }
}

