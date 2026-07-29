package nl.devhub.crateseditor;

import nl.devhub.crateseditor.commands.CratesBalanceCommand;
import nl.devhub.crateseditor.commands.CratesEditorCommand;
import nl.devhub.crateseditor.commands.CratesPercentagesCommand;
import nl.devhub.crateseditor.commands.CratesScaleCommand;
import nl.devhub.crateseditor.commands.CrateGUICommand;
import nl.devhub.crateseditor.gui.CratesEditorGUI;
import nl.devhub.crateseditor.gui.GUIListener;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ExcellentCratesEditor extends JavaPlugin {

    private static ExcellentCratesEditor instance;
    private CrateDataManager dataManager;
    private CratesEditorGUI gui;
    
    private CratesEditorCommand editorCommand;
    private CratesPercentagesCommand percentagesCommand;
    private CratesBalanceCommand balanceCommand;
    private CratesScaleCommand scaleCommand;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        
        this.dataManager = new CrateDataManager(this);
        this.gui = new CratesEditorGUI(this);
        
        this.editorCommand = new CratesEditorCommand(this);
        this.percentagesCommand = new CratesPercentagesCommand(this);
        this.balanceCommand = new CratesBalanceCommand(this);
        this.scaleCommand = new CratesScaleCommand(this);
        
        // Register GUI command
        new CrateGUICommand();
        
        getServer().getPluginManager().registerEvents(new GUIListener(this, gui), this);
        
        getLogger().info("ExcellentCrates Editor enabled!");
        getLogger().info("Use " + ChatColor.YELLOW + "/ce" + ChatColor.WHITE + " to open the GUI editor");
    }

    @Override
    public void onDisable() {
        if (this.dataManager != null) {
            this.dataManager.saveAll();
        }
        getLogger().info("ExcellentCrates Editor disabled!");
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
