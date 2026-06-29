package com.livestockhusbandry.entity;

import com.livestockhusbandry.block.ChickenNestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CollectionCrateBlockEntity extends RandomizableContainerBlockEntity {

    private static final Component DEFAULT_NAME =
            Component.translatable("container.livestockhusbandry.collection_crate");

    private static final int SLOT_COUNT = 27;
    private static final int COLLECT_INTERVAL_TICKS = 10;

    private NonNullList<ItemStack> items =
            NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public CollectionCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLLECTION_CRATE, pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CollectionCrateBlockEntity crate
    ) {
        if (level.getGameTime() % COLLECT_INTERVAL_TICKS != 0) {
            return;
        }

        BlockPos nestPos = findNestAbove(level, pos);

        if (nestPos == null) {
            return;
        }

        AABB collectBox = new AABB(nestPos);

        List<ItemEntity> itemEntities = level.getEntitiesOfClass(
                ItemEntity.class,
                collectBox,
                Entity::isAlive
        );

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack remaining = crate.insertStack(itemEntity.getItem());

            if (remaining.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remaining);
            }
        }
    }

    @Nullable
    private static BlockPos findNestAbove(Level level, BlockPos cratePos) {
        BlockPos directlyAbove = cratePos.above();

        if (level.getBlockState(directlyAbove).getBlock() instanceof ChickenNestBlock) {
            return directlyAbove;
        }

        BlockPos twoAbove = cratePos.above(2);

        if (level.getBlockState(twoAbove).getBlock() instanceof ChickenNestBlock) {
            return twoAbove;
        }

        return null;
    }

    private ItemStack insertStack(ItemStack stack) {
        ItemStack remaining = stack.copy();

        for (int i = 0; i < items.size(); i++) {
            ItemStack slotStack = items.get(i);

            if (slotStack.isEmpty()) {
                int moveAmount = Math.min(
                        remaining.getCount(),
                        remaining.getMaxStackSize()
                );

                ItemStack inserted = remaining.copy();
                inserted.setCount(moveAmount);

                items.set(i, inserted);
                remaining.shrink(moveAmount);

                setChanged();

                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            } else if (ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();

                if (space > 0) {
                    int moveAmount = Math.min(space, remaining.getCount());

                    slotStack.grow(moveAmount);
                    remaining.shrink(moveAmount);

                    setChanged();

                    if (remaining.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        return remaining;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        if (!this.trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.items);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);

        if (!this.tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, this.items);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }
}