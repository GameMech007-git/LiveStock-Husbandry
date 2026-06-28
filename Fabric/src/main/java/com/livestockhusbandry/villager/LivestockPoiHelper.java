package com.livestockhusbandry.villager;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.mixin.PoiTypesAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class LivestockPoiHelper {

    private LivestockPoiHelper() {
    }

    public static void register() {
        registerPoiForBlock(
                PoiTypes.SHEPHERD,
                ModBlocks.SHEPHERD_RACK
        );

        registerPoiForBlock(
                PoiTypes.BUTCHER,
                ModBlocks.BUTCHER_RACK
        );
    }

    private static void registerPoiForBlock(
            net.minecraft.resources.ResourceKey<PoiType> poiKey,
            Block block
    ) {
        Holder<PoiType> poi = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(
                poiKey
        );

        Map<BlockState, Holder<PoiType>> typeByState =
                PoiTypesAccessor.livestockhusbandry$getTypeByState();

        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            Holder<PoiType> existing = typeByState.get(state);

            if (existing != null) {
                continue;
            }

            typeByState.put(state, poi);
        }
    }
}