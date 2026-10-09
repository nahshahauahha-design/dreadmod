package com.dreadmod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** /dread fear <0-100> | /dread stalker | /dread night | /dread calm  (richiede permessi da operatore) */
public final class DreadCommands {
    private DreadCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("dread")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(literal("fear")
                                .then(argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                            int v = IntegerArgumentType.getInteger(ctx, "value");
                                            FearManager.set(p, v);
                                            ctx.getSource().sendFeedback(() -> Text.literal("Paura impostata a " + v), false);
                                            return 1;
                                        })))
                        .then(literal("stalker")
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                    boolean ok = FearManager.spawnStalker(p.getServerWorld(), p);
                                    ctx.getSource().sendFeedback(() -> Text.literal(ok
                                            ? "Lo Stalker ti sta osservando..."
                                            : "Nessuno spazio adatto trovato per lo Stalker."), false);
                                    return ok ? 1 : 0;
                                }))
                        .then(literal("night")
                                .executes(ctx -> {
                                    FearManager.forceNightOfTerror(600);
                                    ctx.getSource().sendFeedback(() -> Text.literal("Notte del Terrore attivata per 10 minuti."), true);
                                    return 1;
                                }))
                        .then(literal("calm")
                                .executes(ctx -> {
                                    FearManager.calmAll(ctx.getSource().getServer());
                                    ctx.getSource().sendFeedback(() -> Text.literal("La calma torna. Per ora."), true);
                                    return 1;
                                }))));
    }
}
