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
import java.util.Map;
import java.util.stream.Collectors;

public class CratesBalanceCommand implements CommandExecutor, TabCompleter {

    private ExcellentCratesEditor plugin;

    public CratesBalanceCommand() {
        this.plugin = ExcellentCratesEditor.getInstance();
    }
    
    public CratesBalanceCommand(ExcellentCratesEditor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crateseditor.use")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /cb <crate> [rarity]");
            sender.sendMessage(ChatColor.GRAY + "Balance weights to equal percentages");
            return true;
        }

        CrateDataManager dm = plugin.getDataManager();
        CrateData crate = dm.getCrate(args[0]);
        if (crate == null) {
            sender.sendMessage(ChatColor.RED + "Crate not found: " + args[0]);
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Balancing " + crate.getId() + " ===");

        sender.sendMessage(ChatColor.YELLOW + "Before balancing:");
        showCurrentChances(sender, crate);

        boolean success;
        if (args.length > 1) {
            success = dm.balanceRarityWeights(args[0], args[1]);
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Balanced reward weights within rarity: " + args[1]);
            }
        } else {
            success = dm.balanceAllRarities(args[0]);
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Balanced all weights in crate");
            }
        }

        if (!success) {
            sender.sendMessage(ChatColor.RED + "Failed to balance weights");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "After balancing:");
        showCurrentChances(sender, crate);

        return true;
    }

    private void showCurrentChances(CommandSender sender, CrateData crate) {
        for (RarityData rarity : crate.getRarities().values()) {
            double rarityChance = crate.getRarityChance(rarity.getId());
            sender.sendMessage(ChatColor.AQUA + "  " + rarity.getName() + ": " + 
                    String.format("%.2f", rarityChance) + "%");

            var rewards = crate.getRewardsByRarity(rarity.getId());
            if (!rewards.isEmpty()) {
                for (var reward : rewards) {
                    double rewardChance = crate.getRewardChance(reward.getId());
                    sender.sendMessage(ChatColor.GRAY + "    - " + 
                            reward.getPreviewName() + ": " + 
                            String.format("%.4f", rewardChance) + "%");
                }
            }
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

        return completions;
    }
}
