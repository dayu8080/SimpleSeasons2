package com.customseasons;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Замедляет рост растений вне их сезона. Плагин ничего не трогает,
 * если блок не описан в growth.crops.
 */
public class GrowthListener implements Listener {

    private final SimpleSeasonsPlugin plugin;
    private final SeasonManager seasonManager;
    private final Random random = new Random();

    private double offSeasonChance;
    private double treesOffSeasonChance;
    private final Map<Material, Set<Season>> crops = new EnumMap<>(Material.class);

    public GrowthListener(SimpleSeasonsPlugin plugin, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();
        offSeasonChance = cfg.getDouble("growth.off-season-chance", 0.15);
        treesOffSeasonChance = cfg.getDouble("growth.trees-off-season-chance", 0.25);

        crops.clear();
        if (cfg.isConfigurationSection("growth.crops")) {
            for (String key : cfg.getConfigurationSection("growth.crops").getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m == null) continue;
                Set<Season> seasons = EnumSet.noneOf(Season.class);
                List<String> list = cfg.getStringList("growth.crops." + key);
                for (String s : list) {
                    try {
                        seasons.add(Season.valueOf(s.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                        // некорректное имя сезона в конфиге - пропускаем
                    }
                }
                crops.put(m, seasons);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        Material type = event.getNewState().getType();
        Set<Season> allowed = crops.get(type);
        if (allowed == null) return; // блок не настроен - сезоны на него не влияют

        World world = event.getBlock().getWorld();
        if (!seasonManager.isEnabled(world)) return;

        Season current = seasonManager.getSeason(world);
        if (allowed.contains(current)) return; // сезон подходящий - растим как обычно

        if (random.nextDouble() >= offSeasonChance) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        World world = event.getWorld();
        if (!seasonManager.isEnabled(world)) return;

        // Для деревьев в конфиге нет списка сезонов - зимой рост саженцев
        // просто замедляется на trees-off-season-chance.
        Season current = seasonManager.getSeason(world);
        if (current != Season.WINTER) return;

        if (random.nextDouble() >= treesOffSeasonChance) {
            event.setCancelled(true);
        }
    }
}
