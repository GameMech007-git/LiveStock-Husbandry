package com.livestockhusbandry;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.entity.ModBlockEntities;
import com.livestockhusbandry.villager.LivestockPoiHelper;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveStockHusbandry implements ModInitializer {
	public static final String MOD_ID = "livestockhusbandry";


	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.registerModBlocks();
		LivestockPoiHelper.register();
		ModBlockEntities.registerModBlockEntities();

		LOGGER.info("LiveStock & Husbandry Initialized!");
	}
}