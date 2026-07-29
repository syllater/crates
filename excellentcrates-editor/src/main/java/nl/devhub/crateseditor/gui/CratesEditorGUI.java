package nl.devhub.crateseditor.gui;

import nl.devhub.crateseditor.CrateDataManager;
import nl.devhub.crateseditor.CrateDataManager.CrateData;
import nl.devhub.crateseditor.CrateDataManager.RarityData;
import nl.devhub.crateseditor.CrateDataManager.RewardData;
import nl.devhub.crateseditor.ExcellentCratesEditor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CratesEditorGUI {

    private final ExcellentCratesEditor plugin;
    private final Map<UUID, CrateData> playerEditingCrate;
    private final Map<UUID, String> playerEditingRarity;
    private final Map<UUID, Integer> playerPage;

    public CratesEditorGUI(ExcellentCratesEditor plugin) {
        this.plugin = plugin;
        this.playerEditingCrate = new HashMap<>();
        this.playerEditingRarity = new HashMap<>();
        this.playerPage = new HashMap<>();
    }

    public void openCratesList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Crates Editor");

        Collection<CrateData> crates = plugin.getDataManager().getAllCrates();
        List<CrateData> crateList = new ArrayList<>(crates);
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int start = page * 45;
        int end = Math.min(start + 45, crateList.size());

        for (int i = 0; i < 45 && start + i < end; i++) {
            CrateData crate = crateList.get(start + i);
            ItemStack item = createCrateItem(crate);
            inv.setItem(i, item);
        }

        setNavigationItems(inv, page, crateList.size());

        player.openInventory(inv);
    }

    public void openCrateEditor(Player player, CrateData crate) {
        playerEditingCrate.put(player.getUniqueId(), crate);
        playerEditingRarity.remove(player.getUniqueId());
        playerPage.put(player.getUniqueId(), 0);

        Inventory inv = Bukkit.createInventory(null, 54, 
                ChatColor.DARK_PURPLE + "Crate: " + ChatColor.WHITE + crate.getId());

        int slot = 0;
        for (RarityData rarity : crate.getRarities().values()) {
            if (slot < 45) {
                double chance = crate.getRarityChance(rarity.getId());
                ItemStack item = createRarityItem(rarity, chance, crate.getRewardsByRarity(rarity.getId()).size());
                inv.setItem(slot, item);
                slot++;
            }
        }

        ItemStack rewardsBtn = new ItemStack(Material.CHEST);
        ItemMeta meta = rewardsBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Edit Rewards");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to edit reward weights"
        ));
        rewardsBtn.setItemMeta(meta);
        inv.setItem(49, rewardsBtn);

        ItemStack balanceBtn = new ItemStack(Material.EMERALD);
        meta = balanceBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Balance All Weights");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Set all weights to equal percentages"
        ));
        balanceBtn.setItemMeta(meta);
        inv.setItem(50, balanceBtn);

        ItemStack refreshBtn = new ItemStack(Material.ARROW);
        meta = refreshBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Refresh");
        refreshBtn.setItemMeta(meta);
        inv.setItem(53, refreshBtn);

        setBorderItems(inv);

        player.openInventory(inv);
    }

    public void openRarityEditor(Player player, CrateData crate, RarityData rarity) {
        playerEditingRarity.put(player.getUniqueId(), rarity.getId());

        String title = ChatColor.DARK_AQUA + "Rarity: " + ChatColor.WHITE + rarity.getName();
        Inventory inv = Bukkit.createInventory(null, 54, title);

        Collection<RewardData> rewards = crate.getRewardsByRarity(rarity.getId());
        List<RewardData> rewardList = new ArrayList<>(rewards);
        
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int start = page * 45;
        int end = Math.min(start + 45, rewardList.size());

        for (int i = 0; i < 45 && start + i < end; i++) {
            RewardData reward = rewardList.get(start + i);
            double chance = crate.getRewardChance(reward.getId());
            ItemStack item = createRewardItem(reward, chance);
            inv.setItem(i, item);
        }

        setNavigationItems(inv, page, rewardList.size());

        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta meta = backBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back to Crate");
        backBtn.setItemMeta(meta);
        inv.setItem(48, backBtn);

        ItemStack balanceBtn = new ItemStack(Material.EMERALD);
        meta = balanceBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Balance Rewards");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Equal weight for all rewards in this rarity"
        ));
        balanceBtn.setItemMeta(meta);
        inv.setItem(49, balanceBtn);

        setBorderItems(inv);

        player.openInventory(inv);
    }

    public void openRewardEditor(Player player, CrateData crate, RewardData reward) {
        Inventory inv = Bukkit.createInventory(null, 27, 
                ChatColor.DARK_GREEN + "Edit Reward");

        ItemStack previewItem = new ItemStack(Material.PAPER);
        ItemMeta meta = previewItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + reward.getPreviewName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Reward: " + reward.getId(),
                ChatColor.GRAY + "Current weight: " + reward.getWeight(),
                ChatColor.GRAY + "Current chance: " + String.format("%.4f%%", 
                        crate.getRewardChance(reward.getId()))
        ));
        previewItem.setItemMeta(meta);
        inv.setItem(4, previewItem);

        ItemStack decreaseBtn = new ItemStack(Material.REDSTONE_BLOCK);
        meta = decreaseBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Decrease Weight");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to decrease weight by 1"
        ));
        decreaseBtn.setItemMeta(meta);
        inv.setItem(10, decreaseBtn);

        ItemStack increaseBtn = new ItemStack(Material.GOLD_BLOCK);
        meta = increaseBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Increase Weight");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to increase weight by 1"
        ));
        increaseBtn.setItemMeta(meta);
        inv.setItem(16, increaseBtn);

        ItemStack decreaseSmallBtn = new ItemStack(Material.BRICK);
        meta = decreaseSmallBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Decrease by 0.5");
        decreaseSmallBtn.setItemMeta(meta);
        inv.setItem(11, decreaseSmallBtn);

        ItemStack increaseSmallBtn = new ItemStack(Material.GOLD_INGOT);
        meta = increaseSmallBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Increase by 0.5");
        increaseSmallBtn.setItemMeta(meta);
        inv.setItem(15, increaseSmallBtn);

        ItemStack resetBtn = new ItemStack(Material.BARRIER);
        meta = resetBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Reset to 10");
        resetBtn.setItemMeta(meta);
        inv.setItem(22, resetBtn);

        ItemStack closeBtn = new ItemStack(Material.BOOK);
        meta = closeBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Close");
        closeBtn.setItemMeta(meta);
        inv.setItem(26, closeBtn);

        player.openInventory(inv);
    }

    private ItemStack createCrateItem(CrateData crate) {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + crate.getId());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Rarities: " + crate.getRarities().size());
        lore.add(ChatColor.GRAY + "Rewards: " + crate.getRewards().size());
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to edit");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRarityItem(RarityData rarity, double chance, int rewardCount) {
        Material material = switch (rarity.getName().toLowerCase()) {
            case "legendary", "legend" -> Material.NETHER_STAR;
            case "epic", "rare" -> Material.AMETHYST_SHARD;
            case "uncommon", "uncommon" -> Material.EMERALD;
            case "common", "basic" -> Material.COAL;
            default -> Material.DIAMOND;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + rarity.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Weight: " + rarity.getWeight());
        lore.add(ChatColor.GRAY + "Chance: " + String.format("%.2f%%", chance));
        lore.add(ChatColor.GRAY + "Rewards: " + rewardCount);
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to edit rarity");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRewardItem(RewardData reward, double chance) {
        Material material = Material.PAPER;
        if (chance >= 10) material = Material.GOLD_INGOT;
        else if (chance >= 1) material = Material.IRON_INGOT;
        else if (chance >= 0.1) material = Material.COPPER_INGOT;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String displayName = reward.getPreviewName();
        if (displayName.length() > 30) {
            displayName = displayName.substring(0, 27) + "...";
        }
        meta.setDisplayName(ChatColor.WHITE + displayName);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Weight: " + ChatColor.WHITE + reward.getWeight());
        lore.add(ChatColor.GRAY + "Chance: " + 
                (chance >= 1 ? ChatColor.GREEN : chance >= 0.1 ? ChatColor.YELLOW : ChatColor.RED) +
                String.format("%.4f%%", chance));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to edit weight");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }

    private void setNavigationItems(Inventory inv, int page, int totalItems) {
        int totalPages = (int) Math.ceil(totalItems / 45.0);
        
        ItemStack prevBtn = new ItemStack(Material.ARROW);
        ItemMeta meta = prevBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Previous Page");
        prevBtn.setItemMeta(meta);
        
        ItemStack nextBtn = new ItemStack(Material.ARROW);
        meta = nextBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Next Page");
        nextBtn.setItemMeta(meta);
        
        ItemStack infoBtn = new ItemStack(Material.BOOK);
        meta = infoBtn.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Page " + (page + 1) + "/" + Math.max(1, totalPages));
        infoBtn.setItemMeta(meta);

        inv.setItem(45, page > 0 ? prevBtn : null);
        inv.setItem(48, infoBtn);
        inv.setItem(50, page < totalPages - 1 ? nextBtn : null);
    }

    private void setBorderItems(Inventory inv) {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);

        int[] borderSlots = {46, 47, 51, 52};
        for (int slot : borderSlots) {
            inv.setItem(slot, border);
        }
    }

    public CrateData getPlayerCrate(Player player) {
        return playerEditingCrate.get(player.getUniqueId());
    }

    public String getPlayerRarity(Player player) {
        return playerEditingRarity.get(player.getUniqueId());
    }

    public int getNextPage(Player player) {
        int current = playerPage.getOrDefault(player.getUniqueId(), 0);
        playerPage.put(player.getUniqueId(), current + 1);
        return current + 1;
    }

    public int getPrevPage(Player player) {
        int current = playerPage.getOrDefault(player.getUniqueId(), 0);
        playerPage.put(player.getUniqueId(), Math.max(0, current - 1));
        return Math.max(0, current - 1);
    }

    public void clearPlayerData(Player player) {
        playerEditingCrate.remove(player.getUniqueId());
        playerEditingRarity.remove(player.getUniqueId());
        playerPage.remove(player.getUniqueId());
    }
}
