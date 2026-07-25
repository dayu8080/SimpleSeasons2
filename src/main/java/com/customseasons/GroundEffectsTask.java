package com.customseasons;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Snow;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Настоящий снег/лёд без привязки к радиусу вокруг игрока.
 *
 * Каждый запуск:
 *  1) Собирает список ВСЕХ загруженных чанков во всех разрешённых мирах.
 *  2) Перемешивает его случайным образом (поэтому не образуется
 *     фиксированный паттерн "всегда с одной стороны").
 *  3) В каждом чанке пробует несколько случайных колонок (x/z) и,
 *     в зависимости от текущего сезона мира, либо чуть-чуть наращивает
 *     снег/лёд (зимой), либо чуть-чуть их подтапливает (не зимой) -
 *     совсем как естественные осадки в ванильном Minecraft, только без
 *     привязки к погоде.
 *  4) Останавливается, как только потрачен бюджет blocks-per-run -
 *     это ограничивает нагрузку на сервер за один тик, а не количество
 *     обрабатываемых чанков или их положение.
 */
public class GroundEffectsTask implements Runnable {

    private static final int ATTEMPTS_PER_CHUNK = 3;

    private final SimpleSeasonsPlugin plugin;
    private final SeasonManager seasonManager;
    private final Random random = new Random();

    private boolean enabled;
    private int intervalTicks;
    private int blocksPerRun;
    private boolean freezeWater;
    private final Set<Material> snowSurfaces = EnumSet.noneOf(Material.class);

    public GroundEffectsTask(SimpleSeasonsPlugin plugin, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();
        enabled = cfg.getBoolean("ground-effects.enabled", true);
        intervalTicks = Math.max(1, cfg.getInt("ground-effects.interval-ticks", 100));
        blocksPerRun = Math.max(0, cfg.getInt("ground-effects.blocks-per-run", 150));
        freezeWater = cfg.getBoolean("ground-effects.freeze-water", true);

        snowSurfaces.clear();
        for (String name : cfg.getStringList("ground-effects.snow-surfaces")) {
            Material m = Material.matchMaterial(name);
            if (m != null) snowSurfaces.add(m);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getIntervalTicks() {
        return intervalTicks;
    }

    @Override
    public void run() {
        if (!enabled || blocksPerRun <= 0) return;

        List<Chunk> chunks = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (!seasonManager.isEnabled(world)) continue;
            Collections.addAll(chunks, world.getLoadedChunks());
        }
        if (chunks.isEmpty()) return;

        // Перемешиваем каждый запуск - иначе первые в списке чанки
        // всегда съедали бы весь бюджет и эффект выглядел бы однобоко.
        Collections.shuffle(chunks, random);

        int budget = blocksPerRun;
        for (Chunk chunk : chunks) {
            if (budget <= 0) break;
            if (!chunk.isLoaded()) continue;

            World world = chunk.getWorld();
            boolean winter = seasonManager.getSeason(world) == Season.WINTER;

            for (int i = 0; i < ATTEMPTS_PER_CHUNK && budget > 0; i++) {
                int localX = random.nextInt(16);
                int localZ = random.nextInt(16);
                int x = (chunk.getX() << 4) + localX;
                int z = (chunk.getZ() << 4) + localZ;

                Block top = world.getHighestBlockAt(x, z);
                Material type = top.getType();

                if (winter) {
                    if (applyWinter(top, type)) budget--;
                } else {
                    if (applyThaw(top, type)) budget--;
                }
            }
        }
    }

    private boolean applyWinter(Block top, Material type) {
        if (type == Material.SNOW) {
            return growSnowLayer(top);
        }
        if (snowSurfaces.contains(type)) {
            Block above = top.getRelative(0, 1, 0);
            if (above.getType() == Material.AIR) {
                above.setType(Material.SNOW, false);
                Snow data = (Snow) above.getBlockData();
                data.setLayers(data.getMinimumLayers());
                above.setBlockData(data, false);
                return true;
            }
            return false;
        }
        if (freezeWater && type == Material.WATER) {
            top.setType(Material.ICE, false);
            return true;
        }
        return false;
    }

    private boolean applyThaw(Block top, Material type) {
        if (type == Material.SNOW) {
            return shrinkSnowLayer(top);
        }
        if (type == Material.ICE) {
            top.setType(Material.WATER, false);
            return true;
        }
        return false;
    }

    private boolean growSnowLayer(Block block) {
        Snow data = (Snow) block.getBlockData();
        int layers = data.getLayers();
        int max = data.getMaximumLayers();
        if (layers >= max) return false;
        data.setLayers(layers + 1);
        block.setBlockData(data, false);
        return true;
    }

    private boolean shrinkSnowLayer(Block block) {
        Snow data = (Snow) block.getBlockData();
        int layers = data.getLayers();
        int min = data.getMinimumLayers();
        if (layers <= min) {
            block.setType(Material.AIR, false);
        } else {
            data.setLayers(layers - 1);
            block.setBlockData(data, false);
        }
        return true;
    }
}
