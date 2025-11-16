package wtf.dettex.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.types.EventType;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.Direction;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.common.util.task.TaskPriority;
import wtf.dettex.Main;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.event.impl.player.EventMoveInput1;
import wtf.dettex.event.impl.player.RotationUpdateEvent;
import wtf.dettex.event.impl.render.WorldRenderEvent;
import wtf.dettex.implement.features.draggables.Notifications;
import wtf.dettex.modules.setting.implement.*;
import wtf.dettex.modules.impl.combat.killaura.attack.AttackHandler;
import wtf.dettex.modules.impl.combat.killaura.attack.AttackPerpetrator;
import wtf.dettex.modules.impl.combat.killaura.rotation.*;
import wtf.dettex.modules.impl.combat.killaura.rotation.angle.*;
import wtf.dettex.modules.impl.combat.killaura.target.TargetSelector;
import wtf.dettex.modules.impl.render.Hud;

import java.util.Objects;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Aura extends Module {
    public static Aura getInstance() {
        return Instance.get(Aura.class);
    }

    Animation esp_anim = new DecelerateAnimation().setMs(400).setValue(1);
    TargetSelector targetSelector = new TargetSelector();
    PointFinder pointFinder = new PointFinder();
    @NonFinal
    LivingEntity target, lastTarget;

    MultiSelectSetting targetType = new MultiSelectSetting("Target Type", "Filters the entire list of targets by type")
            .value("Players", "Mobs", "Animals", "Friends", "Invisible", "Naked Invisible");

    MultiSelectSetting attackSetting = new MultiSelectSetting("Attack Setting", "Allows you to customize the attack")
            .value("Only Critical", "Dynamic Cooldown", "Break Shield", "UnPress Shield", "No Attack When Eat", "Ignore The Walls", "Smart Crits");

    SelectSetting correctionType = new SelectSetting("Correction Type", "Selects the type of correction")
            .value("Free", "Focused", "Targeted").selected("Free");

    SelectSetting aimMode = new SelectSetting("Rotation Type", "Allows you to select the rotation type")
            .value("FuntimeSmooth", "FuntimeSnap", "Snap", "Universal", "ReallyWorld", "SpookyTime", "HvH", "FuntimeTest", "Matrix|Bypass"
                    //, "AI"
            ).selected("Snap");

    GroupSetting correctionGroup = new GroupSetting("Move correction", "Prevents detection by movement sensitive anti-cheats")
            .settings(correctionType).setValue(true).visible(() -> !aimMode.isSelected("Snap"));

    SelectSetting targetEspType = new SelectSetting("Target Esp Type", "Selects the type of target esp")
            .value("Cube", "Circle", "Ghosts").selected("Circle");

    SelectSetting cubeType = new SelectSetting("Cube Type", "Select cube esp style")
            .value("Default", "Modern", "Skid").selected("Default").visible(() -> targetEspType.isSelected("Cube"));

    ValueSetting ghostSpeed = new ValueSetting("Ghost Speed", "Speed of ghost flying around the target")
            .setValue(1).range(1F, 2F).visible(() -> targetEspType.isSelected("Ghosts"));

    ValueSetting crystalSpeed = new ValueSetting("Crystal Speed", "Orbital speed for crystal ESP")
            .setValue(1.0F).range(0.2F, 3.0F).visible(() -> targetEspType.isSelected("Crystal"));

    GroupSetting targetEspGroup = new GroupSetting("Target Esp", "Displays the player in the world")
            .settings(targetEspType, cubeType, ghostSpeed, crystalSpeed).setValue(true);

//    BooleanSetting aiTraining = new BooleanSetting("AI Training", "Log dataset for AI rotation training").setValue(false);
//    GroupSetting aiGroup = new GroupSetting("AI", "AI related settings").settings(aiTraining).setValue(false);

    SelectSetting sprintResetMode = new SelectSetting("Reset Type", "Selects the sprint reset type")
            .value("Rage", "Normal", "Legit").selected("Normal");

    GroupSetting sprintReset = new GroupSetting("Sprint Reset", "Resets the sprint before attack")
            .settings(sprintResetMode).setValue(true);

    BooleanSetting multiPointMode = new BooleanSetting("MultiPoint", "Random hit part of target").setValue(true);


    ValueSetting maxDistance = new ValueSetting("Max Distance", "Maximum distance to target").setValue(3.0f).range(1f, 6f);

    ValueSetting extraDistance = new ValueSetting("Aim Extra Distance", "Extra distance added to rotation range").visible(() -> !aimMode.isSelected("Snap")).setValue(1.0f).range(0f, 6f);

    public BooleanSetting onlySpaceCrits = new BooleanSetting("Only Space Crits", "Crits only if the jump button is pressed").setValue(true).visible(() -> attackSetting.isSelected("Only Critical"));

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        setup(targetType, attackSetting, correctionGroup, aimMode, targetEspGroup,
                //aiGroup,
                sprintReset, maxDistance, extraDistance, onlySpaceCrits, multiPointMode);
        if(aimMode.isSelected("Snap")) {

            extraDistance.setValue(0.0F);

        }
    }

    @Override

    public void deactivate() {
        targetSelector.releaseTarget();
        target = null;
        super.deactivate();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onMoveInput(EventMoveInput1 e) {
        if (!isState() || target == null) return;
        if (!correctionGroup.isValue()) return;
        if (!correctionType.isSelected("Targeted")) return;

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        float targetYaw = (float) (MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));

        float currentYaw = mc.player.getYaw();

        float delta = MathHelper.wrapDegrees(targetYaw - currentYaw);
        double rad = Math.toRadians(delta);

        float f = e.getForward();
        float s = e.getStrafe();
        float rotatedForward = (float)(f * Math.cos(rad) - s * Math.sin(rad));
        float rotatedStrafe  = (float)(f * Math.sin(rad) + s * Math.cos(rad));

        e.setForward(rotatedForward);
        e.setStrafe(rotatedStrafe);

        if (rotatedForward > 0.0f && mc.player != null && !sprintReset.isValue() && correctionType.isSelected("Targeted")) {
            mc.player.setSprinting(true);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        esp_anim.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);
        float anim = esp_anim.getOutput().floatValue();
        if (targetEspGroup.isValue() && lastTarget != null && !esp_anim.isFinished(Direction.BACKWARDS)) {
            float red = MathHelper.clamp((lastTarget.hurtTime - tickCounter.getTickDelta(false)) / 10, 0, 1);
            switch (targetEspType.getSelected()) {
                case "Cube" -> {
                    switch (cubeType.getSelected()) {
                        case "Default" -> Render3DUtil.drawCube(lastTarget, anim, red);
                        case "Modern" -> Render3DUtil.drawCubeModern(lastTarget, anim, red);
                        case "Skid" -> Render3DUtil.drawCubeSkid(lastTarget, anim, red);
                    }
                }
                case "Circle" -> Render3DUtil.drawCircle(e.getStack(), lastTarget, anim, red);
                case "Ghosts" -> Render3DUtil.drawGhosts(lastTarget, anim, red, ghostSpeed.getValue());
                case "Crystal" -> Render3DUtil.drawCrystal(e.getStack(), lastTarget, anim, red, crystalSpeed.getValue());
            }
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 30) {
            Entity entity = status.getEntity(mc.world);
            if (entity != null && entity.equals(target) && Hud.getInstance().notificationSettings.isSelected("Break Shield")) {
                Notifications.getInstance().addList(Text.literal("Сломали щит игроку - ").append(entity.getDisplayName()), 3000);
            }
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent e) {
        switch (e.getType()) {
            case EventType.PRE -> {
                target = updateTarget();
                if (target != null) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                }
            }
            case EventType.POST -> {
                Render3DUtil.updateTargetEsp();
                if (target != null) Main.getInstance().getAttackPerpetrator().performAttack(getConfig());
            }
        }
    }

    private LivingEntity updateTarget() {
        TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(targetType.getSelected());
        float rotationRange = maxDistance.getValue() + extraDistance.getValue();
        targetSelector.searchTargets(mc.world.getEntities(), rotationRange, 360, attackSetting.isSelected("Ignore The Walls"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }
    @Native(type = Native.Type.VMProtectBeginUltra)
    private void rotateToTarget(AttackPerpetrator.AttackPerpetratorConfigurable config) {
        AttackHandler attackHandler = Main.getInstance().getAttackPerpetrator().getAttackHandler();
        RotationController controller = RotationController.INSTANCE;
        Angle.VecRotation rotation = new Angle.VecRotation(config.getAngle(), config.getAngle().toVector());
        RotationConfig rotationConfig = getRotationConfig();
        switch (aimMode.getSelected()) {
            case "Snap" -> {
                if (attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(100)) {
                    controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }
//            case "SpookyDuels" -> {
//                if (attackHandler.canAttack(config, 2) || !attackHandler.getAttackTimer().finished(100)) {
//                    controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
//                }
//            }
            case "FuntimeTest" -> {
                controller.rotateTo(rotation, target, 20, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }
            case "FuntimeSnap" -> {
                if (attackHandler.canAttack(config, 2)) {
                    controller.clear();
                    controller.rotateTo(rotation, target, 20, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }
            case "Universal", "HvH", "Trax", "Manda", "Matrix|Bypass" -> {
                controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }
            case "ReallyWorld"  , "FuntimeSmooth" -> {
                controller.rotateTo(rotation, target, 90, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }
            case "SpookyTime" -> {
                controller.rotateTo(rotation, target, 70, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }
        }
    }

    public AttackPerpetrator.AttackPerpetratorConfigurable getConfig() {
        Vec3d baseVelocity = getSmoothMode().randomValue();
        Vec3d modifiedVelocity = Vec3d.ZERO;


        Pair<Vec3d, Box> point = pointFinder.computeVector(target, maxDistance.getValue(), RotationController.INSTANCE.getRotation(), modifiedVelocity, attackSetting.isSelected("Ignore The Walls"));
        Angle angle = AngleUtil.fromVec3d(point.getLeft().subtract(Objects.requireNonNull(mc.player).getEyePos()));
        Box box = point.getRight();
        return new AttackPerpetrator.AttackPerpetratorConfigurable(target, angle, maxDistance.getValue(), attackSetting.getSelected(), aimMode, box, onlySpaceCrits.isValue());
    }

    public RotationConfig getRotationConfig() {
        return new RotationConfig(getSmoothMode(), correctionGroup.isValue(), correctionType.isSelected("Free"));
    }

    public AngleSmoothMode getSmoothMode() {
        return switch (aimMode.getSelected()) {
            case "FuntimeSmooth" -> new FunTimeTestSmoothMode();
            case "Universal" -> new UniversalSmoothMode();
            case "Snap" -> new SnapSmoothMode();
            case "ReallyWorld" -> new ReallyWorldSmoothMode();
            case "SpookyTime" -> new SpookyTimeTestSmoothMode();
            case "HvH" -> new HvHSmoothMode();
            case "FuntimeSnap" -> new FTSmoothMode();
            case "Trax" -> new TraxSmoothMode();
            case "Manda" -> new MandaSmoothMode();
            case "FuntimeTest" -> new FTSmoothMode();
            case "Matrix|Bypass" -> new MatrixBypassSmoothMode();
            default -> new ReallyWorldSmoothMode();
        };
    }
}