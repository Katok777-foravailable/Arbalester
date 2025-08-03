package com.katok.arbalester.arbalesters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import com.katok.arbalester.utills.config.IConfigManager;
import com.katok.arbalester.utills.database.Database;
import com.katok.arbalester.utills.database.IDatabase;

import io.papermc.paper.event.entity.EntityMoveEvent;

public class ArbalesterManager implements Listener {
    private final IConfigManager configManager;
    private final IDatabase database;
    private final JavaPlugin instance;

    private final List<Arbalester> arbalesters = new ArrayList<>();

    @Inject
    public ArbalesterManager(IConfigManager configManager, IDatabase database, JavaPlugin instance) {
        this.configManager = configManager;
        this.database = database;
        this.instance = instance;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onSkeletonDeathFromPlayer(EntityDamageByEntityEvent e) {
        String skeletonName = e.getEntity().getPersistentDataContainer().get(Arbalester.SKELETON_KEY, PersistentDataType.STRING);
        if(skeletonName == null) return;
        if(e.getDamage() < ((LivingEntity) e.getEntity()).getHealth()) return;
        if(!(e.getDamager() instanceof Player player)) return;

        Arbalester arbalester = Objects.requireNonNull(getArbalester(skeletonName));

        player.addPotionEffects(arbalester.getRandomEffects(player.getInventory().getItemInMainHand()));
        player.getInventory().addItem(arbalester.getRandomItems(player.getInventory().getItemInMainHand()).toArray(new ItemStack[0]));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onSkeletonDeath(EntityDeathEvent e) {
        String skeletonName = e.getEntity().getPersistentDataContainer().get(Arbalester.SKELETON_KEY, PersistentDataType.STRING);
        if(skeletonName == null) return;

        Arbalester arbalester = Objects.requireNonNull(getArbalester(skeletonName));

        arbalester.despawn();

        arbalester.setLevel(arbalester.getLevel() + 1);
        arbalester.spawn();
    }

    @EventHandler
    private void onProjectileHit(ProjectileHitEvent e) {
        if(!e.getEntity().getPersistentDataContainer().has(Arbalester.ARROW_BOOM_STRENGTH_KEY)) return;
        if(e.getHitEntity() == null) return;

        e.getEntity().getLocation().createExplosion(e.getEntity(), e.getEntity().getPersistentDataContainer().get(Arbalester.ARROW_BOOM_STRENGTH_KEY, PersistentDataType.INTEGER));
    }

    @EventHandler
    private void onMove(EntityMoveEvent e) {
        if(!e.getEntity().getPersistentDataContainer().has(Arbalester.SKELETON_KEY)) return;

        e.setCancelled(true);
    }

    @Nullable
    public Arbalester getArbalester(String name) {
        for(Arbalester arbalester: arbalesters) {
            if(!arbalester.getName().equals(name)) continue;

            return arbalester;
        }

        return null;
    }

    public int getLevel(String skeletonName) throws SQLException {
        try(Connection connection = database.getConnection()) {
            try(PreparedStatement preparedStatement = connection.prepareStatement(database.format("SELECT * FROM {TABLE_NAME} WHERE {NAME} = ?;"))) {
                preparedStatement.setString(1, skeletonName);
                
                try(ResultSet resultSet = preparedStatement.executeQuery()) {
                    if(resultSet.next()) {
                        return resultSet.getInt(Database.LVL);
                    }
                }
            }
        }

        return -1;
    }

    public void respawn() throws SQLException {
        despawn();

        YamlConfiguration config = configManager.getConfiguration("config.yml");
        for(String key: config.getConfigurationSection("skelets").getKeys(false)) {
            ConfigurationSection skeletonCFG = config.getConfigurationSection("skelets." + key);

            Arbalester arbalester = new Arbalester(skeletonCFG, instance, config.getInt("shoot-distance"), Math.max(1, getLevel(skeletonCFG.getName())));
            Bukkit.getScheduler().runTask(instance, () -> {
                arbalester.spawn();
                arbalesters.add(arbalester);
            });
        }
    }

    public void despawn() {
        for(Arbalester arbalester: arbalesters) {
            try {
                if(getLevel(arbalester.getName()) < arbalester.getLevel()) saveLevel(arbalester); // оно и должно быть синхронным, иначе при выключении сервера будет ошибка
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            arbalester.despawn();
        }
        arbalesters.clear();
    }

    public void saveLevel(Arbalester arbalester) throws SQLException {
        try(Connection connection = database.getConnection()) {
            try(PreparedStatement preparedStatement = connection.prepareStatement(database.format("DELETE FROM {TABLE_NAME} WHERE {NAME} = ?;"))) {
                preparedStatement.setString(1, arbalester.getName());

                preparedStatement.executeUpdate();
            }
            try(PreparedStatement preparedStatement = connection.prepareStatement(database.format("INSERT {TABLE_NAME}({NAME}, {LVL}) VALUES(?, ?);"))) {
                preparedStatement.setString(1, arbalester.getName());
                preparedStatement.setInt(2, arbalester.getLevel());

                preparedStatement.executeUpdate();
            }
        }
    }
}
