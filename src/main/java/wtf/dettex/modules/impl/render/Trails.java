package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.Identifier;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.event.impl.render.WorldRenderEvent;
import wtf.dettex.modules.setting.implement.*;
import wtf.dettex.modules.setting.implement.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Trails extends Module {
    final MultiSelectSetting predmeti = new MultiSelectSetting("Предметы", "Предметы на которые будут трейлы").value("Arrow", "Pearl", "Xp");
    final SelectSetting trailType = new SelectSetting("Trail Type", "Тип трейла").value("Trail", "Particles").selected("Particles");
    final GroupSetting group = new GroupSetting("Trails", "Trails of different things").settings(predmeti, trailType);
    final ValueSetting speed = new ValueSetting("Speed", "Скорость частиц").setValue(2f).range(1f, 20f);
    final BooleanSetting physics = new BooleanSetting("Physics", "Физика частиц").setValue(true);
    final ValueSetting starsScale = new ValueSetting("Scale", "Размер частиц").setValue(3f).range(1f, 10f);
    final ValueSetting lifeTime = new ValueSetting("LifeTime", "Время жизни").setValue(2f).range(1f, 10f);

    final List<Particle> particles = new ArrayList<>();

    public Trails() {
        super("Trails", ModuleCategory.RENDER);
        setup(group,
                speed, physics, starsScale, lifeTime
        );
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Projectiles-based particles
        for (Entity en : mc.world.getEntities()) {
            if (en instanceof ArrowEntity && trailType.isSelected("Particles") && predmeti.isSelected("Arrow"))
                if (en.prevY != en.getY()) for (int i = 0; i < 5; i++) particles.add(new Particle(en.getX(), en.getY(), en.getZ(), colorForAge()));
            if (en instanceof EnderPearlEntity && trailType.isSelected("Particles") && predmeti.isSelected("Pearl"))
                for (int i = 0; i < 5; i++) particles.add(new Particle(en.getX(), en.getY(), en.getZ(), colorForAge()));
            if (en instanceof ExperienceBottleEntity && predmeti.isSelected("Xp"))
                for (int i = 0; i < 3; i++) particles.add(new Particle(en.getX(), en.getY(), en.getZ(), colorForAge()));
        }

        // Cleanup particles by lifetime
        particles.removeIf(particle -> System.currentTimeMillis() - particle.time > 1000f * lifeTime.getValue());
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;
        MatrixStack stack = e.getStack();
        float tickDelta = e.getPartialTicks();

        // Draw projectile trails
        for (Entity en : mc.world.getEntities()) {
            if ((en instanceof EnderPearlEntity && trailType.isSelected("Trail")) || predmeti.isSelected("Pearl") ||
                    (en instanceof ArrowEntity && trailType.isSelected("Trail")) || predmeti.isSelected("Arrow") ||
                    (en instanceof ExperienceBottleEntity && predmeti.isSelected("Xp"))) {
                calcTrajectory(stack, en);
            }
        }

        // Render floating particles as billboards
        if (!particles.isEmpty()) {
            for (Particle p : particles) p.render(tickDelta);
        }
    }

    private void calcTrajectory(MatrixStack stack, Entity e) {
        double motionX = e.getVelocity().x;
        double motionY = e.getVelocity().y;
        double motionZ = e.getVelocity().z;
        double x = e.getX();
        double y = e.getY();
        double z = e.getZ();
        Vec3d lastPos = new Vec3d(x, y, z);
        for (int i = 0; i < 300; i++) {
            lastPos = new Vec3d(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            if (mc.world.getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock() == Blocks.WATER) {
                motionX *= 0.8;
                motionY *= 0.8;
                motionZ *= 0.8;
            } else {
                motionX *= 0.99;
                motionY *= 0.99;
                motionZ *= 0.99;
            }
            if (e instanceof ArrowEntity) motionY -= 0.05000000074505806; else motionY -= 0.03f;
            Vec3d pos = new Vec3d(x, y, z);

            HitResult hit = mc.world.raycast(new RaycastContext(lastPos, pos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (hit != null && (hit.getType() == HitResult.Type.ENTITY || hit.getType() == HitResult.Type.BLOCK)) break;
            if (y <= -65) break;
            if (e.getVelocity().x == 0 && e.getVelocity().y == 0 && e.getVelocity().z == 0) continue;

            int alpha = (int) net.minecraft.util.math.MathHelper.clamp(255f * (i / 8f), 0f, 255f);
            int col = withAlpha(new Color(ColorUtil.fade(i * 5)), alpha);
            Render3DUtil.drawLine(lastPos, pos, col, 2f, false);
        }
    }

    private Color colorForAge() {
        return new Color(ColorUtil.fade(mc.player.age));
    }

    private static int withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha))).getRGB();
    }

    private static Color applyOpacity(Color c, float opacity) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (Math.max(0f, Math.min(1f, opacity)) * 255));
    }

    private static float randRange(float a, float b) { return (float) (ThreadLocalRandom.current().nextDouble(Math.min(a,b), Math.max(a,b))); }

    @Override
    public void deactivate() {
        particles.clear();
        super.deactivate();
    }

    public class Particle {
        double x, y, z;
        double motionX, motionY, motionZ;
        long time;
        Color color;

        public Particle(double x, double y, double z, Color color) {
            this.x = x; this.y = y; this.z = z;
            float sp = speed.getValue() / 200f;
            motionX = randRange(-sp, sp);
            motionY = randRange(-sp, sp);
            motionZ = randRange(-sp, sp);
            time = System.currentTimeMillis();
            this.color = color;
        }

        public void update() {
            double sp = starsScale.getValue() / 10f;
            x += motionX; y += motionY; z += motionZ;
            if (posBlock(x, y - starsScale.getValue() / 10f, z)) motionY = -motionY / 1.1; else {
                if (posBlock(x, y, z) || posBlock(x - sp, y, z - sp) || posBlock(x + sp, y, z + sp)
                        || posBlock(x + sp, y, z - sp) || posBlock(x - sp, y, z + sp)
                        || posBlock(x + sp, y, z) || posBlock(x - sp, y, z)
                        || posBlock(x, y, z - sp) || posBlock(x, y, z + sp)) {
                    motionX = -motionX; motionZ = -motionZ;
                }
            }
            if (physics.isValue()) motionY -= 0.0005f;
            motionX /= 1.005; motionZ /= 1.005; motionY /= 1.005;
        }

        public void render(float tickDelta) {
            update();
            float scale = starsScale.getValue() / 10f;
            Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
            double posX = x - cam.getX();
            double posY = y - cam.getY();
            double posZ = z - cam.getZ();

            Camera camera = mc.gameRenderer.getCamera();
            MatrixStack matrices = new MatrixStack();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            matrices.translate(posX, posY, posZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            MatrixStack.Entry entry = matrices.peek().copy();

            float colorAnim = (float) (System.currentTimeMillis() - time) / (1000f * lifeTime.getValue());
            Color c = new Color(ColorUtil.fade((int) (360 * colorAnim)));
            Color finalColor = applyOpacity(c, 1f - colorAnim);

            // Map modes to available texture (bloom). If needed we can add star/heart assets later.
            Render3DUtil.drawTexture(entry, Identifier.of("textures/bloom.png"), -scale / 2, -scale / 2, scale, scale,
                    new org.joml.Vector4i(finalColor.getRGB()), false);
        }

        private boolean posBlock(double x, double y, double z) {
            Block b = mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock();
            return b != Blocks.AIR && b != Blocks.WATER && b != Blocks.LAVA;
        }
    }
}

