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
import net.minecraft.util.Formatting;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.Main;
import wtf.dettex.modules.impl.render.Hud;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.implement.features.draggables.MediaPlayer;
import wtf.dettex.common.QuickImports;
import wtf.dettex.implement.features.altmanager.AltManagerScreen;
import wtf.dettex.implement.features.changelog.ChangeLogScreen;

import java.util.ArrayList;
import java.util.List;

public class CustomMainMenu extends Screen implements QuickImports {
    private static final Identifier[] BACKGROUNDS = new Identifier[] {
            Identifier.of("minecraft", "textures/gui/mainmenu.png"),
            Identifier.of("minecraft", "textures/gui/mainmenu1.png"),
            Identifier.of("minecraft", "textures/gui/mainmenu2.png"),
            Identifier.of("minecraft", "textures/gui/mainmenu3.png")
    };

    private static Identifier resolvedBackground;
    private static int currentBackgroundIndex = 0;
    private static boolean resourcesChecked;

    private final List<AbstractCustomButton> buttons = new ArrayList<>();
    private AltManagerScreen altOverlay;

    private final AbstractCustomButton singleplayer = new CustomButton("Singleplayer", () -> mc.setScreen(new SelectWorldScreen(this)));
    private final AbstractCustomButton multiplayer = new CustomButton("Multiplayer", () -> mc.setScreen(new MultiplayerScreen(this)));
    private final AbstractCustomButton accounts = new CustomButton("Alt Manager", this::toggleAltOverlay);
    private final AbstractCustomButton options = new CustomButton("Options", () -> mc.setScreen(new OptionsScreen(this, mc.options)));
    private final AbstractCustomButton exit = new CustomButton("Exit", MinecraftClient.getInstance()::scheduleStop);
    private final AbstractCustomButton guiToggle = new CustomButton("Switch GUI", this::toggleBackground);
    private final AbstractCustomButton changeLog = new CustomButton("ChangeLog", () -> mc.setScreen(new ChangeLogScreen()));

    public CustomMainMenu() {
        super(Text.translatable("menu.dettex.title"));
        buttons.add(singleplayer);
        buttons.add(multiplayer);
        buttons.add(guiToggle);
        buttons.add(changeLog);
        buttons.add(accounts);
        buttons.add(options);
        buttons.add(exit);
    }

    private void toggleBackground() {
        currentBackgroundIndex = (currentBackgroundIndex + 1) % BACKGROUNDS.length;
        resolvedBackground = BACKGROUNDS[currentBackgroundIndex];
    }

    private void toggleAltOverlay() {
        if (altOverlay == null || altOverlay.isClosed()) {
            altOverlay = new AltManagerScreen().setEmbedded(true);
            altOverlay.setSize(this.width, this.height);
        } else {
            altOverlay.close();
        }
    }

    private void positionButtons() {
        int screenWidth = this.width;
        int screenHeight = this.height;

        int centerX = screenWidth / 2;
        int buttonHeight = 27;
        int buttonSpacing = 8;
        int mainButtonWidth = 180;
        int totalHeight = buttonHeight * 4 + buttonSpacing * 3;
        int startY = (screenHeight - totalHeight) / 2;

        // Place the main button column near the right edge with a small margin
        int sideMargin = 28;
        int baseX = screenWidth - sideMargin - mainButtonWidth;

        singleplayer.position(baseX, startY).size(mainButtonWidth, buttonHeight);
        multiplayer.position(baseX, startY + buttonHeight + buttonSpacing).size(mainButtonWidth, buttonHeight);

        int thirdRowY = startY + (buttonHeight + buttonSpacing) * 2;
        int fourthRowY = thirdRowY + buttonHeight + buttonSpacing;

        // Full-width rows
        accounts.position(baseX, thirdRowY).size(mainButtonWidth, buttonHeight);

        // Split last row: Options | Exit
        int auxSpacing = buttonSpacing;
        int halfWidth = (mainButtonWidth - auxSpacing) / 2;
        int auxStartX = baseX;
        options.position(auxStartX, fourthRowY).size(halfWidth, buttonHeight);
        exit.position(auxStartX + halfWidth + auxSpacing, fourthRowY).size(halfWidth, buttonHeight);

        // Top-left small controls: ChangeLog and Switch GUI
        int tlX = 10;
        int tlY = 10;
        int smallW = 110;
        int smallH = 20;
        int brX = this.width - 10 - smallW;
        int brY = this.height - 10 - smallH;
        changeLog.position(brX, brY - smallH - 6).size(smallW, smallH);
        guiToggle.position(brX, brY).size(smallW, smallH);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        positionButtons();

        renderTitleBackground(context, this.width, this.height);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        int centerX = this.width / 2;
        int buttonHeight = 27;
        int buttonSpacing = 8;
        int totalHeight = buttonHeight * 4 + buttonSpacing * 3;
        int startY = (this.height - totalHeight) / 2;
        int titleY = startY - 16;

        context.getMatrices().push();
        float scale = 1.15f;
        context.getMatrices().scale(scale, scale, 1.0f);
        int columnCenterX = singleplayer.x + singleplayer.width / 2;
        int scaledX = (int) (columnCenterX / scale);
        int scaledY = (int) (titleY / scale);
        Fonts.getSize(28, Fonts.Type.NEWBOLD).drawCenteredString(context.getMatrices(), "Epsilon Client", scaledX, scaledY, 0xFFFFFFFF);
        context.getMatrices().pop();

        buttons.forEach(button -> button.render(context, mouseX, mouseY, delta));

        // Media Player: always visible on main menu, stretched like main column and placed under Options/Exit
        MediaPlayer media = MediaPlayer.getInstance();
        int thirdRowY = startY + (buttonHeight + buttonSpacing) * 2;
        int fourthRowY = thirdRowY + buttonHeight + buttonSpacing;
        int baseX = singleplayer.x;
        int mediaWidth = singleplayer.width;
        int mediaHeight = 50;
        int mediaY = fourthRowY + buttonHeight + buttonSpacing;
        // Clamp within screen bottom margin
        int bottomMargin = 12;
        if (mediaY + mediaHeight > this.height - bottomMargin) {
            mediaY = Math.max(startY + (buttonHeight + buttonSpacing) * 4 + buttonSpacing, this.height - bottomMargin - mediaHeight);
        }

        media.startAnimation();
        float scaleA = media.getScaleAnimation().getOutput().floatValue();
        final int fx = baseX;
        final int fy = mediaY;
        final int fw = mediaWidth;
        final int fh = mediaHeight;
        final float fdelta = delta;
        if (!media.isCloseAnimationFinished()) {
            MathUtil.setAlpha(scaleA, () -> media.renderAtMenu(context, fx, fy, fw, fh, fdelta));
        } else {
            media.stopAnimation();
        }

        if (altOverlay != null) {
            altOverlay.setSize(this.width, this.height);
            altOverlay.render(context, mouseX, mouseY, delta);
            if (altOverlay.isClosed()) altOverlay = null;
        }
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

        resolvedBackground = BACKGROUNDS[currentBackgroundIndex];
        resourcesChecked = true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (altOverlay != null && altOverlay.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        // Allow dragging Media Player on main menu
        if (MediaPlayer.getInstance().mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        for (AbstractCustomButton titleButton : buttons) {
            if (titleButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (altOverlay != null && altOverlay.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (altOverlay != null && altOverlay.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (altOverlay != null && altOverlay.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        MediaPlayer.getInstance().mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
