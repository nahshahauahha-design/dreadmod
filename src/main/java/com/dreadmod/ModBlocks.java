package com.dreadmod;

import com.dreadmod.block.CursedCandleBlock;
import com.dreadmod.block.TombstoneBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final Block CURSED_CANDLE = registerBlock("cursed_candle",
            new CursedCandleBlock(FabricBlockSettings.create()
                    .mapColor(MapColor.PURPLE)
                    .strength(0.1f)
                    .sounds(BlockSoundGroup.CANDLE)
                    .luminance(state -> 12)
                    .nonOpaque()
                    .noCollision()));

    public static final Block TOMBSTONE = registerBlock("tombstone",
            new TombstoneBlock(FabricBlockSettings.create()
                    .mapColor(MapColor.STONE_GRAY)
                    .strength(2.0f, 6.0f)
                    .sounds(BlockSoundGroup.DEEPSLATE)));

    private ModBlocks() {}

    private static Block registerBlock(String name, Block block) {
        Identifier id = new Identifier(DreadMod.MOD_ID, name);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
        return Registry.register(Registries.BLOCK, id, block);
    }

    public static void register() {
        // Il caricamento della classe registra i blocchi.
    }
}
