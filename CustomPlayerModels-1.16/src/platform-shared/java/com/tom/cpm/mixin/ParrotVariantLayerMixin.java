package com.tom.cpm.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.layers.ParrotVariantLayer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.player.PlayerEntity;

import com.mojang.blaze3d.matrix.MatrixStack;

import com.tom.cpl.util.ItemSlot;
import com.tom.cpm.client.CustomPlayerModelsClient;
import com.tom.cpm.client.PlayerRenderManager;
import com.tom.cpm.shared.definition.ModelDefinition;
import com.tom.cpm.shared.model.render.ItemTransform;
import com.tom.cpm.shared.model.render.ModelRenderManager.BoundPlayer;

@Mixin(ParrotVariantLayer.class)
public abstract class ParrotVariantLayerMixin extends LayerRenderer<PlayerEntity, PlayerModel<PlayerEntity>> {

	public ParrotVariantLayerMixin(IEntityRenderer<PlayerEntity, PlayerModel<PlayerEntity>> p_i50926_1_) {
		super(p_i50926_1_);
	}

	@Inject(at = @At("HEAD"),
			method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/player/PlayerEntity;FFFFZ)V")
	public void onRenderPre(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, PlayerEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, boolean leftShoulderIn, CallbackInfo cbi) {
		matrixStackIn.pushPose();
		BoundPlayer pl = CustomPlayerModelsClient.INSTANCE.manager.getPlayerFromModel(getParentModel());
		if(pl != null) {
			ModelDefinition def = pl.definition;
			if(def != null) {
				ItemTransform tr = def.getTransform(leftShoulderIn ? ItemSlot.LEFT_SHOULDER : ItemSlot.RIGHT_SHOULDER);
				if(tr != null) {
					PlayerRenderManager.multiplyStacks(tr.getMatrix(), matrixStackIn);
					if(entitylivingbaseIn.isCrouching())
						matrixStackIn.translate(0, -0.2f, 0);
				}
			}
		}
	}

	@Inject(at = @At("RETURN"),
			method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/player/PlayerEntity;FFFFZ)V")
	public void onRenderPost(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, PlayerEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, boolean leftShoulderIn, CallbackInfo cbi) {
		matrixStackIn.popPose();
	}
}
