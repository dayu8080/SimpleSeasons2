package com.realisticseasons;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SeasonTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("info", "set", "next", "reload");
    private static final List<String> SEASONS = Arrays.asList("SPRING", "SUMMER", "AUTUMN", "WINTER");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set")) {
                return filter(SEASONS, args[1]);
            }
            if (sub.equals("info") || sub.equals("next")) {
                return filter(worldNames(), args[1]);
            }
            return result;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(worldNames(), args[2]);
        }

        return result;
    }

    private List<String> worldNames() {
        return Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
