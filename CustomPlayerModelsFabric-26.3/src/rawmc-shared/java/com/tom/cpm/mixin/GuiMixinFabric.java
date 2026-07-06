package com.tom.cpm.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

import com.tom.cpm.client.GuiImpl;
import com.tom.cpm.shared.editor.gui.EditorGui;

@Mixin(Gui.class)
public abstract class GuiMixinFabric {

	@Shadow public abstract void setScreen(Screen screen);

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	public void onSetScreen(Screen screen, CallbackInfo cbi) {
		if (screen instanceof TitleScreen && EditorGui.doOpenEditor()) {
			cbi.cancel();
			setScreen(new GuiImpl(EditorGui::new, screen));
		}
	}
}
