package com.livestockhusbandry.ai.shepherd;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.entity.ShepherdRackBlockEntity;
import com.livestockhusbandry.ai.sheep.SheepTroughReservations;
import com.livestockhusbandry.ai.sheep.SheepTroughUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.timeline.Timeline;
import net.minecraft.world.timeline.Timelines;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShepherdShearingManager {

    private static final int TICK_INTERVAL = 20;
    private static final int SEARCH_RANGE = 12;
    private static final int RACK_RANGE = 10;

    private static final double RACK_DISTANCE_SQR = 4.0D;
    private static final double SHEAR_DISTANCE_SQR = 3.0D;
    private static final double WOOL_PICKUP_RANGE = 5.0D;

    private static final int MAX_SHEARS_PER_DAY = 3;
    private static final long REST_AFTER_SHEAR_TICKS = 2000L;

    private static final Map<ShepherdKey, ShepherdWorkState> WORK_STATES = new HashMap<>();

    private ShepherdShearingManager() {
    }

    public static void tick(ServerLevel level, Villager villager) {
        if (villager.tickCount % TICK_INTERVAL != 0) {
            return;
        }

        if (!isVanillaShepherd(villager)) {
            return;
        }

        if (villager.isBaby() || villager.isSleeping() || villager.isTrading()) {
            return;
        }

        ShepherdRackBlockEntity rack = findNearestRack(level, villager.blockPosition());

        if (rack == null) {
            return;
        }

        BlockPos rackPos = rack.getBlockPos();
        boolean nearRack = villager.blockPosition().distSqr(rackPos) <= RACK_DISTANCE_SQR;

        if (nearRack) {
            depositWoolIntoRack(villager, rack);
        }

        if (hasWoolInInventory(villager)) {
            if (!nearRack) {
                moveToBlock(villager, rackPos);
            }

            return;
        }

        if (!isWorking(villager)) {
            return;
        }

        pickupNearbyWool(level, villager);

        if (hasWoolInInventory(villager)) {
            moveToBlock(villager, rackPos);
            return;
        }

        if (!canShearNow(level, villager)) {
            return;
        }

        Sheep targetSheep = findNearestShearableRegisteredSheep(level, villager);

        if (targetSheep == null) {
            return;
        }

        if (villager.distanceToSqr(targetSheep) > SHEAR_DISTANCE_SQR) {
            villager.getNavigation().moveTo(targetSheep, 0.55D);
            villager.getLookControl().setLookAt(targetSheep);
            return;
        }

        villager.getNavigation().stop();
        villager.getLookControl().setLookAt(targetSheep);

        targetSheep.shear(level, SoundSource.NEUTRAL, ItemStack.EMPTY);
        markSheared(level, villager);

        pickupNearbyWool(level, villager);

        if (hasWoolInInventory(villager)) {
            moveToBlock(villager, rackPos);
        }

        level.broadcastEntityEvent(villager, (byte) 14);
    }

    private static boolean isVanillaShepherd(Villager villager) {
        return villager.getVillagerData()
                .profession()
                .is(VillagerProfession.SHEPHERD);
    }

    private static boolean isWorking(Villager villager) {
        return villager.getBrain().isActive(Activity.WORK);
    }

    @Nullable
    private static ShepherdRackBlockEntity findNearestRack(ServerLevel level, BlockPos villagerPos) {
        ShepherdRackBlockEntity bestRack = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                villagerPos.offset(-RACK_RANGE, -2, -RACK_RANGE),
                villagerPos.offset(RACK_RANGE, 2, RACK_RANGE)
        )) {
            if (!level.getBlockState(pos).is(ModBlocks.SHEPHERD_RACK)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (!(blockEntity instanceof ShepherdRackBlockEntity rack)) {
                continue;
            }

            double distance = pos.distSqr(villagerPos);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestRack = rack;
            }
        }

        return bestRack;
    }

    private static void moveToBlock(Villager villager, BlockPos pos) {
        villager.getNavigation().moveTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                0.55D
        );

        villager.getLookControl().setLookAt(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );
    }

    private static boolean hasWoolInInventory(Villager villager) {
        SimpleContainer inventory = villager.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (ShepherdRackBlockEntity.isWool(inventory.getItem(slot))) {
                return true;
            }
        }

        return false;
    }

    private static void depositWoolIntoRack(Villager villager, ShepherdRackBlockEntity rack) {
        SimpleContainer inventory = villager.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (!ShepherdRackBlockEntity.isWool(stack)) {
                continue;
            }

            int inserted = rack.insertWool(stack);

            if (inserted <= 0) {
                continue;
            }

            stack.shrink(inserted);

            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            } else {
                inventory.setItem(slot, stack);
            }
        }

        inventory.setChanged();
    }

    private static void pickupNearbyWool(ServerLevel level, Villager villager) {
        AABB box = villager.getBoundingBox().inflate(
                WOOL_PICKUP_RANGE,
                1.5D,
                WOOL_PICKUP_RANGE
        );

        List<ItemEntity> woolDrops = level.getEntitiesOfClass(
                ItemEntity.class,
                box,
                itemEntity -> !itemEntity.isRemoved()
                        && ShepherdRackBlockEntity.isWool(itemEntity.getItem())
        );

        woolDrops.sort(Comparator.comparingDouble(villager::distanceToSqr));

        for (ItemEntity itemEntity : woolDrops) {
            ItemStack stack = itemEntity.getItem();
            ItemStack leftover = insertIntoVillagerInventory(villager, stack.copy());

            int inserted = stack.getCount() - leftover.getCount();

            if (inserted <= 0) {
                continue;
            }

            stack.shrink(inserted);

            if (stack.isEmpty()) {
                itemEntity.discard();
            }

            return;
        }
    }

    private static ItemStack insertIntoVillagerInventory(Villager villager, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        SimpleContainer inventory = villager.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack existing = inventory.getItem(slot);

            if (existing.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                continue;
            }

            int room = existing.getMaxStackSize() - existing.getCount();

            if (room <= 0) {
                continue;
            }

            int moved = Math.min(room, stack.getCount());
            existing.grow(moved);
            stack.shrink(moved);
            inventory.setItem(slot, existing);

            if (stack.isEmpty()) {
                inventory.setChanged();
                return ItemStack.EMPTY;
            }
        }

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack existing = inventory.getItem(slot);

            if (!existing.isEmpty()) {
                continue;
            }

            int moved = Math.min(stack.getMaxStackSize(), stack.getCount());
            ItemStack inserted = stack.copyWithCount(moved);
            inventory.setItem(slot, inserted);
            stack.shrink(moved);

            if (stack.isEmpty()) {
                inventory.setChanged();
                return ItemStack.EMPTY;
            }
        }

        inventory.setChanged();
        return stack;
    }

    @Nullable
    private static Sheep findNearestShearableRegisteredSheep(ServerLevel level, Villager villager) {
        AABB searchBox = villager.getBoundingBox().inflate(
                SEARCH_RANGE,
                4.0D,
                SEARCH_RANGE
        );

        List<Sheep> candidates = level.getEntitiesOfClass(
                Sheep.class,
                searchBox,
                sheep -> isValidTargetSheep(level, sheep)
        );

        return candidates.stream()
                .min(Comparator.comparingDouble(villager::distanceToSqr))
                .orElse(null);
    }

    private static boolean isValidTargetSheep(ServerLevel level, Sheep sheep) {
        if (!sheep.isAlive()) {
            return false;
        }

        if (sheep.isBaby()) {
            return false;
        }

        if (!sheep.readyForShearing()) {
            return false;
        }

        if (SheepTroughReservations.getRegisteredFoldKey(
                level,
                sheep.getUUID()
        ) == null) {
            return false;
        }

        var fold = SheepTroughUtil.getRegisteredFold(sheep);

        if (fold == null) {
            return false;
        }

        return SheepTroughUtil.isInsideTroughArea(sheep, fold);
    }

    private static boolean canShearNow(ServerLevel level, Villager villager) {
        ShepherdWorkState state = getWorkState(level, villager);

        if (state.shearsToday >= MAX_SHEARS_PER_DAY) {
            return false;
        }

        return level.getGameTime() >= state.cooldownUntilGameTime;
    }

    private static void markSheared(ServerLevel level, Villager villager) {
        ShepherdWorkState state = getWorkState(level, villager);

        state.shearsToday++;
        state.cooldownUntilGameTime = level.getGameTime() + REST_AFTER_SHEAR_TICKS;
    }

    private static ShepherdWorkState getWorkState(ServerLevel level, Villager villager) {
        ShepherdKey key = new ShepherdKey(level.dimension(), villager.getUUID());
        ShepherdWorkState state = WORK_STATES.get(key);

        long currentDay = getCurrentDay(level);

        if (state == null) {
            state = new ShepherdWorkState(currentDay);
            WORK_STATES.put(key, state);
            return state;
        }

        if (state.day != currentDay) {
            state.day = currentDay;
            state.shearsToday = 0;
            state.cooldownUntilGameTime = 0L;
        }

        return state;
    }

    private static long getCurrentDay(ServerLevel level) {
        return level.registryAccess()
                .get(Timelines.OVERWORLD_DAY)
                .map(timeline -> (long) ((Timeline) timeline.value()).getPeriodCount(level.clockManager()))
                .orElse(level.getGameTime() / 24000L);
    }

    private record ShepherdKey(ResourceKey<Level> dimension, UUID villagerId) {
    }

    private static final class ShepherdWorkState {
        private long day;
        private int shearsToday;
        private long cooldownUntilGameTime;

        private ShepherdWorkState(long day) {
            this.day = day;
        }
    }
}