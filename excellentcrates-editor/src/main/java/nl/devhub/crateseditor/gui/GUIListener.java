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
import java.util.List;
import java.util.Set;

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

        if (!isOurGUI(title)) return;
        
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        int slot = event.getRawSlot();

        switch (title) {
            case "Crate % Editor" -> handleMainMenu(player, slot);
            case "Select Crate" -> handleCrateSelect(player, slot);
            default -> {
                if (title.startsWith("Crate:")) handleCrateEditor(player, slot);
                else if (title.startsWith("Rarity Editor:")) handleRarityEditor(player, slot);
                else if (title.startsWith("Edit Rarity:")) handleRarityWeight(player, slot);
                else if (title.contains("Rewards") && !title.equals("Select Crate") && !title.startsWith("All Rewards:")) handleRarityRewards(player, slot);
                else if (title.startsWith("All Rewards:")) handleAllRewards(player, slot);
                else if (title.startsWith("Edit:")) handleRewardEditor(player, slot);
                else if (title.startsWith("Bulk Edit:")) handleBulkEdit(player, slot);
                else if (title.startsWith("Statistics:")) handleStatistics(player, slot);
                else if (title.equals("Search Rewards")) handleSearch(player, slot);
                else if (title.startsWith("Scale:")) handleScale(player, slot);
            }
        }
    }

    private boolean isOurGUI(String title) {
        return title.equals("Crate % Editor") ||
               title.equals("Select Crate") ||
               title.startsWith("Crate:") ||
               title.startsWith("Rarity Editor:") ||
               title.startsWith("Edit Rarity:") ||
               title.contains("Rewards") ||
               title.startsWith("All Rewards:") ||
               title.startsWith("Edit:") ||
               title.startsWith("Bulk Edit:") ||
               title.startsWith("Statistics:") ||
               title.equals("Search Rewards") ||
               title.startsWith("Scale:");
    }

    private void handleMainMenu(Player player, int slot) {
        switch (slot) {
            case 0 -> gui.openCratesList(player, "balance");
            case 2 -> gui.openCratesList(player, "scale");
            case 4 -> gui.openCratesList(player, "rarity");
            case 6 -> gui.openSearch(player);
            case 8 -> gui.openCratesList(player, "bulk");
            case 11 -> gui.openCratesList(player, "edit");
            case 13 -> gui.openCratesList(player, "stats");
            case 22 -> {
                dataManager.loadAll();
                player.sendMessage(ChatColor.GREEN + "✓ Data reloaded!");
                gui.openMainMenu(player);
            }
        }
    }

    private void handleCrateSelect(Player player, int slot) {
        if (slot == 45) { gui.getPrevPage(player); gui.openCratesList(player, gui.getPlayerMode(player)); return; }
        if (slot == 50) { gui.getNextPage(player); gui.openCratesList(player, gui.getPlayerMode(player)); return; }
        if (slot == 49) { gui.openMainMenu(player); return; }

        if (slot >= 0 && slot < 45) {
            List<CrateData> crateList = new ArrayList<>(dataManager.getAllCrates());
            int page = gui.getPrevPage(player);
            int start = page * 45;
            
            if (start + slot < crateList.size()) {
                CrateData crate = crateList.get(start + slot);
                String mode = gui.getPlayerMode(player);
                if (mode == null) mode = "edit";
                
                switch (mode) {
                    case "balance" -> {
                        dataManager.balanceAllRarities(crate.getId());
                        player.sendMessage(ChatColor.GREEN + "✓ Balanced " + crate.getId());
                        gui.openMainMenu(player);
                    }
                    case "rarity" -> gui.openRarityEditor(player, crate);
                    case "stats" -> gui.openStatistics(player, crate);
                    case "bulk" -> gui.openBulkEdit(player, crate);
                    case "search" -> gui.openSearch(player);
                    default -> gui.openCrateEditor(player, crate);
                }
            }
        }
    }

    private void handleCrateEditor(Player player, int slot) {
        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) { gui.openCratesList(player, "edit"); return; }

        if (slot == 45) { dataManager.loadAll(); gui.openCrateEditor(player, dataManager.getCrate(crate.getId())); return; }
        if (slot == 49) {
            player.sendMessage(ChatColor.GOLD + "=== " + crate.getId() + " ===");
            player.sendMessage(ChatColor.GRAY + "Rarities: " + ChatColor.WHITE + crate.getRarities().size());
            player.sendMessage(ChatColor.GRAY + "Rewards: " + ChatColor.WHITE + crate.getRewards().size());
            for (RarityData r : crate.getRarities().values()) {
                player.sendMessage(ChatColor.WHITE + r.getName() + ": " + String.format("%.2f%%", crate.getRarityChance(r.getId())));
            }
            return;
        }
        if (slot == 53) { gui.openCratesList(player, "edit"); return; }
        
        if (slot == 36) { gui.openAllRewardsList(player, crate); return; }
        if (slot == 38) { dataManager.balanceAllRarities(crate.getId()); gui.openCrateEditor(player, dataManager.getCrate(crate.getId())); player.sendMessage(ChatColor.GREEN + "✓ Balanced!"); return; }
        if (slot == 40 || slot == 42) { gui.openRarityEditor(player, crate); return; }
        if (slot == 44) { gui.openBulkEdit(player, crate); return; }

        if (slot >= 0 && slot < 36) {
            List<RarityData> rarityList = new ArrayList<>(crate.getRarities().values());
            if (slot < rarityList.size()) {
                RarityData rarity = rarityList.get(slot);
                String mode = gui.getPlayerMode(player);
                if ("scale".equals(mode)) {
                    gui.setPendingScaleRarity(player, rarity.getId());
                    gui.openScaleMenu(player, crate);
                } else {
                    gui.openRarityRewardsList(player, crate, rarity);
                }
            }
        }
    }

    private void handleRarityEditor(Player player, int slot) {
        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) { gui.openMainMenu(player); return; }

        if (slot == 45) { gui.getPrevPage(player); gui.openRarityEditor(player, crate); return; }
        if (slot == 50) { gui.getNextPage(player); gui.openRarityEditor(player, crate); return; }
        if (slot == 49 || slot == 53) { gui.openCrateEditor(player, crate); return; }

        if (slot >= 0 && slot < 36) {
            List<RarityData> rarityList = new ArrayList<>(crate.getRarities().values());
            if (slot < rarityList.size()) {
                RarityData rarity = rarityList.get(slot);
                gui.openRarityWeightEditor(player, crate, rarity);
            }
        }
    }

    private void handleRarityWeight(Player player, int slot) {
        CrateData crate = gui.getPlayerCrate(player);
        String rarityId = gui.getPlayerRarity(player);
        if (crate == null || rarityId == null) { gui.openMainMenu(player); return; }

        RarityData rarity = crate.getRarity(rarityId);
        if (rarity == null) { gui.openCrateEditor(player, crate); return; }

        double currentWeight = rarity.getWeight();
        double newWeight = currentWeight;
        boolean changed = false;

        switch (slot) {
            case 10 -> { newWeight = Math.max(1, currentWeight - 10); changed = true; }
            case 11 -> { newWeight = Math.max(1, currentWeight - 1); changed = true; }
            case 13 -> { newWeight = 25; changed = true; }
            case 14 -> { newWeight = currentWeight + 1; changed = true; }
            case 16 -> { newWeight = currentWeight + 10; changed = true; }
            case 19 -> { newWeight = 70; changed = true; }
            case 20 -> { newWeight = 50; changed = true; }
            case 21 -> { newWeight = 25; changed = true; }
            case 22 -> { newWeight = 10; changed = true; }
            case 23 -> { newWeight = 5; changed = true; }
            case 31 -> { gui.openRarityEditor(player, crate); return; }
            case 35 -> { gui.openMainMenu(player); return; }
        }

        if (changed && newWeight != currentWeight) {
            dataManager.setRarityWeight(crate.getId(), rarityId, newWeight);
            crate = dataManager.getCrate(crate.getId());
            rarity = crate.getRarity(rarityId);
            if (rarity != null) {
                player.sendMessage(ChatColor.GREEN + "✓ Weight: " + newWeight + " (" + String.format("%.2f%%", crate.getRarityChance(rarityId)) + ")");
                gui.openRarityWeightEditor(player, crate, rarity);
            }
        }
    }

    private void handleRarityRewards(Player player, int slot) {
        CrateData crate = gui.getPlayerCrate(player);
        String rarityId = gui.getPlayerRarity(player);
        if (crate == null || rarityId == null) { gui.openMainMenu(player); return; }

        RarityData rarity = crate.getRarity(rarityId);
        if (rarity == null) { gui.openCrateEditor(player, crate); return; }

        if (slot == 45) { gui.getPrevPage(player); gui.openRarityRewardsList(player, crate, rarity); return; }
        if (slot == 50) { gui.getNextPage(player); gui.openRarityRewardsList(player, crate, rarity); return; }
        if (slot == 49) { dataManager.balanceRarityWeights(crate.getId(), rarityId); gui.openRarityRewardsList(player, dataManager.getCrate(crate.getId()), rarity); player.sendMessage(ChatColor.GREEN + "✓ Balanced!"); return; }
        if (slot == 53) { gui.openCrateEditor(player, crate); return; }

        if (slot >= 0 && slot < 45) {
            List<RewardData> rewardList = new ArrayList<>(crate.getRewardsByRarity(rarityId));
            int page = gui.getPrevPage(player);
            int start = page * 45;
            if (start + slot < rewardList.size()) {
                gui.openRewardEditor(player, crate, rewardList.get(start + slot), rarity);
            }
        }
    }

    private void handleAllRewards(Player player, int slot) {
        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) { gui.openMainMenu(player); return; }

        if (slot == 45) { gui.getPrevPage(player); gui.openAllRewardsList(player, crate); return; }
        if (slot == 50) { gui.getNextPage(player); gui.openAllRewardsList(player, crate); return; }
        if (slot == 49) { gui.openCrateEditor(player, crate); return; }

        if (slot >= 0 && slot < 45) {
            List<RewardData> rewards = new ArrayList<>(crate.getRewards().values());
            rewards.sort((a, b) -> Double.compare(crate.getRewardChance(b.getId()), crate.getRewardChance(a.getId())));
            int page = gui.getPrevPage(player);
            int start = page * 45;
            if (start + slot < rewards.size()) {
                RewardData reward = rewards.get(start + slot);
                RarityData rarity = crate.getRarity(reward.getRarityId());
                if (rarity != null) gui.openRewardEditor(player, crate, reward, rarity);
            }
        }
    }

    private void handleRewardEditor(Player player, int slot) {
        CrateData crate = gui.getPlayerCrate(player);
        String rarityId = gui.getPlayerRarity(player);
        String rewardId = gui.getPlayerReward(player);
        if (crate == null || rarityId == null || rewardId == null) { gui.openMainMenu(player); return; }

        RarityData rarity = crate.getRarity(rarityId);
        RewardData reward = crate.getReward(rewardId);
        if (rarity == null || reward == null) { gui.openCrateEditor(player, crate); return; }

        double currentWeight = reward.getWeight();
        double newWeight = currentWeight;
        boolean changed = false;

        switch (slot) {
            case 10 -> { newWeight = Math.max(0.5, currentWeight - 1.0); changed = true; }
            case 11 -> { newWeight = Math.max(0.5, currentWeight - 0.1); changed = true; }
            case 13 -> { newWeight = 10.0; changed = true; }
            case 14, 15, 16 -> { newWeight = currentWeight + 0.1; changed = true; }
            case 19 -> { newWeight = 50.0; changed = true; }
            case 20 -> { newWeight = 25.0; changed = true; }
            case 21 -> { newWeight = 10.0; changed = true; }
            case 22 -> { newWeight = 5.0; changed = true; }
            case 23 -> { newWeight = 1.0; changed = true; }
            case 31 -> { gui.openCrateEditor(player, crate); return; }
            case 35 -> { gui.openMainMenu(player); return; }
        }

        if (changed && newWeight != currentWeight) {
            dataManager.setRewardWeight(crate.getId(), rewardId, newWeight);
            crate = dataManager.getCrate(crate.getId());
            reward = crate.getReward(rewardId);
            rarity = crate.getRarity(rarityId);
            if (reward != null && rarity != null) {
                player.sendMessage(ChatColor.GREEN + "✓ Weight: " + newWeight + " (" + String.format("%.4f%%", crate.getRewardChance(rewardId)) + ")");
                gui.openRewardEditor(player, crate, reward, rarity);
            }
        }
    }

    private void handleBulkEdit(Player player, int slot) {
        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) { gui.openMainMenu(player); return; }

        if (slot == 45) { gui.getPrevPage(player); gui.openBulkEdit(player, crate); return; }
        if (slot == 50) { gui.getNextPage(player); gui.openBulkEdit(player, crate); return; }
        if (slot == 53) { gui.openCrateEditor(player, crate); return; }

        if (slot == 49) {
            Set<String> selected = gui.getPlayerSelectedRewards(player);
            if (!selected.isEmpty()) {
                for (String rId : selected) {
                    dataManager.setRewardWeight(crate.getId(), rId, 10.0);
                }
                player.sendMessage(ChatColor.GREEN + "✓ Balanced " + selected.size() + " rewards!");
                gui.openBulkEdit(player, dataManager.getCrate(crate.getId()));
            } else {
                player.sendMessage(ChatColor.RED + "No rewards selected!");
            }
            return;
        }

        if (slot == 48) { gui.getPlayerSelectedRewards(player).clear(); gui.openBulkEdit(player, crate); return; }

        if (slot >= 0 && slot < 45) {
            List<RewardData> rewards = new ArrayList<>(crate.getRewards().values());
            rewards.sort((a, b) -> Double.compare(crate.getRewardChance(b.getId()), crate.getRewardChance(a.getId())));
            int page = gui.getPrevPage(player);
            int start = page * 45;
            if (start + slot < rewards.size()) {
                RewardData reward = rewards.get(start + slot);
                gui.toggleRewardSelection(player, reward.getId());
                gui.openBulkEdit(player, crate);
            }
        }
    }

    private void handleStatistics(Player player, int slot) {
        if (slot == 49 || slot == 53) gui.openMainMenu(player);
    }

    private void handleSearch(Player player, int slot) {
        if (slot == 22 || slot == 49) gui.openMainMenu(player);
    }

    private void handleScale(Player player, int slot) {
        CrateData crate = gui.getPlayerCrate(player);
        String rarityId = gui.getPendingScaleRarity(player);
        if (crate == null || rarityId == null) { gui.openMainMenu(player); return; }

        RarityData rarity = crate.getRarity(rarityId);
        if (rarity == null) { gui.openCrateEditor(player, crate); return; }

        if (slot == 31) { gui.openCrateEditor(player, crate); return; }

        int[] percentages = {1, 5, 10, 25, 50, 75, 90};
        int[] slots = {19, 20, 21, 22, 23, 24, 25};

        for (int i = 0; i < percentages.length; i++) {
            if (slot == slots[i]) {
                dataManager.scaleRarityToPercentage(crate.getId(), rarityId, percentages[i]);
                crate = dataManager.getCrate(crate.getId());
                rarity = crate.getRarity(rarityId);
                if (rarity != null) {
                    player.sendMessage(ChatColor.GREEN + "✓ Scaled " + rarity.getName() + " to " + percentages[i] + "%!");
                    gui.openCrateEditor(player, crate);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            gui.clearPlayerData(player);
        }
    }
}
