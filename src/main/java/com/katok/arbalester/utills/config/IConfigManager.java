package com.katok.arbalester.utills.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;

public interface IConfigManager {
    public static String color(String message) {
        if (message == null) {
            return "";
        }

        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String color = matcher.group(1);
            String replacement = ChatColor.of("#" + color).toString();
            message = message.replace("&#" + color, replacement);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static Component colorComponent(String message) {
        Component result = Component.empty();

        if (message == null) {
            return result;
        }

        {
            Pattern pattern = Pattern.compile("&([A-Za-z0-9])");
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                String color = matcher.group(1);
                message = message.replaceAll("&" + color, "§" + color);
            }
        }

        List<String> colors_text = new ArrayList<>(List.of(message.split("&#")));

        for(int i = 1; i < colors_text.size(); i++) {
            colors_text.set(i, "#" + colors_text.get(i));
        }

        for(String text: colors_text) {
            Pattern pattern = Pattern.compile("#([A-Fa-f0-9]{6})");
            Matcher matcher = pattern.matcher(text);
            if(matcher.find()) {
                String color = matcher.group(1);
                result = result.append(Component.text(text.replaceAll("#" + color, "")).color(TextColor.fromHexString("#" + color)));
            } else {
                result = result.append(Component.text(text));
            }
        }
        return result;
    }

    /**
     * выдает конфигурационный файл, по пути, от папки самого плагина, например - getConfiguration("config.yml")
     * @param path
     * @return конфигурацию
     */
    @Nullable
    public YamlConfiguration getConfiguration(String path);

    /**
     * сканирует конфигурации, и выдает список конфигураций путь которых начинаеться с startWithPath
     * @param startWithPath - путь
     * @return список конфигураций
     */
    @Nullable
    public List<YamlConfiguration> getConfigurations(String startWithPath);

    /**
     * загружает определенный файл из resources в папку плагина
     * @param path
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidConfigurationException
     */
    public void loadConfigFile(String path) throws FileNotFoundException, IOException, InvalidConfigurationException;

    /**
     * сохраняет конфигурационный файл по определенному пути
     * @param path - путь где будет сохранена конфигурация
     * @param configuration - сам конфигурационный файл
     * @throws IOException
     */
    public void saveConfiguration(String path, FileConfiguration configuration) throws IOException;

    /**
     * сохраняет определенную конфигурацию с оперативной памяти в папку плагина
     * @param path - путь конфигурации
     * @throws IOException
     */
    public void saveConfiguration(String path) throws IOException;

    /**
     * сохраняет все конфигурационные файлы в папку плагина
     */
    public void saveAll();

    /**
     * сканирует папку плагина, и сохраняет все файлы с расширением .yml
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidConfigurationException
     */
    public void scanPluginFolder() throws FileNotFoundException, IOException, InvalidConfigurationException;
}