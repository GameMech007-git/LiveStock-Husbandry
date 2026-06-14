package com.livestockhusbandry.block.entity;

import com.livestockhusbandry.LiveStockHusbandry;
import com.livestockhusbandry.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static final BlockEntityType<CollectionCrateBlockEntity> COLLECTION_CRATE =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(
                            LiveStockHusbandry.MOD_ID,
                            "collection_crate"
                    ),
                    FabricBlockEntityTypeBuilder
                            .create(
                                    CollectionCrateBlockEntity::new,
                                    ModBlocks.COLLECTION_CRATE
                            )
                            .build()
            );

    public static void registerModBlockEntities() {
        LiveStockHusbandry.LOGGER.info(
                "Registering block entities for " + LiveStockHusbandry.MOD_ID
        );
    }
}