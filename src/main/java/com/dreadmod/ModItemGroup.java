package com.dreadmod;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroup {
    public static final ItemGroup DREAD = Registry.register(Registries.ITEM_GROUP,
            new Identifier(DreadMod.MOD_ID, "main"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.dreadmod.main"))
                    .icon(() -> new ItemStack(ModItems.HOLY_CHARM))
                    .entries((ctx, entries) -> {
                        entries.add(ModItems.HOLY_CHARM);
                        entries.add(ModItems.NIGHTMARE_SHARD);
                        entries.add(ModBlocks.CURSED_CANDLE);
                        entries.add(ModBlocks.TOMBSTONE);
                        entries.add(ModItems.STALKER_SPAWN_EGG);
                    })
                    .build());

    private ModItemGroup() {}

    public static void register() {
    }
}
