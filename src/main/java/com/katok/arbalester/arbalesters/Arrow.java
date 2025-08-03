package com.katok.arbalester.arbalesters;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Skeleton;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public record Arrow(JavaPlugin instance, int damage, int boomStrength) {
    public void fire(Skeleton from, Entity to) {
        org.bukkit.entity.Arrow arrow = from.launchProjectile(org.bukkit.entity.Arrow.class, from.getLocation().getDirection().add(new Vector(0, Math.toRadians(from.getLocation().distance(to.getLocation()) * Math.PI / 2), 0)));
        arrow.setDamage(damage);

        if(boomStrength > 0) {
            arrow.getPersistentDataContainer().set(Arbalester.ARROW_BOOM_STRENGTH_KEY, PersistentDataType.INTEGER, boomStrength);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if(arrow.isOnGround() || arrow.isDead()) cancel();

                arrow.getLocation().getWorld().spawnParticle(Particle.SPELL_WITCH, arrow.getLocation(), 1);
            }
        }.runTaskTimer(instance, 1, 1);
    }
}
