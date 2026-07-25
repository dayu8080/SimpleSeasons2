package com.realisticseasons;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class SeasonCommand implements CommandExecutor {

    private final RealisticSeasonsPlugin plugin;
    private final SeasonManager seasonManager;

    public SeasonCommand(RealisticSeasonsPlugin plugin, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "info":
                return handleInfo(sender, args);
            case "set":
                return handleSet(sender, args);
            case "next":
                return handleNext(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendUsage(sender, label);
                return true;
        }
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        // /season info [мир] - для игрока по умолчанию берём его текущий мир,
        // для консоли - можно указать мир явно, иначе покажем все включённые миры.
        if (sender instanceof Player && args.length < 2) {
            printInfo(sender, ((Player) sender).getWorld());
            return true;
        }

        if (args.length >= 2) {
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                sender.sendMessage("§cМир не найден: §f" + args[1]);
                return true;
            }
            printInfo(sender, world);
            return true;
        }

        // Консоль без указания мира - покажем все обслуживаемые миры сразу.
        for (World world : seasonManager.getEnabledWorlds()) {
            printInfo(sender, world);
        }
        return true;
    }

    private void printInfo(CommandSender sender, World world) {
        if (!seasonManager.isEnabled(world)) {
            sender.sendMessage("§cСезоны не работают в мире §f" + world.getName());
            return;
        }

        Season season = seasonManager.getSeason(world);
        int day = seasonManager.getDayProgress(world) + 1;
        int length = seasonManager.getSeasonLengthDays();

        sender.sendMessage("§8§m----------§r §b" + SeasonManager.seasonIcon(season) + " Сезон §8§m----------");
        sender.sendMessage("§7Мир: §f" + world.getName());
        sender.sendMessage("§7Сейчас: " + SeasonManager.seasonDisplay(season));
        sender.sendMessage("§7День сезона: §f" + Math.min(day, length) + "§7/§f" + length);
        sender.sendMessage("§7Следующий сезон: " + SeasonManager.seasonDisplay(season.next()));
        sender.sendMessage("§8§m--------------------------------");
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("seasons.admin")) {
            sender.sendMessage("§cНедостаточно прав.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§7Использование: /season set <SPRING|SUMMER|AUTUMN|WINTER> [мир]");
            return true;
        }
        Season season;
        try {
            season = Season.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§cНеизвестный сезон: §f" + args[1]
                    + " §7(доступно: SPRING, SUMMER, AUTUMN, WINTER)");
            return true;
        }

        World world = resolveWorld(sender, args, 2);
        if (world == null) return true;

        seasonManager.setSeason(world, season);
        sender.sendMessage("§aСезон в мире §f" + world.getName() + " §aустановлен: " + SeasonManager.seasonDisplay(season));
        return true;
    }

    private boolean handleNext(CommandSender sender, String[] args) {
        if (!sender.hasPermission("seasons.admin")) {
            sender.sendMessage("§cНедостаточно прав.");
            return true;
        }

        World world = resolveWorld(sender, args, 1);
        if (world == null) return true;

        Season current = seasonManager.getSeason(world);
        Season nextSeason = current.next();
        seasonManager.setSeason(world, nextSeason);
        sender.sendMessage("§aСезон в мире §f" + world.getName() + " §aпереключен на: " + SeasonManager.seasonDisplay(nextSeason));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("seasons.admin")) {
            sender.sendMessage("§cНедостаточно прав.");
            return true;
        }
        plugin.reloadPluginConfig();
        sender.sendMessage("§aКонфигурация RealisticSeasons перезагружена.");
        return true;
    }

    /**
     * Игрок по умолчанию работает со своим текущим миром.
     * Консоль обязана указать мир явно (аргументом по индексу worldArgIndex).
     */
    private World resolveWorld(CommandSender sender, String[] args, int worldArgIndex) {
        if (args.length > worldArgIndex) {
            World world = Bukkit.getWorld(args[worldArgIndex]);
            if (world == null) {
                sender.sendMessage("§cМир не найден: §f" + args[worldArgIndex]);
                return null;
            }
            return world;
        }
        if (sender instanceof Player) {
            return ((Player) sender).getWorld();
        }
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("§cИз консоли нужно указать мир явно, например: world");
        }
        return null;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage("§8§m----------§r §bRealisticSeasons §8§m----------");
        sender.sendMessage("§f/" + label + " info §7[мир] §8- текущий сезон");
        sender.sendMessage("§f/" + label + " set <сезон> §7[мир] §8- установить сезон");
        sender.sendMessage("§f/" + label + " next §7[мир] §8- переключить на следующий сезон");
        sender.sendMessage("§f/" + label + " reload §8- перезагрузить конфиг");
        sender.sendMessage("§8§m--------------------------------");
    }
}
