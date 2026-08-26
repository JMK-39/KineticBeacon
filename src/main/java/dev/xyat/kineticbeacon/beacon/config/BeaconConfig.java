package dev.xyat.kineticbeacon.beacon.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.kineticbeacon.KineticBeacon;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.*;

public class BeaconConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("kineticcore/beacon.toml");
    private static CommentedFileConfig configData;

    public static final Set<String> BEACON_SPAWN_WHITELIST_CACHE = new HashSet<>();
    public static final Set<String> BEACON_SPAWN_BLACKLIST_CACHE = new HashSet<>();
    public static final Map<String, String> BEACON_RULES_CACHE = new HashMap<>();

    public static boolean enableBeaconChunkLoading = true;
    public static List<Integer> beaconLevelRadii = new ArrayList<>();
    public static boolean enableBeaconSpawnPrevention = true;
    public static List<String> beaconSpawnWhitelist = new ArrayList<>();
    public static List<String> beaconSpawnBlacklist = new ArrayList<>();
    public static List<String> beaconRulesRaw = new ArrayList<>();

    public static int globalChunkLoadLimit = 500;
    public static boolean perPlayerLimitEnabled = false;
    public static int perPlayerChunkLoadLimit = 100;
    public static int beaconOfflineTimeout = 4320;
    public static boolean offlineDisableDeactivate = true;
    public static boolean offlineDisableChunkLoad = true;
    public static boolean offlineDisableSpawnPrevent = true;

    public static void load() {
        try {
            configData = CommentedFileConfig.builder(CONFIG_PATH)
                    .sync().preserveInsertionOrder().writingMode(WritingMode.REPLACE).build();
            configData.load();
            setupConfig();
            configData.save();
            readValues();
        } catch (Exception e) {
            KineticBeacon.LOGGER.error("BeaconConfig Load Failed", e);
        }
    }

    private static void setupConfig() {
        configData.setComment("beacon", "Beacon Settings\n信标功能设置 (区块加载与防刷怪)");
        define("beacon.enableChunkLoading", true,
                "是否允许信标加载周围的区块 (保持机器运行)\nWhether to allow Beacons to load surrounding chunks.");
        define("beacon.radii", Arrays.asList(0, 1, 2, 3),
                "信标各等级 (1-4级) 对应的强加载半径 (单位: 区块)。\n0=1x1, 1=3x3, 2=5x5, 3=7x7\nChunk load radius per beacon level (Level 1-4).");

        configData.setComment("beacon.limits", "Beacon Load Limits\n信标强加载容量上限设置");
        define("beacon.limits.globalLimit", 500, "全局强加载区块上限 (所有维度总和)\nGlobal chunk load limit (sum of all dimensions).");
        define("beacon.limits.perPlayerEnable", false, "是否启用按玩家独立计算上限\nWhether to enable per-player load limit.");
        define("beacon.limits.perPlayerLimit", 100, "每个玩家可支配的强加载区块上限(不得超过全局上限)\nPer-player load limit.");

        configData.setComment("beacon.offline", "Beacon Offline Timeout\n玩家下线超时设置");
        define("beacon.offline.timeout", 4320, "玩家离线多少分钟后功能失效 (默认3天，0为立刻，-1为永不失效)\nMinutes after logout to deactivate (0=instant, -1=never).");
        define("beacon.offline.deactivate", true, "离线失效后，是否连同信标发光状态一起关闭(如同未激活)\nDeactivate the beacon light/buffs entirely when timeout.");
        define("beacon.offline.disableChunkLoad", true, "离线失效后，是否关闭区块强加载\nDisable chunk loading when timeout.");
        define("beacon.offline.disableSpawnPrevent", true, "离线失效后，是否关闭防刷怪\nDisable spawn prevention when timeout.");

        configData.setComment("beacon.spawnPrevention", "Beacon Spawn Prevention\n信标区域防刷怪设置 (在强加载范围内禁止怪物生成)");
        define("beacon.spawnPrevention.enable", true,
                "是否开启信标防刷怪功能\nWhether to prevent hostile mob natural spawning.");
        define("beacon.spawnPrevention.whitelist", Arrays.asList("minecraft:villager"),
                "允许生成的生物白名单 (即使在信标范围内也允许生成)。\nEntity spawn whitelist.");
        define("beacon.spawnPrevention.blacklist", new ArrayList<>(),
                "严禁生成的生物黑名单 (无论是否在信标范围内均拦截生成)。\nEntity spawn blacklist.");
        define("beacon.spawnPrevention.rules", Arrays.asList("minecraft:zombie;A", "minecraft:skeleton;AE"),
                "高级生成规则。格式: '实体ID;拦截代码'\nAdvanced Spawn Rules List. Format: 'EntityID;Codes'.");
    }

    private static void define(String path, Object def, String comment) {
        if (!configData.contains(path)) configData.set(path, def);
        configData.setComment(path, " " + comment.trim());
    }

    private static void readValues() {
        enableBeaconChunkLoading = configData.getOrElse("beacon.enableChunkLoading", true);
        beaconLevelRadii = sanitizeRadii(configData.getOrElse("beacon.radii", Arrays.asList(0, 1, 2, 3)));
        enableBeaconSpawnPrevention = configData.getOrElse("beacon.spawnPrevention.enable", true);
        beaconSpawnWhitelist = sanitizeStrings(configData.getOrElse("beacon.spawnPrevention.whitelist", new ArrayList<>()));
        beaconSpawnBlacklist = sanitizeStrings(configData.getOrElse("beacon.spawnPrevention.blacklist", new ArrayList<>()));
        beaconRulesRaw = sanitizeStrings(configData.getOrElse("beacon.spawnPrevention.rules", new ArrayList<>()));

        globalChunkLoadLimit = Math.max(0, configData.getOrElse("beacon.limits.globalLimit", 500));
        perPlayerLimitEnabled = configData.getOrElse("beacon.limits.perPlayerEnable", false);
        perPlayerChunkLoadLimit = Math.max(0, Math.min(
                configData.getOrElse("beacon.limits.perPlayerLimit", 100),
                globalChunkLoadLimit
        ));

        beaconOfflineTimeout = Math.max(-1, configData.getOrElse("beacon.offline.timeout", 4320));
        offlineDisableDeactivate = configData.getOrElse("beacon.offline.deactivate", true);
        offlineDisableChunkLoad = configData.getOrElse("beacon.offline.disableChunkLoad", true);
        offlineDisableSpawnPrevent = configData.getOrElse("beacon.offline.disableSpawnPrevent", true);

        rebuildCaches();
    }

    private static void rebuildCaches() {
        BEACON_SPAWN_WHITELIST_CACHE.clear();
        for (String s : beaconSpawnWhitelist) BEACON_SPAWN_WHITELIST_CACHE.add(s.trim());

        BEACON_SPAWN_BLACKLIST_CACHE.clear();
        for (String s : beaconSpawnBlacklist) BEACON_SPAWN_BLACKLIST_CACHE.add(s.trim());

        BEACON_RULES_CACHE.clear();
        for (String r : beaconRulesRaw) {
            String[] p = r.split(";");
            if (p.length >= 2) BEACON_RULES_CACHE.put(p[0].trim(), p[1].trim().toUpperCase());
        }
    }

    private static List<Integer> sanitizeRadii(List<Integer> values) {
        if (values == null) return new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        List<Integer> result = new ArrayList<>(values.size());
        for (Integer value : values) {
            if (value != null) result.add(Math.max(-1, Math.min(16, value)));
        }
        return result;
    }

    private static List<String> sanitizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) return new ArrayList<>();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) continue;
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) unique.add(trimmed);
        }
        return new ArrayList<>(unique);
    }

    public static int getBeaconRadius(int level) {
        if (!enableBeaconChunkLoading || level <= 0 || beaconLevelRadii.isEmpty()) return -1;
        int idx = level - 1;
        int r = idx < beaconLevelRadii.size() ? beaconLevelRadii.get(idx) : beaconLevelRadii.get(beaconLevelRadii.size() - 1);
        return Math.min(Math.max(r, -1), 16);
    }

    public static void save() {
        if (configData == null) return;
        beaconLevelRadii = sanitizeRadii(beaconLevelRadii);
        beaconSpawnWhitelist = sanitizeStrings(beaconSpawnWhitelist);
        beaconSpawnBlacklist = sanitizeStrings(beaconSpawnBlacklist);
        beaconRulesRaw = sanitizeStrings(beaconRulesRaw);
        globalChunkLoadLimit = Math.max(0, globalChunkLoadLimit);
        perPlayerChunkLoadLimit = Math.max(0, Math.min(perPlayerChunkLoadLimit, globalChunkLoadLimit));
        beaconOfflineTimeout = Math.max(-1, beaconOfflineTimeout);
        configData.set("beacon.enableChunkLoading", enableBeaconChunkLoading);
        configData.set("beacon.radii", beaconLevelRadii);
        configData.set("beacon.spawnPrevention.enable", enableBeaconSpawnPrevention);
        configData.set("beacon.spawnPrevention.whitelist", beaconSpawnWhitelist);
        configData.set("beacon.spawnPrevention.blacklist", beaconSpawnBlacklist);
        configData.set("beacon.spawnPrevention.rules", beaconRulesRaw);

        configData.set("beacon.limits.globalLimit", globalChunkLoadLimit);
        configData.set("beacon.limits.perPlayerEnable", perPlayerLimitEnabled);
        configData.set("beacon.limits.perPlayerLimit", perPlayerChunkLoadLimit);
        configData.set("beacon.offline.timeout", beaconOfflineTimeout);
        configData.set("beacon.offline.deactivate", offlineDisableDeactivate);
        configData.set("beacon.offline.disableChunkLoad", offlineDisableChunkLoad);
        configData.set("beacon.offline.disableSpawnPrevent", offlineDisableSpawnPrevent);
        configData.save();
        readValues();
    }
}
