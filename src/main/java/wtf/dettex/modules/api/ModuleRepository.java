package wtf.dettex.modules.api;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;


import wtf.dettex.modules.impl.combat.*;
import wtf.dettex.modules.impl.misc.*;
import wtf.dettex.modules.impl.movement.*;
import wtf.dettex.modules.impl.player.*;
import wtf.dettex.modules.impl.render.*;
import wtf.dettex.modules.impl.combat.*;
import wtf.dettex.modules.impl.misc.*;
import wtf.dettex.modules.impl.movement.*;
import wtf.dettex.modules.impl.player.*;
import wtf.dettex.modules.impl.render.*;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<Module> modules = new ArrayList<>();

    public void setup() {
        register(
                new ServerHelper(),
                new WaterSpeed(),
                new ClickAction(),
                new Particles(),
                new AutoPotion(),
                new ItemTweaks(),
                new SlotLocker(),
                new Hud(),
                new Trails(),
                new AuctionHelper(),
                new ProjectilePrediction(),
                new XRay(),
                new Aura(),
                new Velocity(),
                new AutoSwap(),
                new FireFlies(),
                new NoFriendDamage(),
                new HitBoxModule(),
                new AutoWindCharge(),
                new AutoWeb(),
                new AntiBot(),
                new AutoCrystal(),
                new AutoSprint(),
                new NoPush(),
                new ElytraHelper(),
                new ClanUpgrade(),
                new NoDelay(),
                new GrimElytra(),
                new FakeLag(),
                new AutoRespawn(),
                new NoSlow(),
                new Sneak(),
                new ScreenWalk(),
                new ElytraFly(),
                new Speed(),
                new Timer(),
                //new Phase(),
                new Fov(),
                new Blink(),
                new ElytraRecast(),
                new AutoTool(),
                new Nuker(),
                //new AirPlace(),
                new FastBreak(),
                new CameraTweaks(),
                new HandTweaks(),
//                new BetterMinecraft(),
                new BlockHighLight(),
                new EntityESP(),
                new TNTTimer(),
                new AutoTotem(),
                new Scaffold(),
                new EnderChestPlus(),
                new ServerUtilities(),
                new MultiActions(),
                new DebugCamera(),
                new TriggerBot(),
                new BowSpam(),
                new ContainerStealer(),
                new AutoTpAccept(),
                new Arrows(),
                new AutoLeave(),
                new WorldTweaks(),
                new ItemPhysic(),
                new NoRender(),
                new PerfectDelay(),
                new HitColor(),
                new IRC(),
                new Criticals(),
                new TargetPearl(),
                new BedrockClip(),
                new NameProtect(),
                new AbilitiesFly(),
                new SeeInvisible(),
                new AutoArmor(),
                new AutoUse(),
                new AirJump(),
                new HighJump(),
                new NoInteract(),
                new CrossHair(),
                new FireWorkBooster(),
                new Spider(),
                new ServerRPSpoofer(),
                new JumpCircle(),
                new ZakoMoment(),
                new WaterLeave(),
                new SPDuelsJoiner(),
                new NoFall(),
                new AirStuck(),
                //new ChinaHat(),
                new AimBot(),
                new ClickGUI(),
                new SkeletonESP(),
                new ElytraMotion()
        );
    }

    
    public void register(Module... module) {
        modules.addAll(List.of(module));
    }

    public List<Module> modules() {
        return modules;
    }
}
