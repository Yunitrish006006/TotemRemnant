package dev.totem.remnant.echo;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Crystallizes the experience stored in one Sculk block into an Echo Shard. */
public final class EchoShardCrystallization {
    private EchoShardCrystallization() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(EchoShardCrystallization::interact);
    }

    public static InteractionResult interact(
            net.minecraft.world.entity.player.Player player,
            Level level,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        ItemStack catalyst = player.getItemInHand(hand);
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (player.isSpectator() || !catalyst.is(Items.AMETHYST_SHARD) || !state.is(Blocks.SCULK)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (!mayCrystallize(serverPlayer, serverLevel, pos, hit, catalyst)) {
            return InteractionResult.FAIL;
        }
        if (!serverLevel.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), 3)) {
            return InteractionResult.FAIL;
        }

        if (!serverPlayer.getAbilities().instabuild) {
            catalyst.shrink(1);
        }
        ItemStack result = new ItemStack(Items.ECHO_SHARD);
        if (!serverPlayer.addItem(result)) {
            serverPlayer.drop(result, false);
        }
        serverPlayer.awardStat(Stats.ITEM_USED.get(Items.AMETHYST_SHARD));
        serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 1.0F, 0.9F);
        serverLevel.playSound(null, pos, SoundEvents.SCULK_BLOCK_BREAK,
                SoundSource.BLOCKS, 0.75F, 1.25F);
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D,
                8, 0.25D, 0.25D, 0.25D, 0.01D);
        serverLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                12, 0.3D, 0.3D, 0.3D, 0.02D);
        return InteractionResult.SUCCESS;
    }

    private static boolean mayCrystallize(
            ServerPlayer player,
            ServerLevel level,
            BlockPos pos,
            BlockHitResult hit,
            ItemStack catalyst
    ) {
        return !level.getServer().isUnderSpawnProtection(level, pos, player)
                && level.mayInteract(player, pos)
                && !player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())
                && player.mayUseItemAt(pos, hit.getDirection(), catalyst);
    }
}
