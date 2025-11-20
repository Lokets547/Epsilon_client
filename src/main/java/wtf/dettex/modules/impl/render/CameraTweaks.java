package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.event.impl.keyboard.HotBarScrollEvent;
import wtf.dettex.event.impl.keyboard.KeyEvent;
import wtf.dettex.event.impl.keyboard.MouseRotationEvent;
import wtf.dettex.event.impl.render.AspectRatioEvent;
import wtf.dettex.event.impl.render.CameraEvent;
import wtf.dettex.event.impl.render.FovEvent;
import wtf.dettex.modules.setting.implement.BindSetting;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.GroupSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CameraTweaks extends Module {
    private float fov = 110, smoothFov = 30, lastChangedFov = 30;
    private Perspective perspective;
    private Angle angle;

    private final ValueSetting ratioSetting = new ValueSetting("Ratio", "Aspect Ratio value setting")
            .setValue(1.0F).range(0.1F, 2.0F);
    private final GroupSetting ratioGroup = new GroupSetting("Aspect Ratio", "Change screen resolution")
            .settings(ratioSetting).setValue(true);
    private final BooleanSetting clipSetting = new BooleanSetting("Camera Clip", "The camera passes through the blocks").setValue(true);
    private final ValueSetting distanceSetting = new ValueSetting("Camera Distance", "Camera distance value setting")
            .setValue(3.0F).range(2.0F, 5.0F);
    private final BindSetting zoomSetting = new BindSetting("Zoom", "Key to zoom in camera");
    private final BindSetting freeLookSetting = new BindSetting("Free Look", "Key to free look");

    public CameraTweaks() {
        super("CameraTweaks", "Camera Tweaks", ModuleCategory.RENDER);
        setup(ratioGroup, clipSetting, distanceSetting, zoomSetting, freeLookSetting);
    }

    @EventHandler
     
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(zoomSetting.getKey())) {
            fov = Math.min(lastChangedFov, mc.options.getFov().getValue() - 20);
        }
        if (e.isKeyReleased(zoomSetting.getKey(), true)) {
            lastChangedFov = fov;
            fov = mc.options.getFov().getValue();
        }
        if (e.isKeyDown(freeLookSetting.getKey())) {
            perspective = mc.options.getPerspective();
        }
    }

    @EventHandler
     
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (PlayerIntersectionUtil.isKey(zoomSetting)) {
            fov = (int) MathHelper.clamp(fov - e.getVertical() * 10,10, mc.options.getFov().getValue());
            e.cancel();
        }
    }

    @EventHandler
     
    public void onFov(FovEvent e) {
        if (PlayerIntersectionUtil.isKey(freeLookSetting)) {
            if (mc.options.getPerspective().isFirstPerson()) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        } else if (perspective != null) {
            mc.options.setPerspective(perspective);
            perspective = null;
        }
        e.setFov((int) MathHelper.clamp((smoothFov = MathUtil.interpolateSmooth(1.6, smoothFov, fov)) + 1, 10, mc.options.getFov().getValue()));
        e.cancel();
    }

    @EventHandler
     
    public void onMouseRotation(MouseRotationEvent e) {
        if (PlayerIntersectionUtil.isKey(freeLookSetting)) {
            angle.setYaw(angle.getYaw() + e.getCursorDeltaX() * 0.15F);
            angle.setPitch(MathHelper.clamp(angle.getPitch() + e.getCursorDeltaY() * 0.15F, -90F, 90F));
            e.cancel();
        } else angle = AngleUtil.cameraAngle();
    }

    @EventHandler
     
    public void onCamera(CameraEvent e) {
        e.setCameraClip(clipSetting.isValue());
        e.setDistance(distanceSetting.getValue());
        e.setAngle(angle);
        e.cancel();
    }

    @EventHandler
     
    public void onAspectRatio(AspectRatioEvent e) {
        if (ratioGroup.isValue()) {
            e.setRatio(ratioSetting.getValue());
            e.cancel();
        }
    }
}
