package com.tom.cpm.bukkit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import com.tom.cpm.bukkit.text.BukkitText;
import com.tom.cpm.shared.MinecraftObjectHolder;
import com.tom.cpm.shared.config.PlayerData;
import com.tom.cpm.shared.io.FastByteArrayInputStream;
import com.tom.cpm.shared.network.NetH.ServerNetH;
import com.tom.cpm.shared.network.NetHandler;
import com.tom.cpm.shared.util.Log;

public class Network implements PluginMessageListener, Listener {
    public static final String PLAYER_DATA = MinecraftObjectHolder.NETWORK_ID + ":data";
    private final CPMBukkitPlugin plugin;
    public NetHandler<String, Player, Meta> netHandler;
    private boolean hasAttributes = false;
    
    // Use a thread-safe map to isolate Metadata from Bukkit's Entity state, 
    // avoiding Async entity access exceptions from GlobalRegionScheduler!
    private final Map<UUID, Meta> playerData = new ConcurrentHashMap<>();

    @SuppressWarnings("deprecation")
    public Network(CPMBukkitPlugin plugin) {
        this.plugin = plugin;
        try {
            netHandler = new NetHandler<>((k, v) -> k + ":" + v);
            // Schedule plugin messages safely on the player's thread
            netHandler.setSendPacketDirect((pl, pck, dt) -> pl.owner.getScheduler().run(plugin, task -> pl.owner.sendPluginMessage(plugin, pck, dt), null), this::sendToAllTrackingAndSelf);
            netHandler.setGetPlayerUUID(Player::getUniqueId);
            // Use live tracking resolution without delay
            netHandler.setFindTracking((p, c) -> getPlayersWithin(p, 64, c));
            netHandler.setSendChat((pl, msg) -> msg.<BukkitText>remap().sendTo(pl));
            netHandler.setExecutor(() -> Runnable::run);
            netHandler.setGetNet(this::getMetadata);
            netHandler.setGetPlayer(n -> n.owner);
            netHandler.setGetPlayerId(Player::getEntityId);
            netHandler.setGetOnlinePlayers(Bukkit::getOnlinePlayers);
            netHandler.setKickPlayer((p, m) -> p.getScheduler().run(plugin, t -> p.kickPlayer(m.<Object>remap().toString()), null));

            try {
                Attribute.values();
                hasAttributes = true;
                Log.info("Attributes available, loading scalers");
            } catch (Throwable e) {
                Log.warn("Attributes not found", e);
            }
            
            // Pass cached safe values rather than accessing Entity methods off-thread
            netHandler.setGetPlayerAnimGetters((t, u) -> {
                Meta m = getMetadata(t);
                u.updated = true;
                u.creativeFlying = m.creativeFlying;
                u.falling = m.falling;
                u.health = m.health;
                u.air = m.air;
                u.hunger = m.hunger;
                u.inMenu = m.inMenu;
            });
            
            if(hasAttributes) netHandler.addScaler(new AttributeScaler());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void register() {
        netHandler.registerOut(c -> Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, c));
        netHandler.registerIn(c -> Bukkit.getMessenger().registerIncomingPluginChannel(plugin, c, this));
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Tick on GlobalRegionScheduler, safely synchronized to prevent ConcurrentModificationException
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            synchronized(netHandler) {
                netHandler.tick();
            }
        }, 1L, 1L);
    }

    public static void getPlayersWithin(Player player, int distance, Consumer<Player> cons) {
        cons.accept(player); // Always include self
        // O(K) Folia tracker integration. We avoid getDistanceSquared to prevent Async entity access 
        // to other players' locations. getTrackedPlayers() guarantees they are in view distance.
        for (Player p : player.getTrackedPlayers()) {
            cons.accept(p);
        }
    }

    private void sendToAllTrackingAndSelf(Player player, String packet, byte[] data) {
        player.getScheduler().run(plugin, task -> {
            getPlayersWithin(player, 64, pl -> {
                if(getMetadata(pl).cpm$hasMod()) {
                    pl.sendPluginMessage(plugin, packet, data);
                }
            });
        }, null);
    }

    @Override
    public void onPluginMessageReceived(String name, Player player, byte[] packet) {
        player.getScheduler().run(plugin, task -> {
            synchronized(netHandler) {
                netHandler.receiveServer(name, new FastByteArrayInputStream(packet), getMetadata(player));
            }
        }, null);
    }

    // Per-Player tick task for caching animations and tracking detection
    private void startPlayerTick(Player player) {
        Meta mt = getMetadata(player);
        if (mt.tickTask != null && !mt.tickTask.isCancelled()) {
            mt.tickTask.cancel();
        }
        
        mt.tickTask = player.getScheduler().runAtFixedRate(plugin, task -> {
            // Update animations cache safely inside EntityScheduler
            mt.creativeFlying = player.isFlying();
            mt.falling = player.getFallDistance();
            if (hasAttributes) {
                mt.health = (float) (player.getHealth() / player.getAttribute(AttributeScaler.MAX_HEALTH).getValue());
            } else {
                mt.health = (float) (player.getHealth() / player.getMaxHealth());
            }
            mt.air = Math.max(player.getRemainingAir() / (float) player.getMaximumAir(), 0);
            mt.hunger = player.getFoodLevel() / 20f;
            mt.inMenu = player.getOpenInventory() != null;

            mt.tickCount++;
            if (mt.tickCount % 20 == 0) { // Every 20 ticks (1 sec) -> Tracker logic
                if(mt.cpm$hasMod()) {
                    List<Player> nearbyPlayers = new ArrayList<>();
                    getPlayersWithin(player, 64, nearbyPlayers::add);
                    for (Player observer : nearbyPlayers) {
                        if(!mt.trackingPlayers.contains(observer)) {
                            synchronized(netHandler) {
                                onTrackingStart(player, observer);
                            }
                        }
                    }
                    mt.trackingPlayers.clear();
                    mt.trackingPlayers.addAll(nearbyPlayers);
                }
            }
        }, null, 1L, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        Player player = evt.getPlayer();
        try {
            Method addChn = player.getClass().getMethod("addChannel", String.class);
            netHandler.registerOut(c -> addChn.invoke(player, c));
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized(netHandler) {
            netHandler.onJoin(player);
        }
        startPlayerTick(player);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent evt) {
        evt.getPlayer().getScheduler().runDelayed(plugin, t -> startPlayerTick(evt.getPlayer()), null, 1L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent evt) {
        evt.getPlayer().getScheduler().runDelayed(plugin, t -> startPlayerTick(evt.getPlayer()), null, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent evt) {
        Meta mt = playerData.remove(evt.getPlayer().getUniqueId());
        if (mt != null && mt.tickTask != null && !mt.tickTask.isCancelled()) {
            mt.tickTask.cancel();
        }
    }

    @EventHandler
    public void onPlayerStatisticIncrement(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() == Statistic.JUMP) {
            Player p = event.getPlayer();
            p.getScheduler().run(plugin, t -> {
                synchronized(netHandler) {
                    netHandler.onJump(p);
                }
            }, null);
        }
    }

    public Meta getMetadata(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(), k -> {
            Meta mt = new Meta(player);
            mt.cpm$setEncodedModelData(new PlayerData());
            return mt;
        });
    }

    public static class Meta implements ServerNetH {
        private final Player owner;
        private volatile boolean hasMod;
        private volatile PlayerData data;
        public final List<Player> trackingPlayers = new CopyOnWriteArrayList<>();
        
        public int tickCount = 0;
        public volatile boolean creativeFlying;
        public volatile float falling, health, air, hunger;
        public volatile boolean inMenu;
        public ScheduledTask tickTask;

        public Meta(Player owner) {
            this.owner = owner;
        }

        @Override
        public boolean cpm$hasMod() {
            return hasMod;
        }

        @Override
        public void cpm$setHasMod(boolean v) {
            hasMod = v;
        }

        @Override
        public PlayerData cpm$getEncodedModelData() {
            return data;
        }

        @Override
        public void cpm$setEncodedModelData(PlayerData data) {
            this.data = data;
        }
    }

    public void onTrackingStart(Player to, Player player) {
        netHandler.sendPlayerData(player, to);
    }
}