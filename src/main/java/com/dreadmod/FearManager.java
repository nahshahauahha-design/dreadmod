package com.dreadmod;

import com.dreadmod.entity.StalkerEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cuore della mod: il sistema della PAURA (0-100) di ogni giocatore.
 *
 * La paura sale al buio, in caverna, nel Nether, durante la Notte del Terrore e quando lo Stalker è vicino.
 * Scende alla luce del giorno, vicino a candele e con l'Amuleto Sacro.
 * Più la paura è alta, più la tua mente ti gioca brutti scherzi (suoni, buio, nausea, battito cardiaco)
 * e più è probabile che compaia uno Stalker.
 */
public final class FearManager {
    // ---- Valori facilmente modificabili ----
    private static final int GAIN_DARK = 2;          // luce <= 2
    private static final int GAIN_DIM = 1;          // luce <= 6
    private static final int LOSS_NORMAL = 1;       // luce media
    private static final int LOSS_BRIGHT = 3;       // luce >= 12
    private static final int NETHER_BONUS = 1;
    private static final int NIGHT_EVENT_BONUS = 1;
    private static final int STALKER_NEAR_BONUS = 4;
    private static final int CANDLE_RELIEF = 3;
    private static final int STALKER_SPAWN_CHANCE = 70;        // 1 su N ogni secondo con paura >= 60
    private static final int STALKER_SPAWN_CHANCE_EVENT = 25;  // durante la Notte del Terrore

    private static final Map<UUID, Integer> FEAR = new HashMap<>();
    private static boolean naturalNight = false;
    private static int forcedSeconds = 0;
    private static boolean announced = false;
    private static long lastRolledDay = -1;

    private record Delayed(long due, Runnable task) {}
    private static final List<Delayed> QUEUE = new ArrayList<>();

    private static final SoundEvent[] AMBIENT_SCARES = {
            SoundEvents.ENTITY_ENDERMAN_STARE, SoundEvents.ENTITY_PHANTOM_AMBIENT,
            SoundEvents.ENTITY_WARDEN_AMBIENT, SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT,
            SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT, SoundEvents.ENTITY_GHAST_AMBIENT,
            SoundEvents.BLOCK_PORTAL_AMBIENT, SoundEvents.ENTITY_SKELETON_AMBIENT
    };

    private FearManager() {}

    // ------------------------------------------------------------------ API

    public static boolean isNightOfTerror() {
        return naturalNight || forcedSeconds > 0;
    }

    public static int get(ServerPlayerEntity p) {
        return FEAR.getOrDefault(p.getUuid(), 0);
    }

    public static void set(ServerPlayerEntity p, int value) {
        int v = MathHelper.clamp(value, 0, 100);
        FEAR.put(p.getUuid(), v);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(v);
        ServerPlayNetworking.send(p, DreadMod.FEAR_PACKET, buf);
    }

    public static void add(ServerPlayerEntity p, int delta) {
        set(p, get(p) + delta);
    }

    public static void forceNightOfTerror(int seconds) {
        forcedSeconds = Math.max(0, seconds);
    }

    public static void calmAll(MinecraftServer server) {
        naturalNight = false;
        forcedSeconds = 0;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) set(p, 0);
    }

    // ------------------------------------------------------------------ init

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(FearManager::onServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> FEAR.remove(handler.getPlayer().getUuid()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> set(newPlayer, 0));
    }

    private static void schedule(MinecraftServer server, int delayTicks, Runnable task) {
        QUEUE.add(new Delayed(server.getTicks() + delayTicks, task));
    }

    private static void onServerTick(MinecraftServer server) {
        if (!QUEUE.isEmpty()) {
            long now = server.getTicks();
            List<Runnable> due = new ArrayList<>();
            Iterator<Delayed> it = QUEUE.iterator();
            while (it.hasNext()) {
                Delayed d = it.next();
                if (d.due() <= now) {
                    due.add(d.task());
                    it.remove();
                }
            }
            due.forEach(Runnable::run);
        }

        if (server.getTicks() % 20 != 0) return;

        updateNightEvent(server);
        for (ServerPlayerEntity p : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
            tickPlayer(server, p);
        }
    }

    // ------------------------------------------------------------------ Notte del Terrore

    private static void updateNightEvent(MinecraftServer server) {
        ServerWorld ow = server.getOverworld();
        long time = ow.getTimeOfDay();
        long day = time / 24000L;
        long tod = time % 24000L;

        // All'inizio di ogni notte c'è 1 probabilità su 5 che sia una Notte del Terrore.
        if (tod >= 13000 && tod < 13200 && day != lastRolledDay) {
            lastRolledDay = day;
            if (ow.getRandom().nextInt(5) == 0) naturalNight = true;
        }
        if (naturalNight && (tod >= 23000 || tod < 12000)) naturalNight = false;
        if (forcedSeconds > 0) forcedSeconds--;

        boolean now = isNightOfTerror();
        if (now != announced) {
            announced = now;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (now) {
                    p.sendMessage(Text.translatable("message.dreadmod.night_begin")
                            .formatted(Formatting.DARK_RED, Formatting.ITALIC), true);
                    DreadUtil.playAtPlayer(p, SoundEvents.ENTITY_WARDEN_NEARBY_CLOSER, 1.0f, 0.7f);
                } else {
                    p.sendMessage(Text.translatable("message.dreadmod.night_end")
                            .formatted(Formatting.GRAY, Formatting.ITALIC), true);
                }
            }
        }
    }

    // ------------------------------------------------------------------ tick giocatore

    private static void tickPlayer(MinecraftServer server, ServerPlayerEntity p) {
        if (p.isCreative() || p.isSpectator() || !p.isAlive()) {
            if (get(p) != 0) set(p, 0);
            return;
        }

        ServerWorld w = p.getServerWorld();
        BlockPos pos = p.getBlockPos();
        boolean overworld = w.getRegistryKey() == World.OVERWORLD;

        int light = w.getLightLevel(pos);
        int delta;
        if (light <= 2) delta = GAIN_DARK;
        else if (light <= 6) delta = GAIN_DIM;
        else if (light >= 12) delta = -LOSS_BRIGHT;
        else delta = -LOSS_NORMAL;

        if (w.getRegistryKey() == World.NETHER) delta += NETHER_BONUS;
        if (overworld && isNightOfTerror()) delta += NIGHT_EVENT_BONUS;

        List<StalkerEntity> stalkers = w.getEntitiesByClass(StalkerEntity.class,
                p.getBoundingBox().expand(48), e -> true);
        for (StalkerEntity s : stalkers) {
            if (s.squaredDistanceTo(p) < 24 * 24) {
                delta += STALKER_NEAR_BONUS;
                break;
            }
        }

        if (DreadUtil.hasCharm(p) && delta > 0) delta /= 2;
        if (nearCandle(w, pos)) delta -= CANDLE_RELIEF;

        add(p, delta);
        int fear = get(p);
        Random r = p.getRandom();

        // ---- allucinazioni e malesseri in base alla paura ----
        if (fear >= 25 && r.nextInt(14) == 0) ambientScare(p);
        if (fear >= 45 && r.nextInt(18) == 0) closeScare(server, p);
        if (fear >= 55 && r.nextInt(25) == 0) whisper(p);
        if (fear >= 60 && r.nextInt(30) == 0) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100, 0, false, false, false));
        }
        if (fear >= 80 && r.nextInt(25) == 0) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 140, 0, false, false, false));
        }
        if (fear >= 95 && r.nextInt(12) == 0) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0, false, false, false));
        }
        if (fear >= 70) {
            DreadUtil.playAtPlayer(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.9f, fear >= 90 ? 1.25f : 1.0f);
        }

        // ---- comparsa dello Stalker ----
        if (stalkers.isEmpty() && fear >= 60 && light <= 9) {
            int chance = isNightOfTerror() ? STALKER_SPAWN_CHANCE_EVENT : STALKER_SPAWN_CHANCE;
            if (fear >= 90) chance = Math.max(8, chance / 2);
            if (r.nextInt(chance) == 0 && spawnStalker(w, p)) {
                DreadUtil.playAtPlayer(p, SoundEvents.ENTITY_WARDEN_NEARBY_CLOSE, 0.8f, 0.6f);
            }
        }
    }

    private static boolean nearCandle(ServerWorld w, BlockPos center) {
        for (BlockPos bp : BlockPos.iterate(center.add(-5, -3, -5), center.add(5, 3, 5))) {
            BlockState s = w.getBlockState(bp);
            if (s.isOf(ModBlocks.CURSED_CANDLE)) return true;
            if (s.isIn(BlockTags.CANDLES) && s.contains(Properties.LIT) && s.get(Properties.LIT)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ spaventi

    private static void ambientScare(ServerPlayerEntity p) {
        Random r = p.getRandom();
        SoundEvent s = AMBIENT_SCARES[r.nextInt(AMBIENT_SCARES.length)];
        double ang = r.nextDouble() * Math.PI * 2;
        double dist = 14 + r.nextDouble() * 10;
        DreadUtil.playTo(p, s, p.getX() + Math.cos(ang) * dist, p.getY(), p.getZ() + Math.sin(ang) * dist,
                1.0f, 0.5f + r.nextFloat() * 0.4f);
    }

    private static void closeScare(MinecraftServer server, ServerPlayerEntity p) {
        Random r = p.getRandom();
        switch (r.nextInt(4)) {
            case 0 -> { // passi che si avvicinano da dietro
                SoundEvent step = p.getServerWorld().getBlockState(p.getBlockPos().down())
                        .getSoundGroup().getStepSound();
                for (int i = 0; i < 6; i++) {
                    final double d = 9.0 - i * 1.4;
                    schedule(server, i * 9, () -> {
                        Vec3d at = DreadUtil.behind(p, d);
                        DreadUtil.playTo(p, step, at.x, p.getY(), at.z, 1.0f, 0.8f);
                    });
                }
            }
            case 1 -> { // creeper che sfrigola alle spalle
                Vec3d at = DreadUtil.behind(p, 3.0);
                DreadUtil.playTo(p, SoundEvents.ENTITY_CREEPER_PRIMED, at.x, p.getY(), at.z, 1.0f, 1.0f);
            }
            case 2 -> { // porta che si apre e si chiude
                Vec3d at = DreadUtil.behind(p, 7.0);
                DreadUtil.playTo(p, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, at.x, p.getY(), at.z, 1.0f, 0.8f);
                schedule(server, 30, () ->
                        DreadUtil.playTo(p, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, at.x, p.getY(), at.z, 1.0f, 0.8f));
            }
            default -> { // respiro vicinissimo
                Vec3d at = DreadUtil.behind(p, 1.5);
                DreadUtil.playTo(p, SoundEvents.ENTITY_ENDERMAN_STARE, at.x, p.getY() + 1, at.z, 0.7f, 0.5f);
            }
        }
    }

    private static void whisper(ServerPlayerEntity p) {
        int idx = p.getRandom().nextInt(8);
        p.sendMessage(Text.translatable("message.dreadmod.whisper." + idx)
                .formatted(Formatting.DARK_RED, Formatting.ITALIC), true);
    }

    // ------------------------------------------------------------------ spawn Stalker

    /** Fa comparire uno Stalker in un punto buio, preferibilmente fuori dalla visuale del giocatore. */
    public static boolean spawnStalker(ServerWorld w, ServerPlayerEntity p) {
        Random r = p.getRandom();
        Vec3d look = p.getRotationVec(1.0f);

        for (int i = 0; i < 50; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double dist = 14 + r.nextDouble() * 14;
            int x = MathHelper.floor(p.getX() + Math.cos(ang) * dist);
            int z = MathHelper.floor(p.getZ() + Math.sin(ang) * dist);

            for (int dy = 6; dy >= -6; dy--) {
                BlockPos bp = new BlockPos(x, MathHelper.floor(p.getY()) + dy, z);
                if (!w.getBlockState(bp.down()).isSolidBlock(w, bp.down())) continue;
                if (!w.getBlockState(bp).isAir() || !w.getBlockState(bp.up()).isAir()
                        || !w.getBlockState(bp.up(2)).isAir()) continue;
                if (i < 35 && w.getLightLevel(bp) > 9) continue;

                Vec3d to = new Vec3d(bp.getX() + 0.5 - p.getX(), 0, bp.getZ() + 0.5 - p.getZ()).normalize();
                if (i < 30 && look.x * to.x + look.z * to.z > 0.5) continue;

                StalkerEntity s = ModEntities.STALKER.create(w);
                if (s == null) return false;
                s.refreshPositionAndAngles(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5, r.nextFloat() * 360f, 0f);
                if (!w.isSpaceEmpty(s)) continue;
                s.setTarget(p);
                w.spawnEntity(s);
                return true;
            }
        }
        return false;
    }
}
