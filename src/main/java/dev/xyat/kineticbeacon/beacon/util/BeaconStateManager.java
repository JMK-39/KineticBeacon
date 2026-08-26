package dev.xyat.kineticbeacon.beacon.util;

import dev.xyat.kineticbeacon.KineticBeacon;
import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Mod.EventBusSubscriber(modid = KineticBeacon.MODID)
public class BeaconStateManager extends SavedData {
    private static final String DATA_NAME = "kineticbeacon_state";
    private static final UUID UUID_UNKNOWN = new UUID(0, 0);

    private final Map<UUID, Long> offlineTimes = new HashMap<>();

    private final Map<String, Map<Long, Integer>> globalRefs = new HashMap<>();
    private final Map<UUID, Map<String, Map<Long, Integer>>> playerRefs = new HashMap<>();

    private int globalUniqueCount = 0;
    private final Map<UUID, Integer> playerUniqueCount = new HashMap<>();

    public static BeaconStateManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                BeaconStateManager::load,
                BeaconStateManager::new,
                DATA_NAME
        );
    }

    public BeaconStateManager() {}

    public static BeaconStateManager load(CompoundTag tag) {
        BeaconStateManager state = new BeaconStateManager();

        ListTag times = tag.getList("OfflineTimes", 10);
        for (int i = 0; i < times.size(); i++) {
            CompoundTag pt = times.getCompound(i);
            state.offlineTimes.put(pt.getUUID("UUID"), pt.getLong("Time"));
        }

        if (tag.contains("GlobalRefs")) {
            CompoundTag gRefs = tag.getCompound("GlobalRefs");
            for (String dim : gRefs.getAllKeys()) {
                CompoundTag dimTag = gRefs.getCompound(dim);
                long[] posArr = dimTag.getLongArray("Pos");
                int[] countArr = dimTag.getIntArray("Count");
                Map<Long, Integer> map = new HashMap<>();
                for (int i = 0; i < posArr.length && i < countArr.length; i++) {
                    map.put(posArr[i], countArr[i]);
                }
                state.globalRefs.put(dim, map);
            }
        }

        if (tag.contains("PlayerRefs")) {
            ListTag pRefs = tag.getList("PlayerRefs", 10);
            for (int j = 0; j < pRefs.size(); j++) {
                CompoundTag pTag = pRefs.getCompound(j);
                UUID uuid = pTag.getUUID("UUID");
                CompoundTag dimsTag = pTag.getCompound("Dims");
                Map<String, Map<Long, Integer>> pMap = new HashMap<>();
                for (String dim : dimsTag.getAllKeys()) {
                    CompoundTag dimTag = dimsTag.getCompound(dim);
                    long[] posArr = dimTag.getLongArray("Pos");
                    int[] countArr = dimTag.getIntArray("Count");
                    Map<Long, Integer> map = new HashMap<>();
                    for (int i = 0; i < posArr.length && i < countArr.length; i++) {
                        map.put(posArr[i], countArr[i]);
                    }
                    pMap.put(dim, map);
                }
                state.playerRefs.put(uuid, pMap);
            }
        }

        state.recalcGlobalCount();
        for (UUID u : state.playerRefs.keySet()) {
            state.recalcPlayerCount(u);
        }

        return state;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag times = new ListTag();
        for (Map.Entry<UUID, Long> e : offlineTimes.entrySet()) {
            CompoundTag pt = new CompoundTag();
            pt.putUUID("UUID", e.getKey());
            pt.putLong("Time", e.getValue());
            times.add(pt);
        }
        tag.put("OfflineTimes", times);

        CompoundTag gRefs = new CompoundTag();
        for (Map.Entry<String, Map<Long, Integer>> e : globalRefs.entrySet()) {
            gRefs.put(e.getKey(), mapArrays(e.getValue()));
        }
        tag.put("GlobalRefs", gRefs);

        ListTag pRefs = new ListTag();
        for (Map.Entry<UUID, Map<String, Map<Long, Integer>>> pEntry : playerRefs.entrySet()) {
            CompoundTag pTag = new CompoundTag();
            pTag.putUUID("UUID", pEntry.getKey());
            CompoundTag dimsTag = new CompoundTag();
            for (Map.Entry<String, Map<Long, Integer>> e : pEntry.getValue().entrySet()) {
                dimsTag.put(e.getKey(), mapArrays(e.getValue()));
            }
            pTag.put("Dims", dimsTag);
            pRefs.add(pTag);
        }
        tag.put("PlayerRefs", pRefs);

        return tag;
    }

    private CompoundTag mapArrays(Map<Long, Integer> map) {
        long[] posArr = new long[map.size()];
        int[] countArr = new int[map.size()];
        int idx = 0;
        for (Map.Entry<Long, Integer> e : map.entrySet()) {
            posArr[idx] = e.getKey();
            countArr[idx] = e.getValue();
            idx++;
        }
        CompoundTag ct = new CompoundTag();
        ct.putLongArray("Pos", posArr);
        ct.putIntArray("Count", countArr);
        return ct;
    }

    public long getOfflineMinutes(UUID uuid) {
        if (uuid == null) return -1;
        Long time = offlineTimes.get(uuid);
        if (time == null || time == -1L) return -1;
        return (System.currentTimeMillis() - time) / 60000L;
    }

    public int getUsedQuota(UUID uuid) {
        if (BeaconConfig.perPlayerLimitEnabled) {
            return playerUniqueCount.getOrDefault(uuid == null ? UUID_UNKNOWN : uuid, 0);
        } else {
            return globalUniqueCount;
        }
    }

    public boolean tryUpdateChunks(UUID rawUuid, String dim, List<Long> oldChunks, List<Long> newChunks) {
        UUID uuid = rawUuid == null ? UUID_UNKNOWN : rawUuid;

        Set<Long> added = new HashSet<>(newChunks);
        oldChunks.forEach(added::remove);
        Set<Long> removed = new HashSet<>(oldChunks);
        newChunks.forEach(removed::remove);

        if (added.isEmpty() && removed.isEmpty()) return true;

        int newGlobalUnique = 0;
        Map<Long, Integer> gMap = globalRefs.computeIfAbsent(dim, k -> new HashMap<>());
        for (Long pos : added) {
            if (gMap.getOrDefault(pos, 0) == 0) newGlobalUnique++;
        }

        int newPersonalUnique = 0;
        Map<String, Map<Long, Integer>> pDimMap = playerRefs.computeIfAbsent(uuid, k -> new HashMap<>());
        Map<Long, Integer> pMap = pDimMap.computeIfAbsent(dim, k -> new HashMap<>());
        if (BeaconConfig.perPlayerLimitEnabled) {
            for (Long pos : added) {
                if (pMap.getOrDefault(pos, 0) == 0) newPersonalUnique++;
            }
            if (playerUniqueCount.getOrDefault(uuid, 0) + newPersonalUnique > BeaconConfig.perPlayerChunkLoadLimit) {
                return false;
            }
        }

        if (globalUniqueCount + newGlobalUnique > BeaconConfig.globalChunkLoadLimit) {
            return false;
        }

        for (Long pos : added) {
            gMap.put(pos, gMap.getOrDefault(pos, 0) + 1);
            pMap.put(pos, pMap.getOrDefault(pos, 0) + 1);
        }
        for (Long pos : removed) {
            int gc = gMap.getOrDefault(pos, 0) - 1;
            if (gc <= 0) gMap.remove(pos); else gMap.put(pos, gc);

            int pc = pMap.getOrDefault(pos, 0) - 1;
            if (pc <= 0) pMap.remove(pos); else pMap.put(pos, pc);
        }

        recalcGlobalCount();
        recalcPlayerCount(uuid);
        setDirty();
        return true;
    }

    private void recalcGlobalCount() {
        int total = 0;
        for (Map<Long, Integer> map : globalRefs.values()) total += map.size();
        globalUniqueCount = total;
    }

    private void recalcPlayerCount(UUID uuid) {
        int total = 0;
        Map<String, Map<Long, Integer>> dimMap = playerRefs.get(uuid);
        if (dimMap != null) {
            for (Map<Long, Integer> map : dimMap.values()) total += map.size();
        }
        playerUniqueCount.put(uuid, total);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server != null) {
            BeaconStateManager manager = get(server);
            manager.offlineTimes.put(event.getEntity().getUUID(), -1L);
            manager.setDirty();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server != null) {
            BeaconStateManager manager = get(server);
            manager.offlineTimes.put(event.getEntity().getUUID(), System.currentTimeMillis());
            manager.setDirty();
        }
    }
}