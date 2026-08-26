package dev.xyat.kineticbeacon;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;
import dev.xyat.kineticbeacon.beacon.config.BeaconConfigGui;
import dev.xyat.kineticbeacon.beacon.network.BeaconNetwork;
import dev.xyat.kineticcore.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.config.server.KTServerConfigSpec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(KineticBeacon.MODID)
public final class KineticBeacon {
    public static final String MODID = "kineticbeacon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KineticBeacon(FMLJavaModLoadingContext context) {
        BeaconConfig.load();
        KTServerConfigApi.register(KTServerConfigSpec.builder("kineticbeacon:beacon")
                .booleanValue("enable_chunk_loading", () -> BeaconConfig.enableBeaconChunkLoading, value -> BeaconConfig.enableBeaconChunkLoading = value)
                .integerList("level_radii", () -> BeaconConfig.beaconLevelRadii, value -> BeaconConfig.beaconLevelRadii = value)
                .booleanValue("enable_spawn_prevention", () -> BeaconConfig.enableBeaconSpawnPrevention, value -> BeaconConfig.enableBeaconSpawnPrevention = value)
                .stringList("spawn_whitelist", () -> BeaconConfig.beaconSpawnWhitelist, value -> BeaconConfig.beaconSpawnWhitelist = value)
                .stringList("spawn_blacklist", () -> BeaconConfig.beaconSpawnBlacklist, value -> BeaconConfig.beaconSpawnBlacklist = value)
                .stringList("spawn_rules", () -> BeaconConfig.beaconRulesRaw, value -> BeaconConfig.beaconRulesRaw = value)
                .intValue("global_chunk_load_limit", () -> BeaconConfig.globalChunkLoadLimit, value -> BeaconConfig.globalChunkLoadLimit = value, 0, Integer.MAX_VALUE)
                .booleanValue("per_player_limit_enabled", () -> BeaconConfig.perPlayerLimitEnabled, value -> BeaconConfig.perPlayerLimitEnabled = value)
                .intValue("per_player_chunk_load_limit", () -> BeaconConfig.perPlayerChunkLoadLimit, value -> BeaconConfig.perPlayerChunkLoadLimit = value, 0, Integer.MAX_VALUE)
                .intValue("offline_timeout", () -> BeaconConfig.beaconOfflineTimeout, value -> BeaconConfig.beaconOfflineTimeout = value, -1, Integer.MAX_VALUE)
                .booleanValue("offline_deactivate", () -> BeaconConfig.offlineDisableDeactivate, value -> BeaconConfig.offlineDisableDeactivate = value)
                .booleanValue("offline_chunk_loading", () -> BeaconConfig.offlineDisableChunkLoad, value -> BeaconConfig.offlineDisableChunkLoad = value)
                .booleanValue("offline_spawn_prevention", () -> BeaconConfig.offlineDisableSpawnPrevent, value -> BeaconConfig.offlineDisableSpawnPrevent = value)
                .onSave(BeaconConfig::save)
                .build());
        BeaconNetwork.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BeaconConfigGui::load);
    }
}
