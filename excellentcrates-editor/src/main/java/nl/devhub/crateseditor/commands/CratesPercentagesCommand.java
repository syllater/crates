package nl.devhub.crateseditor.commands;

import nl.devhub.crateseditor.CrateDataManager;
import nl.devhub.crateseditor.CrateDataManager.CrateData;
import nl.devhub.crateseditor.CrateDataManager.RarityData;
import nl.devhub.crateseditor.CrateDataManager.RewardData;
import nl.devhub.crateseditor.ExcellentCratesEditor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CratesPercentagesCommand implements CommandExecutor, TabCompleter {

    private final ExcellentCratesEditor plugin;
    private static final int ITEMS_PER_PAGE = 10;

    public CratesPercentagesCommand(ExcellentCratesEditor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crateseditor.use")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        CrateDataManager dm = plugin.getDataManager();

        if (args.length == 0) {
            listAllCrates(sender, dm, 1);
            return true;
        }

        String crateId = args[0];
        CrateData crate = dm.getCrate(crateId);
        if (crate == null) {
            sender.sendMessage(ChatColor.RED + "Crate not found: " + crateId);
            return true;
        }

        String rarityFilter = null;
        int page = 1;

        if (args.length > 1) {
            if (crate.getRarity(args[1]) != null) {
                rarityFilter = args[1];
                if (args.length > 2) {
                    try {
                        page = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        page = 1;
                    }
                }
            } else {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    page = 1;
                }
            }
        }

        showRewards(sender, crate, rarityFilter, page);
        return true;
    }

    private void listAllCrates(CommandSender sender, CrateDataManager dm, int page) {
        Collection<CrateData> crates = dm.getAllCrates();
        if (crates.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No crates found!");
            return;
        }

        List<CrateData> sortedCrates = crates.stream()
                .sorted(Comparator.comparing(CrateData::getId))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil(sortedCrates.size() / (double) ITEMS_PER_PAGE);
        page = Math.max(1, Math.min(page, totalPages));

        int start = (page - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, sortedCrates.size());

        sender.sendMessage(ChatColor.GOLD + "=== Crates (" + page + "/" + totalPages + ") ===");
        
        for (int i = start; i < end; i++) {
            CrateData crate = sortedCrates.get(i);
            double totalChance = crate.getRarities().values().stream()
                    .mapToDouble(r -> crate.getRarityChance(r.getId()))
                    .sum();
            sender.sendMessage(ChatColor.YELLOW + (i + 1) + ". " + crate.getId() + 
                    ChatColor.GRAY + " - " + crate.getRarities().size() + " rarities, " + 
                    crate.getRewards().size() + " rewards");
        }

        if (totalPages > 1) {
            sender.sendMessage(ChatColor.GRAY + "Use /cp <crate> [rarity] [page] to view rewards");
        }
    }

    private void showRewards(CommandSender sender, CrateData crate, String rarityFilter, int page) {
        Collection<RewardData> rewards;
        String title;

        if (rarityFilter != null) {
            RarityData rarity = crate.getRarity(rarityFilter);
            rewards = crate.getRewardsByRarity(rarityFilter);
            double rarityChance = crate.getRarityChance(rarityFilter);
            title = crate.getId() + " - " + rarity.getName() + 
                    " (Rarity Chance: " + String.format("%.2f", rarityChance) + "%)";
        } else {
            rewards = crate.getRewards().values();
            title = crate.getId();
        }

        List<RewardData> sortedRewards = rewards.stream()
                .sorted((a, b) -> {
                    double chanceA = crate.getRewardChance(a.getId());
                    double chanceB = crate.getRewardChance(b.getId());
                    return Double.compare(chanceB, chanceA);
                })
                .collect(Collectors.toList());

        if (sortedRewards.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No rewards found!");
            return;
        }

        int totalPages = (int) Math.ceil(sortedRewards.size() / (double) ITEMS_PER_PAGE);
        page = Math.max(1, Math.min(page, totalPages));

        int start = (page - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, sortedRewards.size());

        sender.sendMessage(ChatColor.GOLD + "=== " + title + " (" + page + "/" + totalPages + ") ===");
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + 
                "----------------------------------------");

        for (int i = start; i < end; i++) {
            RewardData reward = sortedRewards.get(i);
            double chance = crate.getRewardChance(reward.getId());
            
            String rarityName = crate.getRarity(reward.getRarityId()) != null ?
                    crate.getRarity(reward.getRarityId()).getName() : reward.getRarityId();

            sender.sendMessage(String.format("%s%-25s %s[Chance: %s%9.4f%%%s] %s[%sWeight: %s%.2f%s] %s(%s)%s",
                    ChatColor.WHITE,
                    reward.getPreviewName().length() > 25 ? 
                            reward.getPreviewName().substring(0, 22) + "..." : 
                            reward.getPreviewName(),
                    ChatColor.GRAY,
                    chance < 1 ? ChatColor.YELLOW : ChatColor.GREEN,
                    chance,
                    ChatColor.GRAY,
                    ChatColor.DARK_GRAY,
                    ChatColor.GRAY,
                    ChatColor.AQUA,
                    reward.getWeight(),
                    ChatColor.GRAY,
                    ChatColor.DARK_GRAY,
                    rarityName,
                    ChatColor.DARK_GRAY
            ));
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + 
                "----------------------------------------");

        if (totalPages > 1) {
            sender.sendMessage(ChatColor.GRAY + "Page " + page + " of " + totalPages);
            sender.sendMessage(ChatColor.GRAY + "Use /cp " + crate.getId() + 
                    (rarityFilter != null ? " " + rarityFilter : "") + 
                    " <page> for more");
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
                List<String> options = new ArrayList<>(crate.getRarities().keySet());
                options.add("all");
                return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
            }
        }

        return completions;
    }
}
