package wtf.dettex.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.entity.SimulatedPlayer;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;



public class ZakoMoment extends Module {
    private ValueSetting collisionRadius =new ValueSetting("Радиус","радиус бупаса").range(0.1f, 3.0f);
    private ValueSetting speed = new ValueSetting("Скорость","скорость бупаса").range(0.1f, 3.0f);
    private BooleanSetting onlyAura = new BooleanSetting("Ток с килкой", "робить тока с килькой").setValue(true);
    public ZakoMoment() {
        super("ZakoMoment",
                "Zako Moment",
                ModuleCategory.COMBAT);
        setup(collisionRadius,speed,onlyAura);
    }
    public static ZakoMoment getInstance() {
        return Instance.get(ZakoMoment.class);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void update(TickEvent eventUpdate){
        if(!Aura.getInstance().isState() &&onlyAura.isValue() ) return;

        applyCollisionSpeed();
    }
    private void applyCollisionSpeed() {
        Entity anyCollision = null;
        Box expandedBox = mc.player.getBoundingBox().expand(collisionRadius.getValue());

        for (Entity ent : mc.world.getEntities()) {
            if (ent == mc.player) continue;
            if ((ent instanceof LivingEntity || ent instanceof BoatEntity)
                    && expandedBox.intersects(ent.getBoundingBox()))  {
                anyCollision =ent;
                break;
            }
        }

        if (anyCollision!=null ) {
            Vec3d eyes = SimulatedPlayer.simulateLocalPlayer(2).pos.add(0, mc.player.getDimensions(mc.player.getPose()).eyeHeight(), 0);
            Angle angle = AngleUtil.fromVec3d(anyCollision.getPos().subtract(eyes));


//            double[] motion = calculateDirection(speed.getValue() * 2 * 0.04, Aura.getInstance().isState() ? angle.getYaw() : mc.player.getYaw());
            double[] motion = calculateDirection(speed.getValue() * 2 * 0.04, Aura.getInstance().isState() ? angle.getYaw():mc.player.getYaw());
            mc.player.addVelocity(motion[0], 0.0, motion[1]);
            mc.player.addVelocity(motion [0], 0.0, motion [1]);
        }
    }

    public double[] calculateDirection(final double distance,float yaw) {
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;


        if (forward != 0.0f) {
            if (sideways > 0.0f) {
                yaw += (forward > 0.0f) ? -45 : 45;
            } else if (sideways < 0.0f) {
                yaw += (forward > 0.0f) ? 45 : -45;
            }
            sideways = 0.0f;
            forward = (forward > 0.0f) ? 1.0f : -1.0f;
        }

        double sinYaw = Math.sin(Math.toRadians(yaw + 90.0f));
        double cosYaw = Math.cos(Math.toRadians(yaw + 90.0f));
        double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
        double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;

        return new double[]{xMovement, zMovement};
    }
    public double[] calculateDirection(final double distance) {
        return calculateDirection(distance,mc.player.getYaw());
    }
}