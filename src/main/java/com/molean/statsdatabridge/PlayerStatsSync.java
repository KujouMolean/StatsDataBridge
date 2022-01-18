package com.molean.statsdatabridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PlayerStatsSync implements Listener {

    private final Map<UUID, String> passwdMap = new ConcurrentHashMap<>();

    public PlayerStatsSync(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // check table
        try {
            PlayerStatsDao.checkTable();
        } catch (SQLException e) {
            e.printStackTrace();
            //stop server if check has error
            Logger.getAnonymousLogger().severe("Database check error!");
            Bukkit.shutdown();
        }

        Tasks.INSTANCE.regDisableTask(() -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (passwdMap.containsKey(onlinePlayer.getUniqueId())) {
                    String stats = StatsSerializeUtils.getStats(onlinePlayer);
                    onLeft(onlinePlayer, stats);
                }
            }
        });

        // load data to current player
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onJoin(onlinePlayer);
        }

        //update one player data per second
        Queue<Player> queue = new ArrayDeque<>();
        Tasks.INSTANCE.intervalAsync(20, () -> {
            if (queue.isEmpty()) {
                queue.addAll(Bukkit.getOnlinePlayers());
                return;
            }
            Player player = queue.poll();
            if (player.isOnline() && passwdMap.containsKey(player.getUniqueId())) {
                Tasks.INSTANCE.sync(() -> {
                    update(player);
                });
            }
        });
    }

    public void update(Player player) {
        try {
            String stats = StatsSerializeUtils.getStats(player);
            Tasks.INSTANCE.async(() -> {
                try {
                    if (!PlayerStatsDao.update(player.getUniqueId(), stats, passwdMap.get(player.getUniqueId()))) {
                        throw new RuntimeException("Unexpected complete player stats error!");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    MessageUtils.warn(player, "你的统计保存失败，请尽快联系管理员处理！");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            MessageUtils.warn(player, "你的统计保存失败，请尽快联系管理员处理！");
        }
    }

    public void onLeft(Player player, String stats) {
        try {
            if (!PlayerStatsDao.complete(player.getUniqueId(), stats, passwdMap.get(player.getUniqueId()))) {
                throw new RuntimeException("Unexpected complete player stats error!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        passwdMap.remove(player.getUniqueId());
    }

    public void waitLockThenLoadData(Player player) {
        Runnable exceptionHandler = () -> {
            MessageUtils.warn(player, "你的统计信息读取错误, 可能已经被污染.");
        };

        new AsyncTryTask(() -> {
            try {
                //try get lock
                String lock = PlayerStatsDao.getLock(player.getUniqueId());
                if (lock != null) {
                    loadDataAsync(player, lock);
                    return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                exceptionHandler.run();
                return true;
                //return true to end task
            }
        }, 20, 15).onFailed(() -> {
            try {
                String lockForce = PlayerStatsDao.getLockForce(player.getUniqueId());
                if (lockForce == null) {
                    //force get lock failed
                    throw new RuntimeException("Unexpected error! Force get lock failed.");
                    //end (failed)
                }
                loadDataAsync(player, lockForce);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                exceptionHandler.run();
            }
        }).run();
    }

    public void onJoin(Player player) {
        new ConditionalAsyncTask(() -> {
            try {
                return !PlayerStatsDao.exist(player.getUniqueId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }).then(() -> {
            //插入数据
            new SyncThenAsyncTask<>(() -> StatsSerializeUtils.getStats(player), stats -> {
                try {
                    PlayerStatsDao.insert(player.getUniqueId(), stats);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }).then(() -> waitLockThenLoadData(player)).run();
        }).orElse(() -> waitLockThenLoadData(player)).run();
    }

    private void loadDataAsync(Player player, String passwd) throws SQLException, IOException {
        passwdMap.put(player.getUniqueId(), passwd);
        //强制拿到锁了, 加载数据
        String stats = PlayerStatsDao.query(player.getUniqueId(), passwd);
        if (stats == null) {
            throw new RuntimeException("Unexpected get player data failed!");
            //end (failed)
        }
        Tasks.INSTANCE.sync(() -> {
            StatsSerializeUtils.loadStats(player, stats);
            PlayerStatsSyncCompleteEvent playerStatsSyncCompleteEvent = new PlayerStatsSyncCompleteEvent(player);
            Bukkit.getPluginManager().callEvent(playerStatsSyncCompleteEvent);
        });
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void on(PlayerJoinEvent event) {
        onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PlayerQuitEvent event) {
        if (passwdMap.containsKey(event.getPlayer().getUniqueId())) {
            String stats = StatsSerializeUtils.getStats(event.getPlayer());
            Tasks.INSTANCE.async(() -> onLeft(event.getPlayer(), stats));
        }
    }


}

