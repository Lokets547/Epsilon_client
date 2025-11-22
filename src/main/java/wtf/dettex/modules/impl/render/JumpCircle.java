package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.event.impl.render.WorldRenderEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JumpCircle extends Module implements QuickImports {

    ValueSetting lifetimeMs = new ValueSetting("Lifetime", "Time to live in ms").setValue(1500F).range(500F, 3000F);
    ValueSetting radiusSetting = new ValueSetting("Radius", "Circle base radius").setValue(1.0F).range(0.5F, 1.5F);
    SelectSetting renderStyle = new SelectSetting("Render Style", "How to render the circle")
            .value("Lines", "Glow", "Lines+Glow").selected("Glow");

    @NonFinal List<Circle> circles = new ArrayList<>();
    @NonFinal boolean wasOnGround = true;
    @NonFinal float airborneFallDistance = 0.0f;

    public JumpCircle() {
        super("JumpCircle", ModuleCategory.RENDER);
        setup(lifetimeMs, radiusSetting, renderStyle);
    }

    @Override
     
    public void deactivate() {
        circles.clear();
        super.deactivate();
    }

    @Override
     
    public void activate() {
        circles.clear();
        if (mc.player != null) {
            wasOnGround = mc.player.isOnGround();
        } else {
            wasOnGround = true;
        }
        airborneFallDistance = 0.0f;
        super.activate();
    }

    @EventHandler
     
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        boolean onGround = mc.player.isOnGround();
        if (onGround) {
            if (!wasOnGround && airborneFallDistance > 0.0f) {
                addCircle(mc.player);
            }
            airborneFallDistance = 0.0f;
        } else {
            airborneFallDistance = Math.max(airborneFallDistance, mc.player.fallDistance);
        }
        wasOnGround = onGround;
    }

    @EventHandler
     
    public void onWorldRender(WorldRenderEvent e) {
        if (circles.isEmpty() || mc.player == null || mc.world == null) return;
        float lifetime = lifetimeMs.getValue();

        Iterator<Circle> it = circles.iterator();
        while (it.hasNext()) {
            Circle c = it.next();
            float elapsed = c.timer.elapsedTime();
            if (elapsed >= lifetime * 2.0f) { // two phases
                it.remove();
                continue;
            }

            // Проверка видимости круга через стены
            Vec3d playerEyePos = mc.player.getCameraPosVec(e.getPartialTicks());
            Vec3d circlePos = c.pos.add(0, 0.1, 0); // Немного приподнимаем точку для проверки
            if (!mc.world.raycast(new net.minecraft.world.RaycastContext(playerEyePos, circlePos, net.minecraft.world.RaycastContext.ShapeType.VISUAL, net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player)).getType().equals(net.minecraft.util.hit.HitResult.Type.MISS)) {
                continue; // Если луч не доходит до круга (есть препятствие), пропускаем рендеринг
            }

            float t = Math.min(1.0f, elapsed / lifetime); // 0..1
            // scale: grow first half, then fade
            float scale = t; // Плавное увеличение масштаба на протяжении всего времени
            float baseR = radiusSetting.getValue();
            float radius = baseR * (0.6f + 0.6f * scale); // Радиус увеличивается от 60% до 120%
            float alpha;
            if (elapsed < lifetime / 4) {
                // Первая четверть времени: от менее прозрачного к более прозрачному
                alpha = Math.max(0.0f, Math.min(1.0f, 0.85f * (0.3f + 0.7f * (elapsed / (lifetime / 4)))));
            } else if (elapsed < lifetime / 2) {
                // Вторая четверть времени: максимальная прозрачность
                alpha = 0.85f;
            } else {
                // Вторая половина времени: уменьшение прозрачности для исчезновения
                alpha = Math.max(0.0f, Math.min(1.0f, 0.85f * (1.0f - (elapsed - lifetime / 2) / (lifetime / 2))));
            }

            switch (renderStyle.getSelected()) {
                case "Lines" -> drawRingLines(c.pos, radius, alpha, 3.22F);
                case "Glow" -> drawGlow(c.pos, radius, alpha);
                case "Lines+Glow" -> {
                    drawGlow(c.pos, radius, alpha);
                    drawRingLines(c.pos, radius, alpha, 3.22F);
                }
            }
        }
    }

    private void drawRingLines(Vec3d center, float radius, float alpha, float widthF) {
        int mul = Math.max(1, Math.round(3.0F));
        int steps = 90; // base
        double twoPi = Math.PI * 2.0;
        int width = Math.max(1, Math.round(widthF));
        for (int i = 0; i <= steps; i++) {
            double a1 = (i * twoPi) / steps;
            double a2 = ((i + mul) * twoPi) / steps;
            Vec3d p1 = new Vec3d(center.x + radius * Math.cos(a1), center.y, center.z + radius * Math.sin(a1));
            Vec3d p2 = new Vec3d(center.x + radius * Math.cos(a2), center.y, center.z + radius * Math.sin(a2));
            int color = ColorUtil.multAlpha(ColorUtil.fade(i * 4), alpha);
            Render3DUtil.drawLine(p1, p2, color, width, false);
        }
    }

    private void drawGlow(Vec3d center, float radius, float alpha) {
        int layers = Math.max(1, Math.round(6.0F));
        float spread = 0.05F;
        float layerAlpha = alpha / (layers + 0.5f);
        for (int l = 0; l < layers; l++) {
            float t = (l + 1) / (float) layers; // 0..1
            float r = radius + t * spread;
            float a = layerAlpha * (1.0f - (t * 0.85f));
            drawRingLines(center, r, a, Math.max(1.0f, 3.22F + (layers - l))); // thicker outer rings
        }
    }

    private void addCircle(Entity entity) {
        Vec3d pos = entity.getPos();
        // small Y offset if on snow layer
        BlockPos bp = BlockPos.ofFloored(pos);
        if (mc.world != null && mc.world.getBlockState(bp).getBlock().toString().toLowerCase().contains("snow")) {
            pos = pos.add(0.0, 0.125, 0.0);
        }
        circles.add(new Circle(pos));
    }

    private static final class Circle {
        final StopWatch timer = new StopWatch();
        final Vec3d pos;
        Circle(Vec3d pos) { this.pos = pos; }
    }
}

