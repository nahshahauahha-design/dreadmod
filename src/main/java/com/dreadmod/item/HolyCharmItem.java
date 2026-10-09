package com.dreadmod.item;

import com.dreadmod.FearManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Amuleto Sacro: finché è nell'inventario dimezza l'accumulo di paura e gli Stalker
 * non ti prendono di mira. Con tasto destro: preghiera che calma (-35 paura, 60s di ricarica).
 */
public class HolyCharmItem extends Item {
    public HolyCharmItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            FearManager.add(sp, -35);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 0.8f, 0.8f);
            sp.sendMessage(Text.translatable("message.dreadmod.charm_prayer")
                    .formatted(Formatting.GOLD, Formatting.ITALIC), true);
        }
        user.getItemCooldownManager().set(this, 1200);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.dreadmod.holy_charm.tooltip.1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.dreadmod.holy_charm.tooltip.2").formatted(Formatting.DARK_GRAY));
    }
}
