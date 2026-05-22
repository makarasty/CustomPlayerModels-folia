package com.tom.cpm.client;

import org.quiltmc.qsl.resource.loader.api.ResourceLoader;
import org.quiltmc.qsl.resource.loader.api.reloader.IdentifiableResourceReloader;

import net.minecraft.resources.ResourceLocation;

import com.tom.cpl.tag.TagManager;

public class CPMTagLoaderFabric extends CPMTagLoader implements IdentifiableResourceReloader {

	public CPMTagLoaderFabric(ResourceLoader mc, TagManager<?> tags, String prefix) {
		super(l -> mc.registerReloader((CPMTagLoaderFabric) l), tags, prefix);
	}

	@Override
	public ResourceLocation getQuiltId() {
		return id;
	}
}