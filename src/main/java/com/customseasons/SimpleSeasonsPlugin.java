package com.customseasons;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class SimpleSeasonsPlugin extends JavaPlugin {

    private SeasonManager seasonManager;
    private GroundEffectsTask groundEffectsTask;
    private VisualTask visualTask;
    private GrowthListener growthListener;

    private BukkitTask groundEffectsHandle;
    private BukkitTask visualHandle;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        seasonManager = new SeasonManager(this);
        groundEffectsTask = new GroundEffectsTask(this, seasonManager);
        visualTask = new VisualTask(this, seasonManager);
        growthListener = new GrowthListener(this, seasonManager);

        loadEverything();

        getServer().getPluginManager().registerEvents(growthListener, this);

        SeasonCommand command = new SeasonCommand(this, seasonManager);
        getCommand("season").setExecutor(command);

        seasonManager.start();
        scheduleTasks();
    }

    @Override
    public void onDisable() {
        if (seasonManager != null) seasonManager.stop();
        cancelTasks();
    }

    public void reloadPluginConfig() {
        reloadConfig();
        loadEverything();
        cancelTasks();
        scheduleTasks();
    }

    private void loadEverything() {
        seasonManager.load();
        groundEffectsTask.load();
        visualTask.load();
        growthListener.load();
    }

    private void scheduleTasks() {
        if (groundEffectsTask.isEnabled()) {
            groundEffectsHandle = Bukkit.getScheduler().runTaskTimer(
                    this, groundEffectsTask, 20L, groundEffectsTask.getIntervalTicks());
        }
        if (visualTask.isEnabled()) {
            visualHandle = Bukkit.getScheduler().runTaskTimer(
                    this, visualTask, 20L, visualTask.getIntervalTicks());
        }
    }

    private void cancelTasks() {
        if (groundEffectsHandle != null) {
            groundEffectsHandle.cancel();
            groundEffectsHandle = null;
        }
        if (visualHandle != null) {
            visualHandle.cancel();
            visualHandle = null;
        }
    }

    public SeasonManager getSeasonManager() {
        return seasonManager;
    }
}
