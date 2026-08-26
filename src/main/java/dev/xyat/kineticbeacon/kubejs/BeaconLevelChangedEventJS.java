package dev.xyat.kineticbeacon.kubejs;

import dev.xyat.kineticbeacon.beacon.event.LevelChangedEvent;
import dev.latvian.mods.kubejs.level.LevelEventJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 信标等级改变事件的 KubeJS 包装类
 * KubeJS Wrapper for Beacon Level Changed Event
 */
public class BeaconLevelChangedEventJS extends LevelEventJS {
    private final LevelChangedEvent event;

    public BeaconLevelChangedEventJS(LevelChangedEvent event) {
        this.event = event;
    }

    @Override
    public Level getLevel() {
        if (event.getLevel() instanceof Level level) {
            return level;
        }
        return null;
    }

    public BlockPos getPos() {
        return event.getPos();
    }

    public BlockState getBlock() {
        return event.getState();
    }

    public BeaconBlockEntity getBeacon() {
        return event.getBeacon();
    }

    public int getOldLevel() {
        return event.getOldLevel();
    }

    public int getNewLevel() {
        return event.getNewLevel();
    }
}