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
    private final Map<UUID, String> playerEditingReward;
    private final Map<UUID, Integer> playerPage;
    private final Map<UUID, Double> pendingWeight;

    public CratesEditorGUI(ExcellentCratesEditor plugin) {
        this.plugin = plugin;
        this.playerEditingCrate = new HashMap<>();
        this.playerEditingRarity = new HashMap<>();
        this.playerEditingReward = new HashMap<>();
        this.playerPage = new HashMap<>();
        this.pendingWeight = new HashMap<>();
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "⚡ Crates Editor");

        ItemStack cratesBtn = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = cratesBtn.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "📦 Edit Crates");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Open the crate editor",
                ChatColor.GRAY + "to modify rarities and rewards",
                "",
                ChatColor.GREEN + "Click to open"
        ));
        cratesBtn.setItemMeta(meta);
        inv.setItem(11, cratesBtn);

        ItemStack balanceBtn = new ItemStack(Material.EMERALD);
        meta = balanceBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "⚖️ Auto Balance");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Balance all weights to",
                ChatColor.GRAY + "equal percentages",
                "",
                ChatColor.YELLOW + "Select a crate first"
        ));
        balanceBtn.setItemMeta(meta);
        inv.setItem(13, balanceBtn);

        ItemStack infoBtn = new ItemStack(Material.BOOK);
        meta = infoBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "📊 View Percentages");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "View all percentages",
                ChatColor.GRAY + "without editing",
                "",
                ChatColor.GREEN + "Click to open"
        ));
        infoBtn.setItemMeta(meta);
        inv.setItem(15, infoBtn);

        ItemStack reloadBtn = new ItemStack(Material.ARROW);
        meta = reloadBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "🔄 Reload Data");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Reload all crate data",
                ChatColor.GRAY + "from disk"
        ));
        reloadBtn.setItemMeta(meta);
        inv.setItem(22, reloadBtn);

        player.openInventory(inv);
    }

    public void openCratesList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Select Crate");

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

        ItemStack backBtn = new ItemStack(Material.BARRIER);
        ItemMeta meta = backBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back to Main Menu");
        backBtn.setItemMeta(meta);
        inv.setItem(49, backBtn);

        player.openInventory(inv);
    }

    public void openCrateEditor(Player player, CrateData crate) {
        playerEditingCrate.put(player.getUniqueId(), crate);
        playerEditingRarity.remove(player.getUniqueId());
        playerEditingReward.remove(player.getUniqueId());
        playerPage.put(player.getUniqueId(), 0);

        Inventory inv = Bukkit.createInventory(null, 54, 
                ChatColor.DARK_PURPLE + "Crate: " + ChatColor.WHITE + crate.getId());

        int slot = 0;
        for (RarityData rarity : crate.getRarities().values()) {
            if (slot < 36) {
                double chance = crate.getRarityChance(rarity.getId());
                ItemStack item = createRarityItem(rarity, chance, crate.getRewardsByRarity(rarity.getId()).size());
                inv.setItem(slot, item);
                slot++;
            }
        }

        // Action buttons
        ItemStack rewardsBtn = new ItemStack(Material.CHEST);
        ItemMeta meta = rewardsBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "📋 All Rewards");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "View and edit all rewards",
                ChatColor.GRAY + "across all rarities"
        ));
        rewardsBtn.setItemMeta(meta);
        inv.setItem(36, rewardsBtn);

        ItemStack balanceBtn = new ItemStack(Material.EMERALD);
        meta = balanceBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "⚖️ Balance All");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Set all weights to",
                ChatColor.GRAY + "equal percentages"
        ));
        balanceBtn.setItemMeta(meta);
        inv.setItem(40, balanceBtn);

        ItemStack scaleBtn = new ItemStack(Material.PAPER);
        meta = scaleBtn.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "📐 Scale Rarity");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Set a specific rarity",
                ChatColor.GRAY + "to a target percentage"
        ));
        scaleBtn.setItemMeta(meta);
        inv.setItem(44, scaleBtn);

        ItemStack refreshBtn = new ItemStack(Material.ARROW);
        meta = refreshBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "🔄 Refresh");
        refreshBtn.setItemMeta(meta);
        inv.setItem(45, refreshBtn);

        ItemStack infoBtn = new ItemStack(Material.BOOK);
        meta = infoBtn.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "📊 Info");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Rarities: " + crate.getRarities().size(),
                ChatColor.GRAY + "Rewards: " + crate.getRewards().size()
        ));
        infoBtn.setItemMeta(meta);
        inv.setItem(49, infoBtn);

        ItemStack backBtn = new ItemStack(Material.BARRIER);
        meta = backBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back");
        backBtn.setItemMeta(meta);
        inv.setItem(53, backBtn);

        setBorderItems(inv);

        player.openInventory(inv);
    }

    public void openRarityEditor(Player player, CrateData crate, RarityData rarity) {
        playerEditingRarity.put(player.getUniqueId(), rarity.getId());
        playerEditingReward.remove(player.getUniqueId());

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

        // Rarity info
        ItemStack rarityInfo = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = rarityInfo.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + rarity.getName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Chance: " + String.format("%.2f%%", crate.getRarityChance(rarity.getId())),
                ChatColor.GRAY + "Weight: " + rarity.getWeight(),
                ChatColor.GRAY + "Rewards: " + rewardList.size()
        ));
        rarityInfo.setItemMeta(meta);
        inv.setItem(46, rarityInfo);

        ItemStack balanceBtn = new ItemStack(Material.EMERALD);
        meta = balanceBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "⚖️ Balance Rewards");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Equal weight for all",
                ChatColor.GRAY + "rewards in this rarity"
        ));
        balanceBtn.setItemMeta(meta);
        inv.setItem(49, balanceBtn);

        ItemStack backBtn = new ItemStack(Material.ARROW);
        meta = backBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back to Crate");
        backBtn.setItemMeta(meta);
        inv.setItem(53, backBtn);

        player.openInventory(inv);
    }

    public void openRewardEditor(Player player, CrateData crate, RewardData reward, RarityData rarity) {
        playerEditingReward.put(player.getUniqueId(), reward.getId());
        double currentChance = crate.getRewardChance(reward.getId());

        Inventory inv = Bukkit.createInventory(null, 36, 
                ChatColor.DARK_GREEN + "Edit: " + ChatColor.WHITE + truncate(reward.getPreviewName(), 20));

        // Preview item
        ItemStack previewItem = new ItemStack(Material.PAPER);
        ItemMeta meta = previewItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + reward.getPreviewName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "ID: " + reward.getId(),
                ChatColor.GRAY + "Rarity: " + rarity.getName(),
                "",
                ChatColor.GOLD + "Chance: " + String.format("%.4f%%", currentChance),
                ChatColor.GRAY + "Weight: " + reward.getWeight()
        ));
        previewItem.setItemMeta(meta);
        inv.setItem(4, previewItem);

        // Weight controls
        ItemStack decreaseBtn = new ItemStack(Material.REDSTONE_BLOCK);
        meta = decreaseBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "-1.0 Weight");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Decrease weight by 1.0"
        ));
        decreaseBtn.setItemMeta(meta);
        inv.setItem(10, decreaseBtn);

        ItemStack decreaseSmallBtn = new ItemStack(Material.BRICK);
        meta = decreaseSmallBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "-0.1 Weight");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Decrease weight by 0.1"
        ));
        decreaseSmallBtn.setItemMeta(meta);
        inv.setItem(11, decreaseSmallBtn);

        ItemStack resetBtn = new ItemStack(Material.BARRIER);
        meta = resetBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Reset (10.0)");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Reset weight to 10.0"
        ));
        resetBtn.setItemMeta(meta);
        inv.setItem(13, resetBtn);

        ItemStack increaseSmallBtn = new ItemStack(Material.GOLD_INGOT);
        meta = increaseSmallBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "+0.1 Weight");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Increase weight by 0.1"
        ));
        increaseSmallBtn.setItemMeta(meta);
        inv.setItem(14, increaseSmallBtn);

        ItemStack increaseBtn = new ItemStack(Material.GOLD_BLOCK);
        meta = increaseBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "+1.0 Weight");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Increase weight by 1.0"
        ));
        increaseBtn.setItemMeta(meta);
        inv.setItem(16, increaseSmallBtn);
        inv.setItem(15, increaseSmallBtn);

        // Quick presets
        ItemStack commonBtn = new ItemStack(Material.COAL);
        meta = commonBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Common (50)");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Set weight to 50"));
        commonBtn.setItemMeta(meta);
        inv.setItem(19, commonBtn);

        ItemStack rareBtn = new ItemStack(Material.IRON_INGOT);
        meta = rareBtn.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Uncommon (25)");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Set weight to 25"));
        rareBtn.setItemMeta(meta);
        inv.setItem(20, rareBtn);

        ItemStack epicBtn = new ItemStack(Material.GOLD_INGOT);
        meta = epicBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Rare (10)");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Set weight to 10"));
        epicBtn.setItemMeta(meta);
        inv.setItem(21, epicBtn);

        ItemStack legendaryBtn = new ItemStack(Material.DIAMOND);
        meta = legendaryBtn.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Epic (5)");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Set weight to 5"));
        legendaryBtn.setItemMeta(meta);
        inv.setItem(22, legendaryBtn);

        ItemStack mythicBtn = new ItemStack(Material.NETHER_STAR);
        meta = mythicBtn.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Mythic (1)");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Set weight to 1"));
        mythicBtn.setItemMeta(meta);
        inv.setItem(23, mythicBtn);

        // Close button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        meta = closeBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Close");
        closeBtn.setItemMeta(meta);
        inv.setItem(31, closeBtn);

        ItemStack backBtn = new ItemStack(Material.ARROW);
        meta = backBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Back");
        backBtn.setItemMeta(meta);
        inv.setItem(35, backBtn);

        player.openInventory(inv);
    }

    public void openAllRewardsList(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 54, 
                ChatColor.DARK_PURPLE + "All Rewards: " + ChatColor.WHITE + crate.getId());

        List<RewardData> allRewards = new ArrayList<>(crate.getRewards().values());
        allRewards.sort((a, b) -> {
            double chanceA = crate.getRewardChance(a.getId());
            double chanceB = crate.getRewardChance(b.getId());
            return Double.compare(chanceB, chanceA);
        });

        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int start = page * 45;
        int end = Math.min(start + 45, allRewards.size());

        for (int i = 0; i < 45 && start + i < end; i++) {
            RewardData reward = allRewards.get(start + i);
            double chance = crate.getRewardChance(reward.getId());
            ItemStack item = createRewardItem(reward, chance);
            inv.setItem(i, item);
        }

        setNavigationItems(inv, page, allRewards.size());

        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta meta = backBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back to Crate");
        backBtn.setItemMeta(meta);
        inv.setItem(49, backBtn);

        player.openInventory(inv);
    }

    private ItemStack createCrateItem(CrateData crate) {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + crate.getId());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Rarities: " + ChatColor.WHITE + crate.getRarities().size());
        lore.add(ChatColor.GRAY + "Rewards: " + ChatColor.WHITE + crate.getRewards().size());
        lore.add("");
        lore.add(ChatColor.GREEN + "▶ Click to edit");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRarityItem(RarityData rarity, double chance, int rewardCount) {
        Material material = switch (rarity.getName().toLowerCase()) {
            case "legendary", "legend", "mythic" -> Material.NETHER_STAR;
            case "epic" -> Material.DIAMOND;
            case "rare" -> Material.AMETHYST_SHARD;
            case "uncommon" -> Material.EMERALD;
            case "common", "basic" -> Material.COAL;
            default -> Material.DIAMOND;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + rarity.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Weight: " + ChatColor.WHITE + rarity.getWeight());
        lore.add(ChatColor.GRAY + "Chance: " + getChanceColor(chance) + String.format("%.2f%%", chance));
        lore.add(ChatColor.GRAY + "Rewards: " + ChatColor.WHITE + rewardCount);
        lore.add("");
        lore.add(ChatColor.YELLOW + "▶ Click to edit");
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
        meta.setDisplayName(ChatColor.WHITE + truncate(displayName, 30));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Weight: " + ChatColor.WHITE + reward.getWeight());
        lore.add(ChatColor.GRAY + "Chance: " + getChanceColor(chance) + String.format("%.4f%%", chance));
        lore.add("");
        lore.add(ChatColor.YELLOW + "▶ Click to edit");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }

    private ChatColor getChanceColor(double chance) {
        if (chance >= 10) return ChatColor.GOLD;
        if (chance >= 5) return ChatColor.YELLOW;
        if (chance >= 1) return ChatColor.WHITE;
        if (chance >= 0.1) return ChatColor.GRAY;
        return ChatColor.DARK_GRAY;
    }

    private String truncate(String text, int length) {
        if (text.length() <= length) return text;
        return text.substring(0, length - 3) + "...";
    }

    private void setNavigationItems(Inventory inv, int page, int totalItems) {
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / 45.0));
        
        ItemStack prevBtn = new ItemStack(Material.ARROW);
        ItemMeta meta = prevBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "◀ Previous");
        prevBtn.setItemMeta(meta);
        
        ItemStack nextBtn = new ItemStack(Material.ARROW);
        meta = nextBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Next ▶");
        nextBtn.setItemMeta(meta);
        
        ItemStack infoBtn = new ItemStack(Material.PAPER);
        meta = infoBtn.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Page " + (page + 1) + "/" + totalPages);
        infoBtn.setItemMeta(meta);

        inv.setItem(45, page > 0 ? prevBtn : createEmptySlot());
        inv.setItem(48, infoBtn);
        inv.setItem(50, page < totalPages - 1 ? nextBtn : createEmptySlot());
    }

    private ItemStack createEmptySlot() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
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

    public String getPlayerReward(Player player) {
        return playerEditingReward.get(player.getUniqueId());
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

    public void setPage(Player player, int page) {
        playerPage.put(player.getUniqueId(), page);
    }

    public void clearPlayerData(Player player) {
        playerEditingCrate.remove(player.getUniqueId());
        playerEditingRarity.remove(player.getUniqueId());
        playerEditingReward.remove(player.getUniqueId());
        playerPage.remove(player.getUniqueId());
        pendingWeight.remove(player.getUniqueId());
    }
}
