package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.GroupSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.event.impl.render.WorldRenderEvent;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.RotationAxis;

import java.util.*;
import java.util.List;
//by oblamovvv
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FireFlies extends Module {

    private static final Identifier BLOOM_TEX = Identifier.of("textures/bloom.png");

    final GroupSetting linesGroup = new GroupSetting("Lines", "Параметры линий").setValue(true);
    final ValueSetting maxLines = new ValueSetting("Max Lines", "Максимум линий").setValue(150f).range(10f, 500f);
    final ValueSetting trailLength = new ValueSetting("Trail Length", "Длина хвоста").setValue(30f).range(5f, 100f);
    final ValueSetting speed = new ValueSetting("Speed", "Скорость линий").setValue(0.10f).range(0.02f, 0.5f);
    final ValueSetting spawnRange = new ValueSetting("Spawn Range", "Радиус спавна").setValue(20f).range(5f, 60f);
    final ValueSetting lifeMin = new ValueSetting("Life Min", "Мин. жизнь (тики)").setValue(160f).range(20f, 600f);
    final BooleanSetting depthLines = new BooleanSetting("Depth", "Рисовать с глубиной (не поверх)").setValue(false);

    final GroupSetting sparksGroup = new GroupSetting("Sparks", "Параметры искр").setValue(true);
    final BooleanSetting enableSparks = new BooleanSetting("Enable", "Включить искры").setValue(true);
    final ValueSetting maxSparks = new ValueSetting("Max Sparks", "Максимум искр").setValue(800f).range(0f, 2000f);
    final ValueSetting sparkChance = new ValueSetting("Spawn Chance", "Шанс спавна искры").setValue(0.18f).range(0f, 1f);
    final ValueSetting sparkScale = new ValueSetting("Scale", "Размер искры").setValue(0.08f).range(0.02f, 0.25f);

    final List<Line> lines = new ArrayList<>();
    final List<Spark> sparks = new ArrayList<>();
    final Random rnd = new Random();

    public FireFlies() {
        super("FireFlies", ModuleCategory.RENDER);
        linesGroup.settings(maxLines, trailLength, speed, spawnRange, lifeMin, depthLines);
        sparksGroup.settings(enableSparks, maxSparks, sparkChance, sparkScale);
        setup(linesGroup, sparksGroup);
    }

    class Line {
        Vec3d pos;
        Vec3d vel;
        Vec3d[] trailBuf;
        int trailSize = 0;
        int trailHead = 0; // points to the oldest element index
        int colorInt;
        float baseR, baseG, baseB; // precomputed base color components [0..1]
        int life, maxLife;
        float baseThickness;
        Line(Vec3d pos, Vec3d vel, int colorInt, int life, float thickness) {
            this.pos = pos;
            this.vel = vel;
            this.colorInt = colorInt;
            this.baseR = ((colorInt >> 16) & 0xFF) / 255f;
            this.baseG = ((colorInt >> 8) & 0xFF) / 255f;
            this.baseB = (colorInt & 0xFF) / 255f;
            this.life = life;
            this.maxLife = life;
            this.baseThickness = thickness;
            // allocate with an initial reasonable capacity; will be reallocated if trailLength grows
            this.trailBuf = new Vec3d[32];
            addTrailPoint(pos, 32);
        }
        void ensureCapacity(int cap) {
            if (trailBuf.length >= cap) return;
            int newCap = Math.max(trailBuf.length << 1, cap);
            Vec3d[] nbuf = new Vec3d[newCap];
            // copy in logical order from oldest to newest
            for (int i = 0; i < trailSize; i++) {
                nbuf[i] = trailBuf[(trailHead + i) % trailBuf.length];
            }
            trailBuf = nbuf;
            trailHead = 0;
        }
        void addTrailPoint(Vec3d p, int maxLen) {
            ensureCapacity(maxLen);
            int insertIdx = (trailHead + trailSize) % trailBuf.length;
            trailBuf[insertIdx] = p;
            if (trailSize < maxLen) {
                trailSize++;
            } else {
                // advance head (drop oldest)
                trailHead = (trailHead + 1) % trailBuf.length;
            }
        }
        boolean isDead() { return life <= 0; }
        float fade() { return (float) life / (float) maxLife; }
        void update(Random rnd, double jitterVal, double speedVal, int maxTrailLen) {
            Vec3d n = new Vec3d(
                    (rnd.nextDouble() - 0.5) * jitterVal,
                    (rnd.nextDouble() - 0.5) * jitterVal * 0.7,
                    (rnd.nextDouble() - 0.5) * jitterVal
            );
            vel = add(vel, n);
            vel = normalize(vel);
            vel = mul(vel, speedVal);
            pos = pos.add(vel);
            addTrailPoint(pos, maxTrailLen);
            life--;
        }
    }

    static class Spark {
        Vec3d pos;
        Vec3d vel;
        int life, maxLife;
        int colorInt;
        Spark(Vec3d pos, Vec3d vel, int life, int colorInt) {
            this.pos = pos;
            this.vel = vel;
            this.life = life;
            this.maxLife = life;
            this.colorInt = colorInt;
        }
        void update() {
            pos = pos.add(vel);
            vel = mul(vel, 0.96);
            life--;
        }
        boolean isDead() { return life <= 0; }
        float alpha() { return (float) life / (float) maxLife; }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;
        MatrixStack stack = e.getStack();
        // Cache frequently used settings per-frame to avoid repeated virtual calls and conversions
        final int targetLines = Math.round(maxLines.getValue());
        final int maxTrailLen = Math.round(trailLength.getValue());
        final double speedVal = speed.getValue();
        final double spawnRangeVal = spawnRange.getValue();
        final int baseLife = Math.round(lifeMin.getValue());
        final boolean depth = depthLines.isValue();
        final boolean doSparks = enableSparks.isValue();
        final int maxSparksCount = Math.round(maxSparks.getValue());
        final double sparkChanceVal = sparkChance.getValue();
        final float sparkScaleVal = sparkScale.getValue();
        final double jitterVal = 0.035;
        final Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();

        while (lines.size() < targetLines) {
            double ang = rnd.nextDouble() * Math.PI * 2.0;
            double dist = rnd.nextDouble() * spawnRangeVal;
            double dx = Math.cos(ang) * dist;
            double dz = Math.sin(ang) * dist;
            double x = mc.player.getX() + dx;
            double y = mc.player.getY() + (rnd.nextDouble() - 0.5) * 4.0;
            double z = mc.player.getZ() + dz;
            Vec3d vel = new Vec3d(rnd.nextDouble() - 0.5, (rnd.nextDouble() - 0.5) * 0.3, rnd.nextDouble() - 0.5);
            vel = normalize(vel);
            vel = mul(vel, speedVal);
            int color = ColorUtil.fade((int) (System.currentTimeMillis() / 10 % 360));
            int life = baseLife;
            float thickness = 1.6f + rnd.nextFloat();
            lines.add(new Line(new Vec3d(x, y, z), vel, color, life, thickness));
        }

        Iterator<Line> lit = lines.iterator();
        while (lit.hasNext()) {
            Line L = lit.next();
            if (doSparks && sparks.size() < maxSparksCount && rnd.nextDouble() < sparkChanceVal) {
                Vec3d svel = add(L.vel, new Vec3d((rnd.nextDouble() - 0.5) * 0.4, (rnd.nextDouble() - 0.5) * 0.4, (rnd.nextDouble() - 0.5) * 0.4));
                svel = normalize(svel);
                svel = mul(svel, 0.35 + rnd.nextDouble() * 0.6);
                int spLife = 12 + rnd.nextInt(18);
                sparks.add(new Spark(L.pos, svel, spLife, L.colorInt));
            }
            L.update(rnd, jitterVal, speedVal, maxTrailLen);
            if (L.isDead()) lit.remove();
        }

        Iterator<Spark> sit = sparks.iterator();
        while (sit.hasNext()) {
            Spark s = sit.next();
            s.update();
            if (s.isDead()) sit.remove();
        }

        for (Line L : lines) {
            int n = L.trailSize;
            if (n < 2) continue;
            float fadeMaster = L.fade();

            // iterate over circular buffer from oldest to newest
            for (int i = 1; i < L.trailSize; i++) {
                Vec3d p1 = L.trailBuf[(L.trailHead + i - 1) % L.trailBuf.length];
                Vec3d p2 = L.trailBuf[(L.trailHead + i) % L.trailBuf.length];
                float t = (float) i / (float) (n - 1);
                float thickness = Math.max(1.0f, L.baseThickness * (1.0f - 0.85f * t) + 0.8f);
                float alpha = (1.0f - t * 0.95f) * fadeMaster;
                float satMult = 1.0f - t * 0.6f;
                float brightMult = 0.6f + (1.0f - t) * 0.4f;
                int r = clampColor(L.baseR * satMult * brightMult);
                int g = clampColor(L.baseG * satMult * brightMult);
                int b = clampColor(L.baseB * satMult * brightMult);
                int a = clampAlpha(alpha);
                int col = (a << 24) | (r << 16) | (g << 8) | b;
                Render3DUtil.drawLine(p1, p2, col, thickness, depth);
            }
        }

        Camera camera = mc.gameRenderer.getCamera();
        if (doSparks) for (Spark s : sparks) {
            float a = s.alpha();
            int sr = (s.colorInt >> 16) & 0xFF;
            int sg = (s.colorInt >> 8) & 0xFF;
            int sb = (s.colorInt) & 0xFF;
            int rr = Math.min(255, (int) (sr * 1.5f));
            int gg = Math.min(255, (int) (sg * 1.5f));
            int bb = Math.min(255, (int) (sb * 1.5f));
            int argb = ((int) (a * 0.9f * 255) << 24) | (rr << 16) | (gg << 8) | bb;
            float scale = sparkScaleVal;

            double posX = s.pos.x - cam.x;
            double posY = s.pos.y - cam.y;
            double posZ = s.pos.z - cam.z;

            stack.push();
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            stack.translate(posX, posY, posZ);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            Render3DUtil.drawTexture(stack.peek(), BLOOM_TEX, -scale / 2, -scale / 2, scale, scale, new org.joml.Vector4i(argb), false);
            stack.pop();
        }
    }

    private static Vec3d add(Vec3d a, Vec3d b) { return new Vec3d(a.x + b.x, a.y + b.y, a.z + b.z); }
    private static Vec3d mul(Vec3d v, double s) { return new Vec3d(v.x * s, v.y * s, v.z * s); }
    private static Vec3d normalize(Vec3d v) {
        double len = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        if (len == 0) return new Vec3d(0, 0, 0);
        return new Vec3d(v.x / len, v.y / len, v.z / len);
    }

    private static int clampColor(float v) { return Math.max(0, Math.min(255, (int) (v * 255f))); }
    private static int clampAlpha(float v) { return Math.max(0, Math.min(255, (int) (v * 255f))); }

    @Override
    public void deactivate() {
        lines.clear();
        sparks.clear();
        super.deactivate();
    }
}
