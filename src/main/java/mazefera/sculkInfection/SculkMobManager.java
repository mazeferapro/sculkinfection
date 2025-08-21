package mazefera.sculkInfection;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SculkMobManager implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();

    // Кулдаун для Sculk Shriekers (UUID блока -> час останнього спавну)
    private final Map<String, Long> shriekerCooldowns = new HashMap<>();

    // Список заражених зомбі для відстеження їх часу життя
    private final Map<UUID, Long> infectedZombies = new HashMap<>();

    public SculkMobManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        // Запускаємо задачу для видалення старих зомбі
        startZombieCleanupTask();
    }

    /**
     * Спробувати заспавнити зомбі на блоці скалку
     */
    public void trySpawnZombieOnSculk(Location sculkLocation) {
        if (!configManager.isPluginEnabled()) {
            return;
        }

        // Перевіряємо шанс спавну (0.001)
        if (random.nextDouble() < configManager.getSculkZombieSpawnChance()) {
            spawnInfectedZombie(sculkLocation, 1);
        }
    }

    /**
     * Обробляє спавн від Sculk Shrieker
     */
    @EventHandler
    public void onPlayerNearShrieker(PlayerMoveEvent event) {
        if (!configManager.isPluginEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        if (player.isSneaking()) {
            return;
        }

        Location playerLoc = player.getLocation();

        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    Location checkLoc = playerLoc.clone().add(x, y, z);
                    Block block = checkLoc.getBlock();

                    if (block.getType() == Material.SCULK_SHRIEKER) {
                        String blockKey = getBlockKey(block.getLocation());
                        long currentTime = System.currentTimeMillis();
                        long cooldownTime = configManager.getShriekerCooldownMinutes() * 60 * 1000;

                        // Перевіряємо кулдаун
                        if (!shriekerCooldowns.containsKey(blockKey) ||
                                currentTime - shriekerCooldowns.get(blockKey) >= cooldownTime) {

                            // Оновлюємо кулдаун
                            shriekerCooldowns.put(blockKey, currentTime);

                            // Спавнимо 4 зомбі
                            spawnInfectedZombie(block.getLocation(), 4);
                        }
                    }
                }
            }
        }
    }

    /**
     * Спавнить заражених зомбі
     */
    private void spawnInfectedZombie(Location location, int count) {
        for (int i = 0; i < count; i++) {
            // Знаходимо безпечне місце для спавну (над блоком)
            Location spawnLoc = findSafeSpawnLocation(location);
            if (spawnLoc == null) continue;

            // Створюємо зомбі
            Zombie zombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);

            // Налаштовуємо зомбі
            setupInfectedZombie(zombie);

            // Додаємо до списку відстеження
            infectedZombies.put(zombie.getUniqueId(), System.currentTimeMillis());

        }
    }

    /**
     * Налаштовує зараженого зомбі
     */
    private void setupInfectedZombie(Zombie zombie) {
        // Встановлюємо назву
        zombie.setCustomName(ChatColor.DARK_GREEN + "Sculk Infected");
        zombie.setCustomNameVisible(true);

        // Робимо зомбі в 10 разів слабшим
        zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2.0); // 20/10 = 2
        zombie.setHealth(2.0);
        zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(0.3); // 3/10 = 0.3

        // Робимо трохи швидшим для компенсації слабкості
        zombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.3);


        // Не може піднімати предмети
        zombie.setCanPickupItems(false);
    }

    /**
     * Обробляє таргетинг зомбі
     */
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!configManager.isPluginEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Zombie)) {
            return;
        }

        Zombie zombie = (Zombie) event.getEntity();

        // Перевіряємо чи це заражений зомбі
        if (!isInfectedZombie(zombie)) {
            return;
        }

        // Отримуємо всіх живих істот поблизу (радіус можна налаштувати)
        double radius = 10.0; // наприклад, 10 блоків
        List<LivingEntity> nearbyEntities;
        nearbyEntities = zombie.getWorld().getEntities().stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(e -> !isInfectedZombie((Zombie) e)) // виключаємо союзників
                .filter(e -> e.getLocation().distance(zombie.getLocation()) <= radius)
                .sorted(Comparator.comparingDouble(e -> e.getLocation().distance(zombie.getLocation())))
                .toList();

        if (!nearbyEntities.isEmpty()) {
            // Встановлюємо найближчу ціль
            zombie.setTarget(nearbyEntities.get(0));
        } else {
            // Якщо ціль відсутня, скасовуємо таргетинг
            event.setCancelled(true);
        }
    }


    /**
     * Перевіряє чи зомбі заражений
     */
    private boolean isInfectedZombie(Zombie zombie) {
        return infectedZombies.containsKey(zombie.getUniqueId()) ||
                (zombie.getCustomName() != null &&
                        zombie.getCustomName().contains("Sculk Infected"));
    }

    /**
     * Знаходить безпечне місце для спавну
     */
    private Location findSafeSpawnLocation(Location center) {
        for (int attempts = 0; attempts < 10; attempts++) {
            Location testLoc = center.clone().add(
                    random.nextDouble() * 6 - 3, // -3 до 3
                    1, // На 1 блок вище
                    random.nextDouble() * 6 - 3  // -3 до 3
            );

            // Перевіряємо чи місце безпечне
            if (testLoc.getBlock().getType().isAir() &&
                    testLoc.clone().add(0, 1, 0).getBlock().getType().isAir() &&
                    testLoc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                return testLoc;
            }
        }

        // Якщо не знайшли безпечне місце, спавнимо прямо над центром
        return center.clone().add(0, 2, 0);
    }

    /**
     * Створює ключ для блока
     */
    private String getBlockKey(Location location) {
        return location.getWorld().getName() + "_" +
                location.getBlockX() + "_" +
                location.getBlockY() + "_" +
                location.getBlockZ();
    }

    /**
     * Запускає задачу очищення старих зомбі
     */
    private void startZombieCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long maxLifetime = 30 * 1000; // 30 секунд в мілісекундах

                infectedZombies.entrySet().removeIf(entry -> {
                    UUID zombieId = entry.getKey();
                    long spawnTime = entry.getValue();

                    // Перевіряємо чи минуло 30 секунд
                    if (currentTime - spawnTime >= maxLifetime) {
                        // Знаходимо та видаляємо зомбі
                        for (Entity entity : plugin.getServer().getWorlds().get(0).getEntities()) {
                            if (entity.getUniqueId().equals(zombieId) && entity instanceof LivingEntity living) {
                                living.damage(living.getHealth());
                                break;
                            }
                        }
                        return true; // Видаляємо з мапи
                    }
                    return false; // Залишаємо в мапі
                });
            }
        }.runTaskTimer(plugin, 0, 20); // Кожну секунду
    }

    /**
     * Очищає кулдауни (для перезавантаження)
     */
    public void clearCooldowns() {
        shriekerCooldowns.clear();
        infectedZombies.clear();
    }
}