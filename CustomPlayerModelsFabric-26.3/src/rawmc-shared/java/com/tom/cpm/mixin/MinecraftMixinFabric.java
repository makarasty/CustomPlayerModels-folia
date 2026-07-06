package com.tom.cpm.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import com.tom.cpm.client.CustomPlayerModelsClient;

@Mixin(Minecraft.class)
public abstract class MinecraftMixinFabric {

	@Shadow private DeltaTracker.Timer deltaTracker;

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V"), method = "runTick(Z)V")
	public void onRenderTick(boolean v, CallbackInfo cbi) {
		CustomPlayerModelsClient.mc.getPlayerRenderManager().getAnimationEngine().update(deltaTracker.getGameTimeDeltaPartialTick(true));
	}

	@Inject(at = @At("HEAD"), method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V")
	public void onDisconnect(Screen screen, boolean b, boolean b2, CallbackInfo cbi) {
		CustomPlayerModelsClient.INSTANCE.onLogout();
	}
}
