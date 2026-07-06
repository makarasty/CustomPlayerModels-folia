package com.tom.cpm.client;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;

import com.tom.cpm.shared.animation.AnimationState;
import com.tom.cpm.shared.config.Player;

public interface PlayerRenderStateAccess {
	void cpm$setPlayer(Player<Avatar> player);
	Player<Avatar> cpm$getPlayer();
	void cpm$setAnimationState(AnimationState state);
	AnimationState cpm$getAnimationState();
	void cpm$setModelStatus(Component status);
	Component cpm$getModelStatus();
	void cpm$storeState(PlayerModel model);
	void cpm$loadState(PlayerModel model);
}
