package com.dreadmod.client;

import com.dreadmod.DreadMod;
import com.dreadmod.entity.StalkerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Sagoma umanoide alta e allungata, con occhi rossi luminosi. */
public class StalkerRenderer extends BipedEntityRenderer<StalkerEntity, StalkerModel> {
    private static final Identifier TEXTURE = new Identifier(DreadMod.MOD_ID, "textures/entity/stalker.png");
    private static final Identifier EYES = new Identifier(DreadMod.MOD_ID, "textures/entity/stalker_eyes.png");

    public StalkerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new StalkerModel(ctx.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
        this.addFeature(new EyesFeature(this));
    }

    @Override
    public Identifier getTexture(StalkerEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(StalkerEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(0.95f, 1.3f, 0.95f);
    }

    private static class EyesFeature extends EyesFeatureRenderer<StalkerEntity, StalkerModel> {
        private static final RenderLayer LAYER = RenderLayer.getEyes(EYES);

        EyesFeature(FeatureRendererContext<StalkerEntity, StalkerModel> context) {
            super(context);
        }

        @Override
        public RenderLayer getEyesTexture() {
            return LAYER;
        }
    }
}
