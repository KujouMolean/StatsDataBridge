package com.molean.statsdatabridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public enum Tasks {
    INSTANCE;

    private final List<Runnable> runnableList = new ArrayList<>();

    public void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(StatsDataBridge.class), runnable);
    }


    public void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(StatsDataBridge.class), runnable);
    }

    public void repeatTask(int times, Consumer<Integer> consumer) {
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(StatsDataBridge.class), new Consumer<>() {
            private int cnt = 0;

            @Override
            public void accept(BukkitTask task) {
                consumer.accept(cnt);
                cnt++;
                if (cnt >= times) {
                    task.cancel();
                }
            }
        });
    }

    public void repeatTaskAsync(int times, Consumer<Integer> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(StatsDataBridge.class), new Consumer<>() {
            private int cnt = 0;

            @Override
            public void accept(BukkitTask task) {
                consumer.accept(cnt);
                cnt++;
                if (cnt >= times) {
                    task.cancel();
                }
            }
        });
    }

    public BukkitTask interval(int ticks, Runnable runnable) {

        return Bukkit.getScheduler().runTaskTimer(JavaPlugin.getPlugin(StatsDataBridge.class), runnable, 0, ticks);
    }

    public BukkitTask intervalAsync(int ticks, Runnable runnable) {

        return Bukkit.getScheduler().runTaskTimerAsynchronously(JavaPlugin.getPlugin(StatsDataBridge.class), runnable, 0, ticks);
    }

    public void timeout(int ticks, Runnable runnable) {

        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(StatsDataBridge.class), runnable, ticks);
    }

    public void timeoutAsync(int ticks, Runnable runnable) {

        Bukkit.getScheduler().runTaskLaterAsynchronously(JavaPlugin.getPlugin(StatsDataBridge.class), runnable, ticks);
    }

    public void regDisableTask(Runnable runnable) {
        runnableList.add(runnable);
    }

    public List<Runnable> getDisableTaskList() {
        return runnableList;
    }
}
