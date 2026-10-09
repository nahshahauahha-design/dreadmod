package com.dreadmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** HUD della paura: vignetta rossa pulsante ai bordi + barra in alto al centro. */
public final class ClientFear {
    public static int fear = 0;
    private static float shown = 0f;

    private ClientFear() {}

    public static void reset() {
        fear = 0;
        shown = 0f;
    }

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        shown += (fear - shown) * 0.08f;
        if (shown < 8f) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        float k = Math.min(shown, 100f) / 100f;
        double time = mc.world != null ? mc.world.getTime() + tickDelta : 0;

        // Il battito accelera con la paura.
        float beat = (float) (0.5 + 0.5 * Math.sin(time * (0.15 + 0.25 * k)));
        int alpha = (int) (Math.pow(k, 1.5) * 150 * (0.55 + 0.45 * beat));
        int edge = ((alpha & 0xFF) << 24) | 0x400000;
        int clear = 0x00400000;

        ctx.fillGradient(0, 0, w, h / 3, edge, clear);
        ctx.fillGradient(0, h * 2 / 3, w, h, clear, edge);
        int sideAlpha = (int) (alpha * 0.6f);
        ctx.fill(0, 0, w / 16, h, ((sideAlpha & 0xFF) << 24) | 0x400000);
        ctx.fill(w - w / 16, 0, w, h, ((sideAlpha & 0xFF) << 24) | 0x400000);

        // Barra della paura.
        int barW = 80;
        int x = (w - barW) / 2;
        int y = 10;
        ctx.fill(x - 1, y - 1, x + barW + 1, y + 5, 0x88000000);
        ctx.fill(x, y, x + barW, y + 4, 0xFF200000);
        int filled = (int) (barW * Math.min(shown, 100f) / 100f);
        int red = (int) (110 + 145 * k);
        ctx.fill(x, y, x + filled, y + 4, 0xFF000000 | (red << 16));
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.translatable("hud.dreadmod.fear"),
                w / 2, y + 8, 0xCC3333);
    }
}
