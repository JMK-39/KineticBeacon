package dev.xyat.kineticbeacon.beacon.client;

import dev.xyat.kineticbeacon.KineticBeacon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KineticBeacon.MODID, value = Dist.CLIENT)
public class BeaconRenderEventHandler {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        BeaconRangeRenderer.render(event);
    }
}
