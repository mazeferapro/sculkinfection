package mazefera.sculkInfection;

import org.bukkit.Bukkit;
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

        startZombieTargetingTask();
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

        for (int x = -6; x <= 6; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -6; z <= 6; z++) {
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
                            spawnInfectedZombie(block.getLocation(), configManager.getShriekerZombieSpawnCount());
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
        double weaknessFactor = configManager.getZombieWeakness();

        zombie.setCustomName(ChatColor.DARK_GREEN + "Sculk Infected");
        zombie.setCustomNameVisible(true);

        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double newMaxHealth = maxHealth.getBaseValue() / weaknessFactor;
            maxHealth.setBaseValue(newMaxHealth);
            zombie.setHealth(newMaxHealth);
        }

        var attackDamage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(attackDamage.getBaseValue() / weaknessFactor);
        }

        var movementSpeed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(0.3);
        }

        zombie.setCanPickupItems(false);
    }

    private void startZombieTargetingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                double radius = configManager.getZombieSeekRadius();

                for (UUID zombieId : infectedZombies.keySet()) {
                    Entity entity = Bukkit.getEntity(zombieId);
                    if (!(entity instanceof Zombie zombie)) continue;
                    if (zombie.isDead()) continue;

                    // Знаходимо найближчу ціль
                    LivingEntity nearestTarget = zombie.getWorld().getEntities().stream()
                            .filter(e -> e instanceof LivingEntity)
                            .map(e -> (LivingEntity) e)
                            .filter(e -> {
                                // Виключаємо союзників
                                if (e instanceof Zombie && isInfectedZombie((Zombie) e)) return false;

                                if (e instanceof Player player) {
                                    switch (player.getGameMode()) {
                                        case CREATIVE, SPECTATOR -> { return false; }
                                    }
                                }

                                return true;
                            })
                            .filter(e -> e.getLocation().distance(zombie.getLocation()) <= radius)
                            .min(Comparator.comparingDouble(e -> e.getLocation().distance(zombie.getLocation())))
                            .orElse(null);

                    zombie.setTarget(nearestTarget);
                }
            }
        }.runTaskTimer(plugin, 0, 20);
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
                long maxLifetime = configManager.getZombieLifetimeSeconds() * 1000;

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