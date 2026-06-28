package com.livestockhusbandry.trough.fold;

import com.livestockhusbandry.trough.TroughAnimalType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public record TroughFold(
        ResourceKey<Level> dimension,
        int minX,
        int minZ,
        int maxX,
        int maxZ,
        List<BlockPos> troughPositions,
        TroughAnimalType animalType
) {
    public TroughFoldKey key() {
        return new TroughFoldKey(
                dimension,
                troughY(),
                minX,
                minZ,
                maxX,
                maxZ
        );
    }

    public int troughY() {
        if (troughPositions.isEmpty()) {
            return 0;
        }

        return troughPositions.get(0).getY();
    }

    public int troughCount() {
        return troughPositions.size();
    }

    public int sheepLimit() {
        return troughCount();
    }

    public int stableLargeAnimalLimit() {
        return troughCount();
    }

    public int extraLargeAnimalLimit() {
        return troughCount() / 4 + 2;
    }

    public int breedingLargeAnimalLimit() {
        return stableLargeAnimalLimit() + extraLargeAnimalLimit();
    }

    public boolean contains(BlockPos pos) {
        return pos.getY() == troughY()
                && pos.getX() >= minX
                && pos.getX() <= maxX
                && pos.getZ() >= minZ
                && pos.getZ() <= maxZ;
    }

    public BlockPos centerPos() {
        return new BlockPos(
                (minX + maxX) / 2,
                troughY(),
                (minZ + maxZ) / 2
        );
    }
}