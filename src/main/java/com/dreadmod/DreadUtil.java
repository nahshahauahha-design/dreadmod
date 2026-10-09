package com.dreadmod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

public final class DreadUtil {
    private DreadUtil() {}

    /** True se il giocatore porta con sé un Amuleto Sacro (inventario o seconda mano). */
    public static boolean hasCharm(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack s : inv.main) {
            if (s.isOf(ModItems.HOLY_CHARM)) return true;
        }
        for (ItemStack s : inv.offHand) {
            if (s.isOf(ModItems.HOLY_CHARM)) return true;
        }
        return false;
    }

    /** Riproduce un suono SOLO per quel giocatore, in una posizione arbitraria (per le allucinazioni). */
    public static void playTo(ServerPlayerEntity p, SoundEvent sound, double x, double y, double z,
                              float volume, float pitch) {
        if (p.isRemoved() || p.networkHandler == null) return;
        p.networkHandler.sendPacket(new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(sound), SoundCategory.AMBIENT,
                x, y, z, volume, pitch, p.getRandom().nextLong()));
    }

    public static void playAtPlayer(ServerPlayerEntity p, SoundEvent sound, float volume, float pitch) {
        playTo(p, sound, p.getX(), p.getY(), p.getZ(), volume, pitch);
    }

    /** Punto alle spalle del giocatore (rispetto a dove guarda), a distanza data. */
    public static Vec3d behind(PlayerEntity p, double dist) {
        Vec3d look = p.getRotationVec(1.0f);
        Vec3d flat = new Vec3d(-look.x, 0, -look.z).normalize();
        return p.getPos().add(flat.multiply(dist));
    }
}
