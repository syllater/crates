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

import java.util.Collection;
import java.util.List;
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

        String title = event.getView().getTitle();

        if (title.startsWith(ChatColor.DARK_PURPLE + "Crates Editor")) {
            event.setCancelled(true);
            handleCratesListClick(player, event);
        }
        else if (title.startsWith(ChatColor.DARK_PURPLE + "Crate:")) {
            event.setCancelled(true);
            handleCrateEditorClick(player, event);
        }
        else if (title.startsWith(ChatColor.DARK_AQUA + "Rarity:")) {
            event.setCancelled(true);
            handleRarityEditorClick(player, event);
        }
        else if (title.startsWith(ChatColor.DARK_GREEN + "Edit Reward")) {
            event.setCancelled(true);
            handleRewardEditClick(player, event);
        }
    }

    private void handleCratesListClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        Collection<CrateData> crates = dataManager.getAllCrates();
        List<CrateData> crateList = crates.stream().collect(Collectors.toList());
        int page = gui.getPlayerCrate(player) != null ? 0 : gui.getPrevPage(player);
        int start = page * 45;

        if (slot == 45) {
            gui.getPrevPage(player);
            gui.openCratesList(player);
            return;
        }
        if (slot == 48) return;
        if (slot == 50) {
            gui.getNextPage(player);
            gui.openCratesList(player);
            return;
        }

        if (slot >= 0 && slot < 45 && slot < crateList.size()) {
            CrateData crate = crateList.get(start + slot);
            gui.openCrateEditor(player, crate);
        }
    }

    private void handleCrateEditorClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) return;

        if (slot == 49) {
            String rarityId = gui.getPlayerRarity(player);
            if (rarityId != null) {
                RarityData rarity = crate.getRarity(rarityId);
                if (rarity != null) {
                    gui.openRarityEditor(player, crate, rarity);
                }
            }
            return;
        }

        if (slot == 50) {
            dataManager.balanceAllRarities(crate.getId());
            player.sendMessage(ChatColor.GREEN + "Balanced all weights in " + crate.getId());
            gui.openCrateEditor(player, crate);
            return;
        }

        if (slot == 53) {
            dataManager.loadAll();
            crate = dataManager.getCrate(crate.getId());
            gui.openCrateEditor(player, crate);
            player.sendMessage(ChatColor.YELLOW + "Reloaded crate data");
            return;
        }

        if (slot >= 0 && slot < 45) {
            List<RarityData> rarityList = crate.getRarities().values().stream().collect(Collectors.toList());
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
        if (crate == null || rarityId == null) return;

        RarityData rarity = crate.getRarity(rarityId);
        if (rarity == null) return;

        if (slot == 48) {
            gui.openCrateEditor(player, crate);
            return;
        }

        if (slot == 49) {
            dataManager.balanceRarityWeights(crate.getId(), rarityId);
            player.sendMessage(ChatColor.GREEN + "Balanced rewards in " + rarity.getName());
            gui.openRarityEditor(player, crate, rarity);
            return;
        }

        if (slot == 45) {
            gui.getPrevPage(player);
            gui.openRarityEditor(player, crate, rarity);
            return;
        }
        if (slot == 50) {
            gui.getNextPage(player);
            gui.openRarityEditor(player, crate, rarity);
            return;
        }

        if (slot >= 0 && slot < 45) {
            Collection<RewardData> rewards = crate.getRewardsByRarity(rarityId);
            List<RewardData> rewardList = rewards.stream().collect(Collectors.toList());
            int page = gui.getPrevPage(player);
            int start = page * 45;

            if (start + slot < rewardList.size()) {
                RewardData reward = rewardList.get(start + slot);
                gui.openRewardEditor(player, crate, reward);
            }
        }
    }

    private void handleRewardEditClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        CrateData crate = gui.getPlayerCrate(player);
        if (crate == null) return;

        String rarityId = gui.getPlayerRarity(player);
        if (rarityId == null) return;

        RarityData rarity = crate.getRarity(rarityId);
        if (rarity == null) return;

        List<RewardData> rewards = crate.getRewardsByRarity(rarityId);
        if (rewards.isEmpty()) return;

        RewardData reward = rewards.iterator().next();

        double currentWeight = reward.getWeight();
        double newWeight = currentWeight;

        switch (slot) {
            case 10 -> newWeight = Math.max(0.5, currentWeight - 1);
            case 11 -> newWeight = Math.max(0.5, currentWeight - 0.5);
            case 15 -> newWeight = currentWeight + 0.5;
            case 16 -> newWeight = currentWeight + 1;
            case 22 -> newWeight = 10;
            case 26 -> {
                gui.openRarityEditor(player, crate, rarity);
                return;
            }
            default -> { return; }
        }

        dataManager.setRewardWeight(crate.getId(), reward.getId(), newWeight);
        crate = dataManager.getCrate(crate.getId());
        reward = crate.getReward(reward.getId());
        
        player.sendMessage(ChatColor.GREEN + "Weight set to " + newWeight + 
                " (Chance: " + String.format("%.4f%%", crate.getRewardChance(reward.getId())) + ")");
        
        gui.openRewardEditor(player, crate, reward);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            String title = event.getView().getTitle();
            if (title.startsWith(ChatColor.DARK_PURPLE + "Crates Editor") ||
                title.startsWith(ChatColor.DARK_PURPLE + "Crate:") ||
                title.startsWith(ChatColor.DARK_AQUA + "Rarity:") ||
                title.startsWith(ChatColor.DARK_GREEN + "Edit Reward")) {
                gui.clearPlayerData(player);
            }
        }
    }
}
