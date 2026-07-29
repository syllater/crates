package nl.devhub.crateseditor.commands;

import nl.devhub.crateseditor.CrateDataManager;
import nl.devhub.crateseditor.CrateDataManager.CrateData;
import nl.devhub.crateseditor.ExcellentCratesEditor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CratesEditorCommand implements CommandExecutor, TabCompleter {

    private ExcellentCratesEditor plugin;

    public CratesEditorCommand() {
        this.plugin = ExcellentCratesEditor.getInstance();
    }
    
    public CratesEditorCommand(ExcellentCratesEditor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crateseditor.use")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            plugin.getGUI().openMainMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "setweight" -> handleSetWeight(sender, args);
            case "setrarity" -> handleSetRarity(sender, args);
            case "balance" -> handleBalance(sender, args);
            case "scale" -> handleScale(sender, args);
            default -> plugin.getGUI().openMainMenu(player);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== ExcellentCrates Editor ===");
        sender.sendMessage(ChatColor.YELLOW + "/ce list" + ChatColor.WHITE + " - List all crates");
        sender.sendMessage(ChatColor.YELLOW + "/ce info <crate>" + ChatColor.WHITE + " - Show crate details");
        sender.sendMessage(ChatColor.YELLOW + "/ce reload" + ChatColor.WHITE + " - Reload crate data");
        sender.sendMessage(ChatColor.YELLOW + "/ce setweight <crate> <reward> <weight>" + ChatColor.WHITE + " - Set reward weight");
        sender.sendMessage(ChatColor.YELLOW + "/ce setrarity <crate> <reward> <rarity>" + ChatColor.WHITE + " - Change reward rarity");
        sender.sendMessage(ChatColor.YELLOW + "/ce balance <crate> [rarity]" + ChatColor.WHITE + " - Balance weights");
        sender.sendMessage(ChatColor.YELLOW + "/ce scale <crate> <rarity> <percentage>" + ChatColor.WHITE + " - Scale rarity to %");
        sender.sendMessage(ChatColor.GRAY + "");
        sender.sendMessage(ChatColor.GRAY + "Shortcuts: " + ChatColor.AQUA + "/cp, /cb, /cs");
    }

    private void handleList(CommandSender sender) {
        CrateDataManager dm = plugin.getDataManager();
        if (dm.getAllCrates().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No crates found!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Crates List ===");
        for (CrateData crate : dm.getAllCrates()) {
            int rewardCount = crate.getRewards().size();
            int rarityCount = crate.getRarities().size();
            sender.sendMessage(ChatColor.YELLOW + crate.getId() + 
                    ChatColor.GRAY + " (" + rewardCount + " rewards, " + rarityCount + " rarities)");
        }
        sender.sendMessage(ChatColor.GRAY + "Total: " + dm.getAllCrates().size() + " crates");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ce info <crate>");
            return;
        }

        CrateDataManager dm = plugin.getDataManager();
        CrateData crate = dm.getCrate(args[1]);
        if (crate == null) {
            sender.sendMessage(ChatColor.RED + "Crate not found: " + args[1]);
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + crate.getId() + " ===");
        
        sender.sendMessage(ChatColor.YELLOW + "Rarities:");
        for (var entry : crate.getRarities().entrySet()) {
            var rarity = entry.getValue();
            double chance = crate.getRarityChance(entry.getKey());
            sender.sendMessage(ChatColor.AQUA + "  " + rarity.getName() + 
                    ChatColor.GRAY + " (weight: " + rarity.getWeight() + 
                    ", chance: " + String.format("%.2f", chance) + "%)");
        }

        sender.sendMessage(ChatColor.YELLOW + "Rewards:");
        for (var entry : crate.getRewards().entrySet()) {
            var reward = entry.getValue();
            double chance = crate.getRewardChance(entry.getKey());
            String rarityName = crate.getRarity(reward.getRarityId()) != null ? 
                    crate.getRarity(reward.getRarityId()).getName() : reward.getRarityId();
            sender.sendMessage(ChatColor.GREEN + "  " + reward.getPreviewName() + 
                    ChatColor.GRAY + " [weight: " + reward.getWeight() + 
                    ", chance: " + String.format("%.4f", chance) + "%]" +
                    ChatColor.DARK_GRAY + " (" + rarityName + ")");
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.getDataManager().loadAll();
        sender.sendMessage(ChatColor.GREEN + "Crate data reloaded!");
    }

    private void handleSetWeight(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /ce setweight <crate> <reward> <weight>");
            return;
        }

        CrateDataManager dm = plugin.getDataManager();
        CrateData crate = dm.getCrate(args[1]);
        if (crate == null) {
            sender.sendMessage(ChatColor.RED + "Crate not found: " + args[1]);
            return;
        }

        double weight;
        try {
            weight = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid weight: " + args[3]);
            return;
        }

        if (crate.getReward(args[2]) != null) {
            dm.setRewardWeight(args[1], args[2], weight);
            sender.sendMessage(ChatColor.GREEN + "Set weight of " + args[2] + " to " + weight);
        } else if (crate.getRarity(args[2]) != null) {
            dm.setRarityWeight(args[1], args[2], weight);
            sender.sendMessage(ChatColor.GREEN + "Set weight of rarity " + args[2] + " to " + weight);
        } else {
            sender.sendMessage(ChatColor.RED + "Reward or rarity not found: " + args[2]);
        }
    }

    private void handleSetRarity(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /ce setrarity <crate> <reward> <rarity>");
            return;
        }

        CrateDataManager dm = plugin.getDataManager();
        CrateData crate = dm.getCrate(args[1]);
        if (crate == null) {
            sender.sendMessage(ChatColor.RED + "Crate not found: " + args[1]);
            return;
        }

        if (crate.getReward(args[2]) == null) {
            sender.sendMessage(ChatColor.RED + "Reward not found: " + args[2]);
            return;
        }

        if (crate.getRarity(args[3]) == null) {
            sender.sendMessage(ChatColor.RED + "Rarity not found: " + args[3]);
            return;
        }

        dm.setRewardRarity(args[1], args[2], args[3]);
        sender.sendMessage(ChatColor.GREEN + "Moved " + args[2] + " to rarity " + args[3]);
    }

    private void handleBalance(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ce balance <crate> [rarity]");
            return;
        }

        CrateDataManager dm = plugin.getDataManager();
        CrateData crate = dm.getCrate(args[1]);
        if (crate == null) {
            sender.sendMessage(ChatColor.RED + "Crate not found: " + args[1]);
            return;
        }

        boolean success;
        if (args.length > 2) {
            success = dm.balanceRarityWeights(args[1], args[2]);
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Balanced weights for rarity " + args[2]);
            }
        } else {
            success = dm.balanceAllRarities(args[1]);
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Balanced all weights in crate " + args[1]);
            }
        }

        if (!success) {
            sender.sendMessage(ChatColor.RED + "Failed to balance weights");
        }
    }

    private void handleScale(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /ce scale <crate> <rarity> <percentage>");
            return;
        }

        CrateDataManager dm = plugin.getDataManager();
        CrateData crate = dm.getCrate(args[1]);
        if (crate == null) {
            sender.sendMessage(ChatColor.RED + "Crate not found: " + args[1]);
            return;
        }

        double percentage;
        try {
            percentage = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid percentage: " + args[3]);
            return;
        }

        if (percentage < 0 || percentage > 100) {
            sender.sendMessage(ChatColor.RED + "Percentage must be between 0 and 100");
            return;
        }

        boolean success = dm.scaleRarityToPercentage(args[1], args[2], percentage);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Scaled rarity " + args[2] + " to " + percentage + "%");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to scale rarity");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        CrateDataManager dm = plugin.getDataManager();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "info", "reload", "setweight", "setrarity", "balance", "scale"));
            return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("setweight") ||
                    args[0].equalsIgnoreCase("setrarity") || args[0].equalsIgnoreCase("balance") ||
                    args[0].equalsIgnoreCase("scale")) {
                return StringUtil.copyPartialMatches(args[1], dm.getCrateIds(), new ArrayList<>());
            }
        }

        if (args.length == 3) {
            CrateData crate = dm.getCrate(args[1]);
            if (crate != null) {
                if (args[0].equalsIgnoreCase("setweight") || args[0].equalsIgnoreCase("setrarity")) {
                    List<String> options = new ArrayList<>();
                    options.addAll(crate.getRewards().keySet());
                    options.addAll(crate.getRarities().keySet());
                    return StringUtil.copyPartialMatches(args[2], options, new ArrayList<>());
                }
                if (args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("scale")) {
                    return StringUtil.copyPartialMatches(args[2], crate.getRarities().keySet(), new ArrayList<>());
                }
            }
        }

        if (args.length == 4) {
            CrateData crate = dm.getCrate(args[1]);
            if (crate != null && args[0].equalsIgnoreCase("setrarity")) {
                return StringUtil.copyPartialMatches(args[3], crate.getRarities().keySet(), new ArrayList<>());
            }
        }

        return completions;
    }
}
