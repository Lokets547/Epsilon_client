package wtf.dettex.modules.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.task.TaskPriority;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationConfig;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Scaffold extends Module {
    ValueSetting blocksPerTick = new ValueSetting("Block/Tick", "Сколько блоков ставить за тик").setValue(1f).range(1f, 5f);
    ValueSetting placeDelay = new ValueSetting("Delay", "Задержка между попытками").setValue(1f).range(0f, 10f);

    SelectSetting rotateMode = new SelectSetting("Rotate", "Ротация при установке")
            .value("Off", "Client", "Controller").selected("Controller");
    BooleanSetting strictHit = new BooleanSetting("Strict Hit", "Точно в центр грани").setValue(true);

    @NonFinal int delay = 0;
    @NonFinal int savedSelectedSlot = -1;

    public Scaffold() {
        super("Scaffold", ModuleCategory.PLAYER);
        setup(blocksPerTick, placeDelay, rotateMode, strictHit);
    }

    @Override
     
    public void activate() {
        delay = 0;
        savedSelectedSlot = -1;
    }

    @EventHandler
     
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (delay > 0) {
            delay--;
            return;
        }

        int placed = 0;
        while (placed < Math.round(blocksPerTick.getValue())) {
            BlockPos target = getTargetPosUnderPlayer();
            if (target == null) break;
            if (placeBlockAt(target)) {
                placed++;
                delay = Math.round(placeDelay.getValue());
            } else {
                break;
            }
        }
    }

    private BlockPos getTargetPosUnderPlayer() {
        BlockPos feetBelow = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
        if (canPlaceAt(feetBelow)) return feetBelow;

        // Попробуем вокруг края: впереди, сзади, слева, справа от позиции под ногами
        BlockPos[] candidates = new BlockPos[] {
                feetBelow.add(1, 0, 0),
                feetBelow.add(-1, 0, 0),
                feetBelow.add(0, 0, 1),
                feetBelow.add(0, 0, -1)
        };
        for (BlockPos pos : candidates) {
            if (canPlaceAt(pos)) return pos;
        }
        return null;
    }

    private boolean canPlaceAt(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isReplaceable()) return false;
        // Требуем наличие хотя бы одного твёрдого соседа, чтобы клик был легитимным
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!mc.world.getBlockState(neighbor).isReplaceable()) {
                return true;
            }
        }
        return false;
    }

    private boolean placeBlockAt(BlockPos pos) {
        // Если уже держим блок в руке, не трогаем слот
        boolean holdingBlock = mc.player.getMainHandStack() != null && mc.player.getMainHandStack().getItem() instanceof BlockItem;
        Slot blockSlot = holdingBlock ? null : findPlaceableBlockSlot();
        if (!holdingBlock && blockSlot == null) return false;

        if (!holdingBlock) {
            saveSlot();
            PlayerInventoryUtil.swapHand(blockSlot, Hand.MAIN_HAND, false);
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!mc.world.getBlockState(neighbor).isReplaceable()) {
                Direction face = dir.getOpposite();
                Vec3d faceCenter = Vec3d.ofCenter(neighbor).add(face.getUnitVector().x * 0.5, face.getUnitVector().y * 0.5, face.getUnitVector().z * 0.5);

                Vec3d hit = faceCenter;
                if (!strictHit.isValue()) {
                    double jitter = 0.24;
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    double dx = face.getAxis() == Direction.Axis.X ? 0 : (rnd.nextDouble(-jitter, jitter));
                    double dy = face.getAxis() == Direction.Axis.Y ? 0 : (rnd.nextDouble(-jitter, jitter));
                    double dz = face.getAxis() == Direction.Axis.Z ? 0 : (rnd.nextDouble(-jitter, jitter));
                    hit = hit.add(dx, dy, dz);
                }

                BlockHitResult bhr = new BlockHitResult(hit, face, neighbor, false);
                ActionResult result;

                if (!rotateMode.isSelected("Off")) {
                    Angle angle = AngleUtil.fromVec3d(hit.subtract(mc.player.getEyePos()));
                    if (rotateMode.isSelected("Controller")) {
                        RotationController.INSTANCE.rotateTo(angle, new RotationConfig(false, true), TaskPriority.HIGH_IMPORTANCE_1, this);
                    }
                    float oldYaw = mc.player.getYaw();
                    float oldPitch = mc.player.getPitch();
                    mc.player.setYaw(angle.getYaw());
                    mc.player.setPitch(angle.getPitch());
                    result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    mc.player.setYaw(oldYaw);
                    mc.player.setPitch(oldPitch);
                } else {
                    result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                }

                if (result.isAccepted()) {
                    if (!holdingBlock) returnSlot();
                    return true;
                }
            }
        }
        if (!holdingBlock) returnSlot();
        return false;
    }

    private Slot findPlaceableBlockSlot() {
        return PlayerInventoryUtil.getSlot((Slot s) -> {
            ItemStack stack = s.getStack();
            return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
        });
    }

    private void saveSlot() {
        if (savedSelectedSlot == -1) savedSelectedSlot = mc.player.getInventory().selectedSlot;
    }

    private void returnSlot() {
        if (savedSelectedSlot != -1) {
            mc.player.getInventory().selectedSlot = savedSelectedSlot;
            savedSelectedSlot = -1;
        }
    }
}

