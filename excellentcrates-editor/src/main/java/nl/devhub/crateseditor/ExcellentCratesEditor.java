package nl.devhub.crateseditor;

import nl.devhub.crateseditor.commands.CratesBalanceCommand;
import nl.devhub.crateseditor.commands.CratesEditorCommand;
import nl.devhub.crateseditor.commands.CratesPercentagesCommand;
import nl.devhub.crateseditor.commands.CratesScaleCommand;
import nl.devhub.crateseditor.gui.CratesEditorGUI;
import nl.devhub.crateseditor.gui.GUIListener;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ExcellentCratesEditor extends JavaPlugin {

    private static ExcellentCratesEditor instance;
    private CrateDataManager dataManager;
    private CratesEditorGUI gui;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        
        this.dataManager = new CrateDataManager(this);
        this.gui = new CratesEditorGUI(this);
        
        CratesEditorCommand editorCmd = new CratesEditorCommand(this);
        registerCommand("crateseditor", editorCmd, editorCmd);
        registerCommand("cratespercentages", new CratesPercentagesCommand(this), new CratesPercentagesCommand(this));
        registerCommand("cratesbalance", new CratesBalanceCommand(this), new CratesBalanceCommand(this));
        registerCommand("cratesscale", new CratesScaleCommand(this), new CratesScaleCommand(this));
        
        getServer().getPluginManager().registerEvents(new GUIListener(this, gui), this);
        
        // Override crateseditor to open GUI when no args
        getCommand("crateseditor").setExecutor((sender, command, label, args) -> {
            if (sender.hasPermission("crateseditor.use") && args.length == 0 && sender instanceof Player player) {
                gui.openCratesList(player);
                return true;
            }
            return false;
        });
        
        getLogger().info("ExcellentCrates Editor enabled!");
        getLogger().info("Use /ce or /crateseditor to open the GUI editor");
    }

    @Override
    public void onDisable() {
        if (this.dataManager != null) {
            this.dataManager.saveAll();
        }
        getLogger().info("ExcellentCrates Editor disabled!");
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        org.bukkit.command.Command command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(tabCompleter);
        }
    }

    public static ExcellentCratesEditor getInstance() {
        return instance;
    }

    public CrateDataManager getDataManager() {
        return dataManager;
    }

    public CratesEditorGUI getGUI() {
        return gui;
    }
}
