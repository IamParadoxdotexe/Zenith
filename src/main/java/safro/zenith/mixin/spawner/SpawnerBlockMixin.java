package safro.zenith.mixin.spawner;

import io.github.fabricators_of_create.porting_lib.block.CustomExpBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import safro.zenith.Zenith;
import safro.zenith.advancements.AdvancementTriggers;
import safro.zenith.spawn.SpawnerModule;
import safro.zenith.spawn.modifiers.SpawnerModifier;
import safro.zenith.spawn.modifiers.SpawnerStats;
import safro.zenith.util.IBaseSpawner;

import java.util.List;

@Mixin(value = SpawnerBlock.class, priority = 1100)
public abstract class SpawnerBlockMixin extends BaseEntityBlock implements CustomExpBlock {

    public SpawnerBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public int getExpDrop(BlockState state, LevelReader level, RandomSource randomSource, BlockPos pos, int fortuneLevel, int silkTouchLevel) {
        return 0;
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, BlockEntity te, ItemStack stack) {
        if (Zenith.enableSpawner && SpawnerModule.spawnerSilkLevel != -1 && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, stack) >= SpawnerModule.spawnerSilkLevel) {
            ItemStack s = new ItemStack(this);
            if (te != null) s.getOrCreateTag().put("BlockEntityTag", te.saveWithoutMetadata());
            popResource(world, pos, s);
            player.getMainHandItem().hurtAndBreak(SpawnerModule.spawnerSilkDamage, player, pl -> pl.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.035F);
        }
        else super.playerDestroy(world, player, pos, state, te, stack);
    }

    @Inject(method = "getCloneItemStack", at = @At("HEAD"), cancellable = true)
    private void zenith$getClone(BlockGetter world, BlockPos pos, BlockState blockState, CallbackInfoReturnable<ItemStack> cir) {
        if (Zenith.enableSpawner) {
            ItemStack s = new ItemStack(this);
            BlockEntity te = world.getBlockEntity(pos);
            if (te != null) s.getOrCreateTag().put("BlockEntityTag", te.saveWithoutMetadata());
            cir.setReturnValue(s);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (Zenith.enableSpawner) {
            BlockEntity te = world.getBlockEntity(pos);
            ItemStack stack = player.getItemInHand(hand);
            ItemStack otherStack = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (te instanceof SpawnerBlockEntity tile) {
                SpawnerModifier match = SpawnerModifier.findMatch(tile, stack, otherStack);
                if (match != null && match.apply((IBaseSpawner) tile)) {
                    if (world.isClientSide) return InteractionResult.SUCCESS;
                    if (!player.isCreative()) {
                        stack.shrink(1);
                        if (match.consumesOffhand()) otherStack.shrink(1);
                    }
                    AdvancementTriggers.SPAWNER_MODIFIER.trigger((ServerPlayer) player, tile, match);
                    world.sendBlockUpdated(pos, state, state, 3);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        } else return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(ItemStack stack, BlockGetter blockGetter, List<Component> tooltip, TooltipFlag tooltipFlag) {
        if (Zenith.enableSpawner) {
            if (stack.hasTag() && stack.getTag().contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
                if (Screen.hasShiftDown()) {
                    CompoundTag tag = stack.getTag().getCompound("BlockEntityTag");
                    if (tag.contains("MinSpawnDelay"))
                        tooltip.add(concat(SpawnerStats.MIN_DELAY.name(), tag.getShort("MinSpawnDelay")));
                    if (tag.contains("MaxSpawnDelay"))
                        tooltip.add(concat(SpawnerStats.MAX_DELAY.name(), tag.getShort("MaxSpawnDelay")));
                    if (tag.contains("SpawnCount"))
                        tooltip.add(concat(SpawnerStats.SPAWN_COUNT.name(), tag.getShort("SpawnCount")));
                    if (tag.contains("MaxNearbyEntities"))
                        tooltip.add(concat(SpawnerStats.MAX_NEARBY_ENTITIES.name(), tag.getShort("MaxNearbyEntities")));
                    if (tag.contains("RequiredPlayerRange"))
                        tooltip.add(concat(SpawnerStats.REQ_PLAYER_RANGE.name(), tag.getShort("RequiredPlayerRange")));
                    if (tag.contains("SpawnRange"))
                        tooltip.add(concat(SpawnerStats.SPAWN_RANGE.name(), tag.getShort("SpawnRange")));
                    if (tag.getBoolean("ignore_players"))
                        tooltip.add(SpawnerStats.IGNORE_PLAYERS.name().withStyle(ChatFormatting.DARK_GREEN));
                    if (tag.getBoolean("ignore_conditions"))
                        tooltip.add(SpawnerStats.IGNORE_CONDITIONS.name().withStyle(ChatFormatting.DARK_GREEN));
                    if (tag.getBoolean("redstone_control"))
                        tooltip.add(SpawnerStats.REDSTONE_CONTROL.name().withStyle(ChatFormatting.DARK_GREEN));
                    if (tag.getBoolean("ignore_light"))
                        tooltip.add(SpawnerStats.IGNORE_LIGHT.name().withStyle(ChatFormatting.DARK_GREEN));
                    if (tag.getBoolean("no_ai"))
                        tooltip.add(SpawnerStats.NO_AI.name().withStyle(ChatFormatting.DARK_GREEN));
                    if (tag.getBoolean("silent"))
                        tooltip.add(SpawnerStats.SILENT.name().withStyle(ChatFormatting.DARK_GREEN));
                } else {
                    tooltip.add(Component.translatable("misc.zenith.shift_stats").withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }

    private static Component concat(Object... args) {
        return Component.translatable("misc.zenith.value_concat", args[0], Component.literal(args[1].toString()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.GREEN);
    }
}
