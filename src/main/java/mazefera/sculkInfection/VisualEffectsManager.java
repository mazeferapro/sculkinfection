package mazefera.sculkInfection;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
                        5,
                        0.2, 0.2, 0.2,
                        0.05
                );

                particleLoc.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        particleLoc,
                        3,
                        0.1, 0.1, 0.1,
                        0.02
                );

                particleLoc.getWorld().spawnParticle(
                        Particle.CRIT,
                        particleLoc,
                        2,
                        0.1, 0.1, 0.1,
                        0.03
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

            center.getWorld().spawnParticle(
                    Particle.SCULK_CHARGE,
                    particleLoc,
                    2,
                    0.1, 0.1, 0.1,
                    0.2f
            );

            center.getWorld().spawnParticle(
                    Particle.CRIT,
                    particleLoc,
                    1,
                    0.05, 0.05, 0.05,
                    0.05
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

                createRingEffect(center, currentRadius, currentRadius * 12);

                if (currentRadius % 2 == 0) {
                    center.getWorld().playSound(
                            center,
                            Sound.BLOCK_SCULK_CATALYST_BLOOM,
                            0.7f + (currentRadius * 0.1f),
                            0.7f - (currentRadius * 0.03f)
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

        world.spawnParticle(
                Particle.SCULK_CHARGE_POP,
                center,
                10,
                0.4, 0.4, 0.4,
                0.1
        );

        world.spawnParticle(
                Particle.WARPED_SPORE,
                center,
                15,
                0.6, 0.6, 0.6,
                0.02
        );

        world.spawnParticle(
                Particle.CRIT,
                center,
                5,
                0.3, 0.3, 0.3,
                0.05
        );

        world.playSound(
                center,
                Sound.BLOCK_SCULK_SPREAD,
                0.6f,
                0.8f + random.nextFloat() * 0.4f
        );
    }

    /**
     * Створює ефект "вибуху" Sculk
     */
    public void createSculkBurst(Location center, int power) {
        World world = center.getWorld();

        world.spawnParticle(
                Particle.SCULK_SOUL,
                center,
                power * 15,
                power * 0.7, power * 0.7, power * 0.7,
                0.1
        );

        new BukkitRunnable() {
            int rings = 0;

            @Override
            public void run() {
                if (rings > power) {
                    this.cancel();
                    return;
                }

                double radius = rings * 1.5;
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);

                    Location particleLoc = center.clone().add(x, 0, z);

                    world.spawnParticle(
                            Particle.SONIC_BOOM,
                            particleLoc,
                            2,
                            0, 0, 0,
                            0
                    );
                    world.spawnParticle(
                            Particle.CRIT,
                            particleLoc,
                            1,
                            0.1, 0.1, 0.1,
                            0.05
                    );
                }

                rings++;
            }
        }.runTaskTimer(plugin, 0, 3);

        world.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 0.5f);
        world.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 0.7f);
    }

    /**
     * Створює тентаклі з частинок
     */
    public void createTentacleEffect(Location origin, int count, double length) {
        for (int i = 0; i < count; i++) {
            final Vector direction = new Vector(
                    random.nextDouble() - 0.5,
                    random.nextDouble() * 0.5,
                    random.nextDouble() - 0.5
            ).normalize();

            new BukkitRunnable() {
                double distance = 0;

                @Override
                public void run() {
                    if (distance > length) {
                        this.cancel();
                        return;
                    }

                    Location particleLoc = origin.clone().add(
                            direction.clone().multiply(distance)
                    );

                    particleLoc.getWorld().spawnParticle(
                            Particle.SCULK_SOUL,
                            particleLoc,
                            3,
                            0.1, 0.1, 0.1,
                            0.03
                    );

                    particleLoc.getWorld().spawnParticle(
                            Particle.ENCHANTED_HIT,
                            particleLoc,
                            1,
                            0, 0, 0,
                            0.05
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
                for (int i = 0; i < 5; i++) {
                    double x = center.getX() + (random.nextDouble() - 0.5) * radius * 2;
                    double y = center.getY() + random.nextDouble() * 3;
                    double z = center.getZ() + (random.nextDouble() - 0.5) * radius * 2;

                    Location particleLoc = new Location(center.getWorld(), x, y, z);

                    particleLoc.getWorld().spawnParticle(
                            Particle.WARPED_SPORE,
                            particleLoc,
                            2,
                            0.1, 0.1, 0.1,
                            0
                    );

                    particleLoc.getWorld().spawnParticle(
                            Particle.SOUL_FIRE_FLAME,
                            particleLoc,
                            1,
                            0.1, 0.1, 0.1,
                            0.01
                    );
                }
            }
        }.runTaskTimer(plugin, 0, 10);
    }
}
