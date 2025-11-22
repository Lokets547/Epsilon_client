package wtf.dettex.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.entity.PlayerInventoryComponent;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.entity.SimulatedPlayer;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.types.EventType;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BindSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.api.repository.friend.FriendUtils;
import wtf.dettex.common.util.entity.*;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.task.TaskPriority;
import wtf.dettex.common.util.task.scripts.Script;
import wtf.dettex.event.impl.player.EntitySpawnEvent;
import wtf.dettex.event.impl.player.PostMotionEvent;
import wtf.dettex.event.impl.player.RotationUpdateEvent;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationConfig;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;
import wtf.dettex.modules.impl.combat.killaura.rotation.angle.SnapSmoothMode;
import wtf.dettex.modules.impl.render.ProjectilePrediction;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TargetPearl extends Module {
    StopWatch stopWatch = new StopWatch();
    Script script = new Script();

    SelectSetting modeSetting = new SelectSetting("Mode", "When will target pearl work")
            .value("Bind", "Always").selected("Always");

    SelectSetting targetSetting = new SelectSetting("Targets", "Targets for which pearls will be thrown")
            .value("Aura Target", "All").selected("Aura Target");

    BindSetting throwSetting = new BindSetting("Throw","Throw Key").visible(()-> modeSetting.isSelected("Bind"));

    ValueSetting distanceSetting = new ValueSetting("Distance", "Target Pearl Trigger Distance")
            .setValue(6).range(3, 15);

    public TargetPearl() {
        super("TargetPearl","Target Pearl", ModuleCategory.COMBAT);
        setup(modeSetting, targetSetting, throwSetting, distanceSetting);
    }

    @EventHandler
     
    public void onEntitySpawn(EntitySpawnEvent e) {
        if (e.getEntity() instanceof EnderPearlEntity pearl) mc.world.getPlayers().stream().filter(p -> p.distanceTo(pearl) <= 3)
                .min(Comparator.comparingDouble(p -> p.distanceTo(pearl))).ifPresent(pearl::setOwner);
    }

    @EventHandler

    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() == EventType.PRE) {
            LivingEntity target = Aura.getInstance().getLastTarget();
            Slot slot = PlayerInventoryUtil.getSlot(Items.ENDER_PEARL);

            if (slot == null || !stopWatch.finished(1000)) return;
            if (modeSetting.isSelected("Bind") && !PlayerIntersectionUtil.isKey(throwSetting)) return;
            if (PlayerIntersectionUtil.streamEntities().filter(EnderPearlEntity.class::isInstance).map(EnderPearlEntity.class::cast)
                    .anyMatch(pearl -> Objects.equals(pearl.getOwner(), mc.player))) {
                stopWatch.reset();
                return;
            }

            ProjectilePrediction prediction = ProjectilePrediction.getInstance();
            PlayerIntersectionUtil.streamEntities().filter(EnderPearlEntity.class::isInstance).map(EnderPearlEntity.class::cast)
                    .filter(pearl -> !FriendUtils.isFriend(pearl.getOwner()) && (targetSetting.isSelected("All") || (target != null && target.equals(pearl.getOwner()))))
                    .min(Comparator.comparingDouble(pearl -> RotationController.computeRotationDifference(AngleUtil.cameraAngle(), AngleUtil.calculateAngle(prediction.calcTrajectory(pearl).getPos()))))
                    .ifPresent(pearl -> {
                        HitResult targetResult = prediction.calcTrajectory(pearl);
                        if (targetResult == null || mc.player.getPos().distanceTo(targetResult.getPos()) <= distanceSetting.getInt()) return;
                        Vec3d eyePos = mc.player.getEyePos().add(mc.player.getPos().subtract(SimulatedPlayer.simulateLocalPlayer(1).pos));
                        float yaw = AngleUtil.fromVec3d(targetResult.getPos().subtract(eyePos)).getYaw();
                        IntStream.range(-89, 89).mapToObj(pitch -> new Angle(yaw, pitch)).filter(angle -> {
                            HitResult playerResult = prediction.checkTrajectory(angle.toVector(), new EnderPearlEntity(mc.world, mc.player, slot.getStack()), 1.5);
                            return playerResult != null && playerResult.getPos().distanceTo(targetResult.getPos()) <= 3F;
                        }).max(Comparator.comparingDouble(Angle::getPitch)).ifPresent(angle -> {
                            RotationController.INSTANCE.rotateTo(new Angle.VecRotation(angle, angle.toVector()), mc.player, 1, new RotationConfig(new SnapSmoothMode(),true,true), TaskPriority.HIGH_IMPORTANCE_3, this);
                            PlayerInventoryComponent.unPressMoveKeys();
                            script.cleanup().addTickStep(0, () -> {
                                PlayerInventoryUtil.swapAndUse(Items.ENDER_PEARL, angle, false);
                                PlayerInventoryComponent.enableMoveKeys();
                            });
                            pearl.setOwner(null);
                            stopWatch.reset();
                        });
                    });
        }
    }

    @EventHandler
    public void onPostMotion(PostMotionEvent e) {
        script.update();
    }
}

