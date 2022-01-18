package com.molean.statsdatabridge;


import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class PlayerDataSync implements Listener {

    private final Map<UUID, String> passwdMap = new HashMap<>();

    public PlayerDataSync(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // check table
        try {
            PlayerDataDao.checkTable();
        } catch (SQLException e) {
            e.printStackTrace();
            //stop server if check has error
            Logger.getAnonymousLogger().severe("Database check error!");
            Bukkit.shutdown();
        }

        // add shutdown task

        Tasks.INSTANCE.regDisableTask(() -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (passwdMap.containsKey(onlinePlayer.getUniqueId())) {
                    try {
                        byte[] bytes = PlayerSerializeUtils.serializeSync(onlinePlayer);
                        onQuit(onlinePlayer, bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // load data to current player
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onJoin(onlinePlayer);
        }


        //update one player data per second
        Queue<UUID> queue = new ArrayDeque<>();
        Tasks.INSTANCE.intervalAsync(20, () -> {
            if (queue.isEmpty()) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (passwdMap.containsKey(onlinePlayer.getUniqueId())) {
                        queue.add(onlinePlayer.getUniqueId());
                    }
                }
                return;
            }
            UUID uuid = queue.poll();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && passwdMap.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> update(player));
            }
        });

    }

    /*async or sync call*/
    public void update(Player player) {
        PlayerSerializeUtils.serialize(player, bytes -> {
            if (bytes == null) {
                MessageUtils.warn(player, "你的背包数据保存失败，请尽快联系管理员处理！");
                return;
            }
            try {
                if (!PlayerDataDao.update(player.getUniqueId(), bytes, passwdMap.get(player.getUniqueId()))) {
                    throw new RuntimeException("Unexpected complete player data error!");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                MessageUtils.warn(player, "你的背包数据保存失败，请尽快联系管理员处理！");
            }
        });

    }

    // can't create task this method
    public void onQuit(Player player, byte[] data) {
        try {
            if (!PlayerDataDao.complete(player.getUniqueId(), data, passwdMap.get(player.getUniqueId()))) {
                throw new RuntimeException("Unexpected complete player data error!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        passwdMap.remove(player.getUniqueId());
    }

    public void waitLockThenLoadData(Player player) {
        Location location = player.getLocation().clone();

        //start load player data
        new AsyncTryTask(() -> {
            try {
                //尝试拿锁
                String lock = PlayerDataDao.getLock(player.getUniqueId());
                if (lock != null) {
                    loadDataAsync(player, lock, location);
                    //end
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                PlayerUtils.kickAsync(player, "读取玩家数据出错，请联系管理员！");
                return true;
                //return true to end task
            }
        }, 20, 15).onFailed(() -> {
            try {
                String lockForce = PlayerDataDao.getLockForce(player.getUniqueId());
                if (lockForce == null) {
                    //强制拿锁失败, 出大问题
                    PlayerUtils.kickAsync(player, "读取玩家数据出错，请联系管理员！");
                    throw new RuntimeException("Unexpected error! Force get lock failed.");
                    //end (failed)
                }
                loadDataAsync(player, lockForce, location);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                PlayerUtils.kickAsync(player, "读取玩家数据出错，请联系管理员！");
            }
        }).run();
    }

    /*sync call*/
    public void onJoin(Player player) {
        if (passwdMap.containsKey(player.getUniqueId())) {
            PlayerUtils.kickAsync(player, "上一次数据还未提交完毕，请稍后加入此服务器。");
            return;
        }
        player.setMetadata("JoinGameMode", new FixedMetadataValue(JavaPlugin.getPlugin(StatsDataBridge.class), player.getGameMode().getValue()));
        player.setGameMode(GameMode.SPECTATOR);

        Tasks.INSTANCE.async(() -> {
            try {
                if (!PlayerDataDao.exist(player.getUniqueId())) {
                    player.setMetadata("InitialCommit", new FixedMetadataValue(JavaPlugin.getPlugin(StatsDataBridge.class), true));
                    PlayerSerializeUtils.serialize(player, bytes -> {
                        try {
                            PlayerDataDao.insert(player.getUniqueId(), bytes);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }
                waitLockThenLoadData(player);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

    }

    private void loadDataAsync(Player player, String passwd, Location location) throws SQLException, IOException {
        passwdMap.put(player.getUniqueId(), passwd);
        //强制拿到锁了, 加载数据
        byte[] query = PlayerDataDao.query(player.getUniqueId(), passwd);
        if (query == null) {
            PlayerUtils.kickAsync(player, "严重错误读取玩家数据出错，请联系管理员！");
            throw new RuntimeException("Unexpected get player data failed!");
        }

        PlayerSerializeUtils.deserialize(player, query, () -> {
            player.teleport(location);
            PlayerDataSyncCompleteEvent playerDataSyncCompleteEvent = new PlayerDataSyncCompleteEvent(player);
            Bukkit.getPluginManager().callEvent(playerDataSyncCompleteEvent);
        });
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void on(PlayerJoinEvent event) {
        onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PlayerQuitEvent event) {
        if (passwdMap.containsKey(event.getPlayer().getUniqueId())) {
            PlayerSerializeUtils.serialize(event.getPlayer(), bytes -> onQuit(event.getPlayer(), bytes));
        }
    }
}
