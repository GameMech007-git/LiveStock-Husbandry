package com.livestockhusbandry.ai.butcher;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.entity.ButcherRackBlockEntity;
import com.livestockhusbandry.mixin.LivingEntityAccessor;
import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.ai.cow.CowTroughReservations;
import com.livestockhusbandry.ai.cow.CowTroughUtil;
import com.livestockhusbandry.ai.pig.PigTroughReservations;
import com.livestockhusbandry.ai.pig.PigTroughUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.timeline.Timelines;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ButcherWorkManager {

    private static final int TICK_INTERVAL = 20;
    private static final int SEARCH_RANGE = 14;
    private static final int RACK_RANGE = 10;

    private static final double BUTCHER_DISTANCE_SQR = 3.0D;

    private static final int MAX_BUTCHERS_PER_DAY = 6;
    private static final long REST_AFTER_BUTCHER_TICKS = 1000L;

    private static final int NAME_SCAN_RANGE = 16;
    private static final long NAME_SCAN_INTERVAL_TICKS = 1000L;
    private static final int MAX_NAMES_PER_SCAN = 8;

    private static final Map<ButcherKey, ButcherWorkState> WORK_STATES = new HashMap<>();

    private ButcherWorkManager() {
    }

    public static void tick(ServerLevel level, Villager villager) {
        if (villager.tickCount % TICK_INTERVAL != 0) {
            return;
        }

        if (!isVanillaButcher(villager)) {
            return;
        }

        if (villager.tickCount % NAME_SCAN_INTERVAL_TICKS == 0) {
            nameNearbyRegisteredLivestock(level, villager);
        }

        if (villager.isBaby() || villager.isSleeping() || villager.isTrading()) {
            return;
        }

        ButcherRackBlockEntity rack = findNearestRack(level, villager.blockPosition());

        if (rack == null) {
            return;
        }

        BlockPos rackPos = rack.getBlockPos();

        if (!isWorking(villager)) {
            return;
        }

        if (!canButcherNow(level, villager)) {
            return;
        }

        Cow cow = findNearestSurplusCow(level, villager);
        Pig pig = findNearestSurplusPig(level, villager);

        Object target = chooseNearestTarget(villager, cow, pig);

        if (target instanceof Cow targetCow) {
            handleCowTarget(level, villager, rack, rackPos, targetCow);
            return;
        }

        if (target instanceof Pig targetPig) {
            handlePigTarget(level, villager, rack, rackPos, targetPig);
        }

    }

    private static void handleCowTarget(
            ServerLevel level,
            Villager villager,
            ButcherRackBlockEntity rack,
            BlockPos rackPos,
            Cow cow
    ) {
        if (villager.distanceToSqr(cow) > BUTCHER_DISTANCE_SQR) {
            villager.getNavigation().moveTo(cow, 0.55D);
            villager.getLookControl().setLookAt(cow);
            return;
        }

        villager.getNavigation().stop();
        villager.getLookControl().setLookAt(cow);

        CowTroughReservations.unregister(level, cow.getUUID());

        butcherWithVanillaLoot(level, villager, cow, rack);

        markButchered(level, villager);
        moveToBlock(villager, rackPos);

        level.broadcastEntityEvent(villager, (byte) 14);
    }

    private static void handlePigTarget(
            ServerLevel level,
            Villager villager,
            ButcherRackBlockEntity rack,
            BlockPos rackPos,
            Pig pig
    ) {
        if (villager.distanceToSqr(pig) > BUTCHER_DISTANCE_SQR) {
            villager.getNavigation().moveTo(pig, 0.55D);
            villager.getLookControl().setLookAt(pig);
            return;
        }

        villager.getNavigation().stop();
        villager.getLookControl().setLookAt(pig);

        PigTroughReservations.unregister(level, pig.getUUID());

        butcherWithVanillaLoot(level, villager, pig, rack);

        markButchered(level, villager);
        moveToBlock(villager, rackPos);

        level.broadcastEntityEvent(villager, (byte) 14);
    }

    @Nullable
    private static Object chooseNearestTarget(Villager villager, @Nullable Cow cow, @Nullable Pig pig) {
        if (cow == null) {
            return pig;
        }

        if (pig == null) {
            return cow;
        }

        if (villager.distanceToSqr(cow) <= villager.distanceToSqr(pig)) {
            return cow;
        }

        return pig;
    }

    private static boolean isVanillaButcher(Villager villager) {
        return villager.getVillagerData()
                .profession()
                .is(VillagerProfession.BUTCHER);
    }

    private static boolean isWorking(Villager villager) {
        return villager.getBrain().isActive(Activity.WORK);
    }

    @Nullable
    private static ButcherRackBlockEntity findNearestRack(ServerLevel level, BlockPos villagerPos) {
        ButcherRackBlockEntity bestRack = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                villagerPos.offset(-RACK_RANGE, -2, -RACK_RANGE),
                villagerPos.offset(RACK_RANGE, 2, RACK_RANGE)
        )) {
            if (!level.getBlockState(pos).is(ModBlocks.BUTCHER_RACK)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (!(blockEntity instanceof ButcherRackBlockEntity rack)) {
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

    @Nullable
    private static Cow findNearestSurplusCow(ServerLevel level, Villager villager) {
        AABB searchBox = villager.getBoundingBox().inflate(
                SEARCH_RANGE,
                4.0D,
                SEARCH_RANGE
        );

        List<Cow> candidates = level.getEntitiesOfClass(
                Cow.class,
                searchBox,
                cow -> isValidCowTarget(level, cow)
        );

        return candidates.stream()
                .min(Comparator.comparingDouble(villager::distanceToSqr))
                .orElse(null);
    }

    @Nullable
    private static Pig findNearestSurplusPig(ServerLevel level, Villager villager) {
        AABB searchBox = villager.getBoundingBox().inflate(
                SEARCH_RANGE,
                4.0D,
                SEARCH_RANGE
        );

        List<Pig> candidates = level.getEntitiesOfClass(
                Pig.class,
                searchBox,
                pig -> isValidPigTarget(level, pig)
        );

        return candidates.stream()
                .min(Comparator.comparingDouble(villager::distanceToSqr))
                .orElse(null);
    }

    private static boolean isValidCowTarget(ServerLevel level, Cow cow) {
        if (!cow.isAlive() || cow.isBaby()) {
            return false;
        }

        if (cow.hasCustomName()) {
            return false;
        }

        TroughFold fold = CowTroughUtil.getRegisteredFold(cow);

        if (fold == null) {
            return false;
        }

        if (!CowTroughUtil.isInsideTroughArea(cow, fold)) {
            return false;
        }

        int registered = CowTroughReservations.getRegisteredCount(level, fold.key());

        return registered > fold.stableLargeAnimalLimit();
    }

    private static boolean isValidPigTarget(ServerLevel level, Pig pig) {
        if (!pig.isAlive() || pig.isBaby()) {
            return false;
        }

        if (pig.hasCustomName()) {
            return false;
        }

        TroughFold fold = PigTroughUtil.getRegisteredFold(pig);

        if (fold == null) {
            return false;
        }

        if (!PigTroughUtil.isInsideTroughArea(pig, fold)) {
            return false;
        }

        int registered = PigTroughReservations.getRegisteredCount(level, fold.key());

        return registered > fold.stableLargeAnimalLimit();
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

    private static boolean canButcherNow(ServerLevel level, Villager villager) {
        ButcherWorkState state = getWorkState(level, villager);

        if (state.butchersToday >= MAX_BUTCHERS_PER_DAY) {
            return false;
        }

        return level.getGameTime() >= state.cooldownUntilGameTime;
    }

    private static void markButchered(ServerLevel level, Villager villager) {
        ButcherWorkState state = getWorkState(level, villager);

        state.butchersToday++;
        state.cooldownUntilGameTime = level.getGameTime() + REST_AFTER_BUTCHER_TICKS;
    }

    private static ButcherWorkState getWorkState(ServerLevel level, Villager villager) {
        ButcherKey key = new ButcherKey(level.dimension(), villager.getUUID());
        ButcherWorkState state = WORK_STATES.get(key);

        long currentDay = getCurrentDay(level);

        if (state == null) {
            state = new ButcherWorkState(currentDay);
            WORK_STATES.put(key, state);
            return state;
        }

        if (state.day != currentDay) {
            state.day = currentDay;
            state.butchersToday = 0;
            state.cooldownUntilGameTime = 0L;
        }

        return state;
    }

    private static long getCurrentDay(ServerLevel level) {
        return level.registryAccess()
                .get(Timelines.OVERWORLD_DAY)
                .map(timeline -> (long) timeline.value().getPeriodCount(level.clockManager()))
                .orElse(level.getGameTime() / 24000L);
    }

    private record ButcherKey(ResourceKey<Level> dimension, UUID villagerId) {
    }

    private static final class ButcherWorkState {
        private long day;
        private int butchersToday;
        private long cooldownUntilGameTime;

        private ButcherWorkState(long day) {
            this.day = day;
        }
    }

    private static void nameNearbyRegisteredLivestock(ServerLevel level, Villager butcher) {
        AABB searchBox = butcher.getBoundingBox().inflate(
                NAME_SCAN_RANGE,
                4.0D,
                NAME_SCAN_RANGE
        );

        int named = 0;

        List<Cow> cows = level.getEntitiesOfClass(
                Cow.class,
                searchBox,
                cow -> cow.isAlive()
                        && !cow.isBaby()
                        && !cow.hasCustomName()
                        && shouldReceiveButcherName(cow)
                        && CowTroughUtil.getRegisteredFold(cow) != null
        );

        for (Cow cow : cows) {
            cow.setCustomName(Component.literal(
                    ButcherLivestockNamePool.randomName(level, cow)
            ));
            cow.setCustomNameVisible(true);

            named++;

            if (named >= MAX_NAMES_PER_SCAN) {
                return;
            }
        }

        List<Pig> pigs = level.getEntitiesOfClass(
                Pig.class,
                searchBox,
                pig -> pig.isAlive()
                        && !pig.isBaby()
                        && !pig.hasCustomName()
                        && shouldReceiveButcherName(pig)
                        && PigTroughUtil.getRegisteredFold(pig) != null
        );

        for (Pig pig : pigs) {
            pig.setCustomName(Component.literal(
                    ButcherLivestockNamePool.randomName(level, pig)
            ));
            pig.setCustomNameVisible(true);

            named++;

            if (named >= MAX_NAMES_PER_SCAN) {
                return;
            }
        }
    }

    private static void butcherWithVanillaLoot(
            ServerLevel level,
            Villager villager,
            LivingEntity animal,
            ButcherRackBlockEntity rack
    ) {
        BlockPos dropPos = animal.blockPosition();

        DamageSource source = level.damageSources().mobAttack(villager);

        ((LivingEntityAccessor) animal).livestockhusbandry$dropAllDeathLoot(
                level,
                source
        );

        animal.discard();

        pickupNearbyButcherDrops(level, rack, dropPos);
    }

    private static void pickupNearbyButcherDrops(
            ServerLevel level,
            ButcherRackBlockEntity rack,
            BlockPos centerPos
    ) {
        AABB box = new AABB(centerPos).inflate(
                4.0D,
                2.0D,
                4.0D
        );

        List<ItemEntity> drops = level.getEntitiesOfClass(
                ItemEntity.class,
                box,
                itemEntity -> !itemEntity.isRemoved()
                        && ButcherRackBlockEntity.isButcherDrop(itemEntity.getItem())
        );

        drops.sort(
                Comparator.comparingDouble(
                        itemEntity -> itemEntity.blockPosition().distSqr(centerPos)
                )
        );

        for (ItemEntity itemEntity : drops) {
            ItemStack stack = itemEntity.getItem();

            int inserted = rack.insertButcherDrop(stack);

            if (inserted <= 0) {
                continue;
            }

            stack.shrink(inserted);

            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }
        }
    }

    private static boolean shouldReceiveButcherName(LivingEntity animal) {
        return Math.floorMod(animal.getUUID().hashCode(), 100) < 50;
    }
}