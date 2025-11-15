package wtf.dettex.implement.screen.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.QuickImports;
import wtf.dettex.implement.features.altmanager.AltManagerScreen;

import java.util.ArrayList;
import java.util.List;

public class CustomMainMenu extends Screen implements QuickImports {
    private static final Identifier CUSTOM_BACKGROUND = Identifier.of("minecraft", "textures/gui/mainmenu.png");

    private static Identifier resolvedBackground;
    private static boolean resourcesChecked;

    private final List<AbstractCustomButton> buttons = new ArrayList<>();

    private final AbstractCustomButton singleplayer = new CustomButton("Singleplayer", () -> mc.setScreen(new SelectWorldScreen(this)));
    private final AbstractCustomButton multiplayer = new CustomButton("Multiplayer", () -> mc.setScreen(new MultiplayerScreen(this)));
    private final AbstractCustomButton accounts = new CustomButton("Accounts", () -> mc.setScreen(new AltManagerScreen()));
    private final AbstractCustomButton options = new CustomButton("Options", () -> mc.setScreen(new OptionsScreen(this, mc.options)));
    private final AbstractCustomButton exit = new CustomButton("Exit", MinecraftClient.getInstance()::scheduleStop);

    public CustomMainMenu() {
        super(Text.translatable("menu.dettex.title"));
        buttons.add(singleplayer);
        buttons.add(multiplayer);
        buttons.add(accounts);
        buttons.add(options);
        buttons.add(exit);
    }

    private void positionButtons() {
        int screenWidth = this.width;
        int screenHeight = this.height;

        int centerX = screenWidth / 2;
        int buttonHeight = 27;
        int buttonSpacing = 4;
        int mainButtonWidth = 140;
        int totalHeight = buttonHeight * 3 + buttonSpacing * 2;
        int startY = (screenHeight - totalHeight) / 2;

        singleplayer.position(centerX - mainButtonWidth / 2, startY).size(mainButtonWidth, buttonHeight);
        multiplayer.position(centerX - mainButtonWidth / 2, startY + buttonHeight + buttonSpacing).size(mainButtonWidth, buttonHeight);
        accounts.position(centerX - mainButtonWidth / 2, startY + (buttonHeight + buttonSpacing) * 2).size(mainButtonWidth, buttonHeight);

        int bottomY = startY + (buttonHeight + buttonSpacing) * 3 + buttonSpacing;
        int auxButtonHeight = buttonHeight;
        int auxSpacing = Math.max(2, buttonSpacing / 2);
        int halfWidth = (mainButtonWidth - auxSpacing) / 2;

        int auxStartX = centerX - mainButtonWidth / 2;

        options.position(auxStartX, startY + (buttonHeight + buttonSpacing) * 3).size(halfWidth, auxButtonHeight);
        exit.position(auxStartX + halfWidth + auxSpacing, startY + (buttonHeight + buttonSpacing) * 3).size(halfWidth, auxButtonHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        positionButtons();

        renderTitleBackground(context, this.width, this.height);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        int centerX = this.width / 2;
        int buttonHeight = 27;
        int buttonSpacing = 8;
        int totalHeight = buttonHeight * 3 + buttonSpacing * 2;
        int startY = (this.height - totalHeight) / 2;
        int titleY = startY - 16;

        Fonts.getSize(20, Fonts.Type.NEW).drawCenteredString(context.getMatrices(), "Dettex Client", centerX, titleY, -1);

        buttons.forEach(button -> button.render(context, mouseX, mouseY, delta));
        super.render(context, mouseX, mouseY, delta);
    }

    public static void renderTitleBackground(DrawContext context, int width, int height) {
        ensureResourcesLoaded();
        image.setTexture(resolvedBackground.toString()).render(
                ShapeProperties.create(context.getMatrices(), 0, 0, width, height).build()
        );
    }

    private static void ensureResourcesLoaded() {
        if (resourcesChecked) {
            return;
        }

        resolvedBackground = CUSTOM_BACKGROUND;
        resourcesChecked = true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (AbstractCustomButton titleButton : buttons) {
            if (titleButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
