package mazefera.sculkInfection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SculkManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final VisualEffectsManager visualEffects;
    private SculkMobManager mobManager; // Додано
    private final Random random = new Random();

    // Список блоків, які можуть бути замінені на Sculk
    private static final Set<Material> REPLACEABLE_BLOCKS = new HashSet<>(Arrays.asList(
            Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE,
            Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.PODZOL,
            Material.COBBLESTONE, Material.SAND, Material.GRAVEL, Material.SANDSTONE,
            Material.DEEPSLATE, Material.TUFF, Material.CALCITE, Material.SMOOTH_BASALT,
            Material.CLAY, Material.TERRACOTTA, Material.MUD, Material.PACKED_MUD
    ));

    // Типи Sculk блоків для випадкового вибору (тільки звичайний Sculk)
    private static final Material SCULK_BASE = Material.SCULK;

    public SculkManager(JavaPlugin plugin, ConfigManager configManager, VisualEffectsManager visualEffects) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.visualEffects = visualEffects;
    }

    // Додано метод для встановлення MobManager
    public void setMobManager(SculkMobManager mobManager) {
        this.mobManager = mobManager;
    }

    /**
     * Перевіряє чи є в радіусі відкалібровані скалкові сенсори, які блокують зараження
     */
    public boolean isBlockedByCalibratedSensor(Location location) {
        int blockRadius = configManager.getCalibratedSensorBlockRadius();

        for (int x = -blockRadius; x <= blockRadius; x++) {
            for (int y = -blockRadius; y <= blockRadius; y++) {
                for (int z = -blockRadius; z <= blockRadius; z++) {
                    Location checkLocation = location.clone().add(x, y, z);

                    // Перевіряємо сферичну відстань
                    double distance = checkLocation.distance(location);
                    if (distance <= blockRadius) {
                        Block block = checkLocation.getBlock();

                        // Перевіряємо чи це відкалібрований скалковий сенсор
                        if (block.getType() == Material.CALIBRATED_SCULK_SENSOR) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public void instantSpread(Location center, int radius) {
        // Перевіряємо блокування відкаліброваними сенсорами
        if (isBlockedByCalibratedSensor(center)) {
            return;
        }

        List<Block> blocksToConvert = getBlocksInRadius(center, radius);

        // Візуальний ефект вибуху
        visualEffects.createSculkBurst(center, radius);

        for (Block block : blocksToConvert) {
            // Додаткова перевірка для кожного блока - чи не заблокований він сенсором
            if (canConvertToSculk(block) && !isBlockedByCalibratedSensor(block.getLocation())) {
                convertToSculk(block);

                // Якщо блок став звичайним Sculk, спробувати створити спеціальні блоки
                if (block.getType() == Material.SCULK) {
                    tryCreateSpecialSculkOnSculkBlock(block);

                    // НОВИЙ КОД: Спроба заспавнити зомбі на блоці sculk
                    if (mobManager != null) {
                        mobManager.trySpawnZombieOnSculk(block.getLocation());
                    }
                }

                visualEffects.createInfectionEffect(block.getLocation());
            }
        }

        // Амбієнтні частинки
        visualEffects.createAmbientSculkParticles(center, radius);
    }

    public void gradualSpread(Location center, int radius) {
        // Перевіряємо блокування відкаліброваними сенсорами
        if (isBlockedByCalibratedSensor(center)) {
            return;
        }

        List<Block> blocksToConvert = getBlocksInRadius(center, radius);
        Collections.shuffle(blocksToConvert); // Випадковий порядок для природного вигляду

        // Початкові візуальні ефекти
        visualEffects.createPulseEffect(center, radius);
        visualEffects.createTentacleEffect(center, 8, radius * 0.7);

        new BukkitRunnable() {
            int index = 0;
            int blocksPerTick = Math.max(1, blocksToConvert.size() / 20); // Розподіляємо на ~1 секунду

            @Override
            public void run() {
                int endIndex = Math.min(index + blocksPerTick, blocksToConvert.size());

                for (int i = index; i < endIndex; i++) {
                    Block block = blocksToConvert.get(i);
                    // Додаткова перевірка для кожного блока - чи не заблокований він сенсором
                    if (canConvertToSculk(block) && !isBlockedByCalibratedSensor(block.getLocation())) {
                        convertToSculk(block);

                        // Якщо блок став звичайним Sculk, спробувати створити спеціальні блоки
                        if (block.getType() == Material.SCULK) {
                            tryCreateSpecialSculkOnSculkBlock(block);

                            // НОВИЙ КОД: Спроба заспавнити зомбі на блоці sculk
                            if (mobManager != null) {
                                mobManager.trySpawnZombieOnSculk(block.getLocation());
                            }
                        }

                        // Візуальний ефект для кожного блока
                        visualEffects.createInfectionEffect(block.getLocation());
                    }
                }

                index = endIndex;

                if (index >= blocksToConvert.size()) {
                    this.cancel();
                    // Фінальний ефект
                    visualEffects.createAmbientSculkParticles(center, radius);
                }
            }
        }.runTaskTimer(plugin, 0, configManager.getSpreadSpeed());
    }

    private List<Block> getBlocksInRadius(Location center, int radius) {
        List<Block> blocks = new ArrayList<>();
        int depth = configManager.getSculkSpreadDepth();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -depth; y <= depth; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Перевіряємо сферичну відстань
                    if (Math.sqrt(x*x + y*y + z*z) <= radius) {
                        Block block = center.getWorld().getBlockAt(
                                center.getBlockX() + x,
                                center.getBlockY() + y,
                                center.getBlockZ() + z
                        );
                        blocks.add(block);
                    }
                }
            }
        }

        return blocks;
    }

    private boolean canConvertToSculk(Block block) {
        Material type = block.getType();

        // Якщо дозволено замінити всі блоки
        if (configManager.shouldReplaceAllBlocks()) {
            return !type.isAir() &&
                    type != Material.BEDROCK &&
                    !type.name().contains("SCULK") &&
                    type != Material.WATER &&
                    type != Material.LAVA;
        }

        // Якщо дозволено поширення через повітря
        if (configManager.shouldSpreadThroughAir() && type.isAir()) {
            // Перевіряємо, чи є твердий блок знизу
            Block below = block.getRelative(BlockFace.DOWN);
            return below.getType().isSolid() && !below.getType().name().contains("SCULK");
        }

        // Стандартна перевірка
        return REPLACEABLE_BLOCKS.contains(type);
    }

    private void convertToSculk(Block block) {
        // Не конвертуємо повітряні блоки
        if (block.getType().isAir()) {
            return;
        }

        // Просто ставимо звичайний Sculk блок
        block.setType(Material.SCULK);
    }

    private void tryCreateSpecialSculkOnSculkBlock(Block sculkBlock) {
        if (sculkBlock.getType() != Material.SCULK) {
            return;
        }

        // Перевіряємо чи над блоком є повітря
        Block blockAbove = sculkBlock.getRelative(BlockFace.UP);
        if (!blockAbove.getType().isAir()) {
            return; // Не створюємо спеціальні блоки якщо над ними немає повітря
        }

        // Перевіряємо шанси для спеціальних блоків (розміщуємо їх НАД sculk блоком)
        double rand = random.nextDouble();

        if (rand < configManager.getSculkCatalystChance()) {
            blockAbove.setType(Material.SCULK_CATALYST);
        } else if (rand < configManager.getSculkCatalystChance() + configManager.getSculkShriekerChance()) {
            blockAbove.setType(Material.SCULK_SHRIEKER);
        } else if (rand < configManager.getSculkCatalystChance() + configManager.getSculkShriekerChance() + configManager.getSculkSensorChance()) {
            blockAbove.setType(Material.SCULK_SENSOR);
        }
    }
}