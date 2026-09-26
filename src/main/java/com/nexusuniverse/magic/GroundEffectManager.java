package com.nexusuniverse.magic;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

/** Temporarily swaps a small radius of ground blocks to an element's ground-effect material, then
 * schedules each one back to whatever it originally was. Deliberately conservative: only a
 * handful of elements even have a ground-block configured (see config.yml), the radius/duration
 * are both small by default, and a short blacklist of functional blocks (crafting tables, chests,
 * etc.) is never touched, so this can't casually destroy something a player built. */
public final class GroundEffectManager {

    private static final Set<Material> PROTECTED = Set.of(
            Material.CRAFTING_TABLE, Material.CHEST, Material.BARRIER, Material.OBSERVER,
            Material.PAINTING, Material.JUKEBOX, Material.LOOM, Material.BREWING_STAND
    );

    private final JavaPlugin plugin;

    public GroundEffectManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Location anchor, Material groundBlock, int radius, int durationSeconds) {
        if (groundBlock == null) {
            return;
        }
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }

        int centerX = anchor.getBlockX();
        int centerY = anchor.getBlockY();
        int centerZ = anchor.getBlockZ();
        int radiusSquared = radius * radius;
        long delayTicks = Math.max(1, durationSeconds) * 20L;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }
                Block block = world.getBlockAt(new Location(world, centerX + dx, centerY, centerZ + dz));
                Material original = block.getType();
                if (PROTECTED.contains(original) || original == groundBlock) {
                    continue;
                }
                block.setType(groundBlock);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (block.getType() == groundBlock) {
                        block.setType(original);
                    }
                }, delayTicks);
            }
        }
    }
}
