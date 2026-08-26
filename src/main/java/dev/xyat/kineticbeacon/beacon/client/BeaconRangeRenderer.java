package dev.xyat.kineticbeacon.beacon.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.xyat.kineticbeacon.beacon.config.BeaconConfig;
import dev.xyat.kineticbeacon.beacon.mixin.LevelAccess;
import dev.xyat.kineticbeacon.beacon.util.IKineticBeaconAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Map;

public class BeaconRangeRenderer extends RenderType {
    private BeaconRangeRenderer(String s, VertexFormat v, VertexFormat.Mode m, int i, boolean b, boolean b2, Runnable r, Runnable r2) {
        super(s, v, m, i, b, b2, r, r2);
    }

    public static final RenderType THICK_LINES = RenderType.create(
            "kt_thick_lines",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false)
    );

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        if (!player.isHolding(Items.BEACON) || !player.isCrouching()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(THICK_LINES);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        int renderDistance = mc.options.renderDistance().get();
        ChunkPos playerChunkPos = new ChunkPos(player.blockPosition());

        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                LevelChunk chunk = mc.level.getChunk(playerChunkPos.x + x, playerChunkPos.z + z);

                Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
                if (blockEntities.isEmpty()) continue;

                for (BlockEntity be : blockEntities.values()) {
                    if (be instanceof BeaconBlockEntity beacon && beacon instanceof IKineticBeaconAccessor accessor) {
                        int levels = ((LevelAccess) beacon).kineticbeacon$getLevels();
                        if (levels <= 0) continue;

                        int maxRad = BeaconConfig.getBeaconRadius(levels);
                        int maxPreventRad = maxRad >= 0 ? maxRad + 1 : -1;
                        ChunkPos chunkCenter = new ChunkPos(beacon.getBlockPos());

                        int loadRadius = accessor.kineticbeacon$getActualChunkLoadRadius(levels, maxRad);
                        if (BeaconConfig.enableBeaconChunkLoading && loadRadius >= 0) {
                            drawChunkCage(poseStack, consumer, chunkCenter, loadRadius, 1.0f, 0.0f, 1.0f);
                        }

                        int preventRadius = accessor.kineticbeacon$getActualSpawnPreventRadius(levels, maxPreventRad);
                        if (BeaconConfig.enableBeaconSpawnPrevention && preventRadius >= 0) {
                            drawChunkCage(poseStack, consumer, chunkCenter, preventRadius, 0.5f, 1.0f, 0.8f);
                        }
                    }
                }
            }
        }

        poseStack.popPose();
    }

    private static void drawChunkCage(PoseStack poseStack, VertexConsumer consumer, ChunkPos center, int radius, float g, float b, float a) {
        int minChunkX = center.x - radius;
        int maxChunkX = center.x + radius;
        int minChunkZ = center.z - radius;
        int maxChunkZ = center.z + radius;

        double minX = minChunkX * 16.0;
        double maxX = (maxChunkX + 1) * 16.0;
        double minZ = minChunkZ * 16.0;
        double maxZ = (maxChunkZ + 1) * 16.0;

        double minY = -64.0;
        double maxY = 320.0;
        double th = 0.04;

        Matrix4f matrix = poseStack.last().pose();

        drawSolidLine(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, th, g, b, a);
        drawSolidLine(consumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, th, g, b, a);
        drawSolidLine(consumer, matrix, minX, minY, maxZ, maxX, minY, maxZ, th, g, b, a);
        drawSolidLine(consumer, matrix, minX, maxY, maxZ, maxX, maxY, maxZ, th, g, b, a);

        drawSolidLine(consumer, matrix, minX, minY, minZ, minX, minY, maxZ, th, g, b, a);
        drawSolidLine(consumer, matrix, minX, maxY, minZ, minX, maxY, maxZ, th, g, b, a);
        drawSolidLine(consumer, matrix, maxX, minY, minZ, maxX, minY, maxZ, th, g, b, a);
        drawSolidLine(consumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, th, g, b, a);

        drawSolidLine(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, th, g, b, a);
        drawSolidLine(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, th, g, b, a);
        drawSolidLine(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, th, g, b, a);
        drawSolidLine(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, th, g, b, a);

        for (double x = minX + 16.0; x < maxX; x += 16.0) {
            drawSolidLine(consumer, matrix, x, minY, minZ, x, maxY, minZ, th, g, b, a);
            drawSolidLine(consumer, matrix, x, minY, maxZ, x, maxY, maxZ, th, g, b, a);
        }
        for (double z = minZ + 16.0; z < maxZ; z += 16.0) {
            drawSolidLine(consumer, matrix, minX, minY, z, minX, maxY, z, th, g, b, a);
            drawSolidLine(consumer, matrix, maxX, minY, z, maxX, maxY, z, th, g, b, a);
        }
    }

    private static void drawSolidLine(VertexConsumer consumer, Matrix4f pose, double x1, double y1, double z1, double x2, double y2, double z2, double thickness, float g, float b, float a) {
        double tx = (x1 == x2) ? thickness : 0;
        double ty = (y1 == y2) ? thickness : 0;
        double tz = (z1 == z2) ? thickness : 0;

        double minX = Math.min(x1, x2) - tx;
        double maxX = Math.max(x1, x2) + tx;
        double minY = Math.min(y1, y2) - ty;
        double maxY = Math.max(y1, y2) + ty;
        double minZ = Math.min(z1, z2) - tz;
        double maxZ = Math.max(z1, z2) + tz;

        addQuad(consumer, pose, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, g, b, a);
        addQuad(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, g, b, a);
        addQuad(consumer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, g, b, a);
        addQuad(consumer, pose, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, g, b, a);
        addQuad(consumer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, g, b, a);
        addQuad(consumer, pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, g, b, a);
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f pose, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, float g, float b, float a) {
        consumer.vertex(pose, (float)x1, (float)y1, (float)z1).color((float) 0.0, g, b, a).endVertex();
        consumer.vertex(pose, (float)x2, (float)y2, (float)z2).color((float) 0.0, g, b, a).endVertex();
        consumer.vertex(pose, (float)x3, (float)y3, (float)z3).color((float) 0.0, g, b, a).endVertex();
        consumer.vertex(pose, (float)x4, (float)y4, (float)z4).color((float) 0.0, g, b, a).endVertex();
    }
}