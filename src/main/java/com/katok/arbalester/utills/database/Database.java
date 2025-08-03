package com.katok.arbalester.utills.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.katok.arbalester.utills.config.IConfigManager;
import com.zaxxer.hikari.HikariDataSource;

public class Database implements IDatabase {
    public static final String TABEL_NAME = "skeletons";
    public static final String NAME = "name";
    public static final String LVL = "lvl";

    private boolean isRegistered = false;
    private HikariDataSource db;

    private final IConfigManager configManager;

    @Inject
    public Database(IConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return db.getConnection();
    }

    @Override
    public String format(String message) {
        if (message == null) {
            return "";
        }

        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String replacement = switch (matcher.group(1)) {
                case("TABLE_NAME") -> TABEL_NAME;
                case("NAME") -> NAME;
                case("LVL") -> LVL;
                default -> matcher.group();
            };
            message = message.replace(matcher.group(), replacement);
        }
        return message;
    }

    @Override
    public boolean isRegistered() {
        return isRegistered;
    }

    @Override
    public void register(JavaPlugin instance) {
        if(isRegistered) return;
        isRegistered = true;

        db = new HikariDataSource();
        YamlConfiguration config = configManager.getConfiguration("config.yml");
        db.setJdbcUrl("jdbc:mysql://" + config.getString("database.ip") +"/" + config.getString("database.database"));
        db.setUsername(config.getString("database.username"));
        db.setPassword(config.getString("database.password"));
        db.addDataSourceProperty("cachePrepStmts", "true");
        db.addDataSourceProperty("prepStmtCacheSize", "250");
        db.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try(Connection connection = db.getConnection()) {
            Statement statement = connection.createStatement();

            statement.executeUpdate(format("CREATE TABLE IF NOT EXISTS `{TABLE_NAME}` ( " +
            "`{NAME}` VARCHAR(16) PRIMARY KEY, " +
            "`{LVL}` INT " +
            " );"));

            statement.close();
        } catch (SQLException e) {
            instance.getLogger().severe("Не удалось войти в базу данных и создать таблицы.");
            throw new RuntimeException(e);
        }
    }
    
    public void unRegister() {
        db.close();
        isRegistered = false;
    }
}
