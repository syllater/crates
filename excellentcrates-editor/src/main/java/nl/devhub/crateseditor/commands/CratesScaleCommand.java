package nl.devhub.crateseditor.commands;

import nl.devhub.crateseditor.CrateDataManager;
import nl.devhub.crateseditor.CrateDataManager.CrateData;
import nl.devhub.crateseditor.CrateDataManager.RarityData;
import nl.devhub.crateseditor.ExcellentCratesEditor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CratesScaleCommand implements CommandExecutor, TabCompleter {

    private final ExcellentCratesEditor plugin;

    public CratesScaleCommand(ExcellentCratesEditor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crateseditor.use")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /cs <crate> <rarity> <percentage>");
            sender.sendMessage(ChatColor.GRAY + "Scale a rarity to a specific percentage");
            sender.sendMessage(ChatColor.GRAY + "Example: /cs vote vip 25");
            return true;
        }

        CrateDataManager dm = plugin.getDataManager();
        CrateData crate = dm.getCrate(args[0]);
        if (crate == null) {
            sender.sendMessage(ChatColor.RED + "Crate not found: " + args[0]);
            return true;
        }

        RarityData rarity = crate.getRarity(args[1]);
        if (rarity == null) {
            sender.sendMessage(ChatColor.RED + "Rarity not found: " + args[1]);
            return true;
        }

        double percentage;
        try {
            percentage = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid percentage: " + args[2]);
            return true;
        }

        if (percentage < 0 || percentage > 100) {
            sender.sendMessage(ChatColor.RED + "Percentage must be between 0 and 100");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Scaling Rarity ===");
        
        double oldChance = crate.getRarityChance(args[1]);
        sender.sendMessage(ChatColor.YELLOW + "Crate: " + ChatColor.WHITE + crate.getId());
        sender.sendMessage(ChatColor.YELLOW + "Rarity: " + ChatColor.WHITE + rarity.getName());
        sender.sendMessage(ChatColor.YELLOW + "Old chance: " + ChatColor.RED + String.format("%.2f", oldChance) + "%");
        sender.sendMessage(ChatColor.YELLOW + "Target: " + ChatColor.GREEN + percentage + "%");

        sender.sendMessage(ChatColor.GRAY + "");
        sender.sendMessage(ChatColor.YELLOW + "Before:");
        showRarityChances(sender, crate);

        boolean success = dm.scaleRarityToPercentage(args[0], args[1], percentage);
        if (!success) {
            sender.sendMessage(ChatColor.RED + "Failed to scale rarity");
            return true;
        }

        sender.sendMessage(ChatColor.GRAY + "");
        sender.sendMessage(ChatColor.GREEN + "After:");
        showRarityChances(sender, crate);

        sender.sendMessage(ChatColor.GRAY + "");
        sender.sendMessage(ChatColor.GREEN + "Successfully scaled " + rarity.getName() + 
                " to " + String.format("%.2f", percentage) + "%");

        return true;
    }

    private void showRarityChances(CommandSender sender, CrateData crate) {
        List<RarityData> sortedRarities = crate.getRarities().values().stream()
                .sorted((a, b) -> {
                    double chanceA = crate.getRarityChance(a.getId());
                    double chanceB = crate.getRarityChance(b.getId());
                    return Double.compare(chanceB, chanceA);
                })
                .collect(Collectors.toList());

        for (RarityData rarity : sortedRarities) {
            double chance = crate.getRarityChance(rarity.getId());
            double weight = rarity.getWeight();
            sender.sendMessage(String.format("  %s%-10s %s[%6.2f%%] %s(weight: %.2f)",
                    ChatColor.AQUA,
                    rarity.getName(),
                    chance >= 10 ? ChatColor.GREEN : chance >= 5 ? ChatColor.YELLOW : ChatColor.RED,
                    chance,
                    ChatColor.GRAY,
                    weight
            ));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        CrateDataManager dm = plugin.getDataManager();

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], dm.getCrateIds(), new ArrayList<>());
        }

        if (args.length == 2) {
            CrateData crate = dm.getCrate(args[0]);
            if (crate != null) {
                return StringUtil.copyPartialMatches(args[1], crate.getRarities().keySet(), new ArrayList<>());
            }
        }

        if (args.length == 3) {
            String[] suggestions = {"10", "25", "50", "75", "90"};
            for (String suggestion : suggestions) {
                if (suggestion.startsWith(args[2])) {
                    completions.add(suggestion);
                }
            }
        }

        return completions;
    }
}
