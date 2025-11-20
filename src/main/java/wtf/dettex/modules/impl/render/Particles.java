package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RaytracingUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.impl.player.AttackEvent;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.event.impl.render.WorldRenderEvent;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;

import java.awt.*;
import java.util.*;
import java.util.List;
//by @userbarsix
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Particles extends Module {

    final MultiSelectSetting renderContexts = new MultiSelectSetting("Particles Type", "Где рендерить частицы")
            .value("Свободный мир", "При ударе");
    final ValueSetting maxParticles = new ValueSetting("Max Particles", "Максимум частиц").setValue(100).range(10, 500);
    final ValueSetting particleSize = new ValueSetting("Particle Size", "Размер частиц").setValue(1.0f).range(0.5f, 1.0f);
    final ValueSetting lifeTime = new ValueSetting("Life Time", "Время жизни").setValue(800f).range(400f, 2000f);
    final MultiSelectSetting worldParticleTypes = new MultiSelectSetting("World Particles", "Частицы в свободном мире").value("Звезды", "Снег", "Доллары").visible(() -> renderContexts.isSelected("Свободный мир"));
    final MultiSelectSetting hitParticleTypes = new MultiSelectSetting("Hit Particles", "Частицы при ударе").value("Звезды", "Снег", "Доллары").visible(() -> renderContexts.isSelected("При ударе"));
    final ValueSetting spawnRange = new ValueSetting("Spawn Range", "Радиус спавна").setValue(25f).range(10f, 500f);
    final List<Particle> particles = new ArrayList<>();
    final Random rnd = new Random();
    int spawnDelay = 0;
    int nextSpawnTicks = 0;
    final Map<String, Identifier> textureMap = Map.of(
            "Звезды", Identifier.of("textures/star.png"),
            "Снег", Identifier.of("textures/snow.png"),
            "Доллары", Identifier.of("textures/dollar.png")
    );
    final Identifier defaultTexture = textureMap.get("Звезды");

    public Particles() {
        super("Particles", ModuleCategory.RENDER);
        worldParticleTypes.setSelected(worldParticleTypes.getList());
        hitParticleTypes.setSelected(hitParticleTypes.getList());
        renderContexts.setSelected(renderContexts.getList());
        setup(renderContexts, spawnRange, maxParticles, particleSize, lifeTime, worldParticleTypes, hitParticleTypes);
    }

    private boolean hasTextures(MultiSelectSetting setting) {
        List<String> selected = setting.getSelected();
        return selected != null && !selected.isEmpty();
    }

    private boolean isContextEnabled(String name) {
        List<String> selected = renderContexts.getSelected();
        return selected != null && selected.contains(name);
    }

    private boolean isSourceEnabled(ParticleSource source) {
        return switch (source) {
            case WORLD -> isContextEnabled("Свободный мир");
            case HIT -> isContextEnabled("При ударе");
        };
    }

    enum ParticleSource {
        WORLD,
        HIT
    }

    class Particle {
        Vec3d pos;
        Vec3d vel;
        int life, maxLife;
        int colorInt;
        boolean hitSurface = false;
        int fadeOutTime = 0;
        Identifier textureId;
        ParticleSource source;

        Particle(Vec3d pos, Vec3d vel, int life, int colorInt, Identifier textureId, ParticleSource source) {
            this.pos = pos;
            this.vel = vel;
            this.life = life;
            this.maxLife = life;
            this.colorInt = colorInt;
            this.fadeOutTime = 30;
            this.textureId = textureId;
            this.source = source;
        }

        boolean isDead() {
            return life <= 0;
        }

        float fade() {
            if (hitSurface && life > 0) {
                return (float) life / (float) fadeOutTime;
            }
            return (float) life / (float) maxLife;
        }

        float scaleFactor() {
            if (hitSurface && life > 0) {
                return (float) life / (float) fadeOutTime;
            }
            return 1.0f;
        }

        void update() {
            if (hitSurface) {
                life--;
                vel = new Vec3d(0, 0, 0);
                return;
            }

            double s = 0.02f;
            double drag = 0.98;
            double gravity = -s * (1 - drag);
            double jitter = s * 0.1;

            Vec3d noise = new Vec3d(
                    (rnd.nextDouble() - 0.5) * jitter,
                    (rnd.nextDouble() - 0.5) * jitter * 0.3,
                    (rnd.nextDouble() - 0.5) * jitter
            );

            vel = add(vel, noise);
            vel = mul(vel, drag);
            vel = add(vel, new Vec3d(0, gravity, 0));

            pos = pos.add(vel);

            BlockPos blockPos = BlockPos.ofFloored(pos.x, pos.y, pos.z);
            if (!mc.world.isAir(blockPos)) {
                hitSurface = true;
                life = fadeOutTime;
                vel = new Vec3d(
                        vel.x * 0.3,
                        Math.abs(vel.y) * 0.5,
                        vel.z * 0.3
                );
            } else {
                life--;
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (isContextEnabled("Свободный мир") && hasTextures(worldParticleTypes) && spawnDelay <= 0 && particles.size() < Math.round(maxParticles.getValue())) {
            spawnParticle();
            nextSpawnTicks = Math.round(10f) + rnd.nextInt(10) - 5;
            spawnDelay = Math.max(5, nextSpawnTicks);
        } else {
            spawnDelay = Math.max(0, spawnDelay - 1);
        }

        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update();
            if (particle.isDead()) {
                iterator.remove();
            }
        }

        renderParticles(e.getStack());
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (!(e.getEntity() instanceof LivingEntity living)) return;

        if (isContextEnabled("При ударе") && hasTextures(hitParticleTypes)) {
            Vec3d targetPos = new Vec3d(living.getX(), living.getY() + living.getHeight() * 0.5, living.getZ());
            spawnHitParticles(targetPos);
        }
    }

    private void spawnParticle() {
        double ang = rnd.nextDouble() * Math.PI * 2.0;
        double dist = rnd.nextDouble() * spawnRange.getValue();
        double dx = Math.cos(ang) * dist;
        double dz = Math.sin(ang) * dist;
        double x = mc.player.getX() + dx;
        double y = mc.player.getY() + 20.0 + rnd.nextDouble() * 5.0;
        double z = mc.player.getZ() + dz;
        double s = 0.02f;
        Vec3d vel = new Vec3d(
                (rnd.nextDouble() - 0.5) * s * 0.5,
                0,
                (rnd.nextDouble() - 0.5) * s * 0.5
        );

        int color = ColorUtil.fade((int) (System.currentTimeMillis() / 10 % 360));
        int life = Math.round(lifeTime.getValue()) + rnd.nextInt(50) - 25;
        life = Math.max(20, life);

        Identifier texture = pickRandomTexture(worldParticleTypes);
        if (texture == null) {
            return;
        }

        particles.add(new Particle(new Vec3d(x, y, z), vel, life, color, texture, ParticleSource.WORLD));
    }

    private void spawnHitParticles(Vec3d center) {
        int amount = 8 + rnd.nextInt(6);
        for (int i = 0; i < amount && particles.size() < Math.round(maxParticles.getValue()); i++) {
            Vec3d offset = new Vec3d(
                    (rnd.nextDouble() - 0.5) * 0.6,
                    (rnd.nextDouble() - 0.3) * 0.6,
                    (rnd.nextDouble() - 0.5) * 0.6
            );
            Vec3d pos = center.add(offset);
            Vec3d vel = new Vec3d(
                    (rnd.nextDouble() - 0.5) * 0.12,
                    rnd.nextDouble() * 0.1,
                    (rnd.nextDouble() - 0.5) * 0.12
            );

            int color = ColorUtil.fade((int) (System.currentTimeMillis() / 10 % 360));
            int life = Math.max(20, Math.round(lifeTime.getValue() * 0.6f) + rnd.nextInt(30) - 15);

            Identifier texture = pickRandomTexture(hitParticleTypes);
            if (texture == null) {
                continue;
            }

            particles.add(new Particle(pos, vel, life, color, texture, ParticleSource.HIT));
        }
    }

    private Identifier pickRandomTexture(MultiSelectSetting setting) {
        List<String> selected = setting.getSelected();
        if (selected == null || selected.isEmpty()) {
            return null;
        }
        String name = selected.size() == 1 ? selected.get(0) : selected.get(rnd.nextInt(selected.size()));
        return textureMap.getOrDefault(name, defaultTexture);
    }

    private void renderParticles(MatrixStack stack) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        for (Particle particle : particles) {
            if (!isSourceEnabled(particle.source)) {
                continue;
            }
            if (particle.textureId == null) {
                continue;
            }
            if (!isVisible(camPos, particle.pos)) {
                continue;
            }
            float alpha = particle.fade();
            float scaleFactor = particle.scaleFactor();
            Color baseColor = new Color(particle.colorInt);
            int r = baseColor.getRed();
            int g = baseColor.getGreen();
            int b = baseColor.getBlue();
            float finalAlpha = alpha;
            if (particle.hitSurface) {
                finalAlpha *= 0.7f;
            }
            int a = (int) (finalAlpha * 255 * 0.8f);

            int argb = new Color(r, g, b, a).getRGB();

            double posX = particle.pos.x - camPos.x;
            double posY = particle.pos.y - camPos.y;
            double posZ = particle.pos.z - camPos.z;

            MatrixStack matrices = new MatrixStack();
            matrices.push();

            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            matrices.translate(posX, posY, posZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float baseScale = particleSize.getValue() * (0.5f + alpha * 0.5f);
            float finalScale = baseScale * scaleFactor;

            Render3DUtil.drawTexture(
                    matrices.peek(),
                    particle.textureId,
                    -finalScale / 2,
                    -finalScale / 2,
                    finalScale,
                    finalScale,
                    new org.joml.Vector4i(argb),
                    false
            );

            matrices.pop();
        }
    }

    private boolean isVisible(Vec3d from, Vec3d to) {
        if (mc.world == null) return false;
        BlockHitResult result = RaytracingUtil.raycast(from, to, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player);
        return result.getType() == HitResult.Type.MISS || result.getPos().distanceTo(to) < 0.1;
    }

    private static Vec3d add(Vec3d a, Vec3d b) {
        return new Vec3d(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    private static Vec3d mul(Vec3d v, double s) {
        return new Vec3d(v.x * s, v.y * s, v.z * s);
    }

    private static Vec3d normalize(Vec3d v) {
        double len = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        if (len == 0) return new Vec3d(0, 0, 0);
        return new Vec3d(v.x / len, v.y / len, v.z / len);
    }

    @Override
    public void deactivate() {
        particles.clear();
        spawnDelay = 0;
        super.deactivate();
    }
}
