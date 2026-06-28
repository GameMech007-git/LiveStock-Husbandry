package com.livestockhusbandry.ai.chicken;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChickenNestEggManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("livestockhusbandry");

    private static final long TICKS_PER_DAY = 24000L;

    private static final long DAY_KEY_OFFSET = 12000L;

    private static final Map<UUID, Long> LAST_LAID_DAY_KEY = new HashMap<>();

    private ChickenNestEggManager() {
    }

    private static long getDayKey(ServerLevel level) {
        return Math.floorDiv(level.getOverworldClockTime() + DAY_KEY_OFFSET, TICKS_PER_DAY);
    }

    public static boolean tryLayEggsNow(ServerLevel level, Chicken chicken) {
        if (!ChickenNestUtil.isChickenRestTime(chicken)) {
            return false;
        }

        if (!ChickenNestUtil.isEarlyMorningLayTime(chicken)) {
            return false;
        }

        UUID chickenId = chicken.getUUID();
        long dayKey = getDayKey(level);
        Long lastLaidDayKey = LAST_LAID_DAY_KEY.get(chickenId);

        if (lastLaidDayKey != null && lastLaidDayKey.longValue() == dayKey) {
            return false;
        }

        LAST_LAID_DAY_KEY.put(chickenId, dayKey);

        return true;
    }
}