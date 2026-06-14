package com.livestockhusbandry.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChickenNestReservations {

    private static final Map<NestKey, UUID> RESERVED_NESTS = new HashMap<>();

    public static boolean reserve(Level level, BlockPos pos, UUID chickenId) {
        NestKey key = new NestKey(level.dimension(), pos.immutable());

        UUID current = RESERVED_NESTS.get(key);

        if (current == null || current.equals(chickenId)) {
            RESERVED_NESTS.put(key, chickenId);
            return true;
        }

        return false;
    }

    public static boolean isReservedByOther(Level level, BlockPos pos, UUID chickenId) {
        NestKey key = new NestKey(level.dimension(), pos.immutable());

        UUID current = RESERVED_NESTS.get(key);

        return current != null && !current.equals(chickenId);
    }

    @Nullable
    public static BlockPos getReservedNest(Level level, UUID chickenId) {
        for (Map.Entry<NestKey, UUID> entry : RESERVED_NESTS.entrySet()) {
            NestKey key = entry.getKey();
            UUID reservedBy = entry.getValue();

            if (key.dimension().equals(level.dimension()) && reservedBy.equals(chickenId)) {
                return key.pos();
            }
        }

        return null;
    }

    public static void release(Level level, BlockPos pos, UUID chickenId) {
        NestKey key = new NestKey(level.dimension(), pos.immutable());

        UUID current = RESERVED_NESTS.get(key);

        if (current != null && current.equals(chickenId)) {
            RESERVED_NESTS.remove(key);
        }
    }

    private record NestKey(ResourceKey<Level> dimension, BlockPos pos) {
    }
}