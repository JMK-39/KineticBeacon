package dev.xyat.kineticbeacon.beacon.util;

import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;

import java.util.UUID;

public interface IKineticBeaconAccessor {
    boolean kineticbeacon$isChunkLoadEnabled();
    void kineticbeacon$setChunkLoadEnabled(boolean enabled);

    int kineticbeacon$getChunkLoadRadius();
    void kineticbeacon$setChunkLoadRadius(int radius);

    boolean kineticbeacon$isSpawnPreventEnabled();
    void kineticbeacon$setSpawnPreventEnabled(boolean enabled);

    int kineticbeacon$getSpawnPreventRadius();
    void kineticbeacon$setSpawnPreventRadius(int radius);

    int kineticbeacon$getSpawnPreventType();
    void kineticbeacon$setSpawnPreventType(int type);

    String kineticbeacon$getSpawnPreventCodes();
    void kineticbeacon$setSpawnPreventCodes(String codes);

    UUID kineticbeacon$getOwner();
    void kineticbeacon$setOwner(UUID uuid);

    boolean kineticbeacon$getWasOffline();
    void kineticbeacon$setWasOffline(boolean wasOffline);

    boolean kineticbeacon$checkOffline();

    default int kineticbeacon$getActualChunkLoadRadius(int level, int maxRadius) {
        if (!kineticbeacon$isChunkLoadEnabled() || level <= 0 || maxRadius < 0) return -1;
        if (BeaconConfig.offlineDisableChunkLoad && kineticbeacon$checkOffline()) return -1;
        int rad = kineticbeacon$getChunkLoadRadius();
        return (rad < 0 || rad > maxRadius) ? maxRadius : rad;
    }

    default int kineticbeacon$getActualSpawnPreventRadius(int level, int maxRadius) {
        if (!kineticbeacon$isSpawnPreventEnabled() || level <= 0 || maxRadius < 0) return -1;
        if (BeaconConfig.offlineDisableSpawnPrevent && kineticbeacon$checkOffline()) return -1;
        int rad = kineticbeacon$getSpawnPreventRadius();
        return (rad < 0 || rad > maxRadius) ? maxRadius : rad;
    }
}