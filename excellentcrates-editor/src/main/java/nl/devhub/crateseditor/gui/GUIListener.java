package nl.devhub.crateseditor.gui;

import nl.devhub.crateseditor.CrateDataManager;
import nl.devhub.crateseditor.CrateDataManager.CrateData;
import nl.devhub.crateseditor.CrateDataManager.RarityData;
import nl.devhub.crateseditor.CrateDataManager.RewardData;
import nl.devhub.crateseditor.ExcellentCratesEditor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GUIListener implements Listener {

    private final ExcellentCratesEditor plugin;
    private final CratesEditorGUI gui;
    private final CrateDataManager dataManager;

    public GUIListener(ExcellentCratesEditor plugin, CratesEditorGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
        this.dataManager = plugin.getDataManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = ChatColor.stripColor(event.getView().getTitle());

        // Main Menu
        if (title.equals("Crate % Editor")) {
            event.setCancelled(true);
            handleMainMenuClick(player, event);
        }
        // Crate Selection
        else if (title.equals("Select Crate")) {
            event.setCancelled(true);
            handleCratesListClick(player, event);
        }
        // Crate Editor
        else if (title.startsWith("Crate:")) {
            event.setCancelled(true);
            handleCrateEditorClick(player, event);
        }
        // Rarity Editor (list)
        else if (title.startsWith("Rarity Editor:")) {
            event.setCancelled(true);
            handleRarityListClick(player, event);
        }
        // Rarity Weight Editor
        else if (title.startsWith("Edit Rarity:")) {
            event.setCancelled(true);
            handleRarityWeightClick(player, event);
        }
        // Rarity Rewards List
        else if (title.contains("Rewards") && !title.equals("Select Crate")) {
            event.setCancelled(true);
            handleRarityRewardsClick(player, event);
        }
        // All Rewards List
        else if (title.startsWith("All Rewards:")) {
            event.setCancelled(true);
            handleAllRewardsClick(player, event);
        }
        // Reward Editor
        else if (title.startsWith("Edit:")) {
            event.setCancelled(true);
            handleRewardEditClick(player, event);
        }
        // Bulk Edit
        else if (title.startsWith("Bulk Edit:")) {
            event.setCancelled(true);
            handleBulkEditClick(player, event);
        }
        // Statistics
        else if (title.startsWith("Statistics:")) {
            event.setCancelled(true);
            handleStatisticsClick(player, event);
        }
        // Search Results
        else if (title.equals("Search Rewards")) {
            event.setCancelled(true);
            handleSearchResultsClick(player, event);
        }
    }

    // ========== MAIN MENU ==========
    private void handleMainMenuClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        switch (slot) {
            case 0 -> gui.openCratesList(player, "balance"); // Quick Balance
            case 2 -> gui.openCratesList(player, "scale"); // Scale Rarity
            case 4 -> gui.openCratesList(player, "rarity"); // Rarity Editor
            case 6 -> gui.openCratesList(player, "search"); // Search Rewards
            case 8 -> gui.openCratesList(player, "bulk"); // Bulk Edit
            case 11 -> gui.openCratesList(player, "edit"); // Edit Crates
            case 13 -> gui.openCratesList(player, "stats"); // Statistics
            case 22 -> { // Reload
                dataManager.loadAll();
                player.sendMessage(ChatColor.GREEN + "✓ Crate data reloaded!");
                gui.openMainMenu(player);
            }
        }
    }

    // ========== CRATES LIST ==========
    private void handleCratesListClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        String mode = gui.getPlayerMode(player);
        if (mode == null) mode = "edit";

        // Navigation
        if (slot == 45 && item.getType() == Material.ARROW) { // Previous
            gui.getPrevPage(player);
            gui.openCratesList(player, mode);
            return;
        }
        if (slot == 50 && item.getType() == Material.ARROW) { // Next
            gui.getNextPage(player);
            gui.openCratesList(player, mode);
            return;
        }
        if (slot == 49) { // Back
            gui.openMainMenu(player);
            return;
        }

        // Crate selection
        if (slot >= 0 && slot < 45) {
            List<CrateData> crateList = new ArrayList<>(dataManager.getAllCrates());
            int page = gui.getPrevPage(player);
            int start = page * 45;

            if (start + slot < crateList.size()) {
                CrateData crate = crateList.get(start + slot);
                
                switch (mode) {
                    case "balance" -> {
                        dataManager.balanceAllRarities(crate.getId());
                        player.sendMessage(ChatColor.GREEN + "✓ Balanced " + crate.getId() + "!");
                        gui.openMainMenu(player);
                    }
                    case "rarity" -> gui.openRarityEditor(player, crate);
                    case "stats" -> gui.openStatistics(player, crate);
                    case "scale" -> gui.openCrateEditor(player, crate); // Open crate for scale
                    case "bulk" -> gui.openBulkEdit(player, crate);
                    case "search" -> gui.openSearch(player);
                    default -> gui.openCrateEditor(player, crate);
                }
            }
        }
    }

    // ========== CRATE EDITOR ==========
    private void handleCrateEditorClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) {
            gui.openCratesList(player, "edit");
            return;
        }

        // Navigation buttons
        if (slot == 45) { // Refresh
            dataManager.loadAll();
            crate = dataManager.getCrate(crate.getId());
            gui.openCrateEditor(player, crate);
            player.sendMessage(ChatColor.YELLOW + "✓ Refreshed!");
            return;
        }
        if (slot == 49) { // Info
            player.sendMessage(ChatColor.GOLD + "=== " + crate.getId() + " ===");
            player.sendMessage(ChatColor.GRAY + "Rarities: " + ChatColor.WHITE + crate.getRarities().size());
            player.sendMessage(ChatColor.GRAY + "Rewards: " + ChatColor.WHITE + crate.getRewards().size());
            player.sendMessage(ChatColor.GRAY + "---");
            for (RarityData rarity : crate.getRarities().values()) {
                double chance = crate.getRarityChance(rarity.getId());
                player.sendMessage(ChatColor.WHITE + rarity.getName() + ": " + 
                        String.format("%.2f%%", chance) + " (" + rarity.getWeight() + ")");
            }
            return;
        }
        if (slot == 53) { // Back
            gui.openCratesList(player, "edit");
            return;
        }

        // Action buttons (slots 36-44)
        if (slot == 36) { // All Rewards
            gui.openAllRewardsList(player, crate);
            return;
        }
        if (slot == 38) { // Balance All
            dataManager.balanceAllRarities(crate.getId());
            crate = dataManager.getCrate(crate.getId());
            player.sendMessage(ChatColor.GREEN + "✓ All weights balanced!");
            gui.openCrateEditor(player, crate);
            return;
        }
        if (slot == 40) { // Scale - show rarity list for scaling
            gui.openRarityEditor(player, crate);
            return;
        }
        if (slot == 42) { // Edit Rarities
            gui.openRarityEditor(player, crate);
            return;
        }
        if (slot == 44) { // Bulk Select
            gui.openBulkEdit(player, crate);
            return;
        }

        // Rarity clicks (slots 0-35)
        if (slot >= 0 && slot < 36) {
            List<RarityData> rarityList = new ArrayList<>(crate.getRarities().values());
            if (slot < rarityList.size()) {
                RarityData rarity = rarityList.get(slot);
                
                String mode = gui.getPlayerMode(player);
                if ("scale".equals(mode)) {
                    // Scale mode: set rarity to target percentage
                    gui.setPendingScaleRarity(player, rarity.getId());
                    gui.openScaleMenu(player, crate);
                } else {
                    // Normal mode: show rewards
                    gui.openRarityRewardsList(player, crate, rarity);
                }
            }
        }
    }

    // ========== RARITY LIST (Rarity Editor) ==========
    private void handleRarityListClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) {
            gui.openMainMenu(player);
            return;
        }

        if (slot == 49) { // Back
            gui.openCrateEditor(player, crate);
            return;
        }
        if (slot == 45) { // Previous
            gui.getPrevPage(player);
            gui.openRarityEditor(player, crate);
            return;
        }
        if (slot == 50) { // Next
            gui.getNextPage(player);
            gui.openRarityEditor(player, crate);
            return;
        }

        // Rarity clicks (slots 0-35)
        if (slot >= 0 && slot < 36) {
            List<RarityData> rarityList = new ArrayList<>(crate.getRarities().values());
            if (slot < rarityList.size()) {
                RarityData rarity = rarityList.get(slot);
                gui.openRarityWeightEditor(player, crate, rarity);
            }
        }
    }

    // ========== RARITY WEIGHT EDITOR ==========
    private void handleRarityWeightClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        CrateData crate = gui.getPlayerCrate(player);
        String rarityId = gui.getPlayerRarity(player);
        if (crate == null || rarityId == null) {
            gui.openMainMenu(player);
            return;
        }

        RarityData rarity = crate.getRarity(rarityId);
        if (rarity == null) {
            gui.openCrateEditor(player, crate);
            return;
        }

        double currentWeight = rarity.getWeight();
        double newWeight = currentWeight;
        boolean changed = false;

        switch (slot) {
            case 10 -> { newWeight = Math.max(1, currentWeight - 10); changed = true; }
            case 11 -> { newWeight = Math.max(1, currentWeight - 1); changed = true; }
            case 13 -> { newWeight = 25; changed = true; } // Reset
            case 14 -> { newWeight = currentWeight + 1; changed = true; }
            case 16 -> { newWeight = currentWeight + 10; changed = true; }
            case 19 -> { newWeight = 70; changed = true; }
            case 20 -> { newWeight = 50; changed = true; }
            case 21 -> { newWeight = 25; changed = true; }
            case 22 -> { newWeight = 10; changed = true; }
            case 23 -> { newWeight = 5; changed = true; }
            case 31 -> gui.openCrateEditor(player, crate); // Back to crate
            case 35 -> gui.openMainMenu(player); // Close
        }

        if (changed && newWeight != currentWeight) {
            dataManager.setRarityWeight(crate.getId(), rarityId, newWeight);
            crate = dataManager.getCrate(crate.getId());
            rarity = crate.getRarity(rarityId);
            
            if (rarity != null) {
                double chance = crate.getRarityChance(rarityId);
                player.sendMessage(ChatColor.GREEN + "✓ Weight: " + newWeight + " (Chance: " + 
                        String.format("%.2f%%", chance) + ")");
                gui.openRarityWeightEditor(player, crate, rarity);
            }
        }
    }

    // ========== RARITY REWARDS LIST ==========
    private void handleRarityRewardsClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        CrateData crate = gui.getPlayerCrate(player);
        String rarityId = gui.getPlayerRarity(player);
        if (crate == null || rarityId == null) {
            gui.openMainMenu(player);
            return;
        }

        RarityData rarity = crate.getRarity(rarityId);
        if (rarity == null) {
            gui.openCrateEditor(player, crate);
            return;
        }

        // Navigation
        if (slot == 45 && item.getType() == Material.ARROW) { // Previous
            gui.getPrevPage(player);
            gui.openRarityRewardsList(player, crate, rarity);
            return;
        }
        if (slot == 50 && item.getType() == Material.ARROW) { // Next
            gui.getNextPage(player);
            gui.openRarityRewardsList(player, crate, rarity);
            return;
        }
        if (slot == 49) { // Balance this rarity
            dataManager.balanceRarityWeights(crate.getId(), rarityId);
            crate = dataManager.getCrate(crate.getId());
            rarity = crate.getRarity(rarityId);
            player.sendMessage(ChatColor.GREEN + "✓ Balanced " + rarity.getName() + " rewards!");
            gui.openRarityRewardsList(player, crate, rarity);
            return;
        }
        if (slot == 53) { // Back
            gui.openCrateEditor(player, crate);
            return;
        }

        // Reward clicks (slots 0-44)
        if (slot >= 0 && slot < 45) {
            Collection<RewardData> rewards = crate.getRewardsByRarity(rarityId);
            List<RewardData> rewardList = new ArrayList<>(rewards);
            int page = gui.getPrevPage(player);
            int start = page * 45;

            if (start + slot < rewardList.size()) {
                RewardData reward = rewardList.get(start + slot);
                gui.openRewardEditor(player, crate, reward, rarity);
            }
        }
    }

    // ========== ALL REWARDS LIST ==========
    private void handleAllRewardsClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) {
            gui.openMainMenu(player);
            return;
        }

        // Navigation
        if (slot == 45 && item.getType() == Material.ARROW) { // Previous
            gui.getPrevPage(player);
            gui.openAllRewardsList(player, crate);
            return;
        }
        if (slot == 50 && item.getType() == Material.ARROW) { // Next
            gui.getNextPage(player);
            gui.openAllRewardsList(player, crate);
            return;
        }
        if (slot == 49) { // Back
            gui.openCrateEditor(player, crate);
            return;
        }

        // Reward clicks (slots 0-44)
        if (slot >= 0 && slot < 45) {
            List<RewardData> allRewards = new ArrayList<>(crate.getRewards().values());
            allRewards.sort((a, b) -> Double.compare(
                    crate.getRewardChance(b.getId()),
                    crate.getRewardChance(a.getId())
            ));

            int page = gui.getPrevPage(player);
            int start = page * 45;

            if (start + slot < allRewards.size()) {
                RewardData reward = allRewards.get(start + slot);
                RarityData rarity = crate.getRarity(reward.getRarityId());
                if (rarity != null) {
                    gui.openRewardEditor(player, crate, reward, rarity);
                }
            }
        }
    }

    // ========== REWARD EDITOR ==========
    private void handleRewardEditClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        CrateData crate = gui.getPlayerCrate(player);
        String rarityId = gui.getPlayerRarity(player);
        String rewardId = gui.getPlayerReward(player);

        if (crate == null || rarityId == null || rewardId == null) {
            gui.openMainMenu(player);
            return;
        }

        RarityData rarity = crate.getRarity(rarityId);
        RewardData reward = crate.getReward(rewardId);

        if (reward == null || rarity == null) {
            gui.openCrateEditor(player, crate);
            return;
        }

        double currentWeight = reward.getWeight();
        double newWeight = currentWeight;
        boolean changed = false;

        switch (slot) {
            case 10 -> { newWeight = Math.max(0.5, currentWeight - 1.0); changed = true; }
            case 11 -> { newWeight = Math.max(0.5, currentWeight - 0.1); changed = true; }
            case 13 -> { newWeight = 10.0; changed = true; } // Reset
            case 14, 15, 16 -> { newWeight = currentWeight + 0.1; changed = true; }
            case 19 -> { newWeight = 50.0; changed = true; }
            case 20 -> { newWeight = 25.0; changed = true; }
            case 21 -> { newWeight = 10.0; changed = true; }
            case 22 -> { newWeight = 5.0; changed = true; }
            case 23 -> { newWeight = 1.0; changed = true; }
            case 31 -> gui.openCrateEditor(player, crate); // Back to crate
            case 35 -> gui.openMainMenu(player); // Close
        }

        if (changed && newWeight != currentWeight) {
            dataManager.setRewardWeight(crate.getId(), rewardId, newWeight);
            crate = dataManager.getCrate(crate.getId());
            reward = crate.getReward(rewardId);
            rarity = crate.getRarity(rarityId);
            
            if (reward != null && rarity != null) {
                double chance = crate.getRewardChance(rewardId);
                player.sendMessage(ChatColor.GREEN + "✓ Weight: " + newWeight + " (Chance: " + 
                        String.format("%.4f%%", chance) + ")");
                gui.openRewardEditor(player, crate, reward, rarity);
            }
        }
    }

    // ========== BULK EDIT ==========
    private void handleBulkEditClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) {
            gui.openMainMenu(player);
            return;
        }

        // Navigation
        if (slot == 45 && item.getType() == Material.ARROW) { // Previous
            gui.getPrevPage(player);
            gui.openBulkEdit(player, crate);
            return;
        }
        if (slot == 50 && item.getType() == Material.ARROW) { // Next
            gui.getNextPage(player);
            gui.openBulkEdit(player, crate);
            return;
        }
        if (slot == 53) { // Back
            gui.openCrateEditor(player, crate);
            return;
        }

        // Bulk actions
        if (slot == 49) { // Balance Selected
            Set<String> selected = gui.getPlayerSelectedRewards(player);
            if (!selected.isEmpty()) {
                for (String rId : selected) {
                    RewardData r = crate.getReward(rId);
                    if (r != null) {
                        dataManager.setRewardWeight(crate.getId(), rId, 10.0);
                    }
                }
                player.sendMessage(ChatColor.GREEN + "✓ Balanced " + selected.size() + " rewards to 10!");
                gui.openBulkEdit(player, dataManager.getCrate(crate.getId()));
            } else {
                player.sendMessage(ChatColor.RED + "No rewards selected!");
            }
            return;
        }
        if (slot == 50 && item.getType() == Material.BARRIER) { // Clear Selection
            gui.getPlayerSelectedRewards(player).clear();
            gui.openBulkEdit(player, crate);
            return;
        }

        // Reward clicks (slots 0-44) - toggle selection
        if (slot >= 0 && slot < 45) {
            List<RewardData> allRewards = new ArrayList<>(crate.getRewards().values());
            allRewards.sort((a, b) -> Double.compare(
                    crate.getRewardChance(b.getId()),
                    crate.getRewardChance(a.getId())
            ));

            int page = gui.getPrevPage(player);
            int start = page * 45;

            if (start + slot < allRewards.size()) {
                RewardData reward = allRewards.get(start + slot);
                gui.toggleRewardSelection(player, reward.getId());
                gui.openBulkEdit(player, crate);
            }
        }
    }

    // ========== STATISTICS ==========
    private void handleStatisticsClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (slot == 49 || slot == 53) { // Back
            gui.openMainMenu(player);
        }
    }

    // ========== SEARCH RESULTS ==========
    private void handleSearchResultsClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (slot == 49) { // Back
            gui.openMainMenu(player);
            return;
        }
        if (slot == 45 && item.getType() == Material.ARROW) { // Previous
            gui.getPrevPage(player);
            gui.openSearch(player);
            return;
        }
        if (slot == 50 && item.getType() == Material.ARROW) { // Next
            gui.getNextPage(player);
            gui.openSearch(player);
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            gui.clearPlayerData(player);
        }
    }
}
