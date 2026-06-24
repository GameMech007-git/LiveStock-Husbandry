package com.livestockhusbandry;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.block.entity.ModBlockEntities;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveStockHusbandry implements ModInitializer {
	public static final String MOD_ID = "livestockhusbandry";


	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerModBlockEntities();

		LOGGER.info("LiveStock & Husbandry Initialized!");
	}
}