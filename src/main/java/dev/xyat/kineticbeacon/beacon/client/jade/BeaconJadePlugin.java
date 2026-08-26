package dev.xyat.kineticbeacon.beacon.client.jade;

import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class BeaconJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(BeaconProvider.Server.INSTANCE, BeaconBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BeaconProvider.Client.INSTANCE, BeaconBlock.class);
    }
}