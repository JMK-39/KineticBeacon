package dev.xyat.kineticbeacon.beacon.mixin;

import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;
import dev.xyat.kineticbeacon.beacon.event.LevelChangedEvent;
import dev.xyat.kineticbeacon.beacon.event.SpawnPreventionHandler;
import dev.xyat.kineticbeacon.beacon.util.BeaconStateManager;
import dev.xyat.kineticbeacon.beacon.util.IKineticBeaconAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

public class BeaconMixins {
    @Mixin(BeaconBlockEntity.class)
    public static abstract class Logic implements IKineticBeaconAccessor {
        @Unique private boolean kineticbeacon$ChunkLoadEnabled = false;
        @Unique private int kineticbeacon$ChunkLoadRadius = -1;
        @Unique private boolean kineticbeacon$SpawnPreventEnabled = true;
        @Unique private int kineticbeacon$SpawnPreventRadius = -1;
        @Unique private int kineticbeacon$SpawnPreventType = 0;
        @Unique private String kineticbeacon$SpawnPreventCodes = "";
        @Unique private UUID kineticbeacon$Owner = null;
        @Unique private boolean kineticbeacon$WasOffline = false;

        @Override public boolean kineticbeacon$isChunkLoadEnabled() { return kineticbeacon$ChunkLoadEnabled; }
        @Override public void kineticbeacon$setChunkLoadEnabled(boolean enabled) { this.kineticbeacon$ChunkLoadEnabled = enabled; }
        @Override public int kineticbeacon$getChunkLoadRadius() { return kineticbeacon$ChunkLoadRadius; }
        @Override public void kineticbeacon$setChunkLoadRadius(int radius) { this.kineticbeacon$ChunkLoadRadius = radius; }
        @Override public boolean kineticbeacon$isSpawnPreventEnabled() { return kineticbeacon$SpawnPreventEnabled; }
        @Override public void kineticbeacon$setSpawnPreventEnabled(boolean enabled) {
            this.kineticbeacon$SpawnPreventEnabled = enabled;
            BeaconBlockEntity beacon = (BeaconBlockEntity) (Object) this;
            if (beacon.getLevel() != null && !beacon.getLevel().isClientSide) {
                int levels = ((LevelAccess) beacon).kineticbeacon$getLevels();
                if (enabled && levels > 0) {
                    int max = BeaconConfig.getBeaconRadius(levels);
                    int maxPrevent = max >= 0 ? max + 1 : -1;
                    int actual = kineticbeacon$getActualSpawnPreventRadius(levels, maxPrevent);
                    if (actual >= 0) {
                        SpawnPreventionHandler.updateBeacon(beacon.getLevel(), beacon.getBlockPos(), actual, kineticbeacon$SpawnPreventType, kineticbeacon$SpawnPreventCodes);
                    } else {
                        SpawnPreventionHandler.removeBeacon(beacon.getLevel(), beacon.getBlockPos());
                    }
                } else {
                    SpawnPreventionHandler.removeBeacon(beacon.getLevel(), beacon.getBlockPos());
                }
            }
        }
        @Override public int kineticbeacon$getSpawnPreventRadius() { return kineticbeacon$SpawnPreventRadius; }
        @Override public void kineticbeacon$setSpawnPreventRadius(int radius) { this.kineticbeacon$SpawnPreventRadius = radius; }
        @Override public int kineticbeacon$getSpawnPreventType() { return kineticbeacon$SpawnPreventType; }
        @Override public void kineticbeacon$setSpawnPreventType(int type) { this.kineticbeacon$SpawnPreventType = type; }
        @Override public String kineticbeacon$getSpawnPreventCodes() { return kineticbeacon$SpawnPreventCodes; }
        @Override public void kineticbeacon$setSpawnPreventCodes(String codes) { this.kineticbeacon$SpawnPreventCodes = codes; }
        @Override public UUID kineticbeacon$getOwner() { return kineticbeacon$Owner; }
        @Override public void kineticbeacon$setOwner(UUID uuid) { this.kineticbeacon$Owner = uuid; }
        @Override public boolean kineticbeacon$getWasOffline() { return kineticbeacon$WasOffline; }
        @Override public void kineticbeacon$setWasOffline(boolean wasOffline) { this.kineticbeacon$WasOffline = wasOffline; }

        @Override
        public boolean kineticbeacon$checkOffline() {
            if (this.kineticbeacon$Owner == null || BeaconConfig.beaconOfflineTimeout < 0) return false;
            BeaconBlockEntity beacon = (BeaconBlockEntity) (Object) this;
            if (beacon.getLevel() != null && !beacon.getLevel().isClientSide) {
                MinecraftServer server = beacon.getLevel().getServer();
                if (server == null) return false;

                long offlineMins = BeaconStateManager.get(server).getOfflineMinutes(this.kineticbeacon$Owner);
                return offlineMins >= 0 && offlineMins >= BeaconConfig.beaconOfflineTimeout;
            }
            return false;
        }

        @Inject(method = "saveAdditional", at = @At("TAIL"))
        private void saveKTSettings(CompoundTag tag, CallbackInfo ci) {
            tag.putBoolean("KineticBeaconChunkLoad", this.kineticbeacon$ChunkLoadEnabled);
            tag.putInt("KineticBeaconChunkLoadRadius", this.kineticbeacon$ChunkLoadRadius);
            tag.putBoolean("KineticBeaconSpawnPrevent", this.kineticbeacon$SpawnPreventEnabled);
            tag.putInt("KineticBeaconSpawnPreventRadius", this.kineticbeacon$SpawnPreventRadius);
            tag.putInt("KineticBeaconSpawnPreventType", this.kineticbeacon$SpawnPreventType);
            tag.putString("KineticBeaconSpawnPreventCodes", this.kineticbeacon$SpawnPreventCodes);
            tag.putInt("KineticBeaconStoredLevels", ((LevelAccess) this).kineticbeacon$getLevels());
            tag.putBoolean("KineticBeaconWasOffline", this.kineticbeacon$WasOffline);
            if (this.kineticbeacon$Owner != null) tag.putUUID("KineticBeaconOwner", this.kineticbeacon$Owner);
        }

        @Inject(method = "load", at = @At("TAIL"))
        private void loadKTSettings(CompoundTag tag, CallbackInfo ci) {
            if (tag.contains("KineticBeaconChunkLoad")) this.kineticbeacon$ChunkLoadEnabled = tag.getBoolean("KineticBeaconChunkLoad");
            if (tag.contains("KineticBeaconChunkLoadRadius")) this.kineticbeacon$ChunkLoadRadius = tag.getInt("KineticBeaconChunkLoadRadius");
            if (tag.contains("KineticBeaconSpawnPrevent")) this.kineticbeacon$SpawnPreventEnabled = tag.getBoolean("KineticBeaconSpawnPrevent");
            if (tag.contains("KineticBeaconSpawnPreventRadius")) this.kineticbeacon$SpawnPreventRadius = tag.getInt("KineticBeaconSpawnPreventRadius");
            if (tag.contains("KineticBeaconSpawnPreventType")) this.kineticbeacon$SpawnPreventType = tag.getInt("KineticBeaconSpawnPreventType");
            if (tag.contains("KineticBeaconSpawnPreventCodes")) this.kineticbeacon$SpawnPreventCodes = tag.getString("KineticBeaconSpawnPreventCodes");
            if (tag.contains("KineticBeaconStoredLevels")) ((LevelAccess) this).kineticbeacon$setLevels(tag.getInt("KineticBeaconStoredLevels"));
            if (tag.contains("KineticBeaconWasOffline")) this.kineticbeacon$WasOffline = tag.getBoolean("KineticBeaconWasOffline");
            if (tag.contains("KineticBeaconOwner")) this.kineticbeacon$Owner = tag.getUUID("KineticBeaconOwner");
        }

        @Inject(method = "tick", at = @At("HEAD"))
        private static void kineticbeacon$checkStateBeforeTick(Level level, BlockPos pos, BlockState state, BeaconBlockEntity beacon, CallbackInfo ci) {
            if (level.isClientSide) return;
            IKineticBeaconAccessor accessor = (IKineticBeaconAccessor) beacon;
            boolean isOffline = accessor.kineticbeacon$checkOffline();
            boolean wasOffline = accessor.kineticbeacon$getWasOffline();

            if (isOffline != wasOffline) {
                accessor.kineticbeacon$setWasOffline(isOffline);

                level.sendBlockUpdated(pos, state, state, 3);

                int currentLevel = ((LevelAccess) beacon).kineticbeacon$getLevels();

                MinecraftForge.EVENT_BUS.post(new LevelChangedEvent(level, pos, beacon, currentLevel, currentLevel));

                if (accessor.kineticbeacon$isSpawnPreventEnabled() && currentLevel > 0) {
                    int max = BeaconConfig.getBeaconRadius(currentLevel);
                    int maxPrevent = max >= 0 ? max + 1 : -1;
                    int actual = accessor.kineticbeacon$getActualSpawnPreventRadius(currentLevel, maxPrevent);
                    if (actual >= 0) {
                        SpawnPreventionHandler.updateBeacon(level, pos, actual, accessor.kineticbeacon$getSpawnPreventType(), accessor.kineticbeacon$getSpawnPreventCodes());
                    } else {
                        SpawnPreventionHandler.removeBeacon(level, pos);
                    }
                } else {
                    SpawnPreventionHandler.removeBeacon(level, pos);
                }
            }
        }

        @Redirect(
                method = "tick",
                at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;levels:I", opcode = org.objectweb.asm.Opcodes.PUTFIELD)
        )
        private static void kineticbeacon$interceptLevelUpdate(BeaconBlockEntity beacon, int newLevel) {
            int oldLevel = ((LevelAccess) beacon).kineticbeacon$getLevels();
            ((LevelAccess) beacon).kineticbeacon$setLevels(newLevel);

            if (beacon.getLevel() != null && !beacon.getLevel().isClientSide && oldLevel != newLevel) {
                MinecraftForge.EVENT_BUS.post(new LevelChangedEvent(beacon.getLevel(), beacon.getBlockPos(), beacon, oldLevel, newLevel));
                IKineticBeaconAccessor accessor = (IKineticBeaconAccessor) beacon;
                if (accessor.kineticbeacon$isSpawnPreventEnabled() && newLevel > 0) {
                    int max = BeaconConfig.getBeaconRadius(newLevel);
                    int maxPrevent = max >= 0 ? max + 1 : -1;
                    int actual = accessor.kineticbeacon$getActualSpawnPreventRadius(newLevel, maxPrevent);
                    if (actual >= 0) {
                        SpawnPreventionHandler.updateBeacon(beacon.getLevel(), beacon.getBlockPos(), actual, accessor.kineticbeacon$getSpawnPreventType(), accessor.kineticbeacon$getSpawnPreventCodes());
                    } else {
                        SpawnPreventionHandler.removeBeacon(beacon.getLevel(), beacon.getBlockPos());
                    }
                } else {
                    SpawnPreventionHandler.removeBeacon(beacon.getLevel(), beacon.getBlockPos());
                }
            }
        }

        @Inject(method = "updateBase", at = @At("RETURN"), cancellable = true)
        private static void kineticbeacon$interceptUpdateBase(Level level, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
            if (level != null) {
                BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
                if (be instanceof BeaconBlockEntity && be instanceof IKineticBeaconAccessor accessor) {
                    if (BeaconConfig.offlineDisableDeactivate && accessor.kineticbeacon$getWasOffline()) {
                        cir.setReturnValue(0);
                    }
                }
            }
        }
    }
}