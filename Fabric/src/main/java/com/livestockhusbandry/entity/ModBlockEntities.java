package com.livestockhusbandry.entity;

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

    public static final BlockEntityType<ChickenFeederBlockEntity> CHICKEN_FEEDER =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(
                            LiveStockHusbandry.MOD_ID,
                            "chicken_feeder"
                    ),
                    FabricBlockEntityTypeBuilder
                            .create(
                                    ChickenFeederBlockEntity::new,
                                    ModBlocks.CHICKEN_FEEDER
                            )
                            .build()
            );

    public static final BlockEntityType<ShepherdRackBlockEntity> SHEPHERD_RACK =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(
                            LiveStockHusbandry.MOD_ID,
                            "shepherd_rack"
                    ),
                    FabricBlockEntityTypeBuilder
                            .create(
                                    ShepherdRackBlockEntity::new,
                                    ModBlocks.SHEPHERD_RACK
                            )
                            .build()
            );

    public static final BlockEntityType<TroughBlockEntity> TROUGH = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(
                    LiveStockHusbandry.MOD_ID,
                    "trough"
            ),
            FabricBlockEntityTypeBuilder
                    .create(
                            TroughBlockEntity::new,
                            ModBlocks.TROUGH
                    )
                    .build()
    );

    public static final BlockEntityType<ButcherRackBlockEntity> BUTCHER_RACK =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(
                            LiveStockHusbandry.MOD_ID,
                            "butcher_rack"
                    ),
                    FabricBlockEntityTypeBuilder
                            .create(
                                    ButcherRackBlockEntity::new,
                                    ModBlocks.BUTCHER_RACK
                            )
                            .build()
            );

    public static void registerModBlockEntities() {
        LiveStockHusbandry.LOGGER.info(
                "Registering block entities for " + LiveStockHusbandry.MOD_ID
        );
    }
}