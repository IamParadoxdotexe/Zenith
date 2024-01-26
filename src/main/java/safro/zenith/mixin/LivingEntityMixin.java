package safro.zenith.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import safro.zenith.Zenith;
import safro.zenith.ench.enchantments.ReflectiveEnchant;
import safro.zenith.potion.PotionModule;
import safro.zenith.util.Events;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow public abstract Iterable<ItemStack> getArmorSlots();

    @Shadow protected ItemStack useItem;

    @Shadow protected int useItemRemaining;

    @Shadow public abstract void setHealth(float f);

    @Shadow public abstract float getHealth();

    @Shadow
    public abstract boolean hasEffect(MobEffect ef);

    @Shadow
    public abstract MobEffectInstance getEffect(MobEffect ef);

    @ModifyVariable(method = "heal", at = @At(value = "HEAD"), argsOnly = true)
    private float zenith$healEvent(float value){
        float amount = Events.HealEvent.EVENT.invoker().onLivingHeal((LivingEntity) (Object) this, value);
        return amount >= 0 ? amount : 0;
    }


    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V", shift = At.Shift.BEFORE))
    private void zenithShieldBlock(DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (Zenith.enableEnch) {
            ReflectiveEnchant.reflect(entity, damageSource, f);
        }
    }

    /**
     * @author Shadows
     * @reason Injection of the Sundering potion effect, which is applied during resistance calculations.
     * @param value Damage modifier percentage after resistance has been applied [1.0, -inf]
     * @param max Zero
     * @param source The damage source
     * @param damage The initial damage amount
     */
    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    public float zenithSunderingMax(float value, float max, DamageSource source, float damage) {
        if (Zenith.enablePotion && this.hasEffect(PotionModule.SUNDERING_EFFECT) && source != DamageSource.OUT_OF_WORLD) {
            int level = this.getEffect(PotionModule.SUNDERING_EFFECT).getAmplifier() + 1;
            value += damage * level * 0.2F;
        }
        return Math.max(value, max);
    }

    /**
     * @author Shadows
     * @reason Used to enter an if-condition so the above mixin always triggers.
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    public boolean zenithSunderingHasEffect(LivingEntity ths, MobEffect effect) {
        return true;
    }

    /**
     * @author Shadows
     * @reason Used to prevent an NPE since we're faking true on hasEffect
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;getAmplifier()I"), method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    public int zenithSunderingGetAmplifier(@Nullable MobEffectInstance inst) {
        return inst == null ? -1 : inst.getAmplifier();
    }

/*
    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void createAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        AttributeSupplier.Builder builder = cir.getReturnValue();
        if (Zenith.enableAdventure) {
            addIfExists(builder, AdventureModule.COLD_DAMAGE, AdventureModule.CRIT_CHANCE, AdventureModule.CRIT_DAMAGE, AdventureModule.CURRENT_HP_DAMAGE, AdventureModule.DRAW_SPEED, AdventureModule.FIRE_DAMAGE, AdventureModule.LIFE_STEAL, AdventureModule.OVERHEAL, AdventureModule.PIERCING, AdventureModule.GHOST_HEALTH, AdventureModule.MINING_SPEED, AdventureModule.ARROW_DAMAGE, AdventureModule.ARROW_VELOCITY, AdventureModule.EXPERIENCE_GAINED);
        }
    }*/


    private static void addIfExists(AttributeSupplier.Builder builder, Attribute... attribs) {
        for (Attribute attrib : attribs)
            if (attrib != null) builder.add(attrib);
    }
}
