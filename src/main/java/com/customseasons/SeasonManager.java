package com.customseasons;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Хранит текущий сезон и прогресс дня для каждого мира, тикает таймер
 * смены сезона и уведомляет слушателей (GrowthListener, задачи и т.д.).
 */
public class SeasonManager {

    public interface SeasonChangeListener {
        void onSeasonChange(World world, Season oldSeason, Season newSeason);
    }

    private static class WorldSeasonData {
        Season season;
        int dayProgress;

        WorldSeasonData(Season season) {
            this.season = season;
            this.dayProgress = 0;
        }
    }

    private final SimpleSeasonsPlugin plugin;
    private final Map<String, WorldSeasonData> worldData = new HashMap<>();
    private final Set<String> enabledWorlds = new HashSet<>();
    private boolean allWorlds;

    private int seasonLengthDays;
    private int ticksPerDay;
    private Season defaultSeason;
    private boolean broadcastChange;

    private BukkitTask task;
    private SeasonChangeListener listener;

    private File dataFile;
    private FileConfiguration dataConfig;

    public SeasonManager(SimpleSeasonsPlugin plugin) {
        this.plugin = plugin;
    }

    public void setSeasonChangeListener(SeasonChangeListener listener) {
        this.listener = listener;
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();

        enabledWorlds.clear();
        allWorlds = false;
        List<String> worlds = cfg.getStringList("worlds");
        for (String w : worlds) {
            if (w.equalsIgnoreCase("all")) {
                allWorlds = true;
            } else {
                enabledWorlds.add(w);
            }
        }

        seasonLengthDays = Math.max(1, cfg.getInt("season-length-days", 8));
        ticksPerDay = Math.max(1, cfg.getInt("ticks-per-day", 24000));
        broadcastChange = cfg.getBoolean("broadcast-season-change", true);

        String def = cfg.getString("default-season", "SPRING");
        Season parsedDefault;
        try {
            parsedDefault = Season.valueOf(def.toUpperCase());
        } catch (IllegalArgumentException ex) {
            parsedDefault = Season.SPRING;
        }
        defaultSeason = parsedDefault;

        dataFile = new File(plugin.getDataFolder(), "seasons-data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        worldData.clear();
        for (World world : Bukkit.getWorlds()) {
            if (!isEnabled(world)) continue;
            loadWorld(world);
        }
    }

    private void loadWorld(World world) {
        String path = world.getName();
        Season season = defaultSeason;
        String saved = dataConfig.getString(path + ".season");
        if (saved != null) {
            try {
                season = Season.valueOf(saved);
            } catch (IllegalArgumentException ignored) {
                // некорректное значение в файле - используем дефолт
            }
        }
        WorldSeasonData data = new WorldSeasonData(season);
        data.dayProgress = dataConfig.getInt(path + ".progress", 0);
        worldData.put(world.getName(), data);
    }

    public boolean isEnabled(World world) {
        return allWorlds || enabledWorlds.contains(world.getName());
    }

    public void start() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, ticksPerDay, ticksPerDay);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        save();
    }

    private void tick() {
        for (World world : Bukkit.getWorlds()) {
            if (!isEnabled(world)) continue;
            WorldSeasonData data = worldData.computeIfAbsent(world.getName(), n -> new WorldSeasonData(defaultSeason));
            data.dayProgress++;
            if (data.dayProgress >= seasonLengthDays) {
                data.dayProgress = 0;
                Season old = data.season;
                data.season = old.next();
                onChange(world, old, data.season);
            }
        }
        save();
    }

    private void onChange(World world, Season oldSeason, Season newSeason) {
        if (listener != null) listener.onSeasonChange(world, oldSeason, newSeason);
        if (broadcastChange) {
            String msg = "§bСезон в мире §f" + world.getName() + "§b сменился на §f" + newSeason.name();
            for (Player p : world.getPlayers()) {
                p.sendMessage(msg);
                p.sendTitle("§b" + newSeason.name(), "", 10, 60, 20);
            }
        }
    }

    public Season getSeason(World world) {
        WorldSeasonData data = worldData.get(world.getName());
        if (data == null) return defaultSeason;
        return data.season;
    }

    public void setSeason(World world, Season season) {
        WorldSeasonData data = worldData.computeIfAbsent(world.getName(), n -> new WorldSeasonData(season));
        Season old = data.season;
        data.season = season;
        data.dayProgress = 0;
        onChange(world, old, season);
        save();
    }

    public void save() {
        if (dataConfig == null) return;
        for (Map.Entry<String, WorldSeasonData> e : worldData.entrySet()) {
            dataConfig.set(e.getKey() + ".season", e.getValue().season.name());
            dataConfig.set(e.getKey() + ".progress", e.getValue().dayProgress);
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить данные сезонов: " + e.getMessage());
        }
    }
}
