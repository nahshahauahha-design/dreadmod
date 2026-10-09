package com.dreadmod.client;

import com.dreadmod.entity.StalkerEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.CrossbowPosing;

/** Modello umanoide (stessa struttura dello zombie) con le braccia protese quando attacca. */
public class StalkerModel extends BipedEntityModel<StalkerEntity> {
    public StalkerModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setAngles(StalkerEntity entity, float limbAngle, float limbDistance, float animationProgress,
                          float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        CrossbowPosing.meleeAttack(this.leftArm, this.rightArm, entity.isAttacking(),
                this.handSwingProgress, animationProgress);
    }
}
