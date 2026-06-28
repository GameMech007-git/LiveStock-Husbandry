package com.livestockhusbandry.block;

import com.livestockhusbandry.entity.ChickenFeederBlockEntity;
import com.livestockhusbandry.entity.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ChickenFeederBlock extends BaseEntityBlock {

    public static final MapCodec<ChickenFeederBlock> CODEC =
            simpleCodec(ChickenFeederBlock::new);

    private static final int RANGE = 6;

    public ChickenFeederBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ChickenFeederBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof ChickenFeederBlockEntity feeder)) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            showRange(serverLevel, pos);

            player.sendSystemMessage(
                    Component.literal("Showing feeder range: " + RANGE + " blocks")
            );

            return InteractionResult.SUCCESS;
        }

        ItemStack mainHandStack = player.getMainHandItem();

        if (mainHandStack.is(Items.WHEAT_SEEDS)) {
            int inserted = feeder.insertSeeds(mainHandStack.getCount());

            if (inserted > 0) {
                if (!player.getAbilities().instabuild) {
                    mainHandStack.shrink(inserted);
                }

                player.sendSystemMessage(
                        Component.literal("Inserted " + inserted + " seeds. Stored: " + feeder.getSeedCount())
                );
            } else {
                player.sendSystemMessage(
                        Component.literal("Feeder is full.")
                );
            }

            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(
                Component.literal(
                        "Seeds: " + feeder.getSeedCount()
                                + " | Chickens nearby: " + feeder.countNearbyChickens()
                                + " / " + ChickenFeederBlockEntity.MAX_POPULATION
                                + " | Fed today: " + (feeder.hasFedToday() ? "Yes" : "No")
                )
        );

        return InteractionResult.SUCCESS;
    }

    private static void showRange(ServerLevel level, BlockPos pos) {
        int y = pos.getY() + 1;

        for (int dx = -RANGE; dx <= RANGE; dx++) {
            spawnRangeParticle(level, pos.getX() + dx, y, pos.getZ() - RANGE);
            spawnRangeParticle(level, pos.getX() + dx, y, pos.getZ() + RANGE);
        }

        for (int dz = -RANGE; dz <= RANGE; dz++) {
            spawnRangeParticle(level, pos.getX() - RANGE, y, pos.getZ() + dz);
            spawnRangeParticle(level, pos.getX() + RANGE, y, pos.getZ() + dz);
        }
    }

    private static void spawnRangeParticle(
            ServerLevel level,
            int x,
            int y,
            int z
    ) {
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                x + 0.5D,
                y + 0.15D,
                z + 0.5D,
                1,
                0.05D,
                0.02D,
                0.05D,
                0.0D
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChickenFeederBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.CHICKEN_FEEDER,
                ChickenFeederBlockEntity::serverTick
        );
    }
}