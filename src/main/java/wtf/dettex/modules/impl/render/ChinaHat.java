package wtf.dettex.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.*;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.api.repository.friend.FriendUtils;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.render.WorldRenderEvent;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChinaHat extends Module {
    public static ChinaHat getInstance() {
        return Instance.get(ChinaHat.class);
    }

    MultiSelectSetting showChinaHat = new MultiSelectSetting("Show", "Who to show china hat on")
            .value("Self", "Friends", "All");

    ValueSetting widthSetting = new ValueSetting("Width", "Width of the china hat")
            .setValue(0.6F).range(0.3F, 1.0F);

    ValueSetting heightSetting = new ValueSetting("Height", "Height of the china hat")
            .setValue(0.3F).range(0.1F, 0.5F);

    ValueSetting offsetSetting = new ValueSetting("Offset", "Offset from head")
            .setValue(0.42F).range(0.0F, 1.0F);

    ValueSetting alphaSetting = new ValueSetting("Alpha", "Transparency of the china hat")
            .setValue(0.5F).range(0.1F, 1.0F);

    public ChinaHat() {
        super("ChinaHat", "China Hat", ModuleCategory.RENDER);
        setup(showChinaHat, widthSetting, heightSetting, offsetSetting, alphaSetting);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.world == null || mc.player == null) return;

        List<PlayerEntity> players = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (shouldRenderHat(player)) {
                players.add(player);
            }
        }

        if (players.isEmpty()) return;

        float tickDelta = event.getPartialTicks();
        MatrixStack matrixStack = event.getStack();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(2.0F);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (PlayerEntity player : players) {
            renderChinaHat(matrixStack, player, tickDelta);
        }

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private void renderChinaHat(MatrixStack matrixStack, PlayerEntity player, float tickDelta) {
        boolean isSelf = player == mc.player;
        if (mc.getEntityRenderDispatcher().camera == null) return;
        if (!isSelf && mc.player != null && !mc.player.canSee(player)) return;

        double interpX = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double interpY = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double interpZ = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        matrixStack.push();
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        matrixStack.translate(
                interpX - cameraPos.x,
                interpY - cameraPos.y,
                interpZ - cameraPos.z
        );

        float playerHeight = player.getHeight();
        float width = widthSetting.getValue();
        float height = heightSetting.getValue();
        float offset = offsetSetting.getValue();

        boolean hasHelmet = !player.getInventory().getArmorStack(3).isEmpty();
        float yOffset = playerHeight + (hasHelmet ? offset + 0.08F : offset);

        matrixStack.translate(0, yOffset, 0);
        // Adjust for sneaking pose so the hat stays on the head
        if (player.isInSneakingPose()) {
            matrixStack.translate(0.0F, -0.2F, 0.0F);
        }
        // Rotate with the player's head orientation
        float pitch = player.getPitch(tickDelta);
        matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(player.getHeadYaw()));
        matrixStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(pitch));

        boolean isFriend = FriendUtils.isFriend(player);
        int baseColor = isFriend ? ColorUtil.getFriendColor() : ColorUtil.getClientColor();
        int color = ColorUtil.multAlpha(baseColor, alphaSetting.getValue());

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, 0, height, 0)
            .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);

        int segments = 36;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = MathHelper.sin(angle) * width;
            float z = MathHelper.cos(angle) * width;

            buffer.vertex(matrix, x, 0, z)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        int outlineColor = ColorUtil.multAlpha(baseColor, 1.0F);
        buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (i * 2 * Math.PI / segments);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / segments);
            
            float x1 = MathHelper.sin(angle1) * width;
            float z1 = MathHelper.cos(angle1) * width;
            float x2 = MathHelper.sin(angle2) * width;
            float z2 = MathHelper.cos(angle2) * width;

            buffer.vertex(matrix, x1, 0, z1)
                .color((outlineColor >> 16) & 0xFF, (outlineColor >> 8) & 0xFF, outlineColor & 0xFF, (outlineColor >> 24) & 0xFF);
            buffer.vertex(matrix, x2, 0, z2)
                .color((outlineColor >> 16) & 0xFF, (outlineColor >> 8) & 0xFF, outlineColor & 0xFF, (outlineColor >> 24) & 0xFF);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < 4; i++) {
            float angle = (float) (i * Math.PI / 2);
            float x = MathHelper.sin(angle) * width;
            float z = MathHelper.cos(angle) * width;

            buffer.vertex(matrix, 0, height, 0)
                .color((outlineColor >> 16) & 0xFF, (outlineColor >> 8) & 0xFF, outlineColor & 0xFF, (outlineColor >> 24) & 0xFF);
            buffer.vertex(matrix, x, 0, z)
                .color((outlineColor >> 16) & 0xFF, (outlineColor >> 8) & 0xFF, outlineColor & 0xFF, (outlineColor >> 24) & 0xFF);
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        matrixStack.pop();
    }
    
    private boolean shouldRenderHat(PlayerEntity player) {
        if (player == null || !player.isAlive()) return false;
        
        boolean isSelf = player == mc.player;
        boolean isFriend = FriendUtils.isFriend(player);
        
        if (isSelf && showChinaHat.isSelected("Self")) {
            return true;
        }
        
        if (isFriend && showChinaHat.isSelected("Friends")) {
            return true;
        }
        
        if (!isSelf && showChinaHat.isSelected("All")) {
            return true;
        }
        
        return false;
    }
}

