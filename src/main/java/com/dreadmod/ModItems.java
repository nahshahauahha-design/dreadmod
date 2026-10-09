package com.dreadmod;

import com.dreadmod.item.HolyCharmItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final Item HOLY_CHARM = register("holy_charm",
            new HolyCharmItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));

    public static final Item NIGHTMARE_SHARD = register("nightmare_shard",
            new Item(new FabricItemSettings().rarity(Rarity.UNCOMMON)));

    public static final Item STALKER_SPAWN_EGG = register("stalker_spawn_egg",
            new SpawnEggItem(ModEntities.STALKER, 0x0A0A0A, 0xB00000, new FabricItemSettings()));

    private ModItems() {}

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(DreadMod.MOD_ID, name), item);
    }

    public static void register() {
        // Il caricamento della classe registra gli oggetti.
    }
}
