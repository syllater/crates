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

        if (title.contains("Crates Editor") && !title.contains("Crate:") && !title.contains("Select Crate")) {
            event.setCancelled(true);
            handleMainMenuClick(player, event);
        }
        else if (title.equals("Select Crate")) {
            event.setCancelled(true);
            handleCratesListClick(player, event);
        }
        else if (title.startsWith("Crate:")) {
            event.setCancelled(true);
            handleCrateEditorClick(player, event);
        }
        else if (title.startsWith("Rarity:")) {
            event.setCancelled(true);
            handleRarityEditorClick(player, event);
        }
        else if (title.startsWith("Edit:") || title.contains("Reward")) {
            event.setCancelled(true);
            handleRewardEditClick(player, event);
        }
        else if (title.startsWith("All Rewards:")) {
            event.setCancelled(true);
            handleAllRewardsClick(player, event);
        }
    }

    private void handleMainMenuClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (slot == 11) { // Edit Crates
            gui.openCratesList(player);
        }
        else if (slot == 22) { // Reload
            dataManager.loadAll();
            player.sendMessage(ChatColor.GREEN + "✓ Crate data reloaded!");
            gui.openMainMenu(player);
        }
    }

    private void handleCratesListClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (slot == 49) { // Back
            gui.openMainMenu(player);
            return;
        }

        if (slot == 45) { // Previous
            gui.getPrevPage(player);
            gui.openCratesList(player);
            return;
        }
        if (slot == 50) { // Next
            gui.getNextPage(player);
            gui.openCratesList(player);
            return;
        }

        if (slot >= 0 && slot < 45) {
            List<CrateData> crateList = new ArrayList<>(dataManager.getAllCrates());
            int page = gui.getPrevPage(player);
            int start = page * 45;

            if (start + slot < crateList.size()) {
                CrateData crate = crateList.get(start + slot);
                gui.openCrateEditor(player, crate);
            }
        }
    }

    private void handleCrateEditorClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) {
            gui.openCratesList(player);
            return;
        }

        if (slot == 53) { // Back
            gui.openCratesList(player);
            return;
        }
        if (slot == 45) { // Refresh
            dataManager.loadAll();
            crate = dataManager.getCrate(crate.getId());
            gui.openCrateEditor(player, crate);
            player.sendMessage(ChatColor.YELLOW + "✓ Refreshed!");
            return;
        }
        if (slot == 40) { // Balance All
            dataManager.balanceAllRarities(crate.getId());
            crate = dataManager.getCrate(crate.getId());
            gui.openCrateEditor(player, crate);
            player.sendMessage(ChatColor.GREEN + "✓ All weights balanced!");
            return;
        }
        if (slot == 36) { // All Rewards
            gui.openAllRewardsList(player, crate);
            return;
        }
        if (slot == 44) { // Scale Rarity - show message
            player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/ce scale <crate> <rarity> <percentage>");
            return;
        }

        // Rarity clicks (slots 0-35)
        if (slot >= 0 && slot < 36) {
            List<RarityData> rarityList = new ArrayList<>(crate.getRarities().values());
            if (slot < rarityList.size()) {
                RarityData rarity = rarityList.get(slot);
                gui.openRarityEditor(player, crate, rarity);
            }
        }
    }

    private void handleRarityEditorClick(Player player, InventoryClickEvent event) {
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

        if (slot == 53) { // Back
            gui.openCrateEditor(player, crate);
            return;
        }
        if (slot == 49) { // Balance Rewards
            dataManager.balanceRarityWeights(crate.getId(), rarityId);
            crate = dataManager.getCrate(crate.getId());
            player.sendMessage(ChatColor.GREEN + "✓ Balanced rewards in " + rarity.getName());
            gui.openRarityEditor(player, crate, crate.getRarity(rarityId));
            return;
        }

        if (slot == 45) { // Previous
            gui.getPrevPage(player);
            gui.openRarityEditor(player, crate, rarity);
            return;
        }
        if (slot == 50) { // Next
            gui.getNextPage(player);
            gui.openRarityEditor(player, crate, rarity);
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

    private void handleAllRewardsClick(Player player, InventoryClickEvent event) {
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
            gui.openAllRewardsList(player, crate);
            return;
        }
        if (slot == 50) { // Next
            gui.getNextPage(player);
            gui.openAllRewardsList(player, crate);
            return;
        }

        if (slot >= 0 && slot < 45) {
            List<RewardData> allRewards = new ArrayList<>(crate.getRewards().values());
            allRewards.sort((a, b) -> {
                double chanceA = crate.getRewardChance(a.getId());
                double chanceB = crate.getRewardChance(b.getId());
                return Double.compare(chanceB, chanceA);
            });

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
            case 14, 15, 16 -> { newWeight = currentWeight + 0.1; changed = true; }
            case 19 -> { newWeight = 50.0; changed = true; }
            case 20 -> { newWeight = 25.0; changed = true; }
            case 21 -> { newWeight = 10.0; changed = true; }
            case 22 -> { newWeight = 5.0; changed = true; }
            case 23 -> { newWeight = 1.0; changed = true; }
            case 31 -> { // Close
                gui.openRarityEditor(player, crate, rarity);
                return;
            }
            case 35 -> { // Back
                gui.openRarityEditor(player, crate, rarity);
                return;
            }
        }

        if (changed && newWeight != currentWeight) {
            dataManager.setRewardWeight(crate.getId(), rewardId, newWeight);
            crate = dataManager.getCrate(crate.getId());
            reward = crate.getReward(rewardId);
            rarity = crate.getRarity(rarityId);
            
            double chance = crate.getRewardChance(rewardId);
            player.sendMessage(ChatColor.GREEN + "✓ Weight: " + newWeight + " (Chance: " + 
                    String.format("%.4f%%", chance) + ")");
        }

        if (reward != null && rarity != null) {
            gui.openRewardEditor(player, crate, reward, rarity);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            gui.clearPlayerData(player);
        }
    }
}
