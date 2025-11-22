package wtf.dettex.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import wtf.dettex.api.other.draggable.AbstractDraggable;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.modules.impl.render.Hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScoreBoard extends AbstractDraggable {
    private List<ScoreboardEntry> scoreboardEntries = new ArrayList<>();
    private ScoreboardObjective objective;

    public ScoreBoard() {
        super("Score Board", 10, 100, 100, 120,true);
    }

    @Override
    public boolean visible() {
        return !scoreboardEntries.isEmpty();
    }

    @Override
    public void tick() {
        objective = Objects.requireNonNull(mc.world).getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        scoreboardEntries = mc.world.getScoreboard().getScoreboardEntries(objective).stream().sorted(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER)).toList();
        
        // Управление анимацией в зависимости от наличия записей
        if (!scoreboardEntries.isEmpty()) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(16);
        MutableText text = Text.empty();
        Text mainText = objective != null ? objective.getDisplayName() : Text.empty();

        scoreboardEntries.forEach(entry -> text.append(Team.decorateName(Objects.requireNonNull(mc.world).getScoreboard().getScoreHolderTeam(entry.owner()), entry.name())).append("\n"));

        int padding = 3;
        int offsetText = 14;
        int width = (int) Math.max(font.getStringWidth(text) + padding * 2 + 1,100);

        blurGlass.render(ShapeProperties.create(matrix,getX(),getY(),getWidth(),offsetText)
                .round(4,0,4,0).thickness(2).softness(1).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());
        blurGlass.render(ShapeProperties.create(matrix,getX(),getY() + offsetText - 0.5F,getWidth(),getHeight() - offsetText)
                .quality(40).round(0,4,0,4).thickness(2).softness(1).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        font.drawText(matrix, mainText, (int) (getX() + (getWidth() - font.getStringWidth(mainText)) / 2),getY() + padding + 1.5F);
        font.drawText(matrix, text, getX() + padding,getY() + offsetText + padding);

        if (getX() > mc.getWindow().getScaledWidth() / 2) setX(getX() + getWidth() - width);
        setWidth(width);
        setHeight((int) (font.getStringHeight(text) / 2.16 + offsetText + padding));
    }
}

