package wtf.dettex.implement.screen.menu.components.implement.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import wtf.dettex.modules.setting.Setting;
import wtf.dettex.implement.screen.menu.components.AbstractComponent;

@Getter
@RequiredArgsConstructor
public abstract class AbstractSettingComponent extends AbstractComponent {
    private final Setting setting;

    protected float clipTop = Float.NEGATIVE_INFINITY;
    protected float clipBottom = Float.POSITIVE_INFINITY;

    public void setClipBounds(float clipTop, float clipBottom) {
        if (Float.isNaN(clipTop) || Float.isNaN(clipBottom) || clipBottom <= clipTop) {
            this.clipTop = Float.NEGATIVE_INFINITY;
            this.clipBottom = Float.POSITIVE_INFINITY;
            return;
        }
        this.clipTop = clipTop;
        this.clipBottom = clipBottom;
    }

    protected float getClampedClipTop(float defaultTop) {
        return Math.max(defaultTop, clipTop);
    }

    protected float getClampedClipBottom(float defaultBottom) {
        return Math.min(defaultBottom, clipBottom);
    }
}
