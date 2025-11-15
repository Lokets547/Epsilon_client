package wtf.dettex.modules.impl.render;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.text.Text;
import org.joml.Vector4d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.math.ProjectionUtil;
import wtf.dettex.event.impl.render.DrawEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TNTTimer extends Module {

    ValueSetting sizeSetting = new ValueSetting("Tag Size", "Text size").setValue(13).range(10, 20);

    public TNTTimer() {
        super("TNTTimer", "TNTTimer", ModuleCategory.RENDER);
        setup(sizeSetting);
    }

    @EventHandler
     
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(sizeSetting.getInt(), Fonts.Type.DEFAULT);

        for (Entity entity : PlayerIntersectionUtil.streamEntities().toList()) {
            if (!(entity instanceof TntEntity tnt)) continue;

            Vector4d vec = ProjectionUtil.getVector4D(tnt);
            double centerX = ProjectionUtil.centerX(vec);
            double topY = vec.y - 2;

            int fuseTicks = Math.max(0, tnt.getFuse());
            float seconds = fuseTicks / 20.0f;
            Text text = Text.literal(String.format("%.1fs", seconds));

            drawText(matrix, text, centerX, topY, font);
        }
    }

    private void drawText(MatrixStack matrix, Text text, double startX, double startY, FontRenderer font) {
        int paddingX = 2;
        float paddingY = 0.75F;
        float height = font.getFont().getSize() / 1.5F;
        float width = font.getStringWidth(text);
        float posX = (float) (startX - width / 2);
        float posY = (float) startY - height;
        blur.render(ShapeProperties.create(matrix, posX - paddingX, posY - paddingY, width + paddingX * 2, height + paddingY * 2)
                .round(height / 4).color(ColorUtil.HALF_BLACK).build());
        font.drawText(matrix, text, posX, posY + 3);
    }
}
