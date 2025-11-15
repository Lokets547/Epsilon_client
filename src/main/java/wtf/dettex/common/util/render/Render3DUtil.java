package wtf.dettex.common.util.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.math.ProjectionUtil;
import wtf.dettex.event.impl.render.WorldRenderEvent;
import wtf.dettex.modules.impl.render.Hud;

import java.lang.Math;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class Render3DUtil implements QuickImports {
    private final Map<VoxelShape, Pair<List<Box>, List<Line>>> SHAPE_OUTLINES = new HashMap<>();
    private final Map<VoxelShape, List<Box>> SHAPE_BOXES = new HashMap<>();
    public final List<Texture> TEXTURE_DEPTH = new ArrayList<>();
    public final List<Texture> TEXTURE = new ArrayList<>();
    public final List<Line> LINE_DEPTH = new ArrayList<>();
    public final List<Line> LINE = new ArrayList<>();
    public final List<Quad> QUAD_DEPTH = new ArrayList<>();
    public final List<Quad> QUAD = new ArrayList<>();
    @Setter
    public Matrix4f lastProjMat = new Matrix4f();
    @Setter
    public MatrixStack.Entry lastWorldSpaceMatrix = new MatrixStack().peek();

    private final Identifier captureId = Identifier.of("textures/capture.png"), bloom = Identifier.of("textures/bloom.png");
    private final Identifier target1 = Identifier.of("textures/target1.png"), target2 = Identifier.of("textures/target2.png");
    private final Identifier glowTex = Identifier.of("textures/glow.png");

    public void onWorldRender(WorldRenderEvent e) {
        if (!TEXTURE.isEmpty()) {
            Set<Identifier> identifiers = TEXTURE.stream().map(texture -> texture.id).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            identifiers.forEach(id -> {
                RenderSystem.setShaderTexture(0, id);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                TEXTURE.stream().filter(texture -> texture.id.equals(id)).forEach(tex -> quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.disableBlend();
            TEXTURE.clear();
        }
        if (!TEXTURE_DEPTH.isEmpty()) {
            Set<Identifier> identifiers = TEXTURE_DEPTH.stream().map(texture -> texture.id).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            identifiers.forEach(id -> {
                RenderSystem.setShaderTexture(0, id);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                TEXTURE_DEPTH.stream().filter(texture -> texture.id.equals(id)).forEach(tex -> quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            TEXTURE_DEPTH.clear();
        }
        if (!LINE.isEmpty()) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            Set<Float> widths = LINE.stream().map(line -> line.width).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            widths.forEach(width -> {
                RenderSystem.lineWidth(width);
                BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
                LINE.stream().filter(line -> line.width == width).forEach(line -> vertexLine(line.entry, buffer, line.start.toVector3f(), line.end.toVector3f(), line.colorStart, line.colorEnd));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            LINE.clear();
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        }
        if (!QUAD.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            QUAD.forEach(quad -> vertexQuad(quad.entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            QUAD.clear();
        }
        if (!LINE_DEPTH.isEmpty()) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            Set<Float> widths = LINE_DEPTH.stream().map(line -> line.width).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            widths.forEach(width -> {
                RenderSystem.lineWidth(width);
                BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
                LINE_DEPTH.stream().filter(line -> line.width == width).forEach(line -> vertexLine(line.entry, buffer, line.start.toVector3f(), line.end.toVector3f(), line.colorStart, line.colorEnd));
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            });
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            LINE_DEPTH.clear();
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        }
        if (!QUAD_DEPTH.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            QUAD_DEPTH.forEach(quad -> vertexQuad(quad.entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            QUAD_DEPTH.clear();
        }
    }

    public void drawCrystal(MatrixStack stack, LivingEntity target, float anim, float hurtTint, float speed) {
        if (target == null || anim <= 0.0F) return;

        Vec3d interpolated = MathUtil.interpolate(target);
        double cx = interpolated.x;
        double cy = interpolated.y + target.getHeight() * 0.55;
        double cz = interpolated.z;

        double orbitR1 = 0.9;
        double orbitR2 = 1.1;
        double speedMultiplier = MathHelper.clamp(speed, 0.2F, 3.0F);

        RenderSystem.disableDepthTest();

        renderCrystalRing(stack, target, cx, cy, cz, orbitR1, 1.6 * speedMultiplier, 0.0, anim, hurtTint);
        renderCrystalRing(stack, target, cx, cy, cz, orbitR2, 2.15 * speedMultiplier, 0.45, anim, hurtTint);

        RenderSystem.enableDepthTest();
    }

    private void renderCrystalRing(MatrixStack stack, LivingEntity entity, double cx, double cy, double cz,
                                   double orbitR, double angularSpeed, double yOffset, float anim, float hurtTint) {
        boolean poisoned = entity.hasStatusEffect(StatusEffects.POISON);
        int baseColor = poisoned ? ColorUtil.getColor(0, 255, 0) : ColorUtil.getColor(255, 0, 0);
        float hurtProgress = entity.hurtTime > 0 ? MathHelper.sin(entity.hurtTime * (18F * ((float)Math.PI / 180F))) : 0.0F;
        int themeColor = ColorUtil.overCol(Hud.getInstance().colorSetting.getColor(), baseColor, MathHelper.clamp(hurtProgress, 0.0F, 1.0F));
        int shadedColor = ColorUtil.multAlpha(ColorUtil.multBright(themeColor, 1.25F), (200f / 255f));

        long curMs = System.currentTimeMillis();
        double t = curMs / 1000.0;
        double pulse = 1.0 + 0.08 * Math.sin(t * 3.0);

        float scale = MathHelper.clamp(anim, 0.0F, 1.0F);
        Vec3d top = new Vec3d(0, 0.14 * scale, 0);
        Vec3d bottom = new Vec3d(0, -0.14 * scale, 0);
        Vec3d px = new Vec3d(0.14 * scale, 0, 0);
        Vec3d nx = new Vec3d(-0.14 * scale, 0, 0);
        Vec3d pz = new Vec3d(0, 0, 0.14 * scale);
        Vec3d nz = new Vec3d(0, 0, -0.14 * scale);

        Vec3d[][] faces = new Vec3d[][]{
                {top, px, pz}, {top, pz, nx}, {top, nx, nz}, {top, nz, px},
                {bottom, pz, px}, {bottom, nx, pz}, {bottom, nz, nx}, {bottom, px, nz}
        };

        float shellScale = 1.08f;

        for (int i = 0; i < 6; i++) {
            double ang = t * angularSpeed + i * (2 * Math.PI / 6.0);

            double ox = cx + orbitR * pulse * Math.cos(ang);
            double oy = cy + yOffset + 0.12 * Math.sin(t * 2.5 + i * 0.8);
            double oz = cz + orbitR * pulse * Math.sin(ang);

            Vec3d crystalPos = new Vec3d(ox, oy, oz);
            Vec3d entityCenter = new Vec3d(cx, cy, cz);

            for (Vec3d[] tri : faces) {
                Vec3d l0 = tri[0].multiply(shellScale);
                Vec3d l1 = tri[1].multiply(shellScale);
                Vec3d l2 = tri[2].multiply(shellScale);

                Vec3d v0 = orientCrystal(crystalPos, entityCenter, l0);
                Vec3d v1 = orientCrystal(crystalPos, entityCenter, l1);
                Vec3d v2 = orientCrystal(crystalPos, entityCenter, l2);

                Vec3d n = v1.subtract(v0).crossProduct(v2.subtract(v0)).normalize();
                Vec3d lightDir = new Vec3d(0.6, 1.0, 0.4).normalize();
                double ndl = Math.max(0.1, n.dotProduct(lightDir));
                int faceColor = ColorUtil.multAlpha(shadeColor(shadedColor, (float) ndl), 0.3f * scale);

                Vector2f p0 = project2D(v0.x, v0.y, v0.z);
                Vector2f p1 = project2D(v1.x, v1.y, v1.z);
                Vector2f p2 = project2D(v2.x, v2.y, v2.z);
                if (p0 == null || p1 == null || p2 == null) continue;

                fillTriangle(stack, p0, p1, p2, faceColor);
            }
        }

        for (int i = 0; i < 6; i++) {
            double ang = t * angularSpeed + i * (2 * Math.PI / 6.0);
            double ox = cx + orbitR * pulse * Math.cos(ang);
            double oy = cy + yOffset + 0.12 * Math.sin(t * 2.5 + i * 0.8);
            double oz = cz + orbitR * pulse * Math.sin(ang);

            Vec3d crystalPos = new Vec3d(ox, oy, oz);
            Vec3d entityCenter = new Vec3d(cx, cy, cz);

            Vector2f screenCenter = project2D(ox, oy, oz);
            if (screenCenter != null) {
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(new Vec3d(ox, oy, oz));
                float scaleFactor = MathHelper.clamp(5.0f / Math.max(1.0f, distance), 0.4f, 2.5f);
                float glowSize = 24.0f * scaleFactor * scale;

                int finalColor = ColorUtil.overCol(themeColor, baseColor, hurtTint);
                int fadedColor = ColorUtil.multAlpha(finalColor, 0.07f * scale);
                int mainColor = ColorUtil.multAlpha(finalColor, scale);

                drawBillboardTexture(stack, screenCenter, glowSize, fadedColor, mainColor);
            }

            for (Vec3d[] tri : faces) {
                Vec3d v0 = orientCrystal(crystalPos, entityCenter, tri[0]);
                Vec3d v1 = orientCrystal(crystalPos, entityCenter, tri[1]);
                Vec3d v2 = orientCrystal(crystalPos, entityCenter, tri[2]);

                Vec3d n = v1.subtract(v0).crossProduct(v2.subtract(v0)).normalize();
                Vec3d lightDir = new Vec3d(0.6, 1.0, 0.4).normalize();
                double ndl = Math.max(0.1, n.dotProduct(lightDir));
                int faceColor = ColorUtil.multAlpha(shadeColor(shadedColor, (float) ndl), scale);

                Vector2f p0 = project2D(v0.x, v0.y, v0.z);
                Vector2f p1 = project2D(v1.x, v1.y, v1.z);
                Vector2f p2 = project2D(v2.x, v2.y, v2.z);
                if (p0 == null || p1 == null || p2 == null) continue;

                fillTriangle(stack, p0, p1, p2, faceColor);
            }
        }
    }

    private Vector2f project2D(double x, double y, double z) {
        Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y, z));
        if (screen == null || screen.z <= 0 || screen.z >= 1) {
            return null;
        }
        return new Vector2f((float) screen.x, (float) screen.y);
    }

    private void drawBillboardTexture(MatrixStack stack, Vector2f center, float size, int fadedColor, int mainColor) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderTexture(0, glowTex);
        fillRect(stack, center.x - size / 2, center.y - size / 2, size, size, fadedColor, true, false);
        fillRect(stack, center.x - size / 2, center.y - size / 2, size, size, mainColor, true, false);
        RenderSystem.disableBlend();
    }

    private void fillRect(MatrixStack stack, float x, float y, float width, float height, int color, boolean blend, boolean depth) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float a = ColorUtil.alphaf(color);
        float r = ColorUtil.redf(color);
        float g = ColorUtil.greenf(color);
        float b = ColorUtil.bluef(color);
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, window.getScaledWidth(), window.getScaledHeight(), 0.0F, -1.0F, 1.0F);
        buffer.vertex(ortho, x, y + height, 0).color(r, g, b, a);
        buffer.vertex(ortho, x + width, y + height, 0).color(r, g, b, a);
        buffer.vertex(ortho, x + width, y, 0).color(r, g, b, a);
        buffer.vertex(ortho, x, y, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void fillTriangle(MatrixStack stack, Vector2f p0, Vector2f p1, Vector2f p2, int color) {
        RenderSystem.disableCull();

        float a = ColorUtil.alphaf(color);
        float r = ColorUtil.redf(color);
        float g = ColorUtil.greenf(color);
        float b = ColorUtil.bluef(color);

        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, window.getScaledWidth(), window.getScaledHeight(), 0.0F, -1.0F, 1.0F);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        buf.vertex(ortho, p0.x, p0.y, 0).color(r, g, b, a);
        buf.vertex(ortho, p1.x, p1.y, 0).color(r, g, b, a);
        buf.vertex(ortho, p2.x, p2.y, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableCull();
    }

    private int shadeColor(int color, float factor) {
        float clamped = MathHelper.clamp(factor, 0.0F, 2.0F);
        int r = MathHelper.clamp((int) (ColorUtil.red(color) * clamped), 0, 255);
        int g = MathHelper.clamp((int) (ColorUtil.green(color) * clamped), 0, 255);
        int b = MathHelper.clamp((int) (ColorUtil.blue(color) * clamped), 0, 255);
        return ColorUtil.getColor(r, g, b, ColorUtil.alpha(color));
    }

    private Vec3d orientCrystal(Vec3d crystalPos, Vec3d entityCenter, Vec3d localPos) {
        Vec3d up = entityCenter.subtract(crystalPos).normalize();
        if (up.lengthSquared() < 1.0E-4) {
            up = new Vec3d(0, 1, 0);
        }
        Vec3d fallback = Math.abs(up.dotProduct(new Vec3d(0, 1, 0))) > 0.99
                ? new Vec3d(1, 0, 0)
                : new Vec3d(0, 1, 0);
        Vec3d right = up.crossProduct(fallback).normalize();
        Vec3d forward = right.crossProduct(up).normalize();

        Vec3d offset = right.multiply(localPos.x).add(up.multiply(localPos.y)).add(forward.multiply(localPos.z));
        return crystalPos.add(offset);
    }
    
    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
        drawShape(blockPos, voxelShape, color, width, true, false);
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        if (SHAPE_BOXES.containsKey(voxelShape)) {
            SHAPE_BOXES.get(voxelShape).forEach(box -> {
                box = box.offset(blockPos);
                if (ProjectionUtil.canSee(box)) drawBox(box, color, width, true, fill, depth);
            });
            return;
        }
        SHAPE_BOXES.put(voxelShape, voxelShape.getBoundingBoxes());
    }

    public void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        Vec3d vec3d = Vec3d.of(blockPos);
        if (ProjectionUtil.canSee(new Box(blockPos))) {
            if (SHAPE_OUTLINES.containsKey(voxelShape)) {
                Pair<List<Box>, List<Line>> pair = SHAPE_OUTLINES.get(voxelShape);
                if (fill) pair.getLeft().forEach(box -> drawBox(box.offset(vec3d), color, width, false, true, depth));
                pair.getRight().forEach(line -> drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth));
                return;
            }
            List<Line> lines = new ArrayList<>();
            voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> lines.add(new Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0)));
            SHAPE_OUTLINES.put(voxelShape, new Pair<>(voxelShape.getBoundingBoxes(), lines));
        }
    }

    public void drawBox(Box box, int color, float width) {
        drawBox(box, color, width, true, true, false);
    }

    public void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        drawBox(null, box, color, width, line, fill, depth) ;
    }

    public void drawBox(MatrixStack.Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        box = box.expand(1e-3);

        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        if (fill) {
            int fillColor = ColorUtil.multAlpha(color, 0.1f);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
        }

        if (line) {
            drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
            drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
            drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
        }
    }

    public void vertexLine(MatrixStack matrices, VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor) {
        vertexLine(matrices.peek(), buffer, start.toVector3f(), end.toVector3f(), startColor, endColor);
    }

    public void vertexLine(MatrixStack.Entry entry, VertexConsumer buffer, Vector3f start, Vector3f end, int startColor, int endColor) {
        if (entry == null) entry = lastWorldSpaceMatrix;
        Vector3f vec = getNormal(start, end);
        buffer.vertex(entry, start).color(startColor).normal(entry, vec);
        buffer.vertex(entry, end).color(endColor).normal(entry, vec);
    }

    public void vertexQuad(MatrixStack.Entry entry, VertexConsumer buffer, Vec3d vec1, Vec3d vec2, Vec3d vec3, Vec3d vec4, int color) {
        vertexQuad(entry, buffer, vec1.toVector3f(), vec2.toVector3f(), vec3.toVector3f(), vec4.toVector3f(), color);
    }

    public void vertexQuad(MatrixStack.Entry entry, VertexConsumer buffer, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, int color) {
        if (entry == null) entry = lastWorldSpaceMatrix;
        buffer.vertex(entry, vec1).color(color);
        buffer.vertex(entry, vec2).color(color);
        buffer.vertex(entry, vec3).color(color);
        buffer.vertex(entry, vec4).color(color);
    }

    public void quadTexture(MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
        buffer.vertex(entry, x, y + height, 0).texture(0, 0).color(color.x);
        buffer.vertex(entry, x + width, y + height, 0).texture(0, 1).color(color.y);
        buffer.vertex(entry, x + width, y, 0).texture(1, 1).color(color.w);
        buffer.vertex(entry, x, y, 0).texture(1, 0).color(color.z);
    }

    public @NotNull Vector3f getNormal(Vector3f start, Vector3f end) {
        Vector3f normal = new Vector3f(start).sub(end);
        float sqrt = MathHelper.sqrt(normal.lengthSquared());
        return normal.div(sqrt);
    }

    public void drawCube(LivingEntity lastTarget, float anim, float red) {
        float size = 2.2F - anim;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = MathUtil.interpolate(lastTarget).subtract(camera.getPos());

        MatrixStack matrix = new MatrixStack();
        matrix.push();
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrix.translate(vec.x, vec.y + lastTarget.getBoundingBox().getLengthY() / 2, vec.z);
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathUtil.interpolate(prevEspValue, espValue)));
        MatrixStack.Entry entry = matrix.peek().copy();
        Render3DUtil.drawTexture(entry, captureId, -size / 2, -size / 2, size, size, ColorUtil.multRedAndAlpha(new Vector4i(ColorUtil.fade(90), ColorUtil.fade(0), ColorUtil.fade(180), ColorUtil.fade(270)), 1 + red * 10, anim), false);
        matrix.pop();
    }

    public void drawCubeModern(LivingEntity lastTarget, float anim, float red) {
        float size = 2.2F - anim;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = MathUtil.interpolate(lastTarget).subtract(camera.getPos());

        MatrixStack matrix = new MatrixStack();
        matrix.push();
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrix.translate(vec.x, vec.y + lastTarget.getBoundingBox().getLengthY() / 2, vec.z);
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathUtil.interpolate(prevEspValue, espValue)));
        MatrixStack.Entry entry = matrix.peek().copy();
        Render3DUtil.drawTexture(entry, target1, -size / 2, -size / 2, size, size, ColorUtil.multRedAndAlpha(new Vector4i(ColorUtil.fade(90), ColorUtil.fade(0), ColorUtil.fade(180), ColorUtil.fade(270)), 1 + red * 10, anim), false);
        matrix.pop();
    }

    public void drawCubeSkid(LivingEntity lastTarget, float anim, float red) {
        float size = 2.2F - anim;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = MathUtil.interpolate(lastTarget).subtract(camera.getPos());

        MatrixStack matrix = new MatrixStack();
        matrix.push();
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrix.translate(vec.x, vec.y + lastTarget.getBoundingBox().getLengthY() / 2, vec.z);
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathUtil.interpolate(prevEspValue, espValue)));
        MatrixStack.Entry entry = matrix.peek().copy();
        Render3DUtil.drawTexture(entry, target2, -size / 2, -size / 2, size, size, ColorUtil.multRedAndAlpha(new Vector4i(ColorUtil.fade(90), ColorUtil.fade(0), ColorUtil.fade(180), ColorUtil.fade(270)), 1 + red * 10, anim), false);
        matrix.pop();
    }

    public void drawCircle(MatrixStack matrix, LivingEntity lastTarget, float anim, float red) {
        double cs = MathUtil.interpolate(circleStep - 0.15F, circleStep);
        Vec3d target = MathUtil.interpolate(lastTarget);
        boolean canSee = Objects.requireNonNull(mc.player).canSee(lastTarget);

        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        if (canSee) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0, size = 90; i <= size; i++) {
            float width = lastTarget.getWidth() * 0.9F;
            float height = lastTarget.getHeight();
            double yAnim = MathUtil.absSinAnimation(cs) * height;
            double yAnim2 = MathUtil.absSinAnimation(cs - 0.45) * height;
            Vec3d cosSin = MathUtil.cosSin(i, size, width);
            Vec3d nextCosSin = MathUtil.cosSin(i + 1, size, width);
            int color = ColorUtil.multRed(ColorUtil.fade(i * 4), 1 + red * 10);

            Render3DUtil.vertexLine(matrix, buffer, target.add(cosSin.x, cosSin.y + yAnim, cosSin.z), target.add(cosSin.x, cosSin.y + yAnim2, cosSin.z),
                    ColorUtil.multAlpha(color, 0.6F * anim), ColorUtil.multAlpha(color, 0));
            Render3DUtil.drawLine(target.add(cosSin.x, cosSin.y + yAnim, cosSin.z), target.add(nextCosSin.x, nextCosSin.y + yAnim, nextCosSin.z), ColorUtil.multAlpha(color, anim), 3, canSee);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        if (canSee) {
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
        } else RenderSystem.enableDepthTest();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    }

    public void drawGhosts(LivingEntity lastTarget, float anim, float red, float speed) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d vec = MathUtil.interpolate(lastTarget).subtract(camera.getPos());
        boolean canSee = mc.player.canSee(lastTarget);
        double iAge = MathUtil.interpolate(mc.player.age - 1, mc.player.age);
        float halfHeight = lastTarget.getHeight() / 2 + 0.1F;
        float width = lastTarget.getWidth();

        for (int j = 0; j < 3; j++) {
            for (int i = 0, length = 10; i <= length; i++) {
                double radians = Math.toRadians(((i / 2F + iAge * speed) * length + (j * 120)) % (length * 360));
                double sinQuad = Math.sin(Math.toRadians(iAge * 2.5f * speed + i * (j + halfHeight)) * 2) / 2;

                float offset = ((float) (i + length) / (length + length));
                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                matrices.translate(vec.x + Math.cos(radians) * width, (vec.y + halfHeight + sinQuad), vec.z + Math.sin(radians) * width);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                MatrixStack.Entry entry = matrices.peek().copy();
                int color = ColorUtil.multRedAndAlpha(ColorUtil.fade((int) offset * 180), 1 + red * 10, offset * anim);
                float scale = 0.5f * offset;
                Render3DUtil.drawTexture(entry, bloom, -scale / 2, -scale / 2, scale, scale, new Vector4i(color), canSee);
            }
        }
    }

    private float espValue = 1f,espSpeed = 1f, prevEspValue, circleStep;
    private boolean flipSpeed;

    public void updateTargetEsp() {
        prevEspValue = espValue;
        espValue += espSpeed;
        if (espSpeed > 25) flipSpeed = true;
        if (espSpeed < -25) flipSpeed = false;
        espSpeed = flipSpeed ? espSpeed - 0.5f : espSpeed + 0.5f;
        circleStep += 0.15f;
    }

    public void drawLine(MatrixStack.Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
        drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
    }

    public void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
        drawLine(null, start, end, color, color, width, depth);
    }

    public void drawLine(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        Line line = new Line(entry, start, end, colorStart, colorEnd, width);
        if (depth) LINE_DEPTH.add(line); else LINE.add(line);
    }

    public void drawQuad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        drawQuad(null,x,y,w,z,color,depth);
    }

    public void drawQuad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        Quad quad = new Quad(entry, x, y, w, z, color);
        if (depth) QUAD_DEPTH.add(quad); else QUAD.add(quad);
    }

    public void drawTexture(MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color, boolean depth) {
        Texture texture = new Texture(entry, id, x, y, width, height, color);
        if (depth) TEXTURE_DEPTH.add(texture); else TEXTURE.add(texture);
    }

    public record Texture(MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color) {}
    public record Line(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {}
    public record Quad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {}
}
