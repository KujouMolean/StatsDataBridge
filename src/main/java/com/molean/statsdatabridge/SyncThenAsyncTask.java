package com.molean.statsdatabridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SyncThenAsyncTask<T> extends BukkitRunnable {
    private final Supplier<T> supplier;
    private final Consumer<T> consumer;
    private Runnable runnable = null;

    public SyncThenAsyncTask(Supplier<T> supplier, Consumer<T> consumer) {
        this.supplier = supplier;
        this.consumer = consumer;

    }

    public SyncThenAsyncTask<T> then(Runnable runnable) {
        this.runnable = runnable;

        return this;
    }

    @Override
    public void run() {
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(StatsDataBridge.class), () -> {
            T t = supplier.get();
            Bukkit.getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(StatsDataBridge.class), () -> {
                consumer.accept(t);
                if (runnable != null) {
                    runnable.run();
                }

            });
        });
    }
}
