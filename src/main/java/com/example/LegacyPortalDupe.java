package com.example;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyPortalDupe implements ModInitializer {

	public static final String MOD_ID = "legacyportaldupe";

	public static final Logger LOGGER =
			LoggerFactory.getLogger("Legacy Portal Dupe");

	@Override
	public void onInitialize() {
		LOGGER.info("Legacy Portal Dupe initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(
				MOD_ID,
				path
		);
	}
}