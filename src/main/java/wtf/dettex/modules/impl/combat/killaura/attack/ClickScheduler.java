package wtf.dettex.modules.impl.combat.killaura.attack;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.world.ServerUtil;
import wtf.dettex.Main;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClickScheduler implements QuickImports {
    private final int[] funTimeTicks = new int[]{10, 11, 10, 13}, spookyTicks = new int[]{11, 10, 13, 10, 12, 11, 12}, defaultTicks = new int[]{10, 11};
    long lastClickTime = System.currentTimeMillis();

    public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
        boolean dynamic = hasTicksElapsedSinceLastClick(tickCount() - ticks) || !dynamicCooldown;
        return dynamic && mc.player.getAttackCooldownProgress(ticks) > 0.9F;
    }

    public boolean hasTicksElapsedSinceLastClick(int ticks) {
        return lastClickPassed() >= (ticks * 50L * (20F / ServerUtil.TPS))  ;
    }

    public long lastClickPassed() {
        return System.currentTimeMillis() - lastClickTime;
    }

    public void recalculate() {
        lastClickTime = System.currentTimeMillis();
    }

    int tickCount() {
        int count = Main.getInstance().getAttackPerpetrator().getAttackHandler().getCount();
        return switch (ServerUtil.server) {
            case "FunTime" -> funTimeTicks[count % funTimeTicks.length];
            case "SpookyTime" -> spookyTicks[count % spookyTicks.length];
            default -> defaultTicks[count % defaultTicks.length];
        };
    }
}