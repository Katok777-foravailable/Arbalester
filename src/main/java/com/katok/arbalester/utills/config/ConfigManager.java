package com.katok.arbalester.utills.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class ConfigManager implements IConfigManager {
    private final JavaPlugin instance;

    private final HashMap<String, YamlConfiguration> configs = new HashMap<>();

    @Inject
    public ConfigManager(JavaPlugin instance) {
        this.instance = instance;
    }

    @Nullable
    @Override
    public YamlConfiguration getConfiguration(String path) {
        return configs.get(path);
    }

    @Nullable
    @Override
    public List<YamlConfiguration> getConfigurations(String startWithPath) {
        List<YamlConfiguration> result = new ArrayList<>();
        for(String path: configs.keySet()) {
            if(!path.startsWith(startWithPath)) continue;

            result.add(configs.get(path));
        }

        return result;
    }

    @Override
    public void loadConfigFile(String path) throws FileNotFoundException, IOException, InvalidConfigurationException {
        File configFile = new File(instance.getDataFolder().getAbsolutePath() + File.separator + path);

        if(!configFile.exists()) instance.saveResource(path, false);

        YamlConfiguration configCfg = new YamlConfiguration();
        configCfg.load(configFile);

        if(configs.containsKey(path)) {
            configs.replace(path, configCfg);
        } else {
            configs.put(path, configCfg);
        }
    }

    @Override
    public void saveConfiguration(String path, FileConfiguration configuration) throws IOException {
        File configFile = new File(instance.getDataFolder().toPath().toAbsolutePath() + File.separator + path);

        if(!configFile.exists()) {
            configFile.createNewFile();
        }
        if(configs.containsKey(path)) configs.remove(path);
        
        configuration.save(configFile);

        configs.put(path, YamlConfiguration.loadConfiguration(configFile));
    }

    @Override
    public void saveConfiguration(String path) throws IOException {
        File configFile = new File(instance.getDataFolder().toPath().toAbsolutePath() + File.separator + path);
        if(!configFile.exists()) {
            configFile.createNewFile();
        }
        YamlConfiguration configuration = new YamlConfiguration();
        if(configs.containsKey(path)) configuration = configs.get(path);
        
        configuration.save(configFile);
    }

    @Override
    public void saveAll() {
        for(String path: configs.keySet()) {
            File configFile = new File(instance.getDataFolder().toPath().toAbsolutePath() + File.separator + path);
            try {
                configs.get(path).save(configFile);
            } catch (IOException e) {
                new RuntimeException("Не удалось сохранить конфиг " + path, e);
            }
        }
    }

    @Override
    public void scanPluginFolder() throws FileNotFoundException, IOException, InvalidConfigurationException {
        readFiles(new File(instance.getDataFolder().toPath().toAbsolutePath().toString()));
    }

    public void readFiles(File baseDirectory) throws FileNotFoundException, IOException, InvalidConfigurationException{
        if (baseDirectory.isDirectory()){
            for (File file : baseDirectory.listFiles()) {
                if(file.isFile() && file.getAbsolutePath().endsWith(".yml")) {
                    String path = file.getAbsolutePath().substring((instance.getDataFolder().toPath().toAbsolutePath() + File.separator).length());
                    loadConfigFile(path);
                } else {
                    readFiles(file);
                }
            }
        }
    }
}
