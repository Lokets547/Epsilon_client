package wtf.dettex.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.common.util.task.TaskPriority;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.event.impl.render.WorldRenderEvent;
import wtf.dettex.api.repository.friend.FriendUtils;
import wtf.dettex.modules.setting.implement.*;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationConfig;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;
import wtf.dettex.modules.setting.implement.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoWeb extends Module {
    // Settings
    ValueSetting range = new ValueSetting("Range", "Дистанция поиска цели").setValue(5f).range(1f, 7f);
    ValueSetting placeWallRange = new ValueSetting("WallRange", "Дистанция через стену").setValue(5f).range(1f, 7f);

    SelectSetting placeTiming = new SelectSetting("PlaceTiming", "Режим установки").value("Default", "Vanilla").selected("Default");
    ValueSetting blocksPerTick = new ValueSetting("Block/Tick", "Количество блоков за тик").setValue(8f).range(1f, 12f)
            .visible(() -> placeTiming.isSelected("Default"));
    ValueSetting placeDelay = new ValueSetting("Delay/Place", "Задержка между попытками").setValue(3f).range(0f, 10f);

    BooleanSetting head = new BooleanSetting("Head", "Голова").setValue(true);
    BooleanSetting legs = new BooleanSetting("Legs", "Ноги").setValue(true);
    BooleanSetting surround = new BooleanSetting("Surround", "Кольцо вокруг").setValue(true);
    BooleanSetting upperSurround = new BooleanSetting("UpperSurround", "Верхнее кольцо").setValue(false);
    GroupSetting selectionGroup = new GroupSetting("Selection", "Какие позиции заполнять")
            .settings(head, legs, surround, upperSurround).setValue(true);

    // Rotation/Placement behavior
    SelectSetting rotateMode = new SelectSetting("Rotate", "Ротация перед установкой")
            .value("Off", "Client", "Controller").selected("Controller");
    BooleanSetting strictHit = new BooleanSetting("Strict Hit", "Бить строго в центр грани").setValue(true);
    GroupSetting rotateGroup = new GroupSetting("Placement", "Логика поворота и попадания")
            .settings(rotateMode, strictHit).setValue(true);

    SelectSetting renderMode = new SelectSetting("Render Mode", "Режим отрисовки эффекта").value("Fade", "Decrease").selected("Fade");
    ColorSetting renderFillColor = new ColorSetting("Render Fill Color", "Цвет заливки")
            // Use a safe constant at construction time to avoid accessing Hud/ModuleProvider too early
            .setColor(0xFF6C9AFD)
            .presets(0xFF6C9AFD, 0xFF8C7FFF, 0xFFFFA576, 0xFFFF7B7B);
    ColorSetting renderLineColor = new ColorSetting("Render Line Color", "Цвет контура")
            .setColor(ColorUtil.getOutline())
            .presets(0xFFFFFFFF, ColorUtil.getOutline(), 0xFF000000, 0xFF6C9AFD);
    ValueSetting renderLineWidth = new ValueSetting("Render Line Width", "Толщина контура").setValue(2f).range(1f, 5f);
    ValueSetting effectDurationMs = new ValueSetting("Effect Duration (MS)", "Длительность эффекта").setValue(500f).range(0f, 10000f);
    GroupSetting renderGroup = new GroupSetting("Render", "Отрисовка")
            .settings(renderMode, renderFillColor, renderLineColor, renderLineWidth, effectDurationMs).setValue(true);

    // Target filters
    BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", "Не целиться в друзей").setValue(true);

    // Runtime state
    ArrayList<BlockPos> sequentialBlocks = new ArrayList<>();
    Map<BlockPos, Long> renderPoses = new ConcurrentHashMap<>();

    @NonFinal int delay = 0;
    @NonFinal int savedSelectedSlot = -1;

    public AutoWeb() {
        super("AutoWeb", ModuleCategory.COMBAT);
        setup(range, placeWallRange, placeTiming, blocksPerTick, placeDelay, selectionGroup, rotateGroup, renderGroup, ignoreFriends);
    }

    @Override
    public void activate() {
        sequentialBlocks.clear();
        renderPoses.clear();
        savedSelectedSlot = -1;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        long dur = (long) effectDurationMs.getValue();
        renderPoses.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > dur);
        renderPoses.forEach((pos, time) -> {
            long dt = System.currentTimeMillis() - time;
            int baseFill = renderFillColor.getColor();
            int baseLine = renderLineColor.getColor();
            float k = 1f - Math.min(1f, dt / 500f);
            int fill = ColorUtil.multAlpha(baseFill, k);
            int line = ColorUtil.multAlpha(baseLine, k);
            switch (renderMode.getSelected()) {
                case "Fade" -> {
                    // Fill
                    Render3DUtil.drawBox(new Box(pos), fill, Math.round(renderLineWidth.getValue()), false, true, true);
                    // Line
                    Render3DUtil.drawBox(new Box(pos), line, Math.round(renderLineWidth.getValue()), true, false, true);
                }
                case "Decrease" -> {
                    float scale = Math.max(0f, 1f - (dt / 500f));
                    Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ())
                            .shrink(scale, scale, scale)
                            .offset(0.5 + scale * 0.5, 0.5 + scale * 0.5, 0.5 + scale * 0.5);
                    Render3DUtil.drawBox(box, fill, Math.round(renderLineWidth.getValue()), false, true, true);
                    Render3DUtil.drawBox(box, line, Math.round(renderLineWidth.getValue()), true, false, true);
                }
            }
        });
    }

    @EventHandler

    public void onTick(TickEvent e) {
        BlockPos first = getSequentialPos();
        if (first == null) return;

        if (delay > 0) {
            delay--;
            return;
        }

        saveSlot();
        if (placeTiming.isSelected("Default")) {
            int placed = 0;
            while (placed < Math.round(blocksPerTick.getValue())) {
                BlockPos bp = getSequentialPos();
                if (bp == null) break;
                if (placeCobweb(bp)) {
                    placed++;
                    renderPoses.put(bp, System.currentTimeMillis());
                    delay = Math.round(placeDelay.getValue());
                } else break;
            }
        } else {
            BlockPos bp = getSequentialPos();
            if (bp != null && placeCobweb(bp)) {
                sequentialBlocks.add(bp);
                renderPoses.put(bp, System.currentTimeMillis());
                delay = Math.round(placeDelay.getValue());
            }
        }
        returnSlot();
    }

    private BlockPos getSequentialPos() {
        PlayerEntityNearest nearest = findNearestTarget(range.getValue());
        if (nearest == null) return null;

        BlockPos targetBp = BlockPos.ofFloored(nearest.pos);
        ArrayList<BlockPos> positions = new ArrayList<>();
        if (legs.isValue()) positions.add(targetBp);
        if (head.isValue()) positions.add(targetBp.up());
        if (surround.isValue()) {
            positions.add(targetBp.east());
            positions.add(targetBp.west());
            positions.add(targetBp.south());
            positions.add(targetBp.north());
        }
        if (upperSurround.isValue()) {
            positions.add(targetBp.east().up());
            positions.add(targetBp.west().up());
            positions.add(targetBp.south().up());
            positions.add(targetBp.north().up());
        }

        for (BlockPos bp : positions) {
            // Wall check
            BlockHitResult wallCheck = mc.world.raycast(new RaycastContext(
                    mc.player.getEyePos(), bp.toCenterPos().offset(Direction.UP, 0.5f),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && !wallCheck.getBlockPos().equals(bp)) {
                if (mc.player.getEyePos().squaredDistanceTo(bp.toCenterPos()) > placeWallRange.getValue() * placeWallRange.getValue())
                    continue;
            }
            if (canPlaceAt(bp)) {
                return bp;
            }
        }
        return null;
    }

    private boolean canPlaceAt(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isReplaceable();
    }

    private boolean placeCobweb(BlockPos pos) {
        Slot webSlot = PlayerInventoryUtil.getSlot(Blocks.COBWEB.asItem());
        if (webSlot == null) return false;

        // Swap to cobweb in main hand
        PlayerInventoryUtil.swapHand(webSlot, Hand.MAIN_HAND, false);

        // Try all neighbor faces to place at pos
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.offset(side.getOpposite());
            // Aim to center of the target face (strict) or general center
            Vec3d faceCenter = Vec3d.ofCenter(neighbor).add(side.getUnitVector().x * 0.5, side.getUnitVector().y * 0.5, side.getUnitVector().z * 0.5);
            Vec3d hit = strictHit.isValue() ? faceCenter : Vec3d.ofCenter(neighbor);
            BlockHitResult bhr = new BlockHitResult(hit, side, neighbor, false);
            ActionResult result;

            if (!rotateMode.isSelected("Off")) {
                // Compute angle to the hit point from eye
                Angle angle = AngleUtil.fromVec3d(hit.subtract(mc.player.getEyePos()));
                if (rotateMode.isSelected("Controller")) {
                    // Schedule rotation via controller for consistency, but also set client angles for immediate placement
                    RotationController.INSTANCE.rotateTo(angle, new RotationConfig(false, true), TaskPriority.HIGH_IMPORTANCE_1, this);
                }
                float oldYaw = mc.player.getYaw();
                float oldPitch = mc.player.getPitch();
                mc.player.setYaw(angle.getYaw());
                mc.player.setPitch(angle.getPitch());
                result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                // Restore
                mc.player.setYaw(oldYaw);
                mc.player.setPitch(oldPitch);
            } else {
                result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            }
            if (result.isAccepted()) {
                return true;
            }
        }
        return false;
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

    private record PlayerEntityNearest(Vec3d pos, double distSq) {}

    private PlayerEntityNearest findNearestTarget(float maxRange) {
        double maxSq = maxRange * maxRange;
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(p -> !p.isSpectator() && p.isAlive())
                .filter(p -> !(ignoreFriends.isValue() && FriendUtils.isFriend(p)))
                .filter(p -> p.squaredDistanceTo(mc.player) <= maxSq)
                .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(mc.player)))
                .map(p -> new PlayerEntityNearest(p.getPos(), p.squaredDistanceTo(mc.player)))
                .orElse(null);
    }
}

