package dev.xyat.kineticbeacon.beacon.event;

import dev.xyat.kineticbeacon.util.ColorText;
import dev.xyat.kineticbeacon.KineticBeacon;
import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;
import dev.xyat.kineticbeacon.beacon.network.BeaconNetwork;
import dev.xyat.kineticbeacon.beacon.util.BeaconStateManager;
import dev.xyat.kineticbeacon.beacon.util.IKineticBeaconAccessor;
import dev.xyat.kineticbeacon.beacon.data.WorldChunkLoaderManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = KineticBeacon.MODID)
public class ChunkLoaderHandler {

    private static List<Long> getChunksInRange(BlockPos pos, int radius) {
        List<Long> list = new ArrayList<>();
        if (radius < 0) return list;
        ChunkPos center = new ChunkPos(pos);
        for(int x = center.x - radius; x <= center.x + radius; x++) {
            for(int z = center.z - radius; z <= center.z + radius; z++) {
                list.add(ChunkPos.asLong(x, z));
            }
        }
        return list;
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            BeaconNetwork.syncConfigToPlayer(player);
            BeaconNetwork.syncAllQuotas(player.getServer());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && event.getPlacedBlock().is(Blocks.BEACON)) {
            if (event.getLevel().getBlockEntity(event.getPos()) instanceof IKineticBeaconAccessor accessor) {
                accessor.kineticbeacon$setOwner(player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onBeaconLevelChanged(LevelChangedEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        int newRadius = -1;
        UUID owner = null;
        if (event.getBeacon() instanceof IKineticBeaconAccessor accessor) {
            newRadius = accessor.kineticbeacon$getActualChunkLoadRadius(event.getNewLevel(), BeaconConfig.getBeaconRadius(event.getNewLevel()));
            owner = accessor.kineticbeacon$getOwner();
        }

        BlockPos pos = event.getPos();
        WorldChunkLoaderManager manager = WorldChunkLoaderManager.get(level);
        int storedRadius = manager.getStoredRadius(level, pos);

        if (newRadius == storedRadius) return;

        BeaconStateManager stateManager = BeaconStateManager.get(level.getServer());
        String dimId = level.dimension().location().toString();

        List<Long> oldChunks = getChunksInRange(pos, storedRadius);
        List<Long> newChunks = getChunksInRange(pos, newRadius);

        if (!stateManager.tryUpdateChunks(owner, dimId, oldChunks, newChunks)) {
            notifyPlayers(level, pos, ColorText.translatable("msg.kineticbeacon.beacon.limit_reached").withStyle(ChatFormatting.RED));
            return;
        }

        BeaconNetwork.syncAllQuotas(level.getServer());

        ChunkPos chunkPos = new ChunkPos(pos);
        if (storedRadius >= 0) {
            manager.updateChunkForcing(level, chunkPos.x, chunkPos.z, storedRadius, false);
            if (newRadius < 0) {
                notifyPlayers(level, pos, ColorText.translatable("msg.kineticbeacon.beacon.deactivated").withStyle(ChatFormatting.YELLOW));
            }
        }

        if (newRadius >= 0) {
            manager.updateChunkForcing(level, chunkPos.x, chunkPos.z, newRadius, true);
            String rangeStr = (2 * newRadius + 1) + "x" + (2 * newRadius + 1);

            int used = stateManager.getUsedQuota(owner);
            int max = BeaconConfig.perPlayerLimitEnabled ? BeaconConfig.perPlayerChunkLoadLimit : BeaconConfig.globalChunkLoadLimit;
            Component typeTx = ColorText.translatable(BeaconConfig.perPlayerLimitEnabled ? "msg.kineticbeacon.beacon.quota_personal" : "msg.kineticbeacon.beacon.quota_global");
            Component quotaInfo = ColorText.translatable("msg.kineticbeacon.beacon.quota_info", typeTx, used, max - used);

            notifyPlayers(level, pos, ColorText.translatable("msg.kineticbeacon.beacon.activated", rangeStr).withStyle(ChatFormatting.AQUA).append(quotaInfo));
        }
        manager.setStoredRadius(level, pos, newRadius);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getState().is(Blocks.BEACON)) {
            BlockPos pos = event.getPos();
            WorldChunkLoaderManager manager = WorldChunkLoaderManager.get(level);
            int storedRadius = manager.getStoredRadius(level, pos);
            if (storedRadius >= 0) {
                UUID owner = null;
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof IKineticBeaconAccessor accessor) {
                    owner = accessor.kineticbeacon$getOwner();
                }

                List<Long> oldChunks = getChunksInRange(pos, storedRadius);
                String dimId = level.dimension().location().toString();
                if (BeaconStateManager.get(level.getServer()).tryUpdateChunks(owner, dimId, oldChunks, Collections.emptyList())) {
                    BeaconNetwork.syncAllQuotas(level.getServer());
                }

                manager.updateChunkForcing(level, new ChunkPos(pos).x, new ChunkPos(pos).z, storedRadius, false);
                manager.setStoredRadius(level, pos, -1);
                notifyPlayers(level, pos, ColorText.translatable("msg.kineticbeacon.beacon.broken").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private static void notifyPlayers(ServerLevel level, BlockPos pos, Component msg) {
        Component fullText = ColorText.translatable("msg.kineticbeacon.beacon.prefix").withStyle(ChatFormatting.GOLD).append(msg);
        level.getPlayers(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4096).forEach(p -> p.sendSystemMessage(fullText));
    }
}
