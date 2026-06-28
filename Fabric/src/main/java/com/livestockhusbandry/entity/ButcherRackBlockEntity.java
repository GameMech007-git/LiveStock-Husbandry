package com.livestockhusbandry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ButcherRackBlockEntity extends RandomizableContainerBlockEntity {

    private static final Component DEFAULT_NAME =
            Component.translatable("container.livestockhusbandry.butcher_rack");

    private static final int SLOT_COUNT = 27;

    private NonNullList<ItemStack> items =
            NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public ButcherRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUTCHER_RACK, pos, state);
    }

    public int insertButcherDrop(ItemStack stack) {
        if (!isButcherDrop(stack)) {
            return 0;
        }

        ItemStack remaining = stack.copy();
        int originalCount = remaining.getCount();

        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack slotStack = this.items.get(slot);

            if (slotStack.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                continue;
            }

            int space = slotStack.getMaxStackSize() - slotStack.getCount();

            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining.getCount());
            slotStack.grow(moved);
            remaining.shrink(moved);
            setChanged();

            if (remaining.isEmpty()) {
                return originalCount;
            }
        }

        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack slotStack = this.items.get(slot);

            if (!slotStack.isEmpty()) {
                continue;
            }

            int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            ItemStack inserted = remaining.copyWithCount(moved);

            this.items.set(slot, inserted);
            remaining.shrink(moved);
            setChanged();

            if (remaining.isEmpty()) {
                return originalCount;
            }
        }

        return originalCount - remaining.getCount();
    }

    public static boolean isButcherDrop(ItemStack stack) {
        return stack.is(Items.BEEF)
                || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.LEATHER)
                || stack.is(Items.PORKCHOP)
                || stack.is(Items.COOKED_PORKCHOP);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isButcherDrop(stack);
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