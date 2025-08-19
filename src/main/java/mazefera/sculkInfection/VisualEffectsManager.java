package mazefera.sculkInfection;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Random;

public class VisualEffectsManager {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public VisualEffectsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Створює спіральний ефект частинок від точки смерті до каталізатора
     */
    public void createSpiralEffect(Location from, Location to, int duration) {
        new BukkitRunnable() {
            double t = 0;

            @Override
            public void run() {
                if (t > 1) {
                    this.cancel();
                    return;
                }

                // Обчислення спіральної траєкторії
                double x = Math.cos(t * Math.PI * 4) * (1 - t) * 2;
                double z = Math.sin(t * Math.PI * 4) * (1 - t) * 2;

                Location particleLoc = from.clone().add(
                        (to.getX() - from.getX()) * t + x,
                        (to.getY() - from.getY()) * t + Math.sin(t * Math.PI * 2),
                        (to.getZ() - from.getZ()) * t + z
                );

                particleLoc.getWorld().spawnParticle(
                        Particle.SCULK_SOUL,
                        particleLoc,
                        2,
                        0.1, 0.1, 0.1,
                        0.02
                );

                particleLoc.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        particleLoc,
                        1,
                        0.05, 0.05, 0.05,
                        0.01
                );

                t += 0.05;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /**
     * Створює кільце частинок навколо точки
     */
    public void createRingEffect(Location center, double radius, int particles) {
        for (int i = 0; i < particles; i++) {
            double angle = 2 * Math.PI * i / particles;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0, z);

            // SCULK_CHARGE потребує Float параметр для roll
            center.getWorld().spawnParticle(
                    Particle.SCULK_CHARGE,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0.1,
                    1.0f // roll parameter для SCULK_CHARGE
            );
        }
    }

    /**
     * Створює пульсуючий ефект поширення
     */
    public void createPulseEffect(Location center, int maxRadius) {
        new BukkitRunnable() {
            int currentRadius = 0;

            @Override
            public void run() {
                if (currentRadius > maxRadius) {
                    this.cancel();
                    return;
                }

                createRingEffect(center, currentRadius, currentRadius * 6);

                // Звуковий ефект
                if (currentRadius % 2 == 0) {
                    center.getWorld().playSound(
                            center,
                            Sound.BLOCK_SCULK_CATALYST_BLOOM,
                            0.5f + (currentRadius * 0.1f),
                            0.8f - (currentRadius * 0.05f)
                    );
                }

                currentRadius++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    /**
     * Створює ефект "зараження" блока
     */
    public void createInfectionEffect(Location blockLocation) {
        World world = blockLocation.getWorld();
        Location center = blockLocation.clone().add(0.5, 0.5, 0.5);

        // Використовуємо SCULK_CHARGE_POP замість SCULK_CHARGE
        world.spawnParticle(
                Particle.SCULK_CHARGE_POP,
                center,
                5,
                0.3, 0.3, 0.3,
                0.05
        );

        // Додаткові частинки для атмосфери
        world.spawnParticle(
                Particle.WARPED_SPORE,
                center,
                10,
                0.5, 0.5, 0.5,
                0.01
        );

        // Звук
        world.playSound(
                center,
                Sound.BLOCK_SCULK_SPREAD,
                0.4f,
                0.8f + random.nextFloat() * 0.4f
        );
    }

    /**
     * Створює ефект "вибуху" Sculk
     */
    public void createSculkBurst(Location center, int power) {
        World world = center.getWorld();

        // Центральний вибух частинок
        world.spawnParticle(
                Particle.SCULK_SOUL,
                center,
                power * 10,
                power * 0.5, power * 0.5, power * 0.5,
                0.1
        );

        // Кільця частинок, що розходяться
        new BukkitRunnable() {
            int rings = 0;

            @Override
            public void run() {
                if (rings > power) {
                    this.cancel();
                    return;
                }

                double radius = rings * 1.5;
                for (int i = 0; i < 360; i += 15) {
                    double angle = Math.toRadians(i);
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);

                    Location particleLoc = center.clone().add(x, 0, z);

                    // Використовуємо SONIC_BOOM замість SCULK_CHARGE для кілець
                    world.spawnParticle(
                            Particle.SONIC_BOOM,
                            particleLoc,
                            1,
                            0, 0, 0,
                            0
                    );
                }

                rings++;
            }
        }.runTaskTimer(plugin, 0, 3);

        // Звукові ефекти
        world.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 0.5f);
        world.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 0.7f);
    }

    /**
     * Створює тентаклі з частинок
     */
    public void createTentacleEffect(Location origin, int count, double length) {
        // Генеруємо випадкову кількість щупалець від 5 до 21
        int randomTentacleCount = 5 + random.nextInt(17); // 5 + 0-16 = 5-21

        for (int i = 0; i < randomTentacleCount; i++) {
            // Випадковий розмір для кожного щупальця (від 50% до 150% базової довжини)
            final double randomLength = length * (0.5 + random.nextDouble());

            final Vector direction = new Vector(
                    random.nextDouble() - 0.5,
                    random.nextDouble() * 0.5,
                    random.nextDouble() - 0.5
            ).normalize();

            new BukkitRunnable() {
                double distance = 0;

                @Override
                public void run() {
                    if (distance > randomLength) {
                        this.cancel();
                        return;
                    }

                    Location particleLoc = origin.clone().add(
                            direction.clone().multiply(distance)
                    );

                    // Збільшуємо кількість частинок для більш яскравого ефекту
                    particleLoc.getWorld().spawnParticle(
                            Particle.SCULK_SOUL,
                            particleLoc,
                            2 + random.nextInt(3), // 2-4 частинки замість 1
                            0.1, 0.1, 0.1, // Збільшуємо розкид частинок
                            0.02
                    );

                    distance += 0.3;
                }
            }.runTaskTimer(plugin, i * 2, 1);
        }
    }

    /**
     * Створює амбієнтні частинки навколо зараженої області
     */
    public void createAmbientSculkParticles(Location center, int radius) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    double x = center.getX() + (random.nextDouble() - 0.5) * radius * 2;
                    double y = center.getY() + random.nextDouble() * 3;
                    double z = center.getZ() + (random.nextDouble() - 0.5) * radius * 2;

                    Location particleLoc = new Location(center.getWorld(), x, y, z);

                    particleLoc.getWorld().spawnParticle(
                            Particle.WARPED_SPORE,
                            particleLoc,
                            1,
                            0, 0, 0,
                            0
                    );
                }
            }
        }.runTaskTimer(plugin, 0, 10);
    }
}