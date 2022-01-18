package com.molean.statsdatabridge;

import org.bukkit.plugin.java.JavaPlugin;

public final class StatsDataBridge extends JavaPlugin {
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        if (this.getConfig().getBoolean("enableStatsSync")) {
            new PlayerStatsSync(this);
        }
        if (this.getConfig().getBoolean("enableDataSync")) {
            new PlayerDataSync(this);
        }
    }

    @Override
    public void onDisable() {
        for (Runnable runnable : Tasks.INSTANCE.getDisableTaskList()) {
            runnable.run();
        }
    }
}
