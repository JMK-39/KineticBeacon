package dev.xyat.kineticbeacon.beacon.event;

import dev.xyat.kineticbeacon.KineticBeacon;
import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;
import dev.xyat.kineticbeacon.beacon.mixin.LevelAccess;
import dev.xyat.kineticbeacon.beacon.util.IKineticBeaconAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = KineticBeacon.MODID)
public class SpawnPreventionHandler {

    public record BeaconProtectData(int radius, int type, String codes) {}

    public static final Map<ResourceKey<Level>, Map<BlockPos, BeaconProtectData>> ACTIVE_SPAWN_PREVENTERS = new ConcurrentHashMap<>();

    public static void updateBeacon(Level level, BlockPos pos, int loadRadius, int type, String codes) {
        if (level == null || level.isClientSide || loadRadius < 0) return;
        ACTIVE_SPAWN_PREVENTERS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(pos, new BeaconProtectData(loadRadius, type, codes == null ? "" : codes.toUpperCase()));
    }

    public static void removeBeacon(Level level, BlockPos pos) {
        if (level == null || level.isClientSide) return;
        Map<BlockPos, BeaconProtectData> map = ACTIVE_SPAWN_PREVENTERS.get(level.dimension());
        if (map != null) map.remove(pos);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof BeaconBlockEntity beacon && be instanceof IKineticBeaconAccessor accessor) {
                    int levels = ((LevelAccess) beacon).kineticbeacon$getLevels();
                    if (accessor.kineticbeacon$isSpawnPreventEnabled() && levels > 0) {
                        int max = BeaconConfig.getBeaconRadius(levels);
                        int maxPrevent = max >= 0 ? max + 1 : -1;
                        int actual = accessor.kineticbeacon$getActualSpawnPreventRadius(levels, maxPrevent);
                        if (actual >= 0) {
                            updateBeacon(level, beacon.getBlockPos(), actual, accessor.kineticbeacon$getSpawnPreventType(), accessor.kineticbeacon$getSpawnPreventCodes());
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof BeaconBlockEntity beacon) {
                    removeBeacon(level, beacon.getBlockPos());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getState().is(Blocks.BEACON) && event.getLevel() instanceof ServerLevel level) {
            removeBeacon(level, event.getPos());
        }
    }

    private static boolean shouldCancelSpawn(ServerLevel level, BlockPos spawnPos, Entity entity, String spawnCode) {
        Map<BlockPos, BeaconProtectData> beacons = ACTIVE_SPAWN_PREVENTERS.get(level.dimension());
        if (beacons == null || beacons.isEmpty()) return false;

        ResourceLocation entityRL = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityRL == null) return false;

        int chunkX = spawnPos.getX() >> 4;
        int chunkZ = spawnPos.getZ() >> 4;

        boolean isProtected = false;
        int protectType = 0;
        String localCodes = "";

        for (Map.Entry<BlockPos, BeaconProtectData> entry : beacons.entrySet()) {
            BlockPos bPos = entry.getKey();
            int preventRadiusChunks = entry.getValue().radius();
            int bChunkX = bPos.getX() >> 4;
            int bChunkZ = bPos.getZ() >> 4;

            if (Math.abs(chunkX - bChunkX) <= preventRadiusChunks && Math.abs(chunkZ - bChunkZ) <= preventRadiusChunks) {
                isProtected = true;
                protectType = entry.getValue().type();
                localCodes = entry.getValue().codes();
                break;
            }
        }

        if (!isProtected) return false;

        boolean isEnemy = entity instanceof net.minecraft.world.entity.monster.Enemy;
        if (protectType == 1 && !isEnemy) return false;
        if (protectType == 2 && isEnemy) return false;

        String entityId = entityRL.toString();

        if (BeaconConfig.BEACON_SPAWN_WHITELIST_CACHE.contains(entityId)) return false;
        if (BeaconConfig.BEACON_SPAWN_BLACKLIST_CACHE.contains(entityId)) return true;

        if (localCodes == null || localCodes.isEmpty()) {
            String rules = BeaconConfig.BEACON_RULES_CACHE.get(entityId);
            if (rules != null) {
                return rules.contains(spawnCode);
            } else {
                return "A".equals(spawnCode);
            }
        } else {
            return localCodes.contains(spawnCode);
        }
    }

    @SubscribeEvent
    public static void onCheckSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!BeaconConfig.enableBeaconSpawnPrevention) return;

        String spawnCode = mapSpawnTypeToCode(event.getSpawnType());

        if (shouldCancelSpawn(level, event.getEntity().blockPosition(), event.getEntity(), spawnCode)) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA().level() instanceof ServerLevel level)) return;
        if (!BeaconConfig.enableBeaconSpawnPrevention) return;
        if (event.getChild() == null) return;

        if (shouldCancelSpawn(level, event.getParentA().blockPosition(), event.getChild(), "H")) {
            event.setCanceled(true);
        }
    }

    private static String mapSpawnTypeToCode(MobSpawnType type) {
        return switch (type) {
            case CONVERSION -> "B";
            case COMMAND -> "C";
            case SPAWN_EGG, BUCKET, DISPENSER -> "D";
            case SPAWNER -> "E";
            case MOB_SUMMONED -> "F";
            case EVENT, REINFORCEMENT -> "G";
            case BREEDING -> "H";
            default -> "A";
        };
    }
}