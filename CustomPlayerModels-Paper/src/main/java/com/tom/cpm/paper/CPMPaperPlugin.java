package com.tom.cpm.paper;

import java.io.File;
import java.util.EnumSet;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;

import com.tom.cpl.block.BiomeHandler;
import com.tom.cpl.block.BlockStateHandler;
import com.tom.cpl.block.entity.EntityTypeHandler;
import com.tom.cpl.config.ModConfigFile;
import com.tom.cpl.item.ItemStackHandler;
import com.tom.cpl.text.TextRemapper;
import com.tom.cpl.util.ILogger;
import com.tom.cpm.api.CPMApiManager;
import com.tom.cpm.api.CPMPluginRegistry;
import com.tom.cpm.api.ICPMPlugin;
import com.tom.cpm.shared.MinecraftCommonAccess;
import com.tom.cpm.shared.MinecraftObjectHolder;
import com.tom.cpm.shared.MinecraftServerAccess;
import com.tom.cpm.shared.PlatformFeature;
import com.tom.cpm.shared.network.NetHandler;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public class CPMPaperPlugin extends JavaPlugin {
	private static CPMPaperPlugin INSTANCE;
	public ModConfigFile config;
	private Network net;
	private BukkitLogger log;
	private CPMApiManager api;
	public TextRemapper<Component> textMapper;

	@Override
	public void onDisable() {
		super.onDisable();
		MinecraftObjectHolder.setCommonObject(null);
		MinecraftObjectHolder.setServerObject(null);
		config.save();
		INSTANCE = null;
	}

	@Override
	public void onEnable() {
		super.onEnable();
		INSTANCE = this;
		if (checkDuplicateInstall()) {
			throw new Error("CPM is installed twice, remove either the plugin or the mod.");
		}
		getDataFolder().mkdirs();
		log = new BukkitLogger(getLogger());
		config = new ModConfigFile(new File(getDataFolder(), "cpm.json"));
		File tr = new File(getDataFolder(), "cpm.lang");
		api = new CPMApiManager();
		api.buildCommon().player(Player.class).init();
		getServer().getServicesManager().register(CPMPluginRegistry.class, new CPMPluginRegistry() {

			@Override
			public void register(ICPMPlugin plugin) {
				api.register(plugin);
				api.commonApi().callInit(plugin);
			}
		}, this, ServicePriority.Normal);
		TextComponents text = new TextComponents(tr);
		textMapper = text.remapper();
		this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
			new PaperCommands(commands.registrar(), text).registerCommon();
		});
		MinecraftObjectHolder.setCommonObject(new MinecraftCommonAccess() {

			@Override
			public ModConfigFile getConfig() {
				return config;
			}

			@Override
			public ILogger getLogger() {
				return log;
			}

			@Override
			public EnumSet<PlatformFeature> getSupportedFeatures() {
				return EnumSet.noneOf(PlatformFeature.class);
			}

			@Override
			public String getPlatformVersionString() {
				return "Paper (" + getServer().getVersion() + "/" + getServer().getBukkitVersion() + ") " + getDescription().getVersion();
			}

			@Override
			public TextRemapper<?> getTextRemapper() {
				return textMapper;
			}

			@Override
			public CPMApiManager getApi() {
				return api;
			}

			@Override
			public String getMCVersion() {
				return "paper";
			}

			@Override
			public String getMCBrand() {
				return "Paper (" + getServer().getVersion() + "/" + getServer().getBukkitVersion() + ")";
			}

			@Override
			public String getModVersion() {
				return getDescription().getVersion();
			}

			@Override
			public ItemStackHandler<?> getItemStackHandler() {
				return null;
			}

			@Override
			public BlockStateHandler<?> getBlockStateHandler() {
				return null;
			}

			@Override
			public EntityTypeHandler<?> getEntityTypeHandler() {
				return null;
			}
		});
		MinecraftObjectHolder.setServerObject(new MinecraftServerAccess() {

			@Override
			public ModConfigFile getConfig() {
				return config;
			}

			@Override
			public NetHandler<?, ?, ?> getNetHandler() {
				return net.netHandler;
			}

			@Override
			public BiomeHandler<?> getBiomeHandler() {
				return null;
			}
		});
		net = new Network(this);
		net.register();
		log.info("Customizable Player Models Initialized");
	}

	private boolean checkDuplicateInstall() {
		try {
			Class.forName("com.tom.cpm.CustomPlayerModels");
			return true;
		} catch (Throwable e) {
		}
		return false;
	}

	public static Plugin getInstance() {
		return INSTANCE;
	}

	public static BukkitLogger getLog() {
		return INSTANCE.log;
	}
}
