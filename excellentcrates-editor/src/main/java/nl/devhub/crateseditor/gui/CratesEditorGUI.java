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

    // ==================== Main Menu ====================
    
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Crate % Editor");

        // Row 1: Main actions
        setButton(inv, 0, Material.EMERALD, ChatColor.GREEN + "" + ChatColor.BOLD + "Quick Balance",
            Arrays.asList(ChatColor.GRAY + "Balance all weights", ChatColor.GRAY + "to equal percentages", "", ChatColor.YELLOW + "Select crate first"));
        setButton(inv, 2, Material.PAPER, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Scale Rarity",
            Arrays.asList(ChatColor.GRAY + "Set rarity to", ChatColor.GRAY + "specific percentage", "", ChatColor.YELLOW + "Select crate first"));
        setButton(inv, 4, Material.BEACON, ChatColor.GOLD + "" + ChatColor.BOLD + "Rarity Editor",
            Arrays.asList(ChatColor.GRAY + "Edit rarity weights", ChatColor.GRAY + "and chances", "", ChatColor.GREEN + "Click to open"));
        setButton(inv, 6, Material.NAME_TAG, ChatColor.YELLOW + "" + ChatColor.BOLD + "Search",
            Arrays.asList(ChatColor.GRAY + "Find rewards by", ChatColor.GRAY + "name across crates", "", ChatColor.GREEN + "Click to search"));
        
        // Row 2: Secondary actions
        setButton(inv, 9, Material.HOPPER, ChatColor.AQUA + "" + ChatColor.BOLD + "Bulk Edit",
            Arrays.asList(ChatColor.GRAY + "Select multiple rewards", ChatColor.GRAY + "and edit together", "", ChatColor.YELLOW + "Go to crate first"));
        setButton(inv, 11, Material.ENDER_CHEST, ChatColor.AQUA + "" + ChatColor.BOLD + "Edit Crates",
            Arrays.asList(ChatColor.GRAY + "Open the crate editor", ChatColor.GRAY + "to modify rewards", "", ChatColor.GREEN + "Click to open"));
        setButton(inv, 13, Material.BOOK, ChatColor.WHITE + "" + ChatColor.BOLD + "Statistics",
            Arrays.asList(ChatColor.GRAY + "View crate statistics", ChatColor.GRAY + "and percentage breakdown", "", ChatColor.GREEN + "Click to view"));
        setButton(inv, 15, Material.ARROW, ChatColor.RED + "" + ChatColor.BOLD + "Reload Data",
            Arrays.asList(ChatColor.GRAY + "Reload all crate data", ChatColor.GRAY + "from disk"));
        setButton(inv, 17, Material.NETHER_STAR, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Help",
            Arrays.asList(ChatColor.GRAY + "Shows help info", ChatColor.GRAY + "and usage tips"));

        player.openInventory(inv);
    }

    // ==================== Crate Selection ====================
    
    public void openCratesList(Player player, String mode) {
        playerSearch.put(player.getUniqueId(), mode);
        playerPage.put(player.getUniqueId(), 0);
        
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Select Crate");

        List<CrateData> crateList = new ArrayList<>(plugin.getDataManager().getAllCrates());
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int start = page * 45;
        int end = Math.min(start + 45, crateList.size());

        for (int i = 0; i < 45 && start + i < end; i++) {
            CrateData crate = crateList.get(start + i);
            ItemStack item = createCrateItem(crate);
            inv.setItem(i, item);
        }

        // Navigation
        int totalPages = Math.max(1, (int) Math.ceil(crateList.size() / 45.0));
        setNavigationItems(inv, page, totalPages);

        // Mode indicator
        ItemStack modeItem = new ItemStack(Material.PAPER);
        ItemMeta meta = modeItem.getItemMeta();
        String modeName = switch (mode) {
            case "balance" -> "Quick Balance";
            case "scale" -> "Scale Rarity";
            case "rarity" -> "Rarity Editor";
            case "stats" -> "Statistics";
            case "bulk" -> "Bulk Edit";
            default -> "Edit Crates";
        };
        meta.setDisplayName(ChatColor.WHITE + "Mode: " + ChatColor.YELLOW + modeName);
        modeItem.setItemMeta(meta);
        inv.setItem(48, modeItem);

        // Back button
        setButton(inv, 53, Material.BARRIER, ChatColor.RED + "Back to Menu", Collections.emptyList());

        player.openInventory(inv);
    }

    // ==================== Crate Editor ====================
    
    public void openCrateEditor(Player player, CrateData crate) {
        playerEditingCrate.put(player.getUniqueId(), crate);
        playerEditingRarity.remove(player.getUniqueId());
        playerEditingReward.remove(player.getUniqueId());
        playerPage.put(player.getUniqueId(), 0);
        playerSelectedRewards.put(player.getUniqueId(), new HashSet<>());

        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_PURPLE + "Crate: " + ChatColor.WHITE + crate.getId());

        // Show all rewards in the main grid (slots 0-35)
        List<RewardData> rewards = new ArrayList<>(crate.getRewards().values());
        rewards.sort((a, b) -> {
            int rarityCompare = a.getRarityId().compareToIgnoreCase(b.getRarityId());
            if (rarityCompare != 0) return rarityCompare;
            return Double.compare(crate.getRewardChance(b.getId()), crate.getRewardChance(a.getId()));
        });

        int slot = 0;
        for (RewardData reward : rewards) {
            if (slot >= 36) break;
            double chance = crate.getRewardChance(reward.getId());
            ItemStack item = createRewardItem(reward, chance);
            inv.setItem(slot, item);
            slot++;
        }

        // Show empty slots message if no rewards
        if (rewards.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "No rewards found!");
            empty.setItemMeta(meta);
            inv.setItem(22, empty);
        }

        // Action buttons row (slots 36-44)
        setButton(inv, 36, Material.CHEST, ChatColor.YELLOW + "" + ChatColor.BOLD + "All Rewards",
            Arrays.asList(ChatColor.GRAY + "View all rewards", ChatColor.GRAY + "with pagination"));
        setButton(inv, 38, Material.EMERALD, ChatColor.GREEN + "" + ChatColor.BOLD + "Balance All",
            Arrays.asList(ChatColor.GRAY + "Balance all weights", ChatColor.GRAY + "to equal percentages"));
        setButton(inv, 40, Material.PAPER, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Scale",
            Arrays.asList(ChatColor.GRAY + "Scale rarity to", ChatColor.GRAY + "specific percentage"));
        setButton(inv, 42, Material.BEACON, ChatColor.GOLD + "" + ChatColor.BOLD + "Rarities",
            Arrays.asList(ChatColor.GRAY + "View and edit", ChatColor.GRAY + "rarity weights"));
        setButton(inv, 44, Material.HOPPER, ChatColor.AQUA + "" + ChatColor.BOLD + "Bulk Select",
            Arrays.asList(ChatColor.GRAY + "Select multiple", ChatColor.GRAY + "rewards to edit"));

        // Info row (slots 45-53)
        setButton(inv, 45, Material.ARROW, ChatColor.YELLOW + "Refresh", Collections.emptyList());
        
        ItemStack infoBtn = new ItemStack(Material.BOOK);
        ItemMeta meta = infoBtn.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Info");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Rewards: " + ChatColor.WHITE + crate.getRewards().size(),
            ChatColor.GRAY + "Rarities: " + ChatColor.WHITE + crate.getRarities().size()
        ));
        infoBtn.setItemMeta(meta);
        inv.setItem(49, infoBtn);

        // Border
        setBorderItems(inv);
        setButton(inv, 53, Material.BARRIER, ChatColor.RED + "Back", Collections.emptyList());

        player.openInventory(inv);
    }

    // ==================== Rarity Editor ====================
    
    public void openRarityEditor(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Rarity Editor: " + ChatColor.WHITE + crate.getId());

        int slot = 0;
        for (RarityData rarity : crate.getRarities().values()) {
            if (slot >= 45) break;
            double chance = crate.getRarityChance(rarity.getId());
            ItemStack item = createRarityEditItem(rarity, chance);
            inv.setItem(slot, item);
            slot++;
        }

        // Info
        ItemStack infoBtn = new ItemStack(Material.BEACON);
        ItemMeta meta = infoBtn.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Info");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Click a rarity to",
            ChatColor.GRAY + "edit its weight"
        ));
        infoBtn.setItemMeta(meta);
        inv.setItem(49, infoBtn);

        // Back
        setButton(inv, 53, Material.BARRIER, ChatColor.RED + "Back", Collections.emptyList());
        setBorderItems(inv);

        player.openInventory(inv);
    }

    // ==================== Rarity Weight Editor ====================
    
    public void openRarityWeightEditor(Player player, CrateData crate, RarityData rarity) {
        playerEditingRarity.put(player.getUniqueId(), rarity.getId());
        double currentChance = crate.getRarityChance(rarity.getId());

        Inventory inv = Bukkit.createInventory(null, 36,
                ChatColor.GOLD + "Edit Rarity: " + ChatColor.WHITE + rarity.getName());

        // Preview
        ItemStack previewItem = new ItemStack(Material.BEACON);
        ItemMeta meta = previewItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + rarity.getName());
        meta.setLore(Arrays.asList(
            ChatColor.GOLD + "Current: " + String.format("%.2f%%", currentChance),
            ChatColor.GRAY + "Weight: " + rarity.getWeight()
        ));
        previewItem.setItemMeta(meta);
        inv.setItem(4, previewItem);

        // Weight controls
        setButton(inv, 10, Material.REDSTONE_BLOCK, ChatColor.RED + "" + ChatColor.BOLD + "-10", Collections.emptyList());
        setButton(inv, 11, Material.BRICK, ChatColor.RED + "-1", Collections.emptyList());
        setButton(inv, 13, Material.BARRIER, ChatColor.YELLOW + "Reset (25)", Collections.emptyList());
        setButton(inv, 14, Material.GOLD_INGOT, ChatColor.GREEN + "+1", Collections.emptyList());
        setButton(inv, 16, Material.GOLD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "+10", Collections.emptyList());

        // Quick presets
        setButton(inv, 19, Material.COAL, ChatColor.GRAY + "Common (70)", Collections.emptyList());
        setButton(inv, 20, Material.IRON_INGOT, ChatColor.WHITE + "Uncommon (50)", Collections.emptyList());
        setButton(inv, 21, Material.GOLD_INGOT, ChatColor.YELLOW + "Rare (25)", Collections.emptyList());
        setButton(inv, 22, Material.DIAMOND, ChatColor.AQUA + "Epic (10)", Collections.emptyList());
        setButton(inv, 23, Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Legendary (5)", Collections.emptyList());

        // Navigation
        setButton(inv, 31, Material.ARROW, ChatColor.RED + "Back", Collections.emptyList());
        setButton(inv, 35, Material.BARRIER, ChatColor.RED + "Close", Collections.emptyList());

        player.openInventory(inv);
    }

    // ==================== Rarity Rewards List ====================
    
    public void openRarityRewardsList(Player player, CrateData crate, RarityData rarity) {
        playerEditingRarity.put(player.getUniqueId(), rarity.getId());
        
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_AQUA + rarity.getName() + " Rewards");

        List<RewardData> rewardList = new ArrayList<>(crate.getRewardsByRarity(rarity.getId()));
        rewardList.sort((a, b) -> Double.compare(crate.getRewardChance(b.getId()), crate.getRewardChance(a.getId())));

        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int start = page * 45;
        int end = Math.min(start + 45, rewardList.size());

        for (int i = 0; i < 45 && start + i < end; i++) {
            RewardData reward = rewardList.get(start + i);
            double chance = crate.getRewardChance(reward.getId());
            ItemStack item = createRewardItem(reward, chance);
            inv.setItem(i, item);
        }

        int totalPages = Math.max(1, (int) Math.ceil(rewardList.size() / 45.0));
        setNavigationItems(inv, page, totalPages);

        // Rarity info
        ItemStack rarityInfo = new ItemStack(Material.BEACON);
        ItemMeta meta = rarityInfo.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + rarity.getName());
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Chance: " + String.format("%.2f%%", crate.getRarityChance(rarity.getId())),
            ChatColor.GRAY + "Rewards: " + rewardList.size()
        ));
        rarityInfo.setItemMeta(meta);
        inv.setItem(46, rarityInfo);

        // Balance button
        setButton(inv, 49, Material.EMERALD, ChatColor.GREEN + "" + ChatColor.BOLD + "Balance", Collections.emptyList());

        // Back
        setButton(inv, 53, Material.ARROW, ChatColor.RED + "Back", Collections.emptyList());
        setBorderItems(inv);

        player.openInventory(inv);
    }

    // ==================== All Rewards List ====================
    
    public void openAllRewardsList(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_PURPLE + "All Rewards: " + ChatColor.WHITE + crate.getId());

        List<RewardData> allRewards = new ArrayList<>(crate.getRewards().values());
        allRewards.sort((a, b) -> Double.compare(crate.getRewardChance(b.getId()), crate.getRewardChance(a.getId())));

        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int start = page * 45;
        int end = Math.min(start + 45, allRewards.size());

        for (int i = 0; i < 45 && start + i < end; i++) {
            RewardData reward = allRewards.get(start + i);
            double chance = crate.getRewardChance(reward.getId());
            ItemStack item = createRewardItem(reward, chance);
            inv.setItem(i, item);
        }

        int totalPages = Math.max(1, (int) Math.ceil(allRewards.size() / 45.0));
        setNavigationItems(inv, page, totalPages);

        // Back
        setButton(inv, 49, Material.ARROW, ChatColor.RED + "Back", Collections.emptyList());
        setBorderItems(inv);

        player.openInventory(inv);
    }

    // ==================== Reward Editor ====================
    
    public void openRewardEditor(Player player, CrateData crate, RewardData reward, RarityData rarity) {
        playerEditingReward.put(player.getUniqueId(), reward.getId());
        double currentChance = crate.getRewardChance(reward.getId());
        String rarityName = rarity != null ? rarity.getName() : reward.getRarityId();

        Inventory inv = Bukkit.createInventory(null, 36,
                ChatColor.DARK_GREEN + "Edit: " + ChatColor.WHITE + truncate(reward.getPreviewName(), 20));

        // Preview
        ItemStack previewItem = new ItemStack(Material.PAPER);
        ItemMeta meta = previewItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + reward.getPreviewName());
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "ID: " + reward.getId(),
            ChatColor.GRAY + "Rarity: " + ChatColor.AQUA + rarityName,
            "",
            ChatColor.GOLD + "Chance: " + String.format("%.4f%%", currentChance),
            ChatColor.GRAY + "Weight: " + reward.getWeight()
        ));
        previewItem.setItemMeta(meta);
        inv.setItem(4, previewItem);

        // Weight controls
        setButton(inv, 10, Material.REDSTONE_BLOCK, ChatColor.RED + "" + ChatColor.BOLD + "-1.0", Collections.emptyList());
        setButton(inv, 11, Material.BRICK, ChatColor.RED + "-0.1", Collections.emptyList());
        setButton(inv, 13, Material.BARRIER, ChatColor.YELLOW + "Reset (10.0)", Collections.emptyList());
        setButton(inv, 14, Material.GOLD_INGOT, ChatColor.GREEN + "+0.1", Collections.emptyList());
        setButton(inv, 16, Material.GOLD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "+1.0", Collections.emptyList());

        // Quick presets
        setButton(inv, 19, Material.COAL, ChatColor.GRAY + "Common (50)", Collections.emptyList());
        setButton(inv, 20, Material.IRON_INGOT, ChatColor.WHITE + "Uncommon (25)", Collections.emptyList());
        setButton(inv, 21, Material.GOLD_INGOT, ChatColor.YELLOW + "Rare (10)", Collections.emptyList());
        setButton(inv, 22, Material.DIAMOND, ChatColor.AQUA + "Epic (5)", Collections.emptyList());
        setButton(inv, 23, Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Mythic (1)", Collections.emptyList());

        // Navigation
        setButton(inv, 31, Material.ARROW, ChatColor.RED + "Back", Collections.emptyList());
        setButton(inv, 35, Material.BARRIER, ChatColor.RED + "Close", Collections.emptyList());

        player.openInventory(inv);
    }

    // ==================== Bulk Edit ====================
    
    public void openBulkEdit(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Bulk Edit: " + ChatColor.WHITE + crate.getId());

        Set<String> selected = playerSelectedRewards.getOrDefault(player.getUniqueId(), new HashSet<>());
        List<RewardData> allRewards = new ArrayList<>(crate.getRewards().values());
        allRewards.sort((a, b) -> Double.compare(crate.getRewardChance(b.getId()), crate.getRewardChance(a.getId())));

        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int start = page * 45;
        int end = Math.min(start + 45, allRewards.size());

        for (int i = 0; i < 45 && start + i < end; i++) {
            RewardData reward = allRewards.get(start + i);
            double chance = crate.getRewardChance(reward.getId());
            ItemStack item = createBulkRewardItem(reward, chance, selected.contains(reward.getId()));
            inv.setItem(i, item);
        }

        int totalPages = Math.max(1, (int) Math.ceil(allRewards.size() / 45.0));
        setNavigationItems(inv, page, totalPages);

        // Selected count
        ItemStack countItem = new ItemStack(Material.PAPER);
        ItemMeta meta = countItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Selected: " + ChatColor.YELLOW + selected.size());
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Click rewards to",
            ChatColor.GRAY + "select/deselect"
        ));
        countItem.setItemMeta(meta);
        inv.setItem(48, countItem);

        // Clear selection
        setButton(inv, 49, Material.BARRIER, ChatColor.RED + "Clear Selection", Collections.emptyList());

        // Balance selected
        setButton(inv, 50, Material.EMERALD, ChatColor.GREEN + "" + ChatColor.BOLD + "Balance Selected",
            Arrays.asList(ChatColor.GRAY + "Set all selected to", ChatColor.GRAY + "equal weight"));

        // Back
        setButton(inv, 53, Material.ARROW, ChatColor.RED + "Back", Collections.emptyList());
        setBorderItems(inv);

        player.openInventory(inv);
    }

    // ==================== Scale Menu ====================
    
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
            ChatColor.GRAY + "Weight: " + rarity.getWeight()
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
            btn.setItemMeta(btnMeta);
            inv.setItem(slots[i], btn);
        }

        setButton(inv, 31, Material.ARROW, ChatColor.RED + "Back", Collections.emptyList());

        player.openInventory(inv);
    }

    public void setPendingScaleRarity(Player player, String rarityId) {
        pendingScaleRarity.put(player.getUniqueId(), rarityId);
    }

    public String getPendingScaleRarity(Player player) {
        return pendingScaleRarity.get(player.getUniqueId());
    }

    // ==================== Statistics ====================
    
    public void openStatistics(Player player, CrateData crate) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.WHITE + "" + ChatColor.BOLD + "Stats: " + ChatColor.WHITE + crate.getId());

        int slot = 0;
        for (RarityData rarity : crate.getRarities().values()) {
            if (slot >= 45) break;
            
            double chance = crate.getRarityChance(rarity.getId());
            Material material = getRarityMaterial(rarity.getName());
            ItemStack statItem = new ItemStack(material);
            ItemMeta itemMeta = statItem.getItemMeta();
            itemMeta.setDisplayName(ChatColor.WHITE + rarity.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GOLD + "Chance: " + getChanceColor(chance) + String.format("%.2f%%", chance));
            lore.add(ChatColor.GRAY + "Weight: " + rarity.getWeight());
            lore.add(ChatColor.GRAY + "Rewards: " + crate.getRewardsByRarity(rarity.getId()).size());
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Reward chances:");

            for (RewardData r : crate.getRewardsByRarity(rarity.getId())) {
                double rChance = crate.getRewardChance(r.getId());
                lore.add(ChatColor.GRAY + "  - " + r.getPreviewName() + ": " + String.format("%.3f%%", rChance));
            }
            
            itemMeta.setLore(lore);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            statItem.setItemMeta(itemMeta);
            inv.setItem(slot, statItem);
            slot++;
        }

        setButton(inv, 49, Material.ARROW, ChatColor.RED + "Back", Collections.emptyList());
        setBorderItems(inv);

        player.openInventory(inv);
    }

    // ==================== Search ====================
    
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

        setButton(inv, 22, Material.BARRIER, ChatColor.RED + "Back", Collections.emptyList());

        player.openInventory(inv);
    }

    // ==================== Helper Methods ====================
    
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

    // ==================== Item Creation ====================
    
    private ItemStack createCrateItem(CrateData crate) {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + crate.getId());
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Rarities: " + ChatColor.WHITE + crate.getRarities().size(),
            ChatColor.GRAY + "Rewards: " + ChatColor.WHITE + crate.getRewards().size(),
            "",
            ChatColor.GREEN + "Click to select"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRarityEditItem(RarityData rarity, double chance) {
        Material material = getRarityMaterial(rarity.getName());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + rarity.getName());
        meta.setLore(Arrays.asList(
            ChatColor.GOLD + "Chance: " + getChanceColor(chance) + String.format("%.2f%%", chance),
            ChatColor.GRAY + "Weight: " + rarity.getWeight(),
            "",
            ChatColor.YELLOW + "Click to edit"
        ));
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
        meta.setDisplayName(ChatColor.WHITE + truncate(reward.getPreviewName(), 30));
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Rarity: " + ChatColor.AQUA + reward.getRarityId(),
            ChatColor.GRAY + "Weight: " + ChatColor.WHITE + reward.getWeight(),
            ChatColor.GRAY + "Chance: " + getChanceColor(chance) + String.format("%.4f%%", chance),
            "",
            ChatColor.YELLOW + "Click to edit"
        ));
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
        
        String displayName = selected ? (ChatColor.GREEN + "[X] ") : (ChatColor.WHITE + "");
        displayName += reward.getPreviewName();
        meta.setDisplayName(displayName);
        
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Weight: " + ChatColor.WHITE + reward.getWeight(),
            ChatColor.GRAY + "Chance: " + getChanceColor(chance) + String.format("%.4f%%", chance),
            "",
            selected ? ChatColor.GREEN + "Selected" : ChatColor.YELLOW + "Click to select"
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    // ==================== UI Helpers ====================
    
    private void setButton(Inventory inv, int slot, Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void setNavigationItems(Inventory inv, int page, int totalPages) {
        ItemStack prevBtn = new ItemStack(Material.ARROW);
        ItemMeta meta = prevBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Previous");
        prevBtn.setItemMeta(meta);

        ItemStack nextBtn = new ItemStack(Material.ARROW);
        meta = nextBtn.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Next");
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

    // ==================== Utility ====================
    
    private ChatColor getChanceColor(double chance) {
        if (chance >= 10) return ChatColor.GOLD;
        if (chance >= 5) return ChatColor.YELLOW;
        if (chance >= 1) return ChatColor.WHITE;
        if (chance >= 0.1) return ChatColor.GRAY;
        return ChatColor.DARK_GRAY;
    }

    private Material getRarityMaterial(String name) {
        return switch (name.toLowerCase()) {
            case "legendary", "legend", "mythic" -> Material.NETHER_STAR;
            case "epic" -> Material.DIAMOND;
            case "rare" -> Material.AMETHYST_SHARD;
            case "uncommon" -> Material.EMERALD;
            case "common", "basic" -> Material.COAL;
            default -> Material.DIAMOND;
        };
    }

    private String truncate(String text, int length) {
        if (text.length() <= length) return text;
        return text.substring(0, length - 3) + "...";
    }
}
