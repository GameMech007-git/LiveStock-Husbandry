package com.livestockhusbandry.ai.trough;

import com.livestockhusbandry.trough.fold.TroughFoldKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TroughPendingBabyRegistry {

    private static final Map<PendingPairKey, PendingBabyRegistration> PENDING_BABIES =
            new HashMap<>();

    private TroughPendingBabyRegistry() {
    }

    public static void add(
            ResourceKey<Level> dimension,
            Animal firstParent,
            Animal secondParent,
            TroughFoldKey foldKey,
            long expireGameTime
    ) {
        PENDING_BABIES.put(
                PendingPairKey.of(
                        dimension,
                        firstParent.getUUID(),
                        secondParent.getUUID()
                ),
                new PendingBabyRegistration(
                        dimension,
                        foldKey,
                        expireGameTime
                )
        );
    }

    @Nullable
    public static PendingBabyRegistration get(
            ResourceKey<Level> dimension,
            Animal firstParent,
            Animal secondParent
    ) {
        return PENDING_BABIES.get(
                PendingPairKey.of(
                        dimension,
                        firstParent.getUUID(),
                        secondParent.getUUID()
                )
        );
    }

    public static void remove(
            ResourceKey<Level> dimension,
            Animal firstParent,
            Animal secondParent
    ) {
        PENDING_BABIES.remove(
                PendingPairKey.of(
                        dimension,
                        firstParent.getUUID(),
                        secondParent.getUUID()
                )
        );
    }

    public static boolean isValid(
            PendingBabyRegistration pending,
            ResourceKey<Level> dimension,
            long gameTime
    ) {
        return pending.dimension().equals(dimension)
                && gameTime <= pending.expireGameTime();
    }

    public record PendingBabyRegistration(
            ResourceKey<Level> dimension,
            TroughFoldKey foldKey,
            long expireGameTime
    ) {
    }

    private record PendingPairKey(
            ResourceKey<Level> dimension,
            UUID firstParent,
            UUID secondParent
    ) {
        private static PendingPairKey of(
                ResourceKey<Level> dimension,
                UUID firstParent,
                UUID secondParent
        ) {
            if (firstParent.compareTo(secondParent) <= 0) {
                return new PendingPairKey(dimension, firstParent, secondParent);
            }

            return new PendingPairKey(dimension, secondParent, firstParent);
        }
    }
}