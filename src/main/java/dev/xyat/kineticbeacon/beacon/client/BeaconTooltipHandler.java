package dev.xyat.kineticbeacon.beacon.client;

import dev.xyat.kineticbeacon.util.ColorText;
import dev.xyat.kineticbeacon.KineticBeacon;
import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Mod.EventBusSubscriber(modid = KineticBeacon.MODID, value = Dist.CLIENT)
public class BeaconTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        List<Component> tooltip = event.getToolTip();
        if (stack.is(Items.BEACON)) {
            tooltip.add(ColorText.translatable("tip.kineticbeacon.beacon.grid_hint"));
            tooltip.add(ColorText.translatable("tip.kineticbeacon.beacon.color_hint"));

            int globalMax = ClientQuotaCache.globalMax;
            int personalMax = ClientQuotaCache.personalMax;
            int personalUsed = ClientQuotaCache.personalUsed;
            int globalUsed = ClientQuotaCache.globalUsed;

            if (ClientQuotaCache.perPlayerEnabled) {
                int remain = Math.max(0, personalMax - personalUsed);
                tooltip.add(ColorText.translatable("tip.kineticbeacon.beacon.quota_both", personalUsed, remain, globalMax));
            } else {
                int remain = Math.max(0, globalMax - globalUsed);
                tooltip.add(ColorText.translatable("tip.kineticbeacon.beacon.quota_single", ColorText.translatable("msg.kineticbeacon.beacon.quota_global").getString(), globalUsed, remain));
            }

            if (ClientQuotaCache.offlineTimeout >= 0) {
                String acts = getString();

                tooltip.add(ColorText.translatable("tip.kineticbeacon.beacon.offline_warn", ClientQuotaCache.offlineTimeout, acts.trim()));
            }

            if (Screen.hasShiftDown()) {
                tooltip.add(Component.empty());
                for (int level = 1; level <= 4; level++) {
                    int radius = BeaconConfig.getBeaconRadius(level);
                    if (radius >= 0) {
                        int loadSize = 2 * radius + 1;
                        int preventSize = 2 * (radius + 1) + 1;

                        if (BeaconConfig.enableBeaconSpawnPrevention) {
                            tooltip.add(ColorText.translatable("tip.kineticbeacon.beacon.level_dual",
                                    level, loadSize, loadSize, preventSize, preventSize));
                        } else {
                            tooltip.add(ColorText.translatable("tip.kineticbeacon.beacon.level_single",
                                    level, loadSize, loadSize));
                        }
                    }
                }
            } else {
                tooltip.add(ColorText.translatable("tip.kineticbeacon.beacon.hold_sneak_hint"));
            }
        }
    }

    private static @NotNull String getString() {
        String acts = "";
        if (ClientQuotaCache.offlineDeact) acts += ColorText.translatable("tip.kineticbeacon.beacon.act_deact").getString() + " ";
        if (ClientQuotaCache.offlineCL) acts += ColorText.translatable("tip.kineticbeacon.beacon.act_cl").getString() + " ";
        if (ClientQuotaCache.offlineSP) acts += ColorText.translatable("tip.kineticbeacon.beacon.act_sp").getString() + " ";
        if (acts.isEmpty()) acts = ColorText.translatable("tip.kineticbeacon.beacon.act_none").getString();
        return acts;
    }
}
