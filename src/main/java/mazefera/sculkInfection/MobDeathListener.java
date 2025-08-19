package mazefera.sculkInfection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MobDeathListener implements Listener {

    private final JavaPlugin plugin;
    private final SculkManager sculkManager;
    private final ConfigManager configManager;
    private final VisualEffectsManager visualEffects;

    public MobDeathListener(JavaPlugin plugin, SculkManager sculkManager, ConfigManager configManager, VisualEffectsManager visualEffects) {
        this.plugin = plugin;
        this.sculkManager = sculkManager;
        this.configManager = configManager;
        this.visualEffects = visualEffects;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Location deathLocation = entity.getLocation();

        // Перевіряємо чи не заблоковане зараження відкаліброваним сенсором
        if (sculkManager.isBlockedByCalibratedSensor(deathLocation)) {
            return;
        }

        // Пошук Sculk Catalyst у збільшеному радіусі
        Block catalyst = findNearbySculkCatalyst(deathLocation);

        if (catalyst != null) {
            // Збираємо весь XP
            int totalXP = event.getDroppedExp();

            // Видаляємо стандартний XP drop
            event.setDroppedExp(0);

            // Активуємо поширення Sculk
            handleSculkSpread(catalyst.getLocation(), deathLocation, totalXP);

            // Створюємо візуальний ефект (частинки)
            createSculkParticles(catalyst.getLocation(), deathLocation);
        }
    }

    private Block findNearbySculkCatalyst(Location location) {
        int radius = configManager.getXpCollectionRadius();

        // Пошук Sculk Catalyst у кубічній області
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = location.getWorld().getBlockAt(
                            location.getBlockX() + x,
                            location.getBlockY() + y,
                            location.getBlockZ() + z
                    );

                    if (block.getType() == Material.SCULK_CATALYST) {
                        // Перевіряємо відстань (сферична область)
                        double distance = block.getLocation().distance(location);
                        if (distance <= radius) {
                            return block;
                        }
                    }
                }
            }
        }

        return null;
    }

    private void handleSculkSpread(Location catalystLocation, Location deathLocation, int xpAmount) {
        // Розраховуємо модифікатор на основі кількості XP
        double xpModifier = 1.0 + (xpAmount / 100.0); // Кожні 100 XP додають +1 до радіусу

        int finalRadius = (int) (configManager.getSculkSpreadRadius() * xpModifier);

        // Візуальний ефект передачі XP
        visualEffects.createSpiralEffect(deathLocation, catalystLocation, 20);

        // Запускаємо поширення Sculk
        if (configManager.isInstantSpread()) {
            sculkManager.instantSpread(deathLocation, finalRadius);
        } else {
            sculkManager.gradualSpread(deathLocation, finalRadius);
        }
    }

    private void createSculkParticles(Location catalystLocation, Location deathLocation) {
        // Візуальні ефекти вже обробляються в VisualEffectsManager
        // Цей метод можна видалити або залишити для додаткових ефектів
    }
}