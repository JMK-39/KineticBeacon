package dev.xyat.kineticbeacon.beacon.network;

import dev.xyat.kineticbeacon.KineticBeacon;
import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;
import dev.xyat.kineticbeacon.beacon.event.LevelChangedEvent;
import dev.xyat.kineticbeacon.beacon.mixin.BeaconMenuAccessor;
import dev.xyat.kineticbeacon.beacon.mixin.LevelAccess;
import dev.xyat.kineticbeacon.beacon.util.BeaconStateManager;
import dev.xyat.kineticbeacon.beacon.util.IKineticBeaconAccessor;
import dev.xyat.kineticcore.api.KTNetworkProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

public class BeaconNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final int MAX_SPAWN_CODES_WIRE_LENGTH = 32;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KineticBeacon.MODID, "beacon"),
            () -> PROTOCOL_VERSION,
            KTNetworkProtocol::acceptsAnyVersion,
            KTNetworkProtocol::acceptsAnyVersion
    );

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(BeaconConfigPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BeaconConfigPacket::encode).decoder(BeaconConfigPacket::decode)
                .consumerNetworkThread(BeaconConfigPacket::handle).add();
        CHANNEL.messageBuilder(QuotaSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(QuotaSyncPacket::encode).decoder(QuotaSyncPacket::decode)
                .consumerNetworkThread(QuotaSyncPacket::handle).add();
        CHANNEL.messageBuilder(ServerConfigSyncPacket.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ServerConfigSyncPacket::encode).decoder(ServerConfigSyncPacket::decode)
                .consumerNetworkThread(ServerConfigSyncPacket::handle).add();
    }

    private static NetworkEvent.Context contextForSide(
            Supplier<NetworkEvent.Context> supplier,
            LogicalSide expectedSide
    ) {
        NetworkEvent.Context context = supplier.get();
        NetworkDirection direction = context.getDirection();
        if (direction == null || direction.getReceptionSide() != expectedSide) {
            context.setPacketHandled(true);
            return null;
        }
        return context;
    }

    private static String normalizeSpawnCodes(String raw) {
        if (raw == null || raw.length() > MAX_SPAWN_CODES_WIRE_LENGTH) return null;
        boolean[] seen = new boolean[8];
        StringBuilder normalized = new StringBuilder(8);
        for (int i = 0; i < raw.length(); i++) {
            char code = Character.toUpperCase(raw.charAt(i));
            if (code < 'A' || code > 'H') return null;
            int index = code - 'A';
            if (!seen[index]) {
                seen[index] = true;
                normalized.append(code);
            }
        }
        return normalized.toString();
    }

    private static boolean isRadiusAllowed(int radius, int maximum) {
        return radius == -1 || (maximum >= 0 && radius >= 0 && radius <= maximum);
    }

    public static void syncConfigToPlayer(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ServerConfigSyncPacket(
                BeaconConfig.beaconOfflineTimeout,
                BeaconConfig.offlineDisableDeactivate,
                BeaconConfig.offlineDisableChunkLoad,
                BeaconConfig.offlineDisableSpawnPrevent,
                BeaconConfig.perPlayerLimitEnabled
        ));
    }

    public static void syncAllQuotas(MinecraftServer server) {
        BeaconStateManager state = BeaconStateManager.get(server);
        int globalUsed = state.getUsedQuota(null);
        int globalMax = BeaconConfig.globalChunkLoadLimit;
        int personalMax = BeaconConfig.perPlayerChunkLoadLimit;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int personalUsed = state.getUsedQuota(player.getUUID());
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new QuotaSyncPacket(globalUsed, globalMax, personalUsed, personalMax));
        }
    }

    public record ServerConfigSyncPacket(int offlineTimeout, boolean deact, boolean cl, boolean sp, boolean perPlayer) {
        public static ServerConfigSyncPacket decode(FriendlyByteBuf buf) {
            return new ServerConfigSyncPacket(buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
        }
        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(offlineTimeout);
            buf.writeBoolean(deact);
            buf.writeBoolean(cl);
            buf.writeBoolean(sp);
            buf.writeBoolean(perPlayer);
        }
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.CLIENT);
            if (context == null) return;
            context.enqueueWork(() -> {
                dev.xyat.kineticbeacon.beacon.client.ClientQuotaCache.offlineTimeout = this.offlineTimeout;
                dev.xyat.kineticbeacon.beacon.client.ClientQuotaCache.offlineDeact = this.deact;
                dev.xyat.kineticbeacon.beacon.client.ClientQuotaCache.offlineCL = this.cl;
                dev.xyat.kineticbeacon.beacon.client.ClientQuotaCache.offlineSP = this.sp;
                dev.xyat.kineticbeacon.beacon.client.ClientQuotaCache.perPlayerEnabled = this.perPlayer;
            });
            context.setPacketHandled(true);
        }
    }

    public record QuotaSyncPacket(int globalUsed, int globalMax, int personalUsed, int personalMax) {
        public static QuotaSyncPacket decode(FriendlyByteBuf buf) {
            return new QuotaSyncPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
        }
        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(globalUsed);
            buf.writeInt(globalMax);
            buf.writeInt(personalUsed);
            buf.writeInt(personalMax);
        }
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.CLIENT);
            if (context == null) return;
            context.enqueueWork(() -> {
                dev.xyat.kineticbeacon.beacon.client.ClientQuotaCache.globalUsed = this.globalUsed;
                dev.xyat.kineticbeacon.beacon.client.ClientQuotaCache.globalMax = this.globalMax;
                dev.xyat.kineticbeacon.beacon.client.ClientQuotaCache.personalUsed = this.personalUsed;
                dev.xyat.kineticbeacon.beacon.client.ClientQuotaCache.personalMax = this.personalMax;
            });
            context.setPacketHandled(true);
        }
    }

    public record BeaconConfigPacket(boolean clEnabled, int clRad, boolean spEnabled, int spRad, int spType, String spCodes) {
        public static BeaconConfigPacket decode(FriendlyByteBuf buf) {
            return new BeaconConfigPacket(
                    buf.readBoolean(), buf.readInt(), buf.readBoolean(), buf.readInt(), buf.readInt(),
                    buf.readUtf(MAX_SPAWN_CODES_WIRE_LENGTH)
            );
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(clEnabled);
            buf.writeInt(clRad);
            buf.writeBoolean(spEnabled);
            buf.writeInt(spRad);
            buf.writeInt(spType);
            buf.writeUtf(spCodes == null ? "" : spCodes, MAX_SPAWN_CODES_WIRE_LENGTH);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.SERVER);
            if (context == null) return;
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null
                        || !(player.containerMenu instanceof BeaconMenu menu)
                        || !menu.stillValid(player)) {
                    return;
                }

                ((BeaconMenuAccessor) menu).kineticbeacon$getAccess().execute((level, pos) -> {
                    if (level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon && beacon instanceof IKineticBeaconAccessor accessor) {
                        UUID owner = accessor.kineticbeacon$getOwner();
                        if (owner != null
                                && !owner.equals(player.getUUID())
                                && !player.hasPermissions(2)) {
                            player.sendSystemMessage(Component.translatable("commands.generic.permission"));
                            return;
                        }

                        int currentLevel = ((LevelAccess) beacon).kineticbeacon$getLevels();
                        int maxRadius = BeaconConfig.getBeaconRadius(currentLevel);
                        int maxPreventRadius = maxRadius >= 0 ? maxRadius + 1 : -1;
                        String normalizedCodes = normalizeSpawnCodes(this.spCodes);
                        if (!isRadiusAllowed(this.clRad, maxRadius)
                                || !isRadiusAllowed(this.spRad, maxPreventRadius)
                                || this.spType < 0 || this.spType > 2
                                || normalizedCodes == null) {
                            KineticBeacon.LOGGER.warn("Rejected invalid beacon settings from {}", player.getGameProfile().getName());
                            return;
                        }

                        // Preserve the placement owner so quota/offline accounting cannot
                        // be transferred merely by opening and saving another player's beacon.
                        if (owner == null) {
                            accessor.kineticbeacon$setOwner(player.getUUID());
                        }
                        accessor.kineticbeacon$setChunkLoadEnabled(this.clEnabled);
                        accessor.kineticbeacon$setChunkLoadRadius(this.clRad);
                        accessor.kineticbeacon$setSpawnPreventEnabled(this.spEnabled);
                        accessor.kineticbeacon$setSpawnPreventRadius(this.spRad);
                        accessor.kineticbeacon$setSpawnPreventType(this.spType);
                        accessor.kineticbeacon$setSpawnPreventCodes(normalizedCodes);

                        beacon.setChanged();
                        level.sendBlockUpdated(pos, beacon.getBlockState(), beacon.getBlockState(), 3);

                        MinecraftForge.EVENT_BUS.post(new LevelChangedEvent(level, pos, beacon, currentLevel, currentLevel));

                        if (this.spEnabled && currentLevel > 0) {
                            int max = dev.xyat.kineticbeacon.beacon.config.BeaconConfig.getBeaconRadius(currentLevel);
                            int maxPrevent = max >= 0 ? max + 1 : -1;
                            int act = accessor.kineticbeacon$getActualSpawnPreventRadius(currentLevel, maxPrevent);
                            if (act >= 0) {
                                dev.xyat.kineticbeacon.beacon.event.SpawnPreventionHandler.updateBeacon(level, pos, act, this.spType, normalizedCodes);
                            } else {
                                dev.xyat.kineticbeacon.beacon.event.SpawnPreventionHandler.removeBeacon(level, pos);
                            }
                        } else {
                            dev.xyat.kineticbeacon.beacon.event.SpawnPreventionHandler.removeBeacon(level, pos);
                        }
                    }
                });
            });
            context.setPacketHandled(true);
        }
    }
}
