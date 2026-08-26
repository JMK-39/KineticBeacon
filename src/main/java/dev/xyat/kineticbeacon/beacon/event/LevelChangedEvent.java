package dev.xyat.kineticbeacon.beacon.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraftforge.event.level.BlockEvent;

public class LevelChangedEvent extends BlockEvent {
    private final BeaconBlockEntity beacon;
    private final int oldLevel;
    private final int newLevel;

    public LevelChangedEvent(Level level, BlockPos pos, BeaconBlockEntity beacon, int oldLevel, int newLevel) {
        super(level, pos, level.getBlockState(pos));
        this.beacon = beacon;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public BeaconBlockEntity getBeacon() { return beacon; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }
}