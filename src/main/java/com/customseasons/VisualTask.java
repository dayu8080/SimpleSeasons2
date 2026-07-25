package com.customseasons;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Чисто визуальные частицы падающего "снега"/"листьев" рядом с игроком.
 * Не трогает блоки, поэтому радиус здесь не проблема - это просто
 * частицы вокруг конкретного игрока, как погодные эффекты.
 */
public class VisualTask implements Runnable {

    private final SimpleSeasonsPlugin plugin;
    private final SeasonManager seasonManager;
    private final Random random = new Random();

    private boolean enabled;
    private int intervalTicks;
    private double chance;
    private Material winterBlock;
    private Material autumnBlock;

    public VisualTask(SimpleSeasonsPlugin plugin, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();
        enabled = cfg.getBoolean("visual.enabled", true);
        intervalTicks = Math.max(1, cfg.getInt("visual.interval-ticks", 40));
        chance = cfg.getDouble("visual.chance", 0.35);
        winterBlock = Material.matchMaterial(cfg.getString("visual.winter-particle-block", "SNOW"));
        autumnBlock = Material.matchMaterial(cfg.getString("visual.autumn-particle-block", "OAK_LEAVES"));
        if (winterBlock == null) winterBlock = Material.SNOW;
        if (autumnBlock == null) autumnBlock = Material.OAK_LEAVES;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getIntervalTicks() {
        return intervalTicks;
    }

    @Override
    public void run() {
        if (!enabled) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            World world = player.getWorld();
            if (!seasonManager.isEnabled(world)) continue;
            if (random.nextDouble() > chance) continue;

            Season season = seasonManager.getSeason(world);
            Material mat;
            if (season == Season.WINTER) {
                mat = winterBlock;
            } else if (season == Season.AUTUMN) {
                mat = autumnBlock;
            } else {
                continue;
            }

            Location loc = player.getLocation().add(0, 2.2, 0);
            world.spawnParticle(Particle.FALLING_DUST, loc, 6, 1.2, 0.6, 1.2, 0.0, mat.createBlockData());
        }
    }
}
