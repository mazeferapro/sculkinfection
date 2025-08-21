package mazefera.sculkInfection;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    // Новий метод для перевірки чи плагін увімкнений
    public boolean isPluginEnabled() {
        return config.getBoolean("plugin-enabled", false);
    }

    // Новий метод для встановлення стану плагіна
    public void setPluginEnabled(boolean enabled) {
        config.set("plugin-enabled", enabled);
        plugin.saveConfig();
    }

    public int getXpCollectionRadius() {
        return config.getInt("xp-collection-radius", 16);
    }

    public void setXpCollectionRadius(int radius) {
        config.set("xp-collection-radius", radius);
        plugin.saveConfig();
    }

    public int getSculkSpreadRadius() {
        return config.getInt("sculk-spread-radius", 5);
    }

    public void setSculkSpreadRadius(int radius) {
        config.set("sculk-spread-radius", radius);
        plugin.saveConfig();
    }

    public int getSculkSpreadDepth() {
        return config.getInt("sculk-spread-depth", 3);
    }

    public void setSculkSpreadDepth(int depth) {
        config.set("sculk-spread-depth", depth);
        plugin.saveConfig();
    }

    public int getSpreadSpeed() {
        return config.getInt("spread-speed-ticks", 2);
    }

    public void setSpreadSpeed(int speed) {
        config.set("spread-speed-ticks", speed);
        plugin.saveConfig();
    }

    public boolean isInstantSpread() {
        return config.getBoolean("instant-spread", false);
    }

    public void setInstantSpread(boolean instant) {
        config.set("instant-spread", instant);
        plugin.saveConfig();
    }

    public boolean shouldSpreadThroughAir() {
        return config.getBoolean("spread-through-air", false);
    }

    public boolean shouldReplaceAllBlocks() {
        return config.getBoolean("replace-all-blocks", false);
    }

    public double getSculkCatalystChance() {
        return config.getDouble("sculk-catalyst-chance", 0.03);
    }

    public void setSculkCatalystChance(double chance) {
        config.set("sculk-catalyst-chance", chance);
        plugin.saveConfig();
    }

    public double getSculkShriekerChance() {
        return config.getDouble("sculk-shrieker-chance", 0.01);
    }

    public void setSculkShriekerChance(double chance) {
        config.set("sculk-shrieker-chance", chance);
        plugin.saveConfig();
    }

    public double getSculkSensorChance() {
        return config.getDouble("sculk-sensor-chance", 0.4);
    }

    public void setSculkSensorChance(double chance) {
        config.set("sculk-sensor-chance", chance);
        plugin.saveConfig();
    }

    public int getCalibratedSensorBlockRadius() {
        return config.getInt("calibrated-sensor-block-radius", 16);
    }

    public void setCalibratedSensorBlockRadius(int radius) {
        config.set("calibrated-sensor-block-radius", radius);
        plugin.saveConfig();
    }

    // === НАЛАШТУВАННЯ ДЛЯ МОБІВ ===

    public double getSculkZombieSpawnChance() {
        return config.getDouble("sculk-zombie-spawn-chance", 0.001);
    }

    public void setSculkZombieSpawnChance(double chance) {
        config.set("sculk-zombie-spawn-chance", chance);
        plugin.saveConfig();
    }

    public int getShriekerCooldownMinutes() {
        return config.getInt("shrieker-cooldown-minutes", 2);
    }

    public void setShriekerCooldownMinutes(int minutes) {
        config.set("shrieker-cooldown-minutes", minutes);
        plugin.saveConfig();
    }

    public int getShriekerZombieSpawnCount() {
        return config.getInt("settings.mobs.shrieker-spawn-count", 4);
    }

    public int getZombieSeekRadius() {
        return config.getInt("settings.mobs.zombie-seek-radius", 10);
    }

    public int getZombieWeakness() {
        return config.getInt("settings.mobs.zombie-weakness-multiplier", 10);
    }

    public int getZombieLifetimeSeconds() {
        return config.getInt("zombie-lifetime-seconds", 30);
    }

    public int getXpMultiplier() {
        return config.getInt("settings.xp-multiplier", 100);
    }

    public void setZombieLifetimeSeconds(int seconds) {
        config.set("zombie-lifetime-seconds", seconds);
        plugin.saveConfig();
    }
}