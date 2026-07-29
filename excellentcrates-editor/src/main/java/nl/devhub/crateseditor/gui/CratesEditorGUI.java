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
import java.util.stream.Collectors;

public class CratesEditorGUI {

    private final ExcellentCratesEditor plugin;
    private final Map<UUID, CrateData> playerEditingCrate;
    private final Map<UUID, String> playerEditingRarity;
    private final Map<UUID, String> playerEditingReward;
    private final Map<UUID, Integer> playerPage;
    private final Map<UUID, String> playerSearch;
    private final Map<UUID, Set<String>> playerSelectedRewards;
    private final Map<UUID, String> pendingScaleRarity;

    public CratesEditorGUI(ExcellentCratesEditor plugin) {
        this.plugin = plugin;
        this.playerEditingCrate = new HashMap<>();
        this.playerEditingRarity = new HashMap<>();
        this.playerEditingReward = new HashMap<>();
        this.playerPage = new HashMap<>();
        this.playerSearch = new HashMap<>();
        this.playerSelectedRewards = new HashMap<>();
        this.pendingScaleRarity = new HashMap<>();
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Crate % Editor");

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

        ItemStack quickBalanceBtn = new ItemStack(Material.EMERALD);
        meta = quickBalanceBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "⚡ Quick Balance");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Select crate and balance",
                ChatColor.GRAY + "all weights to equal %",
                "",
                ChatColor.YELLOW + "Select crate first"
        ));
        quickBalanceBtn.setItemMeta(meta);
        inv.setItem(0, quickBalanceBtn);

        ItemStack scaleBtn = new ItemStack(Material.PAPER);
        meta = scaleBtn.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "📐 Scale Rarity");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Set specific rarity to",
                ChatColor.GRAY + "exact percentage",
                "",
                ChatColor.YELLOW + "Select crate first"
        ));
        scaleBtn.setItemMeta(meta);
        inv.setItem(2, scaleBtn);

        ItemStack rarityBtn = new ItemStack(Material.BEACON);
        meta = rarityBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "🎯 Rarity Editor");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Edit rarity weights",
                ChatColor.GRAY + "and chances directly",
                "",
                ChatColor.GREEN + "Click to open"
        ));
        rarityBtn.setItemMeta(meta);
        inv.setItem(4, rarityBtn);

        ItemStack searchBtn = new ItemStack(Material.NAME_TAG);
        meta = searchBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "🔍 Search Rewards");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Find rewards by name",
                ChatColor.GRAY + "across all crates",
                "",
                ChatColor.GREEN + "Click to search"
        ));
        searchBtn.setItemMeta(meta);
        inv.setItem(6, searchBtn);

        ItemStack bulkBtn = new ItemStack(Material.HOPPER);
        meta = bulkBtn.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Bulk Edit");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Select multiple rewards",
                ChatColor.GRAY + "and edit them together",
                "",
                ChatColor.YELLOW + "Go to crate first"
        ));
        bulkBtn.setItemMeta(meta);
        inv.setItem(8, bulkBtn);

        ItemStack statsBtn = new ItemStack(Material.BOOK);
        meta = statsBtn.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "📊 Statistics");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "View crate statistics",
                ChatColor.GRAY + "and percentage breakdown",
                "",
                ChatColor.GREEN + "Click to view"
        ));
        statsBtn.setItemMeta(meta);
        inv.setItem(13, statsBtn);

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

    public void openCratesList(Player player, String mode) {
        playerSearch.put(player.getUniqueId(), mode);
        playerPage.put(player.getUniqueId(), 0);
        
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

        // Mode indicator
        ItemStack modeItem = new ItemStack(Material.PAPER);
        ItemMeta meta = modeItem.getItemMeta();
        String modeName = switch (mode) {
            case "balance" -> "⚡ Quick Balance";
            case "scale" -> "📐 Scale Rarity";
            case "rarity" -> "🎯 Rarity Editor";
            case "search" -> "🔍 Search";
            case "bulk" -> "Bulk Edit";
            default -> "📦 Edit";
        };
        meta.setDisplayName(ChatColor.WHITE + "Mode: " + modeName);
        modeItem.setItemMeta(meta);
        inv.setItem(48, modeItem);

        ItemStack backBtn = new ItemStack(Material.BARRIER);
        meta = backBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back to Main Menu");
        backBtn.setItemMeta(meta);
        inv.setItem(53, backBtn);

        player.openInventory(inv);
    }

    public void openCrateEditor(Player player, CrateData crate) {
        playerEditingCrate.put(player.getUniqueId(), crate);
        playerEditingRarity.remove(player.getUniqueId());
        playerEditingReward.remove(player.getUniqueId());
        playerPage.put(player.getUniqueId(), 0);
        playerSelectedRewards.put(player.getUniqueId(), new HashSet<>());

        Inventory inv = Bukkit.createInventory(null, 54, 
                ChatColor.DARK_PURPLE + "Crate: " + ChatColor.WHITE + crate.getId());

        int slot = 0;
        
        // Show rarities if available
        if (!crate.getRarities().isEmpty()) {
            for (RarityData rarity : crate.getRarities().values()) {
                if (slot < 36) {
                    double chance = crate.getRarityChance(rarity.getId());
                    ItemStack item = createRarityItem(rarity, chance, crate.getRewardsByRarity(rarity.getId()).size());
                    inv.setItem(slot, item);
                    slot++;
                }
            }
        } else {
            // If no rarities, show all rewards directly
            List<RewardData> rewards = new ArrayList<>(crate.getRewards().values());
            rewards.sort((a, b) -> Double.compare(crate.getRewardChance(b.getId()), crate.getRewardChance(a.getId())));
            
            for (RewardData reward : rewards) {
                if (slot < 36) {
                    double chance = crate.getRewardChance(reward.getId());
                    ItemStack item = createRewardItem(reward, chance);
                    inv.setItem(slot, item);
                    slot++;
                }
            }
        }

        // Action buttons
        ItemStack rewardsBtn = new ItemStack(Material.CHEST);
        ItemMeta meta = rewardsBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "📋 All Rewards");
        rewardsBtn.setItemMeta(meta);
        inv.setItem(36, rewardsBtn);

        ItemStack balanceBtn = new ItemStack(Material.EMERALD);
        meta = balanceBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "⚡ Balance All");
        balanceBtn.setItemMeta(meta);
        inv.setItem(38, balanceBtn);

        ItemStack scaleBtn = new ItemStack(Material.PAPER);
        meta = scaleBtn.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "📐 Scale");
        scaleBtn.setItemMeta(meta);
        inv.setItem(40, scaleBtn);

        ItemStack rarityBtn = new ItemStack(Material.BEACON);
        meta = rarityBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "🎯 Edit Rarities");
        rarityBtn.setItemMeta(meta);
        inv.setItem(42, rarityBtn);

        ItemStack bulkBtn = new ItemStack(Material.HOPPER);
        meta = bulkBtn.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "📋 Bulk Select");
        bulkBtn.setItemMeta(meta);
        inv.setItem(44, bulkBtn);

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

    public void openRarityWeightEditor(Player player, CrateData crate, RarityData rarity) {
        playerEditingRarity.put(player.getUniqueId(), rarity.getId());
        double currentChance = crate.getRarityChance(rarity.getId());

        Inventory inv = Bukkit.createInventory(null, 36, 
                ChatColor.GOLD + "Edit Rarity: " + ChatColor.WHITE + rarity.getName());

        // Preview item
        ItemStack previewItem = new ItemStack(Material.BEACON);
        ItemMeta meta = previewItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + rarity.getName());
        meta.setLore(Arrays.asList(
                ChatColor.GOLD + "Current Chance: " + String.format("%.2f%%", currentChance),
                ChatColor.GRAY + "Current Weight: " + rarity.getWeight(),
                ChatColor.GRAY + "Rewards: " + crate.getRewardsByRarity(rarity.getId()).size()
        ));
        previewItem.setItemMeta(meta);
        inv.setItem(4, previewItem);

        // Weight controls
        ItemStack decreaseBtn = new ItemStack(Material.REDSTONE_BLOCK);
        meta = decreaseBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "-10 Weight");
        decreaseBtn.setItemMeta(meta);
        inv.setItem(10, decreaseBtn);

        ItemStack decreaseSmallBtn = new ItemStack(Material.BRICK);
        meta = decreaseSmallBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "-1 Weight");
        decreaseSmallBtn.setItemMeta(meta);
        inv.setItem(11, decreaseSmallBtn);

        ItemStack resetBtn = new ItemStack(Material.BARRIER);
        meta = resetBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Reset (25)");
        resetBtn.setItemMeta(meta);
        inv.setItem(13, resetBtn);

        ItemStack increaseSmallBtn = new ItemStack(Material.GOLD_INGOT);
        meta = increaseSmallBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "+1 Weight");
        increaseSmallBtn.setItemMeta(meta);
        inv.setItem(14, increaseSmallBtn);

        ItemStack increaseBtn = new ItemStack(Material.GOLD_BLOCK);
        meta = increaseBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "+10 Weight");
        increaseBtn.setItemMeta(meta);
        inv.setItem(16, increaseBtn);

        // Quick presets
        ItemStack p1Btn = new ItemStack(Material.COAL);
        meta = p1Btn.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Common (70)");
        p1Btn.setItemMeta(meta);
        inv.setItem(19, p1Btn);

        ItemStack p2Btn = new ItemStack(Material.IRON_INGOT);
        meta = p2Btn.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Uncommon (50)");
        p2Btn.setItemMeta(meta);
        inv.setItem(20, p2Btn);

        ItemStack p3Btn = new ItemStack(Material.GOLD_INGOT);
        meta = p3Btn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Rare (25)");
        p3Btn.setItemMeta(meta);
        inv.setItem(21, p3Btn);

        ItemStack p4Btn = new ItemStack(Material.DIAMOND);
        meta = p4Btn.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Epic (10)");
        p4Btn.setItemMeta(meta);
        inv.setItem(22, p4Btn);

        ItemStack p5Btn = new ItemStack(Material.NETHER_STAR);
        meta = p5Btn.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Legendary (5)");
        p5Btn.setItemMeta(meta);
        inv.setItem(23, p5Btn);

        // Back button
        ItemStack backBtn = new ItemStack(Material.ARROW);
        meta = backBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back");
        backBtn.setItemMeta(meta);
        inv.setItem(31, backBtn);

        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        meta = closeBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Close");
        closeBtn.setItemMeta(meta);
        inv.setItem(35, closeBtn);

        player.openInventory(inv);
    }

    public void openRarityRewardsList(Player player, CrateData crate, RarityData rarity) {
        playerEditingRarity.put(player.getUniqueId(), rarity.getId());
        playerPage.put(player.getUniqueId(), 0);

        String title = ChatColor.DARK_AQUA + rarity.getName() + " Rewards";
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
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "⚡ Balance");
        balanceBtn.setItemMeta(meta);
        inv.setItem(49, balanceBtn);

        ItemStack backBtn = new ItemStack(Material.ARROW);
        meta = backBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back");
        backBtn.setItemMeta(meta);
        inv.setItem(53, backBtn);

        player.openInventory(inv);
    }

    public void openRewardEditor(Player player, CrateData crate, RewardData reward, RarityData rarity) {
        playerEditingReward.put(player.getUniqueId(), reward.getId());
        double currentChance = crate.getRewardChance(reward.getId());
        String rarityName = rarity != null ? rarity.getName() : "Unknown";

        Inventory inv = Bukkit.createInventory(null, 36, 
                ChatColor.DARK_GREEN + "Edit: " + ChatColor.WHITE + truncate(reward.getPreviewName(), 20));

        // Preview item
        ItemStack previewItem = new ItemStack(Material.PAPER);
        ItemMeta meta = previewItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + reward.getPreviewName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "ID: " + reward.getId(),
                ChatColor.GRAY + "Rarity: " + rarityName,
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
        decreaseBtn.setItemMeta(meta);
        inv.setItem(10, decreaseBtn);

        ItemStack decreaseSmallBtn = new ItemStack(Material.BRICK);
        meta = decreaseSmallBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "-0.1 Weight");
        decreaseSmallBtn.setItemMeta(meta);
        inv.setItem(11, decreaseSmallBtn);

        ItemStack resetBtn = new ItemStack(Material.BARRIER);
        meta = resetBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Reset (10.0)");
        resetBtn.setItemMeta(meta);
        inv.setItem(13, resetBtn);

        ItemStack increaseSmallBtn = new ItemStack(Material.GOLD_INGOT);
        meta = increaseSmallBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "+0.1 Weight");
        increaseSmallBtn.setItemMeta(meta);
        inv.setItem(14, increaseSmallBtn);

        ItemStack increaseBtn = new ItemStack(Material.GOLD_BLOCK);
        meta = increaseBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "+1.0 Weight");
        increaseBtn.setItemMeta(meta);
        inv.setItem(16, increaseBtn);

        // Quick presets
        ItemStack commonBtn = new ItemStack(Material.COAL);
        meta = commonBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Common (50)");
        commonBtn.setItemMeta(meta);
        inv.setItem(19, commonBtn);

        ItemStack rareBtn = new ItemStack(Material.IRON_INGOT);
        meta = rareBtn.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Uncommon (25)");
        rareBtn.setItemMeta(meta);
        inv.setItem(20, rareBtn);

        ItemStack epicBtn = new ItemStack(Material.GOLD_INGOT);
        meta = epicBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Rare (10)");
        epicBtn.setItemMeta(meta);
        inv.setItem(21, epicBtn);

        ItemStack legendaryBtn = new ItemStack(Material.DIAMOND);
        meta = legendaryBtn.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Epic (5)");
        legendaryBtn.setItemMeta(meta);
        inv.setItem(22, legendaryBtn);

        ItemStack mythicBtn = new ItemStack(Material.NETHER_STAR);
        meta = mythicBtn.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Mythic (1)");
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

    public void openBulkEdit(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 54, 
                ChatColor.AQUA + "" + ChatColor.BOLD + "Bulk Edit: " + ChatColor.WHITE + crate.getId());

        Set<String> selected = playerSelectedRewards.getOrDefault(player.getUniqueId(), new HashSet<>());
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
            ItemStack item = createBulkRewardItem(reward, chance, selected.contains(reward.getId()));
            inv.setItem(i, item);
        }

        setNavigationItems(inv, page, allRewards.size());

        // Selected count
        ItemStack countItem = new ItemStack(Material.PAPER);
        ItemMeta meta = countItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Selected: " + selected.size());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click rewards to",
                ChatColor.GRAY + "select/deselect"
        ));
        countItem.setItemMeta(meta);
        inv.setItem(48, countItem);

        // Bulk actions
        ItemStack balanceBtn = new ItemStack(Material.EMERALD);
        meta = balanceBtn.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "⚡ Balance Selected");
        balanceBtn.setItemMeta(meta);
        inv.setItem(49, balanceBtn);

        ItemStack clearBtn = new ItemStack(Material.BARRIER);
        meta = clearBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Clear Selection");
        clearBtn.setItemMeta(meta);
        inv.setItem(50, clearBtn);

        player.openInventory(inv);
    }

    public void openStatistics(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 54, 
                ChatColor.WHITE + "" + ChatColor.BOLD + "📊 Stats: " + ChatColor.WHITE + crate.getId());

        // Header
        ItemStack headerItem = new ItemStack(Material.BOOK);
        ItemMeta meta = headerItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + crate.getId());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Total Rarities: " + crate.getRarities().size(),
                ChatColor.GRAY + "Total Rewards: " + crate.getRewards().size()
        ));
        headerItem.setItemMeta(meta);
        inv.setItem(4, headerItem);

        // Rarity breakdown
        int slot = 9;
        for (RarityData rarity : crate.getRarities().values()) {
            if (slot >= 44) break;
            double chance = crate.getRarityChance(rarity.getId());
            int rewardCount = crate.getRewardsByRarity(rarity.getId()).size();
            
            Material material = switch (rarity.getName().toLowerCase()) {
                case "legendary", "legend", "mythic" -> Material.NETHER_STAR;
                case "epic" -> Material.DIAMOND;
                case "rare" -> Material.AMETHYST_SHARD;
                case "uncommon" -> Material.EMERALD;
                case "common", "basic" -> Material.COAL;
                default -> Material.DIAMOND;
            };

            ItemStack statItem = new ItemStack(material);
            ItemMeta itemMeta = statItem.getItemMeta();
            itemMeta.setDisplayName(ChatColor.WHITE + rarity.getName());
            itemMeta.setLore(Arrays.asList(
                    ChatColor.GOLD + "Chance: " + String.format("%.2f%%", chance),
                    ChatColor.GRAY + "Weight: " + rarity.getWeight(),
                    ChatColor.GRAY + "Rewards: " + rewardCount,
                    "",
                    ChatColor.YELLOW + "Rewards breakdown:"
            ));
            
            // Add top 3 rewards
            Collection<RewardData> rewards = crate.getRewardsByRarity(rarity.getId());
            List<RewardData> sortedRewards = rewards.stream()
                    .sorted((a, b) -> Double.compare(
                            crate.getRewardChance(b.getId()),
                            crate.getRewardChance(a.getId())
                    ))
                    .limit(3)
                    .collect(Collectors.toList());
            
            for (RewardData r : sortedRewards) {
                double rChance = crate.getRewardChance(r.getId());
                List<String> lore = new ArrayList<>(itemMeta.getLore());
                lore.add(ChatColor.GRAY + "  - " + r.getPreviewName() + ": " + String.format("%.3f%%", rChance));
                itemMeta.setLore(lore);
            }
            
            statItem.setItemMeta(itemMeta);
            inv.setItem(slot, statItem);
            slot++;
        }

        // Back button
        ItemStack backBtn = new ItemStack(Material.ARROW);
        meta = backBtn.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back");
        backBtn.setItemMeta(meta);
        inv.setItem(49, backBtn);

        setBorderItems(inv);

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
        lore.add(ChatColor.GREEN + "▶ Click to select");
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
        lore.add(ChatColor.YELLOW + "▶ Click to view rewards");
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

    private ItemStack createBulkRewardItem(RewardData reward, double chance, boolean selected) {
        Material material = selected ? Material.LIME_STAINED_GLASS_PANE : Material.PAPER;
        if (!selected && chance >= 10) material = Material.GOLD_INGOT;
        else if (!selected && chance >= 1) material = Material.IRON_INGOT;
        else if (!selected && chance >= 0.1) material = Material.COPPER_INGOT;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String displayName = selected ? (ChatColor.GREEN + "✓ ") : (ChatColor.WHITE + "");
        displayName += reward.getPreviewName();
        meta.setDisplayName(displayName);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Weight: " + ChatColor.WHITE + reward.getWeight());
        lore.add(ChatColor.GRAY + "Chance: " + getChanceColor(chance) + String.format("%.4f%%", chance));
        lore.add("");
        lore.add(selected ? ChatColor.GREEN + "✓ Selected" : ChatColor.YELLOW + "▶ Click to select");
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

    public String getPlayerMode(Player player) {
        return playerSearch.get(player.getUniqueId());
    }

    public Set<String> getPlayerSelectedRewards(Player player) {
        return playerSelectedRewards.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
    }

    public void toggleRewardSelection(Player player, String rewardId) {
        Set<String> selected = getPlayerSelectedRewards(player);
        if (selected.contains(rewardId)) {
            selected.remove(rewardId);
        } else {
            selected.add(rewardId);
        }
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
        playerSearch.remove(player.getUniqueId());
        playerSelectedRewards.remove(player.getUniqueId());
        pendingScaleRarity.remove(player.getUniqueId());
    }

    public void openRarityEditor(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 54, 
                ChatColor.GOLD + "" + ChatColor.BOLD + "Rarity Editor: " + ChatColor.WHITE + crate.getId());

        int slot = 0;
        for (RarityData rarity : crate.getRarities().values()) {
            if (slot < 45) {
                double chance = crate.getRarityChance(rarity.getId());
                ItemStack item = createRarityEditItem(rarity, chance);
                inv.setItem(slot, item);
                slot++;
            }
        }

        // Info panel
        ItemStack infoBtn = new ItemStack(Material.BEACON);
        ItemMeta meta = infoBtn.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Rarity Info");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click a rarity to",
                ChatColor.GRAY + "edit its weight"
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

    public void setPendingScaleRarity(Player player, String rarityId) {
        pendingScaleRarity.put(player.getUniqueId(), rarityId);
    }

    public String getPendingScaleRarity(Player player) {
        return pendingScaleRarity.get(player.getUniqueId());
    }

    public void openScaleMenu(Player player, CrateData crate) {
        String rarityId = pendingScaleRarity.get(player.getUniqueId());
        if (rarityId == null) {
            player.sendMessage(ChatColor.RED + "No rarity selected for scaling!");
            return;
        }

        RarityData rarity = crate.getRarity(rarityId);
        if (rarity == null) {
            player.sendMessage(ChatColor.RED + "Rarity not found!");
            return;
        }

        double currentChance = crate.getRarityChance(rarityId);
        Inventory inv = Bukkit.createInventory(null, 36, 
                ChatColor.LIGHT_PURPLE + "Scale: " + ChatColor.WHITE + rarity.getName());

        // Preview
        ItemStack preview = new ItemStack(Material.PAPER);
        ItemMeta meta = preview.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + rarity.getName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Current: " + String.format("%.2f%%", currentChance),
                ChatColor.GRAY + "Current weight: " + rarity.getWeight()
        ));
        preview.setItemMeta(meta);
        inv.setItem(4, preview);

        // Quick percentage buttons
        int[] percentages = {1, 5, 10, 25, 50, 75, 90};
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        
        for (int i = 0; i < percentages.length; i++) {
            ItemStack btn = new ItemStack(Material.PAPER);
            ItemMeta btnMeta = btn.getItemMeta();
            btnMeta.setDisplayName(ChatColor.GREEN + "" + percentages[i] + "%");
            btnMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to set " + rarity.getName() + " to " + percentages[i] + "%"
            ));
            btn.setItemMeta(btnMeta);
            inv.setItem(slots[i], btn);
        }

        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backBtn.setItemMeta(backMeta);
        inv.setItem(31, backBtn);

        player.openInventory(inv);
    }

    public void openSearch(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Type a reward name in chat to search!");
        player.sendMessage(ChatColor.GRAY + "Or click Back to cancel.");
        
        Inventory inv = Bukkit.createInventory(null, 27, "Search Rewards");
        
        ItemStack infoBtn = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = infoBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Search Rewards");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Type a reward name in chat",
                ChatColor.GRAY + "to search across all crates"
        ));
        infoBtn.setItemMeta(meta);
        inv.setItem(13, infoBtn);

        ItemStack backBtn = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backBtn.setItemMeta(backMeta);
        inv.setItem(22, backBtn);

        player.openInventory(inv);
    }

    private ItemStack createRarityEditItem(RarityData rarity, double chance) {
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
        lore.add(ChatColor.GOLD + "Chance: " + getChanceColor(chance) + String.format("%.2f%%", chance));
        lore.add(ChatColor.GRAY + "Weight: " + ChatColor.WHITE + rarity.getWeight());
        lore.add("");
        lore.add(ChatColor.YELLOW + "▶ Click to edit weight");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
}
