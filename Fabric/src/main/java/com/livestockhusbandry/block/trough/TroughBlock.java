package com.livestockhusbandry.block.trough;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.block.entity.ModBlockEntities;
import com.livestockhusbandry.block.entity.TroughBlockEntity;
import com.livestockhusbandry.entity.ai.sheep.SheepTroughReservations;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class TroughBlock extends BaseEntityBlock {

    public static final MapCodec<TroughBlock> CODEC = simpleCodec(TroughBlock::new);

    public static final EnumProperty<TroughPart> PART = EnumProperty.create("part", TroughPart.class);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(
            2.0D, 0.0D, 2.0D,
            14.0D, 14.0D, 14.0D
    );

    public TroughBlock(BlockBehaviour.Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(PART, TroughPart.SINGLE)
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    public MapCodec<TroughBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(PART, TroughPart.SINGLE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        if (!(blockEntity instanceof TroughBlockEntity trough)) {
            return InteractionResult.SUCCESS;
        }

        refreshVisualGroup(serverLevel, pos);

        TroughUtil.TroughGroup group = TroughUtil.resolveGroup(serverLevel, pos);
        TroughBlockEntity controllerTrough = getControllerTrough(serverLevel, group, trough);

        ItemStack mainHandStack = player.getMainHandItem();

        if (player.isShiftKeyDown()) {
            controllerTrough.refreshFenceArea(serverLevel);

            if (controllerTrough.hasFenceArea()) {
                showSavedFenceArea(serverLevel, controllerTrough);

                player.sendSystemMessage(
                        Component.literal(
                                "Saved fenced trough area: "
                                        + controllerTrough.getFenceMinX() + ", " + controllerTrough.getFenceMinZ()
                                        + " to "
                                        + controllerTrough.getFenceMaxX() + ", " + controllerTrough.getFenceMaxZ()
                                        + " | Group size: " + group.size()
                                        + " | Capacity: " + group.capacity()
                        )
                );
            } else {
                showFoldRange(serverLevel, group.centerPos(), group.radius());

                player.sendSystemMessage(
                        Component.literal(
                                "No closed fence found. Using fallback radius: " + group.radius()
                                        + " blocks | Group size: " + group.size()
                                        + " | Capacity: " + group.capacity()
                        )
                );
            }

            return InteractionResult.SUCCESS;
        }

        if (mainHandStack.is(Items.WHEAT)) {
            int inserted = trough.insertWheat(mainHandStack.getCount());

            if (inserted > 0) {
                if (!player.getAbilities().instabuild) {
                    mainHandStack.shrink(inserted);
                }

                player.sendSystemMessage(
                        Component.literal(
                                "Inserted " + inserted + " wheat.\nStored: " + trough.getWheatCount()
                        )
                );
            } else {
                player.sendSystemMessage(Component.literal("Sheep trough is full."));
            }

            return InteractionResult.SUCCESS;
        }

        String areaText;

        if (controllerTrough.hasFenceArea()) {
            areaText = "Fenced area: "
                    + controllerTrough.getFenceMinX() + ", " + controllerTrough.getFenceMinZ()
                    + " to "
                    + controllerTrough.getFenceMaxX() + ", " + controllerTrough.getFenceMaxZ();
        } else {
            areaText = "Fallback radius: " + group.radius();
        }

        int registeredSheep = SheepTroughReservations.getRegisteredCount(
                serverLevel,
                group.controllerPos()
        );

        player.sendSystemMessage(
                Component.literal(
                        "Wheat: " + trough.getWheatCount()
                                + " | Group: " + group.size()
                                + " trough" + (group.size() == 1 ? "" : "s")
                                + " | Capacity: " + group.capacity()
                                + " | Sheep: " + registeredSheep + "/" + group.capacity()
                                + " | " + areaText
                                + " | Controller: "
                                + group.controllerPos().getX() + ", "
                                + group.controllerPos().getY() + ", "
                                + group.controllerPos().getZ()
                )
        );

        return InteractionResult.SUCCESS;
    }

    public static void refreshVisualGroup(ServerLevel level, BlockPos originPos) {
        List<BlockPos> line = collectStraightLine(level, originPos);

        if (line.isEmpty()) {
            return;
        }

        Direction facing = getFacingForLine(line);
        line = sortForFacing(line, facing);

        applyChunkedVisualLine(level, line, facing);
    }

    private static List<BlockPos> collectStraightLine(ServerLevel level, BlockPos originPos) {
        List<BlockPos> xLine = collectFullLine(level, originPos, AxisChoice.X);
        List<BlockPos> zLine = collectFullLine(level, originPos, AxisChoice.Z);

        if (xLine.size() > zLine.size()) {
            return xLine;
        }

        if (zLine.size() > xLine.size()) {
            return zLine;
        }
        return xLine;
    }

    private static List<BlockPos> collectFullLine(ServerLevel level, BlockPos originPos, AxisChoice axis) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(originPos.immutable());

        collectFullDirection(level, originPos, axis, 1, positions);
        collectFullDirection(level, originPos, axis, -1, positions);

        return positions.stream()
                .distinct()
                .sorted(TroughBlock::compareBlockPos)
                .toList();
    }

    private static void collectFullDirection(
            ServerLevel level,
            BlockPos originPos,
            AxisChoice axis,
            int direction,
            List<BlockPos> positions
    ) {
        for (int distance = 1; distance <= 32; distance++) {
            BlockPos checkPos;

            if (axis == AxisChoice.X) {
                checkPos = originPos.offset(direction * distance, 0, 0);
            } else {
                checkPos = originPos.offset(0, 0, direction * distance);
            }

            if (!isTrough(level, checkPos)) {
                return;
            }

            positions.add(checkPos.immutable());
        }
    }

    private static TroughBlockEntity getControllerTrough(
            ServerLevel level,
            TroughUtil.TroughGroup group,
            TroughBlockEntity fallback
    ) {
        BlockEntity controllerEntity = level.getBlockEntity(group.controllerPos());

        if (controllerEntity instanceof TroughBlockEntity controllerTrough) {
            return controllerTrough;
        }

        return fallback;
    }

    private static void applyChunkedVisualLine(ServerLevel level, List<BlockPos> line, Direction facing) {
        line = sortForFacing(line, facing);

        for (int batchStart = 0; batchStart < line.size(); batchStart += 4) {
            int batchEnd = Math.min(batchStart + 4, line.size());
            List<BlockPos> batch = line.subList(batchStart, batchEnd);

            applyVisualBatch(level, batch, facing);
        }
    }

    private static void applyVisualBatch(ServerLevel level, List<BlockPos> batch, Direction facing) {
        if (batch.isEmpty()) {
            return;
        }

        if (batch.size() == 1) {
            BlockPos pos = batch.get(0);
            Direction oldFacing = getExistingFacing(level, pos);
            setVisualState(level, pos, oldFacing, TroughPart.SINGLE);
            return;
        }

        for (int index = 0; index < batch.size(); index++) {
            BlockPos pos = batch.get(index);

            TroughPart part;

            if (index == 0) {
                part = TroughPart.LEFT;
            } else if (index == batch.size() - 1) {
                part = TroughPart.RIGHT;
            } else {
                part = TroughPart.MIDDLE;
            }

            setVisualState(level, pos, facing, part);
        }
    }

    private static Direction getFacingForLine(List<BlockPos> line) {
        if (line.size() < 2) {
            return Direction.NORTH;
        }

        BlockPos first = line.get(0);
        BlockPos last = line.get(line.size() - 1);

        if (first.getX() != last.getX()) {
            return Direction.EAST;
        }

        return Direction.SOUTH;
    }

    private static Direction getExistingFacing(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.hasProperty(FACING)) {
            return state.getValue(FACING);
        }

        return Direction.NORTH;
    }

    private static List<BlockPos> sortForFacing(List<BlockPos> positions, Direction facing) {
        if (facing.getAxis() == Direction.Axis.X) {
            return positions.stream()
                    .sorted((first, second) -> Integer.compare(first.getX(), second.getX()))
                    .toList();
        }

        return positions.stream()
                .sorted((first, second) -> Integer.compare(first.getZ(), second.getZ()))
                .toList();
    }

    private static int compareBlockPos(BlockPos first, BlockPos second) {
        if (first.getY() != second.getY()) {
            return Integer.compare(first.getY(), second.getY());
        }

        if (first.getX() != second.getX()) {
            return Integer.compare(first.getX(), second.getX());
        }

        return Integer.compare(first.getZ(), second.getZ());
    }

    private static void setVisualState(ServerLevel level, BlockPos pos, Direction facing, TroughPart part) {
        BlockState oldState = level.getBlockState(pos);

        if (!oldState.is(ModBlocks.TROUGH)) {
            return;
        }

        BlockState newState = oldState
                .setValue(FACING, facing)
                .setValue(PART, part);

        if (!oldState.equals(newState)) {
            level.setBlock(pos, newState, 3);
        }
    }

    private static boolean isTrough(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.TROUGH);
    }

    private enum AxisChoice {
        X,
        Z
    }

    private static void showSavedFenceArea(ServerLevel level, TroughBlockEntity trough) {
        int y = trough.getBlockPos().getY() + 1;

        for (int x = trough.getFenceMinX(); x <= trough.getFenceMaxX(); x++) {
            spawnRangeParticle(level, x, y, trough.getFenceMinZ());
            spawnRangeParticle(level, x, y, trough.getFenceMaxZ());
        }

        for (int z = trough.getFenceMinZ(); z <= trough.getFenceMaxZ(); z++) {
            spawnRangeParticle(level, trough.getFenceMinX(), y, z);
            spawnRangeParticle(level, trough.getFenceMaxX(), y, z);
        }
    }

    private static void showFoldRange(ServerLevel level, BlockPos centerPos, int range) {
        int y = centerPos.getY() + 1;

        for (int dx = -range; dx <= range; dx++) {
            spawnRangeParticle(level, centerPos.getX() + dx, y, centerPos.getZ() - range);
            spawnRangeParticle(level, centerPos.getX() + dx, y, centerPos.getZ() + range);
        }

        for (int dz = -range; dz <= range; dz++) {
            spawnRangeParticle(level, centerPos.getX() - range, y, centerPos.getZ() + dz);
            spawnRangeParticle(level, centerPos.getX() + range, y, centerPos.getZ() + dz);
        }
    }

    private static void spawnRangeParticle(ServerLevel level, int x, int y, int z) {
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

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack destroyedWith
    ) {
        if (blockEntity instanceof TroughBlockEntity trough && trough.getWheatCount() > 0) {
            Block.popResource(
                    level,
                    pos,
                    new ItemStack(Items.WHEAT, trough.getWheatCount())
            );
        }

        super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (oldState.is(state.getBlock())) {
            return;
        }

        refreshVisualClusterAround(serverLevel, pos);

        BlockEntity blockEntity = serverLevel.getBlockEntity(pos);

        if (blockEntity instanceof TroughBlockEntity trough) {
            trough.refreshFenceArea(serverLevel);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston
    ) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);

        refreshVisualClusterAround(level, pos);
    }

    private static void refreshVisualClusterAround(ServerLevel level, BlockPos originPos) {
        refreshVisualGroup(level, originPos);

        for (int offset = -8; offset <= 8; offset++) {
            BlockPos xPos = originPos.offset(offset, 0, 0);
            BlockPos zPos = originPos.offset(0, 0, offset);

            if (isTrough(level, xPos)) {
                refreshVisualGroup(level, xPos);
            }

            if (isTrough(level, zPos)) {
                refreshVisualGroup(level, zPos);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TroughBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntityTicker getTicker(
            Level level,
            BlockState state,
            BlockEntityType blockEntityType
    ) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.TROUGH,
                TroughBlockEntity::serverTick
        );
    }
}