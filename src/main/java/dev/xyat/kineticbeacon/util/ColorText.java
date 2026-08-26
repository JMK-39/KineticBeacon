package dev.xyat.kineticbeacon.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Map;

public final class ColorText {
    private static final Map<String, ChatFormatting[][]> ARG_STYLES = new HashMap<>();

    static {
        ARG_STYLES.put("gui.kineticbeacon.beacon.btn_type", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}});
        ARG_STYLES.put("gui.kineticbeacon.beacon.cl_rad.desc", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("gui.kineticbeacon.beacon.sp_rad.desc", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("gui.kineticbeacon.beacon.tt.cl_rad", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("gui.kineticbeacon.beacon.tt.sp_rad", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW}});
        ARG_STYLES.put("jade.kineticbeacon.beacon.chunk", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("jade.kineticbeacon.beacon.cl.on", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GREEN}, new ChatFormatting[]{ChatFormatting.GREEN}, new ChatFormatting[]{ChatFormatting.GREEN}, new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("jade.kineticbeacon.beacon.quota", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW}, new ChatFormatting[]{ChatFormatting.RED}, new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("jade.kineticbeacon.beacon.sp.on", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}});
        ARG_STYLES.put("jade.kineticbeacon.beacon.spawn", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.GOLD}});
        ARG_STYLES.put("msg.kineticbeacon.beacon.activated", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}});
        ARG_STYLES.put("msg.kineticbeacon.beacon.error.cl", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}});
        ARG_STYLES.put("msg.kineticbeacon.beacon.error.sp", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}});
        ARG_STYLES.put("msg.kineticbeacon.beacon.quota_info", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}, new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD}});
        ARG_STYLES.put("tip.kineticbeacon.beacon.level_dual", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.GREEN}, new ChatFormatting[]{ChatFormatting.GREEN}});
        ARG_STYLES.put("tip.kineticbeacon.beacon.level_single", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.DARK_PURPLE}, new ChatFormatting[]{ChatFormatting.AQUA}, new ChatFormatting[]{ChatFormatting.AQUA}});
        ARG_STYLES.put("tip.kineticbeacon.beacon.offline_warn", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}, new ChatFormatting[]{ChatFormatting.DARK_RED}});
        ARG_STYLES.put("tip.kineticbeacon.beacon.quota", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}, new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD}});
        ARG_STYLES.put("tip.kineticbeacon.beacon.quota_both", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}, new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD}, new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}});
        ARG_STYLES.put("tip.kineticbeacon.beacon.quota_single", new ChatFormatting[][]{new ChatFormatting[]{ChatFormatting.GOLD}, new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}, new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD}});
    }

    private ColorText() {
    }

    public static MutableComponent translatable(String key, Object... args) {
        ChatFormatting[][] styles = ARG_STYLES.get(key);
        if (styles == null || args.length == 0) {
            return Component.translatable(key, args);
        }
        Object[] styledArgs = args.clone();
        int count = Math.min(styles.length, styledArgs.length);
        for (int i = 0; i < count; i++) {
            ChatFormatting[] formats = styles[i];
            if (formats == null || formats.length == 0) continue;
            Object value = styledArgs[i];
            boolean preserveColor = value instanceof Component existing && existing.getStyle().getColor() != null;
            MutableComponent component = value instanceof Component existing
                    ? existing.copy()
                    : Component.literal(String.valueOf(value));
            if (preserveColor) {
                for (int j = 1; j < formats.length; j++) {
                    component.withStyle(formats[j]);
                }
            } else {
                component.withStyle(formats);
            }
            styledArgs[i] = component;
        }
        return Component.translatable(key, styledArgs);
    }
}
