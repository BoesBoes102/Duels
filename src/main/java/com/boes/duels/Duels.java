package com.boes.duels;

import co.aikar.commands.PaperCommandManager;
import com.boes.duels.commands.DuelCommand;
import com.boes.duels.commands.DueladminCommand;
import com.boes.duels.commands.DuelqueueCommand;
import com.boes.duels.data.DuelMap;
import com.boes.duels.data.DuelQueue;
import com.boes.duels.data.Kit;
import com.boes.duels.listeners.DamageListener;
import com.boes.duels.listeners.DuelListener;
import com.boes.duels.managers.*;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Duels extends JavaPlugin {
    private static Duels instance;
    private DuelManager duelManager;
    private KitManager kitManager;
    private MapManager mapManager;
    private WorldManager worldManager;
    private PlayerDataManager playerDataManager;
    private KitEditorManager kitEditorManager;
    private VaultManager vaultManager;
    private QueueManager queueManager;
    private DuelListener duelListener;
    private boolean wagerEnabled;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();

        vaultManager = new VaultManager(this);
        wagerEnabled = getConfig().getBoolean("wager-enabled", vaultManager.isEnabled());

        worldManager = new WorldManager(this);
        worldManager.createDuelsWorld();

        kitManager = new KitManager(this);
        mapManager = new MapManager(this);
        playerDataManager = new PlayerDataManager();
        kitEditorManager = new KitEditorManager(this);
        queueManager = new QueueManager(this);
        duelManager = new DuelManager(this);

        kitManager.loadKits();
        mapManager.loadMaps();
        mapManager.loadPregeneratedArenas();
        mapManager.pregenerateAll();
        kitEditorManager.loadData();
        queueManager.loadQueues();

        registerCommands();
        registerListeners();

        getLogger().info("Duels plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (duelManager != null) {
            duelManager.endAllDuels();
        }
        if (kitManager != null) {
            kitManager.saveKits();
        }
        if (mapManager != null) {
            mapManager.saveMaps();
            mapManager.savePregeneratedArenas();
        }
        if (kitEditorManager != null) {
            kitEditorManager.saveData();
        }
        if (queueManager != null) {
            queueManager.saveQueues();
        }
        getLogger().info("Duels plugin disabled!");
    }

    private void registerCommands() {
        PaperCommandManager commandManager = new PaperCommandManager(this);
        
        commandManager.getCommandCompletions().registerAsyncCompletion("kits", c -> 
            kitManager.getAllKits().stream().map(Kit::name).toList()
        );
        commandManager.getCommandCompletions().registerAsyncCompletion("maps", c -> 
            mapManager.getAllMaps().stream().map(DuelMap::name).toList()
        );
        commandManager.getCommandCompletions().registerAsyncCompletion("queues", c ->
                queueManager.getAllQueues().stream().map(DuelQueue::name).toList()
        );
        commandManager.getCommandCompletions().registerAsyncCompletion("items", c ->
                Arrays.stream(Material.values())
                        .filter(m -> !m.isLegacy() && m.isItem())
                        .map(m -> m.name().toLowerCase())
                        .collect(Collectors.toList())
        );
        commandManager.registerCommand(new DuelCommand(this));
        commandManager.registerCommand(new DueladminCommand(this));
        commandManager.registerCommand(new DuelqueueCommand(this));
    }

    private void registerListeners() {
        duelListener = new DuelListener(this);
        getServer().getPluginManager().registerEvents(duelListener, this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
    }

    public static Duels getInstance() {
        return instance;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public KitEditorManager getKitEditorManager() {
        return kitEditorManager;
    }

    public VaultManager getVaultManager() {
        return vaultManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public DuelListener getDuelListener() {
        return duelListener;
    }

    public boolean isWagerEnabled() {
        return wagerEnabled;
    }

    public void setWagerEnabled(boolean enabled) {
        this.wagerEnabled = enabled;
        getConfig().set("wager-enabled", enabled);
        saveConfig();
    }
}