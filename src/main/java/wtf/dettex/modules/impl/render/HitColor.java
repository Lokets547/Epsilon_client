package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.ColorSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.player.AttackEvent;
import wtf.dettex.event.impl.render.EntityColorEvent;
import wtf.dettex.event.impl.player.TickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HitColor extends Module {
    public static HitColor getInstance() { return Instance.get(HitColor.class); }

    ColorSetting hitColor = new ColorSetting("Hit Color", "Tint color applied on hit").setColor(0xFFFF0000);
    ValueSetting duration = new ValueSetting("Duration", "How long the tint lasts (ms)").range(50f, 2000f).setValue(350f);
    BooleanSetting fade = new BooleanSetting("Fade", "Fade out over time").setValue(true);

    Map<LivingEntity, Long> hitTimes = new WeakHashMap<>();

    public HitColor() {
        super("HitColor", "Hit Color", ModuleCategory.RENDER);
        setup(hitColor, duration, fade);
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (!(e.getEntity() instanceof LivingEntity living)) return;
        hitTimes.put(living, System.currentTimeMillis());
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (hitTimes.isEmpty()) return;
        long now = System.currentTimeMillis();
        float life = duration.getValue();
        Iterator<Map.Entry<LivingEntity, Long>> it = hitTimes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, Long> en = it.next();
            if (en.getKey().isRemoved() || now - en.getValue() > life) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onEntityColor(EntityColorEvent e) {
        LivingEntity entity = e.getEntity();
        if (entity == null) return;
        Long t0 = hitTimes.get(entity);
        if (t0 == null) return;
        long dt = System.currentTimeMillis() - t0;
        float life = duration.getValue();
        if (dt > life) return;

        int base = hitColor.getColor();
        int a = (base >>> 24) & 0xFF;
        if (fade.isValue()) {
            float k = 1.0f - (dt / life);
            k = Math.max(0f, Math.min(1f, k));
            a = Math.min(255, Math.max(0, Math.round(a * k)));
        }
        int tinted = (a << 24) | (base & 0x00FFFFFF);
        e.setColor(tinted);
        if (a < 255) e.setCancelled(true);
    }
}
