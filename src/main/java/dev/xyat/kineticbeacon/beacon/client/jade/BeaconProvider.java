package dev.xyat.kineticbeacon.beacon.client.jade;

import dev.xyat.kineticbeacon.util.ColorText;
import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;
import dev.xyat.kineticbeacon.beacon.mixin.LevelAccess;
import dev.xyat.kineticbeacon.beacon.util.BeaconStateManager;
import dev.xyat.kineticbeacon.beacon.util.IKineticBeaconAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class BeaconProvider {
    public static final ResourceLocation ID = new ResourceLocation("kineticbeacon", "beacon_info");

    public enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof BeaconBlockEntity beacon && beacon instanceof IKineticBeaconAccessor ktAccessor) {
                int levels = ((LevelAccess) beacon).kineticbeacon$getLevels();
                int maxRad = BeaconConfig.getBeaconRadius(levels);
                int maxPreventRad = maxRad >= 0 ? maxRad + 1 : -1;

                data.putInt("KineticBeaconLevel", levels);
                data.putInt("KineticBeaconMaxRadius", maxRad);
                data.putInt("KineticBeaconMaxPreventRadius", maxPreventRad);
                data.putInt("KineticBeaconActualLoad", ktAccessor.kineticbeacon$getActualChunkLoadRadius(levels, maxRad));
                data.putInt("KineticBeaconActualPrevent", ktAccessor.kineticbeacon$getActualSpawnPreventRadius(levels, maxPreventRad));
                data.putInt("KineticBeaconJadeSpawnType", ktAccessor.kineticbeacon$getSpawnPreventType());
                data.putString("KineticBeaconJadeSpawnCodes", ktAccessor.kineticbeacon$getSpawnPreventCodes());

                if (accessor.getLevel().getServer() != null) {
                    BeaconStateManager state = BeaconStateManager.get(accessor.getLevel().getServer());
                    boolean perPlayer = BeaconConfig.perPlayerLimitEnabled;
                    data.putBoolean("KineticBeaconPerPlayer", perPlayer);
                    data.putInt("KineticBeaconGlobalMax", BeaconConfig.globalChunkLoadLimit);

                    if (perPlayer) {
                        data.putInt("KineticBeaconPersonalUsed", state.getUsedQuota(ktAccessor.kineticbeacon$getOwner()));
                        data.putInt("KineticBeaconPersonalMax", BeaconConfig.perPlayerChunkLoadLimit);
                    } else {
                        data.putInt("KineticBeaconGlobalUsed", state.getUsedQuota(null));
                    }
                }
            }
        }
        @Override public ResourceLocation getUid() { return ID; }
    }

    public enum Client implements IBlockComponentProvider {
        INSTANCE;
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("KineticBeaconLevel")) return;

            int level = data.getInt("KineticBeaconLevel");
            if (level <= 0) return;

            int maxRad = data.getInt("KineticBeaconMaxRadius");
            int maxPreventRad = data.getInt("KineticBeaconMaxPreventRadius");
            int actLoad = data.getInt("KineticBeaconActualLoad");
            int actPrev = data.getInt("KineticBeaconActualPrevent");
            int spType = data.getInt("KineticBeaconJadeSpawnType");
            String codes = data.getString("KineticBeaconJadeSpawnCodes");

            Component loadText = actLoad >= 0
                    ? ColorText.translatable("jade.kineticbeacon.beacon.cl.on", actLoad * 2 + 1, actLoad * 2 + 1, maxRad * 2 + 1, maxRad * 2 + 1)
                    : ColorText.translatable("jade.kineticbeacon.beacon.cl.off");
            tooltip.add(loadText);

            if (actLoad >= 0 && data.contains("KineticBeaconGlobalMax")) {
                boolean perPlayer = data.getBoolean("KineticBeaconPerPlayer");
                int globalMax = data.getInt("KineticBeaconGlobalMax");

                if (perPlayer) {
                    int pUsed = data.getInt("KineticBeaconPersonalUsed");
                    int pMax = data.getInt("KineticBeaconPersonalMax");
                    int remain = Math.max(0, pMax - pUsed);
                    tooltip.add(ColorText.translatable("tip.kineticbeacon.beacon.quota_both", pUsed, remain, globalMax));
                } else {
                    int gUsed = data.getInt("KineticBeaconGlobalUsed");
                    int remain = Math.max(0, globalMax - gUsed);
                    Component typeTx = ColorText.translatable("msg.kineticbeacon.beacon.quota_global");
                    tooltip.add(ColorText.translatable("tip.kineticbeacon.beacon.quota_single", typeTx.getString(), gUsed, remain));
                }
            }

            Component spTypeTx = ColorText.translatable("gui.kineticbeacon.beacon.type." + spType);
            Component spCodeTx = codes.isEmpty() ? ColorText.translatable("gui.kineticbeacon.beacon.type.global") : Component.literal(codes.toUpperCase());

            Component prevText = actPrev >= 0
                    ? ColorText.translatable("jade.kineticbeacon.beacon.sp.on", actPrev * 2 + 1, actPrev * 2 + 1, maxPreventRad * 2 + 1, maxPreventRad * 2 + 1, spTypeTx, spCodeTx)
                    : ColorText.translatable("jade.kineticbeacon.beacon.sp.off");
            tooltip.add(prevText);
        }

        @Override public ResourceLocation getUid() { return ID; }
    }
}