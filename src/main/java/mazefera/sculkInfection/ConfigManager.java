package mazefera.sculkInfection;


import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    // Налаштування плагіна
    private int xpCollectionRadius;
    private int sculkSpreadRadius;
    private int sculkSpreadDepth;
    private int spreadSpeed;
    private boolean instantSpread;
    private boolean spreadThroughAir;
    private boolean replaceAllBlocks;

    // Шанси для різних типів sculk блоків
    private double sculkCatalystChance;
    private double sculkShriekerChance;
    private double sculkSensorChance;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Завантаження налаштувань
        xpCollectionRadius = config.getInt("xp-collection-radius", 16);
        sculkSpreadRadius = config.getInt("sculk-spread-radius", 5);
        sculkSpreadDepth = config.getInt("sculk-spread-depth", 3);
        spreadSpeed = config.getInt("spread-speed-ticks", 2);
        instantSpread = config.getBoolean("instant-spread", false);
        spreadThroughAir = config.getBoolean("spread-through-air", false);
        replaceAllBlocks = config.getBoolean("replace-all-blocks", false);

        // Шанси для sculk блоків
        sculkCatalystChance = config.getDouble("sculk-catalyst-chance", 0.03);
        sculkShriekerChance = config.getDouble("sculk-shrieker-chance", 0.01);
        sculkSensorChance = config.getDouble("sculk-sensor-chance", 0.4);
    }

    public void saveConfig() {
        plugin.saveConfig();
    }

    // Getters
    public int getXpCollectionRadius() {
        return xpCollectionRadius;
    }

    public int getSculkSpreadRadius() {
        return sculkSpreadRadius;
    }

    public int getSculkSpreadDepth() {
        return sculkSpreadDepth;
    }

    public int getSpreadSpeed() {
        return spreadSpeed;
    }

    public boolean isInstantSpread() {
        return instantSpread;
    }

    public boolean shouldSpreadThroughAir() {
        return spreadThroughAir;
    }

    public boolean shouldReplaceAllBlocks() {
        return replaceAllBlocks;
    }

    public double getSculkCatalystChance() {
        return sculkCatalystChance;
    }

    public double getSculkShriekerChance() {
        return sculkShriekerChance;
    }

    public double getSculkSensorChance() {
        return sculkSensorChance;
    }

    // Setters для динамічної зміни налаштувань
    public void setXpCollectionRadius(int radius) {
        this.xpCollectionRadius = radius;
        config.set("xp-collection-radius", radius);
        saveConfig();
    }

    public void setSculkSpreadRadius(int radius) {
        this.sculkSpreadRadius = radius;
        config.set("sculk-spread-radius", radius);
        saveConfig();
    }

    public void setSculkSpreadDepth(int depth) {
        this.sculkSpreadDepth = depth;
        config.set("sculk-spread-depth", depth);
        saveConfig();
    }

    public void setSpreadSpeed(int speed) {
        this.spreadSpeed = speed;
        config.set("spread-speed-ticks", speed);
        saveConfig();
    }

    public void setInstantSpread(boolean instant) {
        this.instantSpread = instant;
        config.set("instant-spread", instant);
        saveConfig();
    }

    public void setSculkCatalystChance(double chance) {
        this.sculkCatalystChance = chance;
        config.set("sculk-catalyst-chance", chance);
        saveConfig();
    }

    public void setSculkShriekerChance(double chance) {
        this.sculkShriekerChance = chance;
        config.set("sculk-shrieker-chance", chance);
        saveConfig();
    }

    public void setSculkSensorChance(double chance) {
        this.sculkSensorChance = chance;
        config.set("sculk-sensor-chance", chance);
        saveConfig();
    }
}