package com.katok.arbalester.arbalesters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.katok.arbalester.utills.config.IConfigManager;

public class Arbalester {
    public static final NamespacedKey SKELETON_KEY = new NamespacedKey("arbalester", "skeletonkey");
    public static final NamespacedKey LEVEL_KEY = new NamespacedKey("arbalester", "levelkey");

    public static final NamespacedKey ARROW_BOOM_STRENGTH_KEY = new NamespacedKey("arbalester", "arrowboomstrengthkey");

    private final Random random = new Random();

    private final ConfigurationSection config;
    private final JavaPlugin instance;
    private final int refocusingCooldown;
    private final double shootCooldown;

    private Skeleton skeleton = null;
    private BukkitTask bukkitTask = null;
    private Player target = null;
    private long lastTargeting = 0;
    private long lastShoot = 0;
    private int level = 1;

    public Arbalester(ConfigurationSection config, JavaPlugin instance, int refocusingCooldown) {
        this(config, instance, refocusingCooldown, 1);
    }

    public Arbalester(ConfigurationSection config, JavaPlugin instance, int refocusingCooldown, int startLevel) {
        this.config = config;
        this.instance = instance;
        this.level = startLevel;
        this.refocusingCooldown = refocusingCooldown;

        shootCooldown = config.getDouble("shoot-cooldown");
    }

    @SuppressWarnings("deprecation")
    public Collection<PotionEffect> getRandomEffects(ItemStack item) {
        List<PotionEffect> result = new ArrayList<>();

        if(config.contains("kill.DEFAULT.effects")) {
            ConfigurationSection effectsConfigurationSection = config.getConfigurationSection("kill.DEFAULT.effects");
            for(String potionName: effectsConfigurationSection.getKeys(false)) {
                if(random.nextInt(100) > effectsConfigurationSection.getInt(potionName + ".chance")) continue;

                result.add(new PotionEffect(PotionEffectType.getByName(potionName), effectsConfigurationSection.getInt(potionName + ".time") * 20, effectsConfigurationSection.getInt(potionName + ".lvl") - 1));
            }
        }

        if(item != null && item.getType() != null && config.contains("kill." + item.getType().name() + ".effects")) {
            ConfigurationSection effectsConfigurationSection = config.getConfigurationSection("kill." + item.getType().name() + ".effects");
            for(String potionName: effectsConfigurationSection.getKeys(false)) {
                if(random.nextInt(100) > effectsConfigurationSection.getInt(potionName + ".chance")) continue;

                result.add(new PotionEffect(PotionEffectType.getByName(potionName), effectsConfigurationSection.getInt(potionName + ".time") * 20, effectsConfigurationSection.getInt(potionName + ".lvl") - 1));
            }
        }

        return result;
    }

    public Collection<ItemStack> getRandomItems(ItemStack item) {
        List<ItemStack> result = new ArrayList<>();

        if(config.contains("kill.DEFAULT.drop")) {
            ConfigurationSection itemConfigurationSection = config.getConfigurationSection("kill.DEFAULT.drop");
            for(String itemName: itemConfigurationSection.getKeys(false)) {
                if(random.nextInt(100) > itemConfigurationSection.getInt(itemName + ".chance")) continue;

                result.add(new ItemStack(Material.valueOf(itemName), random.nextInt(itemConfigurationSection.getInt(itemName + ".min"), itemConfigurationSection.getInt(itemName + ".max") + 1)));
            }
        }

        if(item != null && item.getType() != null && config.contains("kill." + item.getType().name() + ".drop") && config.contains("kill.DEFAULT.drop")) {
            ConfigurationSection itemConfigurationSection = config.getConfigurationSection("kill." + item.getType().name() + ".drop");
            for(String itemName: itemConfigurationSection.getKeys(false)) {
                if(random.nextInt(100) > itemConfigurationSection.getInt(itemName + ".chance")) continue;

                result.add(new ItemStack(Material.valueOf(itemName), random.nextInt(itemConfigurationSection.getInt(itemName + ".min"), itemConfigurationSection.getInt(itemName + ".max") + 1)));
            }
        }

        return result;
    }

    public String getName() {
        return config.getName();
    }

    public void spawn() {
        despawn();

        Location spawn = new Location(Bukkit.getWorld(config.getString("position.world")), config.getInt("position.x"), config.getInt("position.y"), config.getInt("position.z"));

        Skeleton skeleton = spawn.getWorld().spawn(spawn, Skeleton.class);
        skeleton.customName(IConfigManager.colorComponent(getName() + " LvL." + level));

        ItemStack crossBow = new ItemStack(Material.CROSSBOW);

        CrossbowMeta crossBowMeta = (CrossbowMeta) crossBow.getItemMeta();
        crossBowMeta.addChargedProjectile(new ItemStack(Material.ARROW));

        crossBow.setItemMeta(crossBowMeta);

        skeleton.getEquipment().setItemInMainHand(crossBow);
        // skeleton.setAI(false);
        skeleton.getPersistentDataContainer().set(SKELETON_KEY, PersistentDataType.STRING, getName());
        skeleton.getPersistentDataContainer().set(LEVEL_KEY, PersistentDataType.INTEGER, level);
        
        this.skeleton = skeleton;

        bukkitTask = new BukkitRunnable() {
            @Override
            public void run() {
                if(skeleton.isDead()) cancel();

                if(lastTargeting + refocusingCooldown * 1000 < System.currentTimeMillis()) {
                    Collection<Player> players = skeleton.getLocation().getNearbyPlayers(config.getInt("shoot-distance"));
                
                    if(players.size() < 1) return;

                    target = players.stream().toList().get(random.nextInt(players.size()));
                    lastTargeting = System.currentTimeMillis();
                }

                skeleton.teleport(skeleton.getLocation().setDirection(skeleton.getLocation().toVector().subtract(target.getLocation().toVector()).multiply(-1)));
            
                if(lastShoot + shootCooldown * 1000 < System.currentTimeMillis()) {
                    int damage = Math.min(config.getInt("damage.max"), config.getInt("damage.every-level-up") * level);
                    int boomStrength = -1;

                    if(random.nextInt(100) < Math.min(config.getInt("boom.max-chance"), config.getInt("boom.every-level-up") * level)) {
                        boomStrength = Math.min(config.getInt("boom.max-strength"), config.getInt("boom.every-strength-level-up") * level);
                    }

                    Arrow arrow = new Arrow(instance, damage, boomStrength);
                    arrow.fire(skeleton, target);

                    lastShoot = System.currentTimeMillis();
                }
            }            
        }.runTaskTimer(instance, 1, 1);
    }

    public void despawn() {
        if(bukkitTask != null) {
            bukkitTask.cancel();
            bukkitTask = null;
        }
        if(skeleton == null) return;
        skeleton.remove();
    }
    

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
