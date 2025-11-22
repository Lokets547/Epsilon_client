package wtf.dettex.implement.features.draggables;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import wtf.dettex.api.other.draggable.AbstractDraggable;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.Direction;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.common.util.render.Render2DUtil;
import wtf.dettex.modules.impl.render.Hud;

import java.util.*;

public class StaffList extends AbstractDraggable {
    public static StaffList getInstance() {
        return Instance.getDraggable(StaffList.class);
    }

    public final Map<PlayerListEntry, Animation> list = new HashMap<>();
    private final List<String> staffPrefix = List.of("helper","moder","staff","admin","curator","стажёр", "staff", "сотрудник", "помощник", "админ", "модер");

    public StaffList() {
        super("Staff List", 130, 40, 80, 23,true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        Collection<PlayerListEntry> playerList = Objects.requireNonNull(mc.player).networkHandler.getPlayerList();
        for (PlayerListEntry entry : playerList) {
            GameProfile profile = entry.getProfile();
            Text displayName = entry.getDisplayName();
            if (displayName == null || profile == null) continue;
            String prefix = displayName.getString().replace(profile.getName(), "");
            if (prefix.length() < 2) continue;

            // Remove all color codes (§ followed by any character)
            String cleanPrefix = prefix.replaceAll("§.", "");
            
            PlayerListEntry player = new PlayerListEntry(profile, false);
            player.setDisplayName(displayName);

            if (list.keySet().stream().noneMatch(p -> Objects.equals(p.getDisplayName(), player.getDisplayName()))) {
                staffPrefix.stream().filter(s -> cleanPrefix.toLowerCase().contains(s)).findFirst().ifPresent(s -> {
                    list.put(player, new DecelerateAnimation().setMs(150).setValue(1));
                    if (Hud.getInstance().notificationSettings.isSelected("Staff Join")) {
                        Notifications.getInstance().addList(Text.empty().append(player.getDisplayName()).append(" - Зашел на сервер!"),5000);
                    }
                });
            }
        }
        list.entrySet().stream().filter(s -> playerList.stream().noneMatch(p -> Objects.equals(s.getKey().getDisplayName(), p.getDisplayName()))).forEach(s -> s.getValue().setDirection(Direction.BACKWARDS));
        list.values().removeIf(s -> s.isFinished(Direction.BACKWARDS));
        super.tick();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();

        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontPlayer = Fonts.getSize(13, Fonts.Type.DEFAULT);

        blurGlass.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 17.5F)
                .round(4,0,4,0).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        blurGlass.render(ShapeProperties.create(matrix, getX(), getY() + 17, getWidth(), getHeight() - 17)
                .round(0,4,0,4).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        float centerX = getX() + getWidth() / 2.0F;
        font.drawCenteredString(matrix, getName(), centerX, getY() + 7, ColorUtil.getText());

        int offset = 23;
        int maxWidth = 80;

        for (Map.Entry<PlayerListEntry, Animation> staff : list.entrySet()) {
            PlayerListEntry player = staff.getKey();

            if (player == null) continue;

            Text text = player.getDisplayName();
            float centerY = getY() + offset;
            float width = fontPlayer.getStringWidth(text) + 25;
            float animation = staff.getValue().getOutput().floatValue();

            MathUtil.scale(matrix, centerX, centerY, 1, animation, () -> {
                Render2DUtil.drawTexture(context, player.getSkinTextures().texture(), getX() + 5, centerY - 1, 7, 3.5F, 8, 8, 64,ColorUtil.getRect(1));
                rectangle.render(ShapeProperties.create(matrix, getX() + 15.5F, centerY - 1, 0.5F, 7).color(ColorUtil.getOutline(1, 0.5F)).build());
                fontPlayer.drawText(matrix, text, getX() + 20, centerY + 1);
            });

            offset += (int) (11 * animation);
            maxWidth = (int) Math.max(width, maxWidth);
        }

        setWidth(maxWidth);
        setHeight(offset);
    }
}

