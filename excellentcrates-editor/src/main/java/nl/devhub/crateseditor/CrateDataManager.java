package nl.devhub.crateseditor;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CrateDataManager {

    private final ExcellentCratesEditor plugin;
    private final File cratesFolder;
    private Map<String, CrateData> crates;
    private boolean needsSave;

    public CrateDataManager(ExcellentCratesEditor plugin) {
        this.plugin = plugin;
        this.cratesFolder = new File(plugin.getDataFolder().getParentFile(), "ExcellentCrates/crates");
        this.crates = new HashMap<>();
        this.needsSave = false;
        
        loadAll();
    }

    public void loadAll() {
        crates.clear();
        
        if (!cratesFolder.exists() || !cratesFolder.isDirectory()) {
            plugin.getLogger().warning("ExcellentCrates crates folder not found: " + cratesFolder.getPath());
            return;
        }

        File[] files = cratesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            CrateData crateData = loadCrateFile(file);
            if (crateData != null) {
                crates.put(crateData.getId(), crateData);
            }
        }
        
        plugin.getLogger().info("Loaded " + crates.size() + " crates from ExcellentCrates");
    }

    private CrateData loadCrateFile(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String crateId = file.getName().replace(".yml", "");
        
        CrateData crateData = new CrateData(crateId, file);
        
        if (config.contains("Rarities")) {
            for (String rarityId : config.getConfigurationSection("Rarities").getKeys(false)) {
                String path = "Rarities." + rarityId;
                String name = config.getString(path + ".Name", rarityId);
                double weight = config.getDouble(path + ".Weight", 0.0);
                if (weight == 0.0) {
                    weight = config.getDouble(path + ".Chance", 100.0);
                }
                crateData.addRarity(new RarityData(rarityId, name, weight));
            }
        }
        
        if (config.contains("Rewards")) {
            for (String rewardId : config.getConfigurationSection("Rewards").getKeys(false)) {
                String path = "Rewards." + rewardId;
                String rarityId = config.getString(path + ".Rarity", "common");
                double weight = config.getDouble(path + ".Weight", 10.0);
                String previewName = "Unknown";
                
                if (config.contains(path + ".PreviewData.material")) {
                    previewName = config.getString(path + ".PreviewData.name", rewardId);
                }
                
                crateData.addReward(new RewardData(rewardId, rarityId, weight, previewName));
            }
        }
        
        return crateData;
    }

    public void saveAll() {
        for (CrateData crateData : crates.values()) {
            saveCrate(crateData);
        }
        needsSave = false;
    }

    public void saveCrate(CrateData crateData) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(crateData.getFile());
        
        for (RarityData rarity : crateData.getRarities().values()) {
            config.set("Rarities." + rarity.getId() + ".Name", rarity.getName());
            config.set("Rarities." + rarity.getId() + ".Weight", rarity.getWeight());
        }
        
        for (RewardData reward : crateData.getRewards().values()) {
            config.set("Rewards." + reward.getId() + ".Weight", reward.getWeight());
            config.set("Rewards." + reward.getId() + ".Rarity", reward.getRarityId());
        }
        
        try {
            config.save(crateData.getFile());
            plugin.getLogger().info("Saved crate: " + crateData.getId());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crate " + crateData.getId() + ": " + e.getMessage());
        }
    }

    public CrateData getCrate(String id) {
        return crates.get(id.toLowerCase());
    }

    public Collection<CrateData> getAllCrates() {
        return crates.values();
    }

    public Set<String> getCrateIds() {
        return crates.keySet();
    }

    public boolean setRarityWeight(String crateId, String rarityId, double weight) {
        CrateData crate = getCrate(crateId);
        if (crate == null) return false;
        
        RarityData rarity = crate.getRarity(rarityId);
        if (rarity == null) return false;
        
        rarity.setWeight(weight);
        crate.invalidateChances();
        needsSave = true;
        saveCrate(crate);
        return true;
    }

    public boolean setRewardWeight(String crateId, String rewardId, double weight) {
        CrateData crate = getCrate(crateId);
        if (crate == null) return false;
        
        RewardData reward = crate.getReward(rewardId);
        if (reward == null) return false;
        
        reward.setWeight(weight);
        crate.invalidateChances();
        needsSave = true;
        saveCrate(crate);
        return true;
    }

    public boolean setRewardRarity(String crateId, String rewardId, String rarityId) {
        CrateData crate = getCrate(crateId);
        if (crate == null) return false;
        
        RewardData reward = crate.getReward(rewardId);
        if (reward == null) return false;
        
        if (crate.getRarity(rarityId) == null) return false;
        
        reward.setRarityId(rarityId);
        crate.invalidateChances();
        needsSave = true;
        saveCrate(crate);
        return true;
    }

    public boolean balanceRarityWeights(String crateId, String rarityId) {
        CrateData crate = getCrate(crateId);
        if (crate == null) return false;
        
        RarityData rarity = crate.getRarity(rarityId);
        if (rarity == null) return false;
        
        Collection<RewardData> rewards = crate.getRewardsByRarity(rarityId);
        if (rewards.isEmpty()) return false;
        
        double equalWeight = 100.0 / rewards.size();
        for (RewardData reward : rewards) {
            reward.setWeight(equalWeight);
        }
        
        crate.invalidateChances();
        needsSave = true;
        saveCrate(crate);
        return true;
    }

    public boolean balanceAllRarities(String crateId) {
        CrateData crate = getCrate(crateId);
        if (crate == null) return false;
        
        Collection<RarityData> rarities = crate.getRarities().values();
        if (rarities.isEmpty()) return false;
        
        double equalWeight = 100.0 / rarities.size();
        for (RarityData rarity : rarities) {
            rarity.setWeight(equalWeight);
        }
        
        for (RarityData rarity : rarities) {
            Collection<RewardData> rewards = crate.getRewardsByRarity(rarity.getId());
            if (!rewards.isEmpty()) {
                double rewardWeight = 100.0 / rewards.size();
                for (RewardData reward : rewards) {
                    reward.setWeight(rewardWeight);
                }
            }
        }
        
        crate.invalidateChances();
        needsSave = true;
        saveCrate(crate);
        return true;
    }

    public boolean scaleRarityToPercentage(String crateId, String rarityId, double targetPercentage) {
        CrateData crate = getCrate(crateId);
        if (crate == null) return false;
        
        RarityData targetRarity = crate.getRarity(rarityId);
        if (targetRarity == null) return false;
        
        Collection<RarityData> rarities = crate.getRarities().values();
        if (rarities.size() <= 1) return false;
        
        double targetWeight = targetRarity.getWeight();
        double totalWeight = rarities.stream().mapToDouble(RarityData::getWeight).sum();
        double currentPercentage = (targetWeight / totalWeight) * 100.0;
        
        double scaleFactor = targetPercentage / currentPercentage;
        targetRarity.setWeight(targetWeight * scaleFactor);
        
        double remainingPercentage = 100.0 - targetPercentage;
        double otherWeight = 0;
        for (RarityData rarity : rarities) {
            if (!rarity.getId().equals(rarityId)) {
                otherWeight += rarity.getWeight();
            }
        }
        
        if (otherWeight > 0) {
            for (RarityData rarity : rarities) {
                if (!rarity.getId().equals(rarityId)) {
                    rarity.setWeight((rarity.getWeight() / otherWeight) * remainingPercentage);
                }
            }
        }
        
        crate.invalidateChances();
        needsSave = true;
        saveCrate(crate);
        return true;
    }

    public boolean needsSave() {
        return needsSave;
    }

    public static class CrateData {
        private final String id;
        private final File file;
        private Map<String, RarityData> rarities;
        private Map<String, RewardData> rewards;
        private Map<String, Double> rarityChances;
        private Map<String, Double> rewardChances;

        public CrateData(String id, File file) {
            this.id = id;
            this.file = file;
            this.rarities = new LinkedHashMap<>();
            this.rewards = new LinkedHashMap<>();
            this.rarityChances = new HashMap<>();
            this.rewardChances = new HashMap<>();
        }

        public String getId() { return id; }
        public File getFile() { return file; }
        public Map<String, RarityData> getRarities() { return rarities; }
        public Map<String, RewardData> getRewards() { return rewards; }

        public void addRarity(RarityData rarity) {
            rarities.put(rarity.getId(), rarity);
            invalidateChances();
        }

        public void addReward(RewardData reward) {
            rewards.put(reward.getId(), reward);
            invalidateChances();
        }

        public RarityData getRarity(String id) {
            return rarities.get(id.toLowerCase());
        }

        public RewardData getReward(String id) {
            return rewards.get(id.toLowerCase());
        }

        public Collection<RewardData> getRewardsByRarity(String rarityId) {
            return rewards.values().stream()
                    .filter(r -> r.getRarityId().equalsIgnoreCase(rarityId))
                    .collect(Collectors.toList());
        }

        public double getRarityChance(String rarityId) {
            calculateChances();
            return rarityChances.getOrDefault(rarityId.toLowerCase(), 0.0);
        }

        public double getRewardChance(String rewardId) {
            calculateChances();
            return rewardChances.getOrDefault(rewardId.toLowerCase(), 0.0);
        }

        public void invalidateChances() {
            this.rarityChances.clear();
            this.rewardChances.clear();
        }

        public Collection<String> getRewardRarities() {
            return rewards.values().stream()
                    .map(RewardData::getRarityId)
                    .distinct()
                    .collect(Collectors.toList());
        }

        private void calculateChances() {
            if (!rarityChances.isEmpty()) return;
            
            double totalRarityWeight = rarities.values().stream()
                    .mapToDouble(RarityData::getWeight).sum();
            
            for (RarityData rarity : rarities.values()) {
                double rarityChance = (rarity.getWeight() / totalRarityWeight) * 100.0;
                rarityChances.put(rarity.getId().toLowerCase(), rarityChance);
                
                Collection<RewardData> rarityRewards = getRewardsByRarity(rarity.getId());
                double totalRewardWeight = rarityRewards.stream()
                        .mapToDouble(RewardData::getWeight).sum();
                
                for (RewardData reward : rarityRewards) {
                    double rewardChance = (reward.getWeight() / totalRewardWeight) * 100.0;
                    double totalChance = (rewardChance / 100.0) * (rarityChance / 100.0) * 100.0;
                    rewardChances.put(reward.getId().toLowerCase(), totalChance);
                }
            }
        }
    }

    public static class RarityData {
        private final String id;
        private String name;
        private double weight;

        public RarityData(String id, String name, double weight) {
            this.id = id.toLowerCase();
            this.name = name;
            this.weight = weight;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = Math.max(0, weight); }
    }

    public static class RewardData {
        private final String id;
        private String rarityId;
        private double weight;
        private String previewName;

        public RewardData(String id, String rarityId, double weight, String previewName) {
            this.id = id.toLowerCase();
            this.rarityId = rarityId.toLowerCase();
            this.weight = weight;
            this.previewName = previewName;
        }

        public String getId() { return id; }
        public String getRarityId() { return rarityId; }
        public void setRarityId(String rarityId) { this.rarityId = rarityId.toLowerCase(); }
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = Math.max(0, weight); }
        public String getPreviewName() { return previewName; }
    }
}
