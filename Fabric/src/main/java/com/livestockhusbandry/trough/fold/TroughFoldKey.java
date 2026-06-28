package com.livestockhusbandry.trough.fold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record TroughFoldKey(
        ResourceKey<Level> dimension,
        int y,
        int minX,
        int minZ,
        int maxX,
        int maxZ
) {
}