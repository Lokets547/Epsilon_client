package wtf.dettex.implement.screen.mainmenu;

import wtf.dettex.common.QuickImports;

public abstract class AbstractCustomButton implements QuickImports {
    protected final String name;
    protected final Runnable action;

    protected int x;
    protected int y;
    protected int width;
    protected int height;

    protected AbstractCustomButton(String name, Runnable action) {
        this.name = name;
        this.action = action;
    }

    public AbstractCustomButton size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public AbstractCustomButton position(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public abstract void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta);

    public abstract boolean mouseClicked(double mouseX, double mouseY, int button);
}

