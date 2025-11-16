package wtf.dettex.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.player.AttackEvent;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Criticals extends Module {
    public static Criticals getInstance() {
        return Instance.get(Criticals.class);
    }

    SelectSetting mode = new SelectSetting("Mode", "Select bypass mode").value("Grim Old", "Grim New");
    BooleanSetting debug = new BooleanSetting("Debug", "Show debug messages").setValue(false);

    public Criticals() {
        super("Criticals", ModuleCategory.COMBAT);
        setup(mode, debug);
    }

    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onAttack(AttackEvent e) {
        if (mc.player.isTouchingWater()) return;
        if (mode.isSelected("Grim Old")) {
            if (!mc.player.isOnGround() && mc.player.fallDistance == 0) {
                PlayerIntersectionUtil.grimSuperBypass$$$(-(mc.player.fallDistance = MathUtil.getRandom(1e-5F, 1e-4F)), RotationController.INSTANCE.getRotation().random(1e-3F));
            }
        }
        
        if (mode.isSelected("Grim New")) {
            holyWorld5Criticals();
        }
    }
    
    void holyWorld5Criticals() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        
        if (!mc.player.isOnGround() && !mc.player.isTouchingWater()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                x, 
                y - 0.08f + ThreadLocalRandom.current().nextFloat(0.05f, 0.06f), 
                z, 
                yaw, 
                pitch, 
                false, 
                mc.player.horizontalCollision
            ));
            
            if (debug.isValue()) {
                logDirect("крит прошел");
            }
            
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                x, 
                y - 0.07f + ThreadLocalRandom.current().nextFloat(0.05f, 0.06f), 
                z, 
                yaw, 
                pitch, 
                false, 
                mc.player.horizontalCollision
            ));
            
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                x, 
                y - 0.09f + ThreadLocalRandom.current().nextFloat(0.06f, 0.07f), 
                z, 
                yaw, 
                pitch, 
                false, 
                mc.player.horizontalCollision
            ));
            
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                x, 
                y - 0.1f + ThreadLocalRandom.current().nextFloat(0.07f, 0.08f), 
                z, 
                yaw, 
                pitch, 
                false, 
                mc.player.horizontalCollision
            ));
            
            mc.player.fallDistance = 0.1f;
        }
    }
}