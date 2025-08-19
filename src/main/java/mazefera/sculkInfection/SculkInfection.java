package mazefera.sculkInfection;

import org.bukkit.plugin.java.JavaPlugin;

public class SculkInfection extends JavaPlugin {

    private static SculkInfection instance;
    private ConfigManager configManager;
    private SculkManager sculkManager;
    private VisualEffectsManager visualEffects;

    @Override
    public void onEnable() {
        instance = this;

        // Завантаження конфігурації
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // Ініціалізація менеджерів
        visualEffects = new VisualEffectsManager(this);
        sculkManager = new SculkManager(this, configManager, visualEffects);

        // Реєстрація подій
        getServer().getPluginManager().registerEvents(
                new MobDeathListener(this, sculkManager, configManager, visualEffects),
                this
        );

        // Реєстрація команд
        SculkCommand commandExecutor = new SculkCommand(this, configManager, sculkManager);
        getCommand("enhancedsculk").setExecutor(commandExecutor);
        getCommand("enhancedsculk").setTabCompleter(commandExecutor);

        // Перевіряємо стан плагіна при запуску
        if (configManager.isPluginEnabled()) {
            getLogger().info("EnhancedSculk Plugin увімкнено та активний!");
        } else {
            getLogger().info("EnhancedSculk Plugin завантажено, але вимкнений за замовчуванням!");
            getLogger().info("Використайте команду '/enhancedsculk enable' або консольну команду 'enhancedsculk enable' для увімкнення.");
        }
    }

    public VisualEffectsManager getVisualEffects() {
        return visualEffects;
    }

    @Override
    public void onDisable() {
        getLogger().info("EnhancedSculk Plugin вимкнено!");
    }

    public static SculkInfection getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SculkManager getSculkManager() {
        return sculkManager;
    }
}