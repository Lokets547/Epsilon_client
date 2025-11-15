package wtf.dettex.modules.impl.movement;


import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;


import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.event.impl.player.JumpEvent;
import wtf.dettex.event.impl.player.PostTickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AirJump extends Module {
    StopWatch stopWatch = new StopWatch();

    SelectSetting modeSetting = new SelectSetting("Mode", "Selects the type of mode")
            .value("Polar Block Collision");

    public AirJump() {
        super("AirJump", "Air Jump", ModuleCategory.MOVEMENT);
        setup(modeSetting);
    }


    @EventHandler
    public void onJump(JumpEvent e) {
        stopWatch.reset();
    }


    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onPostTick(PostTickEvent e) {
        if (modeSetting.isSelected("Polar Block Collision")) {
            if (mc.options.jumpKey.isPressed()) return;
            Box playerBox = mc.player.getBoundingBox().expand(-1e-3);
            Box box = new Box(playerBox.minX, playerBox.minY, playerBox.minZ, playerBox.maxX, playerBox.minY + 0.5, playerBox.maxZ);
            if (stopWatch.finished(400) && PlayerIntersectionUtil.isBox(box, this::hasCollision)) {
                box = new Box(playerBox.minX, playerBox.minY + 1, playerBox.minZ, playerBox.maxX, playerBox.maxY, playerBox.maxZ);
                if (PlayerIntersectionUtil.isBox(box, this::hasCollision)) {
                    mc.player.setOnGround(true);
                    mc.player.velocity.y = 0.6;
                } else {
                    mc.player.setOnGround(true);
                    mc.player.jump();
                }
            }
        }
    }


    private boolean hasCollision(BlockPos blockPos) {
        return !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
    }
}
