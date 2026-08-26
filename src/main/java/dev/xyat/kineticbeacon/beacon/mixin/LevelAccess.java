package dev.xyat.kineticbeacon.beacon.mixin;

import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconBlockEntity.class)
public interface LevelAccess {
    @Accessor("levels")
    int kineticbeacon$getLevels();

    @Accessor("levels")
    void kineticbeacon$setLevels(int levels);
}