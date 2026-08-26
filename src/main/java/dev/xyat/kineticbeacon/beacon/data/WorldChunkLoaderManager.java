package dev.xyat.kineticbeacon.beacon.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldChunkLoaderManager extends SavedData {
    private static final String DATA_NAME = "kineticbeacon_loaders";

    // 原始数据存储：Pos (long) -> Radius (int)
    private final Map<Long, Integer> beaconRadii = new HashMap<>();

    // 预计算的保护区域缓存，避免在 isChunkProtected 中频繁创建对象
    private final List<ProtectedRegion> regionCache = new ArrayList<>();

    // 内部记录类：预存储区块坐标边界
    private record ProtectedRegion(int minX, int maxX, int minZ, int maxZ) {}

    public static WorldChunkLoaderManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WorldChunkLoaderManager::load,
                WorldChunkLoaderManager::new,
                DATA_NAME
        );
    }

    public WorldChunkLoaderManager() {}

    /**
     * [高性能检测接口]
     * 检查指定的区块坐标是否处于保护范围内。
     * 优化点：直接对 int 进行比对，不涉及 BlockPos 转换，性能极高。
     */
    public boolean isChunkProtected(int chunkX, int chunkZ) {
        // 直接遍历缓存，O(n) 整数比对，n 为当前活跃信标数
        for (ProtectedRegion region : regionCache) {
            if (chunkX >= region.minX && chunkX <= region.maxX &&
                    chunkZ >= region.minZ && chunkZ <= region.maxZ) {
                return true;
            }
        }
        return false;
    }

    private void rebuildCache() {
        regionCache.clear();
        for (Map.Entry<Long, Integer> entry : this.beaconRadii.entrySet()) {
            int radius = entry.getValue();
            if (radius < 0) continue;
            // 防刷怪半径 = 强加载半径 + 1
            int protectRadius = radius + 1;
            // 从 Long 还原坐标并转为区块坐标
            BlockPos pos = BlockPos.of(entry.getKey());
            int centerX = pos.getX() >> 4;
            int centerZ = pos.getZ() >> 4;

            regionCache.add(new ProtectedRegion(
                    centerX - protectRadius, centerX + protectRadius,
                    centerZ - protectRadius, centerZ + protectRadius
            ));
        }
    }

    public int getStoredRadius(ServerLevel level, BlockPos pos) {
        return beaconRadii.getOrDefault(pos.asLong(), -1);
    }

    public void setStoredRadius(ServerLevel level, BlockPos pos, int radius) {
        if (radius < 0) {
            beaconRadii.remove(pos.asLong());
        } else {
            beaconRadii.put(pos.asLong(), radius);
        }
        rebuildCache();
        setDirty();
    }

    public void updateChunkForcing(ServerLevel level, int centerX, int centerZ, int radius, boolean force) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                // 调用 Forge 强加载接口
                ForgeChunkManager.forceChunk(level, "kineticbeacon", new BlockPos(x, 0, z), x, z, force, true);
            }
        }
    }

    public static WorldChunkLoaderManager load(CompoundTag tag) {
        WorldChunkLoaderManager manager = new WorldChunkLoaderManager();
        ListTag list = tag.getList("Beacons", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag c = (CompoundTag) t;
            manager.beaconRadii.put(c.getLong("Pos"), c.getInt("Radius"));
        }
        manager.rebuildCache();
        return manager;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Integer> entry : beaconRadii.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", entry.getKey());
            c.putInt("Radius", entry.getValue());
            list.add(c);
        }
        tag.put("Beacons", list);
        return tag;
    }
}