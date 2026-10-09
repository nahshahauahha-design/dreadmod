package com.dreadmod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DreadMod implements ModInitializer {
    public static final String MOD_ID = "dreadmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Pacchetto server -> client che trasporta il livello di paura (0-100). */
    public static final Identifier FEAR_PACKET = new Identifier(MOD_ID, "fear");

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModBlocks.register();
        ModItems.register();
        ModItemGroup.register();
        FearManager.init();
        DreadCommands.register();
        LOGGER.info("Dread caricata. Non guardarti alle spalle.");
    }
}
