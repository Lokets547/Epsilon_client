package wtf.dettex.modules.impl.render;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import java.util.Optional;
import org.joml.*;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.api.repository.friend.FriendUtils;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.world.ServerUtil;
import wtf.dettex.common.util.math.ProjectionUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.common.util.render.Render2DUtil;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.event.impl.render.DrawEvent;
import wtf.dettex.event.impl.render.WorldLoadEvent;
import wtf.dettex.event.impl.render.WorldRenderEvent;
import wtf.dettex.modules.impl.combat.AntiBot;

import java.lang.Math;
import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EntityESP extends Module {
    public static EntityESP getInstance() {
        return Instance.get(EntityESP.class);
    }
    Identifier TEXTURE = Identifier.of("textures/container.png");
    List<PlayerEntity> players = new ArrayList<>();
    Map<RegistryKey<Enchantment>, String> encMap = new HashMap<>();
    ValueSetting sizeSetting = new ValueSetting("Tag Size", "Tags size")
            .setValue(13).range(10, 20);
    public MultiSelectSetting entityType = new MultiSelectSetting("Entity Type", "Entity that will be displayed")
            .value("Player", "Item", "TNT");
    MultiSelectSetting playerSetting = new MultiSelectSetting("Player Settings", "Settings for players")
            .value("Box", "Armor", "Enchants", "Prefix", "Hand Items", "3D Box").visible(() -> entityType.isSelected("Player"));
    public SelectSetting boxType = new SelectSetting("Box Type", "Type of Box")
            .value("Corner", "Full").visible(() -> playerSetting.isSelected("Box"));
    public BooleanSetting boxOutline = new BooleanSetting("Outline", "Outline of box").visible(() -> playerSetting.isSelected("Box"));
    public ValueSetting boxAlpha = new ValueSetting("Alpha", "Box transparency")
            .setValue(1.0F).range(0.0F, 1.0F).visible(() -> playerSetting.isSelected("3D Box"));;

    public EntityESP() {
        super("EntityESP", "ESP", ModuleCategory.RENDER);
        setup(sizeSetting, entityType, playerSetting, boxType, boxOutline, boxAlpha);
        encMap.put(Enchantments.BLAST_PROTECTION, "B");
        encMap.put(Enchantments.PROTECTION, "P");
        encMap.put(Enchantments.SHARPNESS, "S");
        encMap.put(Enchantments.EFFICIENCY, "E");
        encMap.put(Enchantments.UNBREAKING, "U");
        encMap.put(Enchantments.POWER, "P");
        encMap.put(Enchantments.THORNS, "T");
        encMap.put(Enchantments.MENDING, "M");
        encMap.put(Enchantments.DEPTH_STRIDER, "D");
        encMap.put(Enchantments.QUICK_CHARGE, "Q");
        encMap.put(Enchantments.MULTISHOT, "MS");
        encMap.put(Enchantments.PIERCING, "P");
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        players.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        players.clear();
        if (mc.world != null) {
            mc.world.getPlayers().stream()
                    .filter(player -> player != mc.player)
                    .forEach(players::add);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!entityType.isSelected("Player")) return;
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        for (PlayerEntity player : players) {
            if (player == null) continue;
            double interpX = MathHelper.lerp(tickDelta, player.prevX, player.getX());
            double interpY = MathHelper.lerp(tickDelta, player.prevY, player.getY());
            double interpZ = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
            float interpYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevYaw, player.getYaw());
            Vec3d interpCenter = new Vec3d(interpX, interpY, interpZ);
            float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(interpCenter);
            if (distance < 1) continue;
            boolean friend = FriendUtils.isFriend(player);
            int baseColor = friend ? ColorUtil.getFriendColor() : ColorUtil.getClientColor();
            int alpha = (int) (boxAlpha.getValue() * 255);
            int fillColor = (baseColor & 0x00FFFFFF) | (alpha << 24);
            int outlineColor = baseColor | 0xFF000000;

            if (playerSetting.isSelected("3D Box")) {
                Box interpBox = player.getDimensions(player.getPose()).getBoxAt(interpX, interpY, interpZ);
                Box boxToRender = interpBox;
                Render3DUtil.drawBox(boxToRender, fillColor, 2, true, true, true); // Fill with alpha
                Render3DUtil.drawBox(boxToRender, outlineColor, 2); // Outline without alpha
            }

        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();
        blurGlass.setup();
        FontRenderer font = Fonts.getSize(sizeSetting.getInt(), Fonts.Type.DEFAULT);
        FontRenderer bigFont = Fonts.getSize(sizeSetting.getInt() + 2, Fonts.Type.DEFAULT);
        if (entityType.isSelected("Player")) {
            for (PlayerEntity player : players) {
                if (player == null) continue;
                Vector4d vec4d = ProjectionUtil.getVector4D(player);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(player.getBoundingBox().getCenter());
                boolean friend = FriendUtils.isFriend(player);
                if (distance < 1) continue;
                if (ProjectionUtil.cantSee(vec4d)) continue;
                if (playerSetting.isSelected("Box")) drawBox(friend, vec4d, player);
                if (playerSetting.isSelected("Armor")) drawArmor(context, player, vec4d, font);
                if (playerSetting.isSelected("Hand Items")) drawHands(matrix, player, font, vec4d);
                MutableText text = getTextPlayer(player, friend);
                if (ServerUtil.isAresMine()) {
                    float startX = (float) ProjectionUtil.centerX(vec4d);
                    float startY = (float) (vec4d.y);
                    float width = mc.textRenderer.getWidth(text);
                    float height = mc.textRenderer.fontHeight;
                    float posX = startX - width / 2f;
                    float posY = startY - 11F;
                    blurGlass.render(ShapeProperties.create(matrix,
                                    posX - 2f,
                                    posY - 0.75f,
                                    width + 4f,
                                    height + 1.5f)
                            .round(height / 4f)
                            .color(ColorUtil.getRect(Hud.newHudAlpha.getValue()))
                            .build());
                    context.drawText(mc.textRenderer, text, (int)posX, (int)posY + 1, ColorUtil.getColor(255), false);
                } else {
                    drawText(matrix, text, ProjectionUtil.centerX(vec4d), vec4d.y - 2, font);
                }
            }
        }
        List<Entity> entities = PlayerIntersectionUtil.streamEntities()
                .sorted(Comparator.comparing(ent -> ent instanceof ItemEntity item && item.getStack().getName().getContent().toString().equals("empty")))
                .toList();
        for (Entity entity : entities) {
            if (entity instanceof ItemEntity item && entityType.isSelected("Item")) {
                Vector4d vec4d = ProjectionUtil.getVector4D(entity);
                ItemStack stack = item.getStack();
                ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);
                List<ItemStack> list = compoundTag != null ? compoundTag.stream().toList() : List.of();
                if (ProjectionUtil.cantSee(vec4d)) continue;
                Text text = item.getStack().getName();
                if (stack.getCount() > 1) text = text.copy().append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                if (!list.isEmpty()) drawShulkerBox(context, stack, list, vec4d);
                else drawText(matrix, text, ProjectionUtil.centerX(vec4d), vec4d.y, text.getContent().toString().equals("empty") ? bigFont : font);
            }
        }
    }

    private void drawBox(boolean friend, Vector4d vec, PlayerEntity player) {
        if (playerSetting.isSelected("3D Box")) {
            return;
        }
        int client = friend ? ColorUtil.getFriendColor() : ColorUtil.getClientColor();
        int black = ColorUtil.HALF_BLACK;
        float posX = (float) vec.x;
        float posY = (float) vec.y;
        float endPosX = (float) vec.z;
        float endPosY = (float) vec.w;
        float size = (endPosX - posX) / 3;
        if (boxType.isSelected("Corner")) {
            Render2DUtil.drawQuad(posX - 0.5F, posY - 0.5F, size, 0.5F, client);
            Render2DUtil.drawQuad(posX - 0.5F, posY, 0.5F, size + 0.5F, client);
            Render2DUtil.drawQuad(posX - 0.5F, endPosY - size - 0.5F, 0.5F, size, client);
            Render2DUtil.drawQuad(posX - 0.5F, endPosY - 0.5F, size, 0.5F, client);
            Render2DUtil.drawQuad(endPosX - size + 1, posY - 0.5F, size, 0.5F, client);
            Render2DUtil.drawQuad(endPosX + 0.5F, posY, 0.5F, size + 0.5F, client);
            Render2DUtil.drawQuad(endPosX + 0.5F, endPosY - size - 0.5F, 0.5F, size, client);
            Render2DUtil.drawQuad(endPosX - size + 1, endPosY - 0.5F, size, 0.5F, client);
            if (boxOutline.isValue()) {
                Render2DUtil.drawQuad(posX - 1F, posY - 1, size + 1, 1.5F, black);
                Render2DUtil.drawQuad(posX - 1F, posY + 0.5F, 1.5F, size + 0.5F, black);
                Render2DUtil.drawQuad(posX - 1F, endPosY - size - 1, 1.5F, size, black);
                Render2DUtil.drawQuad(posX - 1F, endPosY - 1, size + 1, 1.5F, black);
                Render2DUtil.drawQuad(endPosX - size + 0.5F, posY - 1, size + 1, 1.5F, black);
                Render2DUtil.drawQuad(endPosX, posY + 0.5F, 1.5F, size + 0.5F, black);
                Render2DUtil.drawQuad(endPosX, endPosY - size - 1, 1.5F, size, black);
                Render2DUtil.drawQuad(endPosX - size + 0.5F, endPosY - 1, size + 1, 1.5F, black);
            }
        } else if (boxType.isSelected("Full")) {
            if (boxOutline.isValue()) {
                Render2DUtil.drawQuad(posX - 1F, posY - 1F, endPosX - posX + 2F, 1.5F, black);
                Render2DUtil.drawQuad(posX - 1F, posY - 1F, 1.5F, endPosY - posY + 2F, black);
                Render2DUtil.drawQuad(posX - 1F, endPosY - 1F, endPosX - posX + 2F, 1.5F, black);
                Render2DUtil.drawQuad(endPosX - 0.5F, posY - 1F, 1.5F, endPosY - posY + 2F, black);
            }
            Render2DUtil.drawQuad(posX - 0.5F, posY - 0.5F, endPosX - posX + 1F, 0.5F, client);
            Render2DUtil.drawQuad(posX - 0.5F, posY - 0.5F, 0.5F, endPosY - posY + 1F, client);
            Render2DUtil.drawQuad(posX - 0.5F, endPosY - 0.5F, endPosX - posX + 1F, 0.5F, client);
            Render2DUtil.drawQuad(endPosX, posY - 0.5F, 0.5F, endPosY - posY + 1F, client);
        }
    }

    private void drawArmor(DrawContext context, PlayerEntity player, Vector4d vec, FontRenderer font) {
        MatrixStack matrix = context.getMatrices();
        List<ItemStack> items = new ArrayList<>();
        player.getEquippedItems().forEach(s -> {if (!s.isEmpty()) items.add(s);});
        float posX = (float) (ProjectionUtil.centerX(vec) - items.size() * 5.5);
        float posY = (float) (vec.y - sizeSetting.getInt() / 1.5 - 15);
        float padding = 0.5F;
        float offset = -11;
        if (!items.isEmpty()) {
            matrix.push();
            matrix.translate(posX, posY, 0);
            blurGlass.render(ShapeProperties.create(matrix,
                            -padding,
                            -padding,
                            items.size() * 11 - 1 + padding * 2,
                            10 + padding * 2)
                    .round(2.5F)
                    .color(ColorUtil.getRect(Hud.newHudAlpha.getValue()))
                    .build());
            for (ItemStack stack : items) {
                offset += 11;
                Render2DUtil.defaultDrawStack(context, stack, offset, 0, false, false, 0.5F);
                if (playerSetting.isSelected("Enchants")) {
                    ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(stack);
                    float enchantY = -font.getFont().getSize() / 1.5F - 2;
                    for (Map.Entry<RegistryKey<Enchantment>, String> entry : encMap.entrySet()) {
                        RegistryKey<Enchantment> enchantment = entry.getKey();
                        String id = entry.getValue();
                        Optional<RegistryEntry<Enchantment>> registryEntry =
                                mc.world.getRegistryManager()
                                        .getOptional(RegistryKeys.ENCHANTMENT)
                                        .flatMap(registry -> registry.getEntry(enchantment.getValue()));
                        if (registryEntry.isPresent() && enchants.getEnchantments().contains(registryEntry.get())) {
                            int level = enchants.getLevel(registryEntry.get());
                            MutableText enchantText = Text.literal(id + level);
                            float textWidth = font.getStringWidth(enchantText);
                            float textX = offset + 9f - textWidth / 2;
                            float textY = enchantY + 8;
                            drawText(matrix, enchantText, textX, textY, font);
                            enchantY -= font.getFont().getSize() / 1.5F + 1;
                        }
                    }
                }
            }
            matrix.pop();
        }
    }

    private void drawHands(MatrixStack matrix, PlayerEntity player, FontRenderer font, Vector4d vec) {
        double posY = vec.w;
        for (ItemStack stack : player.getHandItems()) {
            if (stack.isEmpty()) continue;
            MutableText text = Text.empty().append(stack.getName());
            if (stack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
            posY += font.getStringHeight(text) / 2 + 3;
            drawText(matrix, text, ProjectionUtil.centerX(vec), posY, font);
        }
    }

    private void drawShulkerBox(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, Vector4d vec) {
        MatrixStack matrix = context.getMatrices();
        int width = 176;
        int height = 67;
        int color = ColorUtil.multBright(ColorUtil.replAlpha(((BlockItem) itemStack.getItem()).getBlock().getDefaultMapColor().color, 1F), 1);
        matrix.push();
        matrix.translate(ProjectionUtil.centerX(vec) - (double) width / 4, vec.w + 2, -200 + Math.cos(vec.x));
        matrix.scale(0.5F, 0.5F, 1);
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, 0, 0, width, height, width, height, color);
        int posX = 7;
        int posY = 6;
        for (ItemStack stack : stacks.stream().toList()) {
            Render2DUtil.defaultDrawStack(context, stack, posX, posY, false, true, 1);
            posX += 18;
            if (posX >= 165) {
                posY += 18;
                posX = 7;
            }
        }
        matrix.pop();
    }

    private void drawText(MatrixStack matrix, Text text, double startX, double startY, FontRenderer font) {
        int paddingX = 2;
        float paddingY = 0.75F;
        float height = font.getFont().getSize() / 1.5F;
        float width = font.getStringWidth(text);
        float posX = (float) (startX - width / 2);
        float posY = (float) startY - height;
        blurGlass.render(ShapeProperties.create(matrix, posX - paddingX, posY - paddingY, width + paddingX * 2, height + paddingY * 2)
                .round(height / 4).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());
        font.drawText(matrix, text, posX, posY + 3);
    }

    private MutableText getTextPlayer(PlayerEntity player, boolean friend) {
        float health = PlayerIntersectionUtil.getHealth(player);
        MutableText text = Text.empty();
        if (friend) text.append("[" + Formatting.GREEN + "F" + Formatting.RESET + "] ");
        if (AntiBot.getInstance().isBot(player)) text.append("[" + Formatting.DARK_RED + "BOT" + Formatting.RESET + "] ");
        if (playerSetting.isSelected("Prefix")) text.append(player.getDisplayName()); else text.append(player.getName());
        if (player.getOffHandStack().getItem().equals(Items.PLAYER_HEAD) || player.getOffHandStack().getItem().equals(Items.TOTEM_OF_UNDYING))
            text.append(Formatting.RESET + getSphere(player.getOffHandStack()));
        if (health >= 0 && health <= player.getMaxHealth())
            text.append(Formatting.RESET + " [" + Formatting.RED + PlayerIntersectionUtil.getHealthString(player) + Formatting.RESET + "]");
        return text;
    }

    private String getSphere(ItemStack stack) {
        var component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (ServerUtil.isFunTime() && component != null) {
            NbtCompound compound = component.copyNbt();
            if (compound.getInt("tslevel") != 0) {
                return " [" + Formatting.GOLD + compound.getString("don-item").replace("sphere-", "").toUpperCase() + Formatting.RESET + "]";
            }
        }
        return "";
    }

    private void drawRotatedBox(MatrixStack matrixStack, Box box, int color, float yaw) {
        Vec3d center = box.getCenter();
        Box rotatedBox = createRotatedBox(box, center, yaw);
        Render3DUtil.drawBox(rotatedBox, color, 2, true, true, true); // Ensure rendering through walls
    }

    private Box createRotatedBox(Box originalBox, Vec3d center, float yaw) {
        double radians = Math.toRadians(yaw);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double halfWidth = (originalBox.maxX - originalBox.minX) / 2.0;
        double halfHeight = (originalBox.maxY - originalBox.minY) / 2.0;
        double halfDepth = (originalBox.maxZ - originalBox.minZ) / 2.0;
        Vec3d[] corners = {
                new Vec3d(-halfWidth, -halfHeight, -halfDepth),
                new Vec3d(halfWidth, -halfHeight, -halfDepth),
                new Vec3d(halfWidth, -halfHeight, halfDepth),
                new Vec3d(-halfWidth, -halfHeight, halfDepth),
                new Vec3d(-halfWidth, halfHeight, -halfDepth),
                new Vec3d(halfWidth, halfHeight, -halfDepth),
                new Vec3d(halfWidth, halfHeight, halfDepth),
                new Vec3d(-halfWidth, halfHeight, halfDepth)
        };
        Vec3d[] rotatedCorners = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            Vec3d corner = corners[i];
            double newX = corner.x * cos - corner.z * sin;
            double newZ = corner.x * sin + corner.z * cos;
            rotatedCorners[i] = new Vec3d(newX, corner.y, newZ).add(center);
        }
        double minX = rotatedCorners[0].x;
        double minY = rotatedCorners[0].y;
        double minZ = rotatedCorners[0].z;
        double maxX = rotatedCorners[0].x;
        double maxY = rotatedCorners[0].y;
        double maxZ = rotatedCorners[0].z;
        for (Vec3d corner : rotatedCorners) {
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

}