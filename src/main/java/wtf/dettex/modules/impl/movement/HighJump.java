package wtf.dettex.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.world.BlockFinder;
import wtf.dettex.event.impl.player.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HighJump extends Module {
    SelectSetting mode = new SelectSetting("Mode", "Выберите режим работы").value("Grim", "Shulker").selected("Grim");
    ValueSetting boostMultiplier = new ValueSetting("Boost", "Множитель буста по Y").range(0.1f, 3.0f).setValue(1.0f).visible(() -> mode.isSelected("Grim"));
    ValueSetting cooldownMs = new ValueSetting("Cooldown", "Задержка между бустами, мс").range(0f, 1000f).setValue(200f).visible(() -> mode.isSelected("Grim"));

    StopWatch boostTimer = new StopWatch();

    private final Map<BlockPos, Boolean> shulkerOpened = new HashMap<>();

    public HighJump() {
        super("HighJump", "High Jump", ModuleCategory.MOVEMENT);
        setup(mode, boostMultiplier, cooldownMs);
    }

    @EventHandler
     
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (mode.isSelected("Grim")) {
            BlockPos feet = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 0.1, mc.player.getZ());
            boolean onSlime = mc.world.getBlockState(feet).isOf(Blocks.SLIME_BLOCK);

            if (onSlime && mc.player.isOnGround() && mc.options.jumpKey.isPressed() && boostTimer.finished(cooldownMs.getValue())) {
                double boost = 0.5f * boostMultiplier.getValue();
                var vel = mc.player.getVelocity();
                mc.player.setVelocity(vel.x, vel.y + boost, vel.z);
                boostTimer.reset();
            }
        } else if (mode.isSelected("Shulker")) {
            Set<BlockPos> present = new HashSet<>();
            for (var be : BlockFinder.INSTANCE.blockEntities) {
                if (be instanceof ShulkerBoxBlockEntity shulker) {
                    var pos = be.getPos();
                    present.add(pos);

                    float progress = shulker.getAnimationProgress(1.0f);
                    boolean hasOpened = shulkerOpened.getOrDefault(pos, false);

                    if (progress >= 1.0f) {
                        shulkerOpened.put(pos, true);
                    }
                    if (hasOpened && progress <= 0.0f) {
                        double dx = mc.player.getX() - (pos.getX() + 1.0);
                        double dz = mc.player.getZ() - (pos.getZ() + 1.0);
                        double dy = mc.player.getY() - (pos.getY() + 1.0);
                        double horiz = Math.sqrt(dx * dx + dz * dz);
                        double vy = mc.player.getVelocity().y;
                        double maxDy = vy > 1 ? 30 : 2;

                        if (horiz <= 1 && Math.abs(dy) <= maxDy && mc.player.fallDistance == 0) {
                            var vel = mc.player.getVelocity();
                            mc.player.setVelocity(vel.x, 2.3f, vel.z);
                            shulkerOpened.put(pos, false);
                            break;
                        } else {
                            shulkerOpened.put(pos, false);
                        }
                    }
                }
            }
            shulkerOpened.keySet().removeIf(p -> !present.contains(p));
        }
    }
}

