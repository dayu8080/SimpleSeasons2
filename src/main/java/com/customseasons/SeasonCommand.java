package com.customseasons;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SeasonCommand implements CommandExecutor {

    private final SimpleSeasonsPlugin plugin;
    private final SeasonManager seasonManager;

    public SeasonCommand(SimpleSeasonsPlugin plugin, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Использование: /" + label + " [info|set|next|reload]");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "info": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cЭту команду можно использовать только в игре.");
                    return true;
                }
                Player player = (Player) sender;
                Season season = seasonManager.getSeason(player.getWorld());
                sender.sendMessage("§bТекущий сезон в мире §f" + player.getWorld().getName() + "§b: §f" + season);
                return true;
            }
            case "set": {
                if (!sender.hasPermission("seasons.admin")) {
                    sender.sendMessage("§cНедостаточно прав.");
                    return true;
                }
                if (args.length < 2 || !(sender instanceof Player)) {
                    sender.sendMessage("§7Использование: /" + label + " set <SPRING|SUMMER|AUTUMN|WINTER>");
                    return true;
                }
                Season season;
                try {
                    season = Season.valueOf(args[1].toUpperCase());
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("§cНеизвестный сезон: " + args[1]);
                    return true;
                }
                Player player = (Player) sender;
                seasonManager.setSeason(player.getWorld(), season);
                sender.sendMessage("§aСезон установлен: §f" + season);
                return true;
            }
            case "next": {
                if (!sender.hasPermission("seasons.admin")) {
                    sender.sendMessage("§cНедостаточно прав.");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cЭту команду можно использовать только в игре.");
                    return true;
                }
                Player player = (Player) sender;
                Season current = seasonManager.getSeason(player.getWorld());
                Season nextSeason = current.next();
                seasonManager.setSeason(player.getWorld(), nextSeason);
                sender.sendMessage("§aСезон переключен на: §f" + nextSeason);
                return true;
            }
            case "reload": {
                if (!sender.hasPermission("seasons.admin")) {
                    sender.sendMessage("§cНедостаточно прав.");
                    return true;
                }
                plugin.reloadPluginConfig();
                sender.sendMessage("§aКонфигурация SimpleSeasons перезагружена.");
                return true;
            }
            default:
                sender.sendMessage("§7Использование: /" + label + " [info|set|next|reload]");
                return true;
        }
    }
}
