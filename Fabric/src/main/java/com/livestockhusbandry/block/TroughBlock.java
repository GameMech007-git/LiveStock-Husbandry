package com.livestockhusbandry.block;

import com.livestockhusbandry.entity.ModBlockEntities;
import com.livestockhusbandry.entity.TroughBlockEntity;
import com.livestockhusbandry.trough.TroughAnimalType;
import com.livestockhusbandry.trough.TroughPart;
import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldUtil;
import com.livestockhusbandry.ai.cow.CowTroughReservations;
import com.livestockhusbandry.ai.pig.PigTroughReservations;
import com.livestockhusbandry.ai.sheep.SheepTroughReservations;
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

        if (!(blockEntity instanceof TroughBlockEntity clickedTrough)) {
            return InteractionResult.SUCCESS;
        }

        refreshVisualGroup(serverLevel, pos);

        ItemStack mainHandStack = player.getMainHandItem();

        if (player.isShiftKeyDown()) {
            TroughFold scannedFold = TroughFoldUtil.scanFromTrough(serverLevel, pos);

            if (scannedFold == null) {
                player.sendSystemMessage(
                        Component.literal(
                                "Nice trough. Now give it a fence so it can stop pretending to be a farm."
                        )
                );

                return InteractionResult.SUCCESS;
            }

            saveFoldAreaToAllTroughs(serverLevel, scannedFold);
            showFoldArea(serverLevel, scannedFold);

            player.sendSystemMessage(
                    Component.literal(
                            "Fold saved. "
                                    + scannedFold.troughCount()
                                    + " trough"
                                    + (scannedFold.troughCount() == 1 ? "" : "s")
                                    + " linked. Now this almost looks intentional."
                    )
            );

            return InteractionResult.SUCCESS;
        }

        TroughFold fold = TroughFoldUtil.getSavedFoldFromTrough(serverLevel, pos);

        if (mainHandStack.is(Items.WHEAT)
                || mainHandStack.is(Items.CARROT)
                || mainHandStack.is(Items.POTATO)
                || mainHandStack.is(Items.BEETROOT)) {
            if (!canInsertFeed(fold.animalType(), mainHandStack)) {
                player.sendSystemMessage(
                        Component.literal(
                                getWrongFeedMessage(fold.animalType(), mainHandStack)
                        )
                );

                return InteractionResult.SUCCESS;
            }
        }

        if (fold == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "This trough is off. Build a proper fence, then shift-click the trough. Revolutionary, I know."
                    )
            );

            return InteractionResult.SUCCESS;
        }

        if (mainHandStack.is(Items.WHEAT)) {
            int inserted = clickedTrough.insertWheat(mainHandStack.getCount());

            if (inserted > 0) {
                if (!player.getAbilities().instabuild) {
                    mainHandStack.shrink(inserted);
                }

                player.sendSystemMessage(
                        Component.literal(
                                "Inserted " + inserted
                                        + " wheat.\nFold wheat: "
                                        + TroughFoldUtil.getTotalWheat(serverLevel, fold)
                        )
                );
            } else {
                player.sendSystemMessage(Component.literal("This trough is full. Impressive problem to have."));
            }

            return InteractionResult.SUCCESS;
        }

        if (mainHandStack.is(Items.CARROT)) {
            int inserted = clickedTrough.insertCarrot(mainHandStack.getCount());

            if (inserted > 0) {
                if (!player.getAbilities().instabuild) {
                    mainHandStack.shrink(inserted);
                }

                player.sendSystemMessage(
                        Component.literal(
                                "Inserted " + inserted
                                        + " carrot"
                                        + (inserted == 1 ? "" : "s")
                                        + ".\nFold pig food: "
                                        + TroughFoldUtil.getTotalPigFood(serverLevel, fold)
                        )
                );
            } else {
                player.sendSystemMessage(Component.literal("This trough is full. Impressive problem to have."));
            }

            return InteractionResult.SUCCESS;
        }

        if (mainHandStack.is(Items.POTATO)) {
            int inserted = clickedTrough.insertPotato(mainHandStack.getCount());

            if (inserted > 0) {
                if (!player.getAbilities().instabuild) {
                    mainHandStack.shrink(inserted);
                }

                player.sendSystemMessage(
                        Component.literal(
                                "Inserted " + inserted
                                        + " potato"
                                        + (inserted == 1 ? "" : "es")
                                        + ".\nFold pig food: "
                                        + TroughFoldUtil.getTotalPigFood(serverLevel, fold)
                        )
                );
            } else {
                player.sendSystemMessage(Component.literal("This trough is full. Impressive problem to have."));
            }

            return InteractionResult.SUCCESS;
        }

        if (mainHandStack.is(Items.BEETROOT)) {
            int inserted = clickedTrough.insertBeetroot(mainHandStack.getCount());

            if (inserted > 0) {
                if (!player.getAbilities().instabuild) {
                    mainHandStack.shrink(inserted);
                }

                player.sendSystemMessage(
                        Component.literal(
                                "Inserted " + inserted
                                        + " beetroot"
                                        + (inserted == 1 ? "" : "s")
                                        + ".\nFold pig food: "
                                        + TroughFoldUtil.getTotalPigFood(serverLevel, fold)
                        )
                );
            } else {
                player.sendSystemMessage(Component.literal("This trough is full. Impressive problem to have."));
            }

            return InteractionResult.SUCCESS;
        }

        String animalText = getFoldAnimalText(serverLevel, fold);

        player.sendSystemMessage(
                Component.literal(
                        "Fold: " + animalText
                                + " | " + getFoldFeedText(serverLevel, fold)
                                + " | Troughs: " + fold.troughCount()
                                + " | Area: "
                                + fold.minX() + ", " + fold.minZ()
                                + " to "
                                + fold.maxX() + ", " + fold.maxZ()
                )
        );

        showFoldArea(serverLevel, fold);

        return InteractionResult.SUCCESS;
    }

    private static void saveFoldAreaToAllTroughs(ServerLevel level, TroughFold fold) {
        for (BlockPos troughPos : fold.troughPositions()) {
            BlockEntity blockEntity = level.getBlockEntity(troughPos);

            if (blockEntity instanceof TroughBlockEntity trough) {
                trough.saveFenceArea(
                        fold.minX(),
                        fold.minZ(),
                        fold.maxX(),
                        fold.maxZ()
                );
            }
        }
    }

    private static String getFoldFeedText(ServerLevel level, TroughFold fold) {
        TroughAnimalType animalType = fold.animalType();

        if (animalType == TroughAnimalType.PIG) {
            return "Pig food: " + TroughFoldUtil.getTotalPigFood(level, fold);
        }

        if (animalType == TroughAnimalType.SHEEP
                || animalType == TroughAnimalType.COW) {
            return "Wheat: " + TroughFoldUtil.getTotalWheat(level, fold);
        }

        return "Wheat: "
                + TroughFoldUtil.getTotalWheat(level, fold)
                + " | Pig food: "
                + TroughFoldUtil.getTotalPigFood(level, fold);
    }

    private static String getFoldAnimalText(ServerLevel level, TroughFold fold) {
        TroughAnimalType animalType = fold.animalType();

        if (animalType == TroughAnimalType.EMPTY) {
            return "Empty";
        }

        if (animalType == TroughAnimalType.SHEEP) {
            int registeredSheep = SheepTroughReservations.getRegisteredCount(
                    level,
                    fold.key()
            );

            return "Sheep: " + registeredSheep + "/" + fold.sheepLimit();
        }

        if (animalType == TroughAnimalType.COW) {
            int registeredCows = CowTroughReservations.getRegisteredCount(
                    level,
                    fold.key()
            );

            return "Cows: "
                    + registeredCows
                    + "/" + fold.breedingLargeAnimalLimit()
                    + " | Stable: "
                    + fold.stableLargeAnimalLimit();
        }

        if (animalType == TroughAnimalType.PIG) {
            int registeredPigs = PigTroughReservations.getRegisteredCount(
                    level,
                    fold.key()
            );

            return "Pigs: "
                    + registeredPigs
                    + "/" + fold.breedingLargeAnimalLimit()
                    + " | Stable: "
                    + fold.stableLargeAnimalLimit();
        }

        return animalType.name();
    }

    private static void showFoldArea(ServerLevel level, TroughFold fold) {
        int y = TroughFoldUtil.getPrimaryTroughPos(fold).getY() + 1;

        for (int x = fold.minX(); x <= fold.maxX(); x++) {
            spawnRangeParticle(level, x, y, fold.minZ());
            spawnRangeParticle(level, x, y, fold.maxZ());
        }

        for (int z = fold.minZ(); z <= fold.maxZ(); z++) {
            spawnRangeParticle(level, fold.minX(), y, z);
            spawnRangeParticle(level, fold.maxX(), y, z);
        }
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
        if (blockEntity instanceof TroughBlockEntity trough) {
            if (trough.getWheatCount() > 0) {
                Block.popResource(
                        level,
                        pos,
                        new ItemStack(Items.WHEAT, trough.getWheatCount())
                );
            }

            if (trough.getCarrotCount() > 0) {
                Block.popResource(
                        level,
                        pos,
                        new ItemStack(Items.CARROT, trough.getCarrotCount())
                );
            }

            if (trough.getPotatoCount() > 0) {
                Block.popResource(
                        level,
                        pos,
                        new ItemStack(Items.POTATO, trough.getPotatoCount())
                );
            }

            if (trough.getBeetrootCount() > 0) {
                Block.popResource(
                        level,
                        pos,
                        new ItemStack(Items.BEETROOT, trough.getBeetrootCount())
                );
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            unregisterAnimalsAssignedToBrokenTrough(serverLevel, pos);
        }

        super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
    }

    private static void unregisterAnimalsAssignedToBrokenTrough(ServerLevel level, BlockPos brokenTroughPos) {
        SheepTroughReservations.unregisterAssignedToTroughBlock(level, brokenTroughPos);
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
                ModBlockEntities.TROUGH,
                TroughBlockEntity::serverTick
        );
    }

    private static boolean canInsertFeed(TroughAnimalType animalType, ItemStack stack) {
        if (animalType == TroughAnimalType.EMPTY) {
            return stack.is(Items.WHEAT)
                    || stack.is(Items.CARROT)
                    || stack.is(Items.POTATO)
                    || stack.is(Items.BEETROOT);
        }

        if (animalType == TroughAnimalType.SHEEP
                || animalType == TroughAnimalType.COW) {
            return stack.is(Items.WHEAT);
        }

        if (animalType == TroughAnimalType.PIG) {
            return stack.is(Items.CARROT)
                    || stack.is(Items.POTATO)
                    || stack.is(Items.BEETROOT);
        }

        return false;
    }

    private static String getWrongFeedMessage(TroughAnimalType animalType, ItemStack stack) {
        if (animalType == TroughAnimalType.PIG && stack.is(Items.WHEAT)) {
            return "Pigs do not eat wheat. Try carrots, potatoes, or beetroots.";
        }

        if ((animalType == TroughAnimalType.SHEEP || animalType == TroughAnimalType.COW)
                && (stack.is(Items.CARROT)
                || stack.is(Items.POTATO)
                || stack.is(Items.BEETROOT))) {
            return "This fold wants wheat. Save the vegetables for pigs.";
        }

        return "That is not useful trough feed.";
    }
}