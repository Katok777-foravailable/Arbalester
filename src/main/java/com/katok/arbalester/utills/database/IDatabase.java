package com.katok.arbalester.utills.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.plugin.java.JavaPlugin;

public interface IDatabase {
    public Connection getConnection() throws SQLException;
    public String format(String message);
    public boolean isRegistered();
    public void register(JavaPlugin instance);
    public void unRegister();
}
