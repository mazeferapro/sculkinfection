package mazefera.sculkInfection;


import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SculkCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final SculkManager sculkManager;

    public SculkCommand(JavaPlugin plugin, ConfigManager configManager, SculkManager sculkManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.sculkManager = sculkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("enhancedsculk.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас немає дозволу на використання цієї команди!");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                configManager.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Конфігурація перезавантажена!");
                break;

            case "info":
                showInfo(sender);
                break;

            case "set":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Використання: /enhancedsculk set <параметр> <значення>");
                    return true;
                }
                handleSet(sender, args[1], args[2]);
                break;

            case "test":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    sculkManager.gradualSpread(player.getLocation(), configManager.getSculkSpreadRadius());
                    sender.sendMessage(ChatColor.GREEN + "Тестове поширення Sculk запущено!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Цю команду може використовувати тільки гравець!");
                }
                break;

            case "testburst":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    SculkInfection.getInstance().getVisualEffects()
                            .createSculkBurst(player.getLocation(), 5);
                    sender.sendMessage(ChatColor.GREEN + "Тестовий вибух Sculk створено!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Цю команду може використовувати тільки гравець!");
                }
                break;

            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_PURPLE + "=== EnhancedSculk Команди ===");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "/enhancedsculk reload" + ChatColor.WHITE + " - Перезавантажити конфігурацію");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "/enhancedsculk info" + ChatColor.WHITE + " - Показати поточні налаштування");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "/enhancedsculk set <параметр> <значення>" + ChatColor.WHITE + " - Змінити налаштування");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "/enhancedsculk test" + ChatColor.WHITE + " - Тестове поширення Sculk");
    }

    private void showInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_PURPLE + "=== Поточні налаштування ===");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Радіус збору XP: " + ChatColor.WHITE + configManager.getXpCollectionRadius());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Радіус поширення Sculk: " + ChatColor.WHITE + configManager.getSculkSpreadRadius());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Глибина поширення: " + ChatColor.WHITE + configManager.getSculkSpreadDepth());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Швидкість поширення (тіки): " + ChatColor.WHITE + configManager.getSpreadSpeed());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Миттєве поширення: " + ChatColor.WHITE + configManager.isInstantSpread());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Поширення через повітря: " + ChatColor.WHITE + configManager.shouldSpreadThroughAir());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Заміна всіх блоків: " + ChatColor.WHITE + configManager.shouldReplaceAllBlocks());
        sender.sendMessage(ChatColor.DARK_PURPLE + "=== Шанси Sculk блоків ===");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Каталізатор: " + ChatColor.WHITE + configManager.getSculkCatalystChance());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Крикун: " + ChatColor.WHITE + configManager.getSculkShriekerChance());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Сенсор: " + ChatColor.WHITE + configManager.getSculkSensorChance());
    }

    private void handleSet(CommandSender sender, String parameter, String value) {
        try {
            switch (parameter.toLowerCase()) {
                case "xp-radius":
                    int xpRadius = Integer.parseInt(value);
                    configManager.setXpCollectionRadius(xpRadius);
                    sender.sendMessage(ChatColor.GREEN + "Радіус збору XP встановлено на " + xpRadius);
                    break;

                case "spread-radius":
                    int spreadRadius = Integer.parseInt(value);
                    configManager.setSculkSpreadRadius(spreadRadius);
                    sender.sendMessage(ChatColor.GREEN + "Радіус поширення Sculk встановлено на " + spreadRadius);
                    break;

                case "spread-depth":
                    int depth = Integer.parseInt(value);
                    configManager.setSculkSpreadDepth(depth);
                    sender.sendMessage(ChatColor.GREEN + "Глибина поширення встановлена на " + depth);
                    break;

                case "spread-speed":
                    int speed = Integer.parseInt(value);
                    configManager.setSpreadSpeed(speed);
                    sender.sendMessage(ChatColor.GREEN + "Швидкість поширення встановлена на " + speed + " тіків");
                    break;

                case "instant":
                    boolean instant = Boolean.parseBoolean(value);
                    configManager.setInstantSpread(instant);
                    sender.sendMessage(ChatColor.GREEN + "Миттєве поширення: " + instant);
                    break;

                case "catalyst-chance":
                    double catalystChance = Double.parseDouble(value);
                    configManager.setSculkCatalystChance(catalystChance);
                    sender.sendMessage(ChatColor.GREEN + "Шанс каталізатора встановлено на " + catalystChance);
                    break;

                case "shrieker-chance":
                    double shriekerChance = Double.parseDouble(value);
                    configManager.setSculkShriekerChance(shriekerChance);
                    sender.sendMessage(ChatColor.GREEN + "Шанс крикуна встановлено на " + shriekerChance);
                    break;

                case "sensor-chance":
                    double sensorChance = Double.parseDouble(value);
                    configManager.setSculkSensorChance(sensorChance);
                    sender.sendMessage(ChatColor.GREEN + "Шанс сенсора встановлено на " + sensorChance);
                    break;

                default:
                    sender.sendMessage(ChatColor.RED + "Невідомий параметр: " + parameter);
                    break;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Невірний формат значення!");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "info", "set", "test"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            completions.addAll(Arrays.asList("xp-radius", "spread-radius", "spread-depth", "spread-speed", "instant", 
                "catalyst-chance", "shrieker-chance", "sensor-chance"));
        }

        return completions;
    }
}