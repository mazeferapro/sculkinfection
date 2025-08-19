package mazefera.sculkInfection;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class VisualEffectsManager {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Set<BukkitTask> activeTasks = new HashSet<>();

    public VisualEffectsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Метод для очищення всіх активних задач
    public void cleanup() {
        synchronized (activeTasks) {
            for (BukkitTask task : new HashSet<>(activeTasks)) {
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }
            }
            activeTasks.clear();
        }

        // Також зупиняємо амбієнтну задачу
        if (ambientTask != null && !ambientTask.isCancelled()) {
            ambientTask.cancel();
            ambientTask = null;
        }
    }

    // Допоміжний метод для додавання задач до трекінгу
    private void addTask(BukkitTask task) {
        synchronized (activeTasks) {
            activeTasks.add(task);
        }
    }

    // Допоміжний метод для видалення задач з трекінгу
    private void removeTask(BukkitTask task) {
        synchronized (activeTasks) {
            activeTasks.remove(task);
        }
    }

    // Спрощений метод для самовидалення задачі
    private void removeSelf(BukkitRunnable runnable) {
        synchronized (activeTasks) {
            activeTasks.removeIf(task -> task.getTaskId() == runnable.getTaskId());
        }
    }

    public void createSpiralEffect(Location from, Location to, int duration) {
        if (from.getWorld() == null || to.getWorld() == null) return;

        BukkitTask task = new BukkitRunnable() {
            double t = 0;
            final World world = from.getWorld();

            @Override
            public void run() {
                if (t > 1 || world == null) {
                    removeTask(this.getTaskId() != -1 ?
                            plugin.getServer().getScheduler().getPendingTasks().stream()
                                    .filter(bt -> bt.getTaskId() == this.getTaskId())
                                    .findFirst().orElse(null) : null);
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

                world.spawnParticle(Particle.SCULK_SOUL, particleLoc, 5, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 3, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.CRIT, particleLoc, 2, 0.1, 0.1, 0.1, 0.03);

                t += 0.05;
            }
        }.runTaskTimer(plugin, 0, 1);

        addTask(task);
    }

    public void createRingEffect(Location center, double radius, int particles) {
        World world = center.getWorld();
        if (world == null) return;

        for (int i = 0; i < particles; i++) {
            double angle = 2 * Math.PI * i / particles;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0, z);

            world.spawnParticle(Particle.SCULK_CHARGE, particleLoc, 2, 0.1, 0.1, 0.1, 0.2);
            world.spawnParticle(Particle.CRIT, particleLoc, 1, 0.05, 0.05, 0.05, 0.05);
        }
    }

    public void createPulseEffect(Location center, int maxRadius) {
        if (center.getWorld() == null) return;

        BukkitTask task = new BukkitRunnable() {
            int currentRadius = 0;
            final World world = center.getWorld();

            @Override
            public void run() {
                if (world == null || currentRadius > maxRadius) {
                    removeTask(this.getTaskId() != -1 ?
                            plugin.getServer().getScheduler().getPendingTasks().stream()
                                    .filter(bt -> bt.getTaskId() == this.getTaskId())
                                    .findFirst().orElse(null) : null);
                    this.cancel();
                    return;
                }

                createRingEffect(center, currentRadius, Math.max(1, currentRadius * 12));

                if (currentRadius % 2 == 0) {
                    world.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM,
                            (float) (0.7 + (currentRadius * 0.1)),
                            (float) (0.7 - (currentRadius * 0.03)));
                }

                currentRadius++;
            }
        }.runTaskTimer(plugin, 0, 2);

        addTask(task);
    }

    public void createInfectionEffect(Location blockLocation) {
        World world = blockLocation.getWorld();
        if (world == null) return;

        Location center = blockLocation.clone().add(0.5, 0.5, 0.5);

        world.spawnParticle(Particle.SCULK_CHARGE_POP, center, 10, 0.4, 0.4, 0.4, 0.1);
        world.spawnParticle(Particle.WARPED_SPORE, center, 15, 0.6, 0.6, 0.6, 0.02);
        world.spawnParticle(Particle.CRIT, center, 5, 0.3, 0.3, 0.3, 0.05);

        world.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.8f + random.nextFloat() * 0.4f);
    }

    public void createSculkBurst(Location center, int power) {
        World world = center.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.SCULK_SOUL, center, power * 15, power * 0.7, power * 0.7, power * 0.7, 0.1);

        BukkitTask task = new BukkitRunnable() {
            int rings = 0;

            @Override
            public void run() {
                if (world == null || rings > power) {
                    removeTask(this.getTaskId() != -1 ?
                            plugin.getServer().getScheduler().getPendingTasks().stream()
                                    .filter(bt -> bt.getTaskId() == this.getTaskId())
                                    .findFirst().orElse(null) : null);
                    this.cancel();
                    return;
                }

                double radius = rings * 1.5;
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);

                    Location particleLoc = center.clone().add(x, 0, z);
                    world.spawnParticle(Particle.SONIC_BOOM, particleLoc, 2, 0, 0, 0, 0);
                    world.spawnParticle(Particle.CRIT, particleLoc, 1, 0.1, 0.1, 0.1, 0.05);
                }

                rings++;
            }
        }.runTaskTimer(plugin, 0, 3);

        addTask(task);

        world.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 0.5f);
        world.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 0.7f);
    }

    public void createTentacleEffect(Location origin, int count, double length) {
        World world = origin.getWorld();
        if (world == null) return;

        for (int i = 0; i < count; i++) {
            final Vector direction = new Vector(
                    random.nextDouble() - 0.5,
                    random.nextDouble() * 0.5,
                    random.nextDouble() - 0.5
            ).normalize();

            BukkitTask task = new BukkitRunnable() {
                double distance = 0;

                @Override
                public void run() {
                    if (distance > length || world == null) {
                        removeTask(this.getTaskId() != -1 ?
                                plugin.getServer().getScheduler().getPendingTasks().stream()
                                        .filter(bt -> bt.getTaskId() == this.getTaskId())
                                        .findFirst().orElse(null) : null);
                        this.cancel();
                        return;
                    }

                    Location particleLoc = origin.clone().add(direction.clone().multiply(distance));
                    world.spawnParticle(Particle.SCULK_SOUL, particleLoc, 3, 0.1, 0.1, 0.1, 0.03);
                    world.spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 1, 0, 0, 0, 0.05);

                    distance += 0.3;
                }
            }.runTaskTimer(plugin, i * 2, 1);

            addTask(task);
        }
    }

    private BukkitTask ambientTask = null;

    public void createAmbientSculkParticles(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;

        // Зупиняємо попередню амбієнтну задачу якщо вона існує
        if (ambientTask != null && !ambientTask.isCancelled()) {
            ambientTask.cancel();
            removeTask(ambientTask);
        }

        ambientTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (world == null) {
                    removeTask(this.getTaskId() != -1 ?
                            plugin.getServer().getScheduler().getPendingTasks().stream()
                                    .filter(bt -> bt.getTaskId() == this.getTaskId())
                                    .findFirst().orElse(null) : null);
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 5; i++) {
                    double x = center.getX() + (random.nextDouble() - 0.5) * radius * 2;
                    double y = center.getY() + random.nextDouble() * 3;
                    double z = center.getZ() + (random.nextDouble() - 0.5) * radius * 2;

                    Location particleLoc = new Location(world, x, y, z);
                    world.spawnParticle(Particle.WARPED_SPORE, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }.runTaskTimer(plugin, 0, 10);

        addTask(ambientTask);
    }

    // Метод для зупинки амбієнтних ефектів
    public void stopAmbientEffects() {
        if (ambientTask != null && !ambientTask.isCancelled()) {
            ambientTask.cancel();
            removeTask(ambientTask);
            ambientTask = null;
        }
    }
}