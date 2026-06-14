package com.livestockhusbandry.block;

import com.livestockhusbandry.LiveStockHusbandry;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public class ModBlocks {

    public static final Block CHICKEN_NEST = registerBlock(
            "chicken_nest",
            new ChickenNestBlock(
                    BlockBehaviour.Properties.of()
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    Identifier.fromNamespaceAndPath(LiveStockHusbandry.MOD_ID, "chicken_nest")
                            ))
                            .strength(0.4f)
                            .sound(SoundType.GRASS)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
            )
    );

    public static final Block COLLECTION_CRATE = registerBlock(
            "collection_crate",
            new Block(
                    BlockBehaviour.Properties.of()
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    Identifier.fromNamespaceAndPath(LiveStockHusbandry.MOD_ID, "collection_crate")
                            ))
                            .strength(2.0f, 3.0f)
                            .sound(SoundType.WOOD)
            )
    );

    private static Block registerBlock(String name, Block block) {
        Identifier id = Identifier.fromNamespaceAndPath(LiveStockHusbandry.MOD_ID, name);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Registry.register(
                BuiltInRegistries.BLOCK,
                id,
                block
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                id,
                new BlockItem(
                        block,
                        new Item.Properties().setId(itemKey)
                )
        );

        return block;
    }

    public static void registerModBlocks() {
        LiveStockHusbandry.LOGGER.info("Registering blocks for " + LiveStockHusbandry.MOD_ID);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(output -> {
                    output.accept(CHICKEN_NEST);
                    output.accept(COLLECTION_CRATE);
                });
    }
}