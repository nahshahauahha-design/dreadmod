package com.dreadmod.client;

import com.dreadmod.DreadMod;
import com.dreadmod.ModBlocks;
import com.dreadmod.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.render.RenderLayer;

public class DreadModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.STALKER, StalkerRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CURSED_CANDLE, RenderLayer.getCutout());

        ClientPlayNetworking.registerGlobalReceiver(DreadMod.FEAR_PACKET, (client, handler, buf, sender) -> {
            int value = buf.readInt();
            client.execute(() -> ClientFear.fear = value);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientFear.reset());

        HudRenderCallback.EVENT.register(ClientFear::render);
    }
}
