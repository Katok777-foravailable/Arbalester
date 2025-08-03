package com.katok.arbalester;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import com.katok.arbalester.arbalesters.ArbalesterManager;
import com.katok.arbalester.utills.DContainer;
import com.katok.arbalester.utills.config.ConfigManager;
import com.katok.arbalester.utills.config.IConfigManager;
import com.katok.arbalester.utills.database.Database;
import com.katok.arbalester.utills.database.IDatabase;
import com.katok.arbalester.utills.events.ArbalesterReloadEvent;

public class Arbalester extends JavaPlugin {
    public final DContainer dContainer = new DContainer();

    @Override
    public void onEnable() {
        registerSingletones();
        registerListeners();
        loadConfiguration();

        if(StringUtils.isEmpty(dContainer.get(IConfigManager.class).getConfiguration("config.yml").getString("database.database"))) {
            getLogger().severe("Пожалуйста, настройте базу данных в config.yml");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        loadDataBase();
        initArbalesters();
        registerProviders();
        registerCommands();
    }

    @Override
    public void onDisable() {
        dContainer.get(ArbalesterManager.class).despawn();

        unLoadDataBase();
    }

    private void registerSingletones() {
        dContainer.registerSingletone(JavaPlugin.class, this);
        dContainer.registerSingletone(Arbalester.class, this);
        dContainer.registerSingletone(DContainer.class, dContainer);
        dContainer.registerSingletone(IConfigManager.class, dContainer.create(ConfigManager.class));
        dContainer.registerSingletone(IDatabase.class, dContainer.create(Database.class));
        dContainer.registerSingletone(ArbalesterManager.class, dContainer.create(ArbalesterManager.class));
    }

    public void loadConfiguration() {
        IConfigManager configManager = dContainer.get(IConfigManager.class);

        try {
            configManager.loadConfigFile("config.yml");
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }

        ArbalesterReloadEvent event = new ArbalesterReloadEvent(this);
        event.callEvent();
    }

    private void loadDataBase() {
        dContainer.get(IDatabase.class).register(this);
    }

    private void unLoadDataBase() {
        IDatabase database = dContainer.get(IDatabase.class);
        if(database.isRegistered()) database.unRegister();
    }

    private void registerProviders() {
        dContainer.register(Connection.class, () -> {
            try {
                return dContainer.get(IDatabase.class).getConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void initArbalesters() {
        ArbalesterManager arbalesterManager = dContainer.get(ArbalesterManager.class);

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                arbalesterManager.respawn();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(dContainer.get(ArbalesterManager.class), this);
    }

    private void registerCommands() {
    }
}
