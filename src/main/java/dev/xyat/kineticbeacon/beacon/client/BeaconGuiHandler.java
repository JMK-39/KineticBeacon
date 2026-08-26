package dev.xyat.kineticbeacon.beacon.client;

import dev.xyat.kineticbeacon.util.ColorText;
import dev.xyat.kineticbeacon.KineticBeacon;
import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;
import dev.xyat.kineticbeacon.beacon.mixin.LevelAccess;
import dev.xyat.kineticbeacon.beacon.network.BeaconNetwork;
import dev.xyat.kineticbeacon.beacon.util.IKineticBeaconAccessor;
import dev.xyat.kineticcore.api.client.GuiToastUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = KineticBeacon.MODID, value = Dist.CLIENT)
public class BeaconGuiHandler {

    private static class ValidatingEditBox extends EditBox {
        private long errorTime = -1;
        public ValidatingEditBox(Font font, int x, int y, int w, int h) { super(font, x, y, w, h, Component.empty()); }

        public void showError() {
            this.errorTime = net.minecraft.Util.getMillis();
            this.setTextColor(0xFF5555);
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics g, int mx, int my, float pt) {
            if (errorTime > 0) {
                long elapsed = net.minecraft.Util.getMillis() - errorTime;
                if (elapsed > 1000) {
                    errorTime = -1;
                    this.setTextColor(0xE0E0E0);
                    this.setHighlightPos(this.getCursorPosition());
                } else if (elapsed > 200) {
                    int cycle = (int) ((elapsed - 200) / 200);
                    if (cycle == 0 || cycle == 2) {
                        this.setHighlightPos(0);
                        this.setCursorPosition(this.getValue().length());
                    } else {
                        this.setHighlightPos(this.getCursorPosition());
                    }
                }
            }
            super.renderWidget(g, mx, my, pt);
        }
    }

    @SubscribeEvent
    public static void onInitGui(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof BeaconScreen screen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !(mc.hitResult instanceof BlockHitResult blockHit)) return;

            BlockPos pos = blockHit.getBlockPos();
            if (!(mc.level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon) || !(beacon instanceof IKineticBeaconAccessor accessor)) return;

            int guiLeft = (screen.width - 214) / 2;
            int guiTop = (screen.height - 108) / 2;
            int startX = guiLeft - 90;

            int maxRad = BeaconConfig.getBeaconRadius(((LevelAccess) beacon).kineticbeacon$getLevels());
            int maxPreventRad = maxRad >= 0 ? maxRad + 1 : -1;

            boolean[] states = { accessor.kineticbeacon$isChunkLoadEnabled(), accessor.kineticbeacon$isSpawnPreventEnabled() };
            int[] typeState = { accessor.kineticbeacon$getSpawnPreventType() };
            if (typeState[0] > 2) typeState[0] = 0; // 兼容旧配置清理

            Button clBtn = Button.builder(getToggleText(0, states[0]), b -> {
                        states[0] = !states[0];
                        b.setMessage(getToggleText(0, states[0]));
                    }).bounds(startX, guiTop - 55, 80, 20)
                    .tooltip(Tooltip.create(ColorText.translatable("gui.kineticbeacon.beacon.tt.cl_btn")))
                    .build();

            ValidatingEditBox clBox = new ValidatingEditBox(mc.font, startX, guiTop - 33, 80, 14);
            clBox.setValue(accessor.kineticbeacon$getChunkLoadRadius() == -1 ? "" : String.valueOf(accessor.kineticbeacon$getChunkLoadRadius()));
            clBox.setFilter(s -> s.isEmpty() || s.matches("-?\\d+"));
            clBox.setTooltip(Tooltip.create(ColorText.translatable("gui.kineticbeacon.beacon.tt.cl_rad", maxRad)));

            Button spBtn = Button.builder(getToggleText(1, states[1]), b -> {
                        states[1] = !states[1];
                        b.setMessage(getToggleText(1, states[1]));
                    }).bounds(startX, guiTop - 17, 80, 20)
                    .tooltip(Tooltip.create(ColorText.translatable("gui.kineticbeacon.beacon.tt.sp_btn")))
                    .build();

            ValidatingEditBox spBox = new ValidatingEditBox(mc.font, startX, guiTop + 5, 80, 14);
            spBox.setValue(accessor.kineticbeacon$getSpawnPreventRadius() == -1 ? "" : String.valueOf(accessor.kineticbeacon$getSpawnPreventRadius()));
            spBox.setFilter(s -> s.isEmpty() || s.matches("-?\\d+"));
            spBox.setTooltip(Tooltip.create(ColorText.translatable("gui.kineticbeacon.beacon.tt.sp_rad", maxPreventRad)));

            Button typeBtn = Button.builder(ColorText.translatable("gui.kineticbeacon.beacon.btn_type", getTypeText(typeState[0])), b -> {
                        typeState[0] = (typeState[0] + 1) % 3;
                        b.setMessage(ColorText.translatable("gui.kineticbeacon.beacon.btn_type", getTypeText(typeState[0])));
                    }).bounds(startX, guiTop + 21, 80, 20)
                    .tooltip(Tooltip.create(ColorText.translatable("gui.kineticbeacon.beacon.tt.sp_target")))
                    .build();

            EditBox codeBox = new EditBox(mc.font, startX, guiTop + 43, 80, 14, Component.empty());
            codeBox.setMaxLength(32);
            codeBox.setValue(accessor.kineticbeacon$getSpawnPreventCodes());
            codeBox.setFilter(s -> s.isEmpty() || s.matches("[a-hA-H]*"));
            codeBox.setResponder(s -> {
                if (!s.equals(s.toUpperCase())) {
                    codeBox.setValue(s.toUpperCase());
                }
            });
            codeBox.setTooltip(Tooltip.create(ColorText.translatable("gui.kineticbeacon.beacon.tt.sp_code")));

            Button applyBtn = Button.builder(ColorText.translatable("gui.kineticbeacon.beacon.apply"), b -> {
                        int cr = -1, sr = -1;
                        boolean hasError = false;

                        if (!clBox.getValue().isEmpty() && !clBox.getValue().equals("-1")) {
                            try { cr = Integer.parseInt(clBox.getValue()); } catch(Exception ignored){}
                            if (cr < -1 || cr > maxRad) {
                                clBox.showError();
                                hasError = true;
                                if (mc.player != null) mc.player.displayClientMessage(ColorText.translatable("msg.kineticbeacon.beacon.error.cl", maxRad), false);
                            }
                        }

                        if (!spBox.getValue().isEmpty() && !spBox.getValue().equals("-1")) {
                            try { sr = Integer.parseInt(spBox.getValue()); } catch(Exception ignored){}
                            if (sr < -1 || sr > maxPreventRad) {
                                spBox.showError();
                                hasError = true;
                                if (mc.player != null) mc.player.displayClientMessage(ColorText.translatable("msg.kineticbeacon.beacon.error.sp", maxPreventRad), false);
                            }
                        }

                        if (hasError) return;

                        String cd = codeBox.getValue().toUpperCase();

                        BeaconNetwork.CHANNEL.sendToServer(new BeaconNetwork.BeaconConfigPacket(states[0], cr, states[1], sr, typeState[0], cd));
                        GuiToastUtil.showToast("kineticbeacon_cfg", ColorText.translatable("msg.kineticbeacon.beacon.toast.saved"));
                    }).bounds(startX, guiTop + 59, 80, 20)
                    .tooltip(Tooltip.create(ColorText.translatable("gui.kineticbeacon.beacon.tt.apply")))
                    .build();

            event.addListener(clBtn);
            event.addListener(clBox);
            event.addListener(spBtn);
            event.addListener(spBox);
            event.addListener(typeBtn);
            event.addListener(codeBox);
            event.addListener(applyBtn);
        }
    }

    private static Component getToggleText(int type, boolean state) {
        if (type == 0) {
            return ColorText.translatable(state ? "gui.kineticbeacon.beacon.chunk_load.on" : "gui.kineticbeacon.beacon.chunk_load.off");
        } else {
            return ColorText.translatable(state ? "gui.kineticbeacon.beacon.spawn_prevent.on" : "gui.kineticbeacon.beacon.spawn_prevent.off");
        }
    }

    private static Component getTypeText(int type) {
        return ColorText.translatable("gui.kineticbeacon.beacon.type." + type);
    }
}
