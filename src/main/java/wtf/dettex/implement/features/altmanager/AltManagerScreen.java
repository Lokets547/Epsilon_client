package wtf.dettex.implement.features.altmanager;

import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.Direction;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.other.StringUtil;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.implement.screen.mainmenu.CustomMainMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AltManagerScreen extends Screen implements QuickImports {
    public static final List<Alt> ALTS = new ArrayList<>();

    private static final int ENTRY_HEIGHT = 20;
    private static final int ENTRY_GAP = 2;

    private boolean typing;
    private String currentUsername;
    private String selectedUsername;
    private double scrollOffset;
    private double targetScrollOffset;

    private final Animation openAnimation = new DecelerateAnimation().setMs(220).setValue(1);
    private final Map<String, Animation> entryAnimations = new HashMap<>();
    private final Set<String> removingEntries = new HashSet<>();
    private boolean closing;

    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;

    private float inputX;
    private float inputY;
    private float inputWidth;
    private float inputHeight;

    private float applyX;
    private float applyY;
    private float applySize;

    private float randomX;
    private float randomY;
    private float randomWidth;
    private float randomHeight;

    private float listX;
    private float listY;
    private float listWidth;
    private float listHeight;

    private boolean embedded;

    public AltManagerScreen() {
        super(Text.translatable("menu.dettex.accounts"));
        String loaded = AltManagerConfig.loadAccounts(ALTS);
        currentUsername = loaded != null ? loaded : (ALTS.isEmpty() ? "" : ALTS.get(0).getUsername());
        if (currentUsername != null && !currentUsername.isEmpty()) {
            applyOfflineSession(currentUsername);
            selectedUsername = currentUsername;
        }

        openAnimation.setDirection(Direction.FORWARDS);
        openAnimation.reset();
        for (Alt alt : ALTS) {
            entryAnimations.put(alt.getUsername().toLowerCase(), createEntryAnimation(Direction.FORWARDS, false));
        }
    }
    private int applyFade(int color, float progress) {
        float clamped = Math.max(0F, Math.min(1F, progress));
        return ColorUtil.replAlpha(color, (int) (ColorUtil.alpha(color) * clamped));
    }

    public AltManagerScreen setEmbedded(boolean embedded) {
        this.embedded = embedded;
        return this;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!embedded) {
            CustomMainMenu.renderTitleBackground(context, this.width, this.height);
        }

        computeLayout();

        double scrollStep = 0.15;
        scrollOffset += (targetScrollOffset - scrollOffset) * scrollStep;

        float animationProgress = Math.max(0F, Math.min(1F, openAnimation.getOutput().floatValue()));
        if (closing && openAnimation.isFinished(Direction.BACKWARDS)) {
            if (!embedded) this.client.setScreen(null);
            return;
        }

        float animatedHeight = panelHeight * animationProgress;
        blurGlass.render(ShapeProperties.create(context.getMatrices(), panelX, panelY + (panelHeight - animatedHeight) / 2F, panelWidth, animatedHeight)
                .round(8).softness(1).color(applyFade(ColorUtil.getAltManager(0.6F), animationProgress)).build());

        if (animationProgress < 0.05F && !openAnimation.isDirection(Direction.BACKWARDS)) {
            return;
        }

        float fade = animationProgress;

        blurGlass.render(ShapeProperties.create(context.getMatrices(), inputX, inputY, inputWidth, inputHeight)
                .round(4).softness(1).thickness(2)
                .outlineColor(applyFade(ColorUtil.getAltManager(typing ? 0.8F : 0.7F), fade))
                .color(applyFade(ColorUtil.getAltManager(0.6F), fade)).build());

        String display = currentUsername.isEmpty() && !typing ? "Введите сюда свой никнейм" : currentUsername;
        if (typing && System.currentTimeMillis() % 1000 > 300) {
            display += " _";
        }
        Fonts.getSize(18, Fonts.Type.DEFAULT).drawString(context.getMatrices(), display, inputX + 8, inputY + 7, applyFade(0xFFFFFFFF, fade));

        if (currentUsername.length() == 16) {
            String warning = "Максимальная длина никнейма 16 символов!";
            float warningWidth = Fonts.getSize(16, Fonts.Type.DEFAULT).getStringWidth(warning);
            float warningX = panelX + (panelWidth - warningWidth) / 2F;
            Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(context.getMatrices(), warning, warningX, inputY - 5, applyFade(0xFFFF6B6B, fade));
        }

        boolean applyHovered = MathUtil.isHovered(mouseX, mouseY, applyX, applyY, applySize, applySize);
        float applyAlpha = applyHovered ? 0.75F : 0.65F;
        blurGlass.render(ShapeProperties.create(context.getMatrices(), applyX, applyY, applySize, applySize)
                .round(4).thickness(2)
                .outlineColor(applyFade(ColorUtil.getAltManager(0.7F), fade))
                .color(applyFade(ColorUtil.getAltManager(applyAlpha), fade)).build());
        image.setTexture("textures/check.png").render(
                ShapeProperties.create(context.getMatrices(), applyX + (applySize - 10) / 2F, applyY + (applySize - 10) / 2F, 10, 10)
                        .color(applyFade(0xFFFFFFFF, fade)).build()
        );

        boolean randomHovered = MathUtil.isHovered(mouseX, mouseY, randomX, randomY, randomWidth, randomHeight);
        float randomAlpha = randomHovered ? 0.7F : 0.6F;
        blurGlass.render(ShapeProperties.create(context.getMatrices(), randomX, randomY, randomWidth, randomHeight)
                .round(5).thickness(2)
                .outlineColor(applyFade(ColorUtil.getAltManager(0.7F), fade))
                .color(applyFade(ColorUtil.getAltManager(randomAlpha), fade)).build());
        Fonts.getSize(18, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), "Случайный аккаунт",
                randomX + randomWidth / 2F, randomY + randomHeight / 2F - 3, applyFade(0xFFFFFFFF, fade));

        blurGlass.render(ShapeProperties.create(context.getMatrices(), listX, listY, listWidth, listHeight)
                .round(5).color(applyFade(ColorUtil.getAltManager(0.6F), fade)).build());

        float contentHeight = ALTS.size() * (ENTRY_HEIGHT + ENTRY_GAP);
        float visibleHeight = listHeight - 8;
        float maxScroll = Math.max(0, contentHeight - visibleHeight);
        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxScroll));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxScroll));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        if (Math.abs(scrollOffset - targetScrollOffset) < 0.01) {
            scrollOffset = targetScrollOffset;
        }

        int scissorX = (int) Math.floor(listX);
        int scissorY = (int) Math.floor(listY);
        int scissorWidth = (int) Math.ceil(listX + listWidth);
        int scissorHeight = (int) Math.ceil(listY + listHeight);
        context.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        float entryY = listY + 4 - (float) scrollOffset;
        for (Alt alt : ALTS) {
            float entryBottom = entryY + ENTRY_HEIGHT;
            if (entryBottom >= listY && entryY <= listY + listHeight - 4) {
                boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= entryY && mouseY <= entryBottom;
                boolean selected = selectedUsername != null && selectedUsername.equalsIgnoreCase(alt.getUsername());
                String key = alt.getUsername().toLowerCase();
                Animation entryAnimation = entryAnimations.computeIfAbsent(key, missed -> createEntryAnimation(Direction.FORWARDS, true));
                float entryProgress = entryAnimation.getOutput().floatValue();
                float entryAlpha = selected ? 0.8F : (hovered ? 0.7F : 0.6F);
                int entryColor = ColorUtil.getAltManager(entryAlpha);
                float entryHeightAnimated = (ENTRY_HEIGHT - ENTRY_GAP) * entryProgress * fade;
                if (entryHeightAnimated > 0.5F) {
                    blurGlass.render(ShapeProperties.create(context.getMatrices(), listX + 4, entryY + (ENTRY_HEIGHT - ENTRY_GAP - entryHeightAnimated) / 2F, listWidth - 8, entryHeightAnimated)
                            .round(4).color(applyFade(entryColor, fade * entryProgress)).build());
                    Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(context.getMatrices(), alt.getUsername(), listX + 12, entryY + 6, applyFade(0xFFFFFFFF, fade * entryProgress));
                }
            }
            entryY += ENTRY_HEIGHT + ENTRY_GAP;
        }

        context.disableScissor();

        if (!removingEntries.isEmpty()) {
            Iterator<Alt> iterator = ALTS.iterator();
            while (iterator.hasNext()) {
                Alt alt = iterator.next();
                String key = alt.getUsername().toLowerCase();
                if (removingEntries.contains(key)) {
                    Animation animation = entryAnimations.get(key);
                    if (animation == null || animation.isFinished(Direction.BACKWARDS)) {
                        iterator.remove();
                        removingEntries.remove(key);
                        finalizeRemoval(alt);
                    }
                }
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        computeLayout();

        if (MathUtil.isHovered(mouseX, mouseY, inputX, inputY, inputWidth, inputHeight)) {
            typing = true;
            return true;
        }

        typing = false;

        if (button == 0) {
            if (MathUtil.isHovered(mouseX, mouseY, applyX, applyY, applySize, applySize)) {
                saveAccounts();
                return true;
            }

            if (MathUtil.isHovered(mouseX, mouseY, randomX, randomY, randomWidth, randomHeight)) {
                currentUsername = AltGenerator.generateGameNickname();
                selectedUsername = currentUsername;
                return true;
            }
        }

        if (MathUtil.isHovered(mouseX, mouseY, listX, listY, listWidth, listHeight)) {
            int index = (int) ((mouseY - (listY + 4) + scrollOffset) / (ENTRY_HEIGHT + ENTRY_GAP));
            if (index >= 0 && index < ALTS.size()) {
                Alt alt = ALTS.get(index);
                if (button == 0) {
                    selectAccount(alt);
                    return true;
                } else if (button == 1) {
                    removeAccount(alt);
                    return true;
                }
            }
            return true;
        }
        // If embedded and clicked outside the panel bounds, close without consuming the event
        if (embedded && button == 0 && !MathUtil.isHovered(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            close();
            return false;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (MathUtil.isHovered(mouseX, mouseY, listX, listY, listWidth, listHeight)) {
            double delta = verticalAmount * 3.5;
            targetScrollOffset -= delta * 6.0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            startClosing();
            return true;
        }

        if (!typing) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !currentUsername.isEmpty()) {
            currentUsername = currentUsername.substring(0, currentUsername.length() - 1);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            saveAccounts();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typing && !Character.isISOControl(chr)) {
            if (currentUsername.length() < 16) {
                currentUsername += chr;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void saveAccounts() {
        String trimmed = currentUsername.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        ALTS.removeIf(alt -> alt.getUsername().equalsIgnoreCase(trimmed));
        ALTS.add(0, new Alt(trimmed));
        Animation animation = createEntryAnimation(Direction.FORWARDS, true);
        entryAnimations.put(trimmed.toLowerCase(), animation);
        removingEntries.remove(trimmed.toLowerCase());
        AltManagerConfig.saveAccounts(trimmed);
        applyOfflineSession(trimmed);
        selectedUsername = trimmed;
        typing = false;
    }

    private void applyOfflineSession(String username) {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(UTF_8));
            Session session = new Session(username, uuid, "", Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
            StringUtil.setSession(session);
        } catch (AuthenticationException exception) {
            exception.printStackTrace();
        }
    }

    private void selectAccount(Alt alt) {
        String username = alt.getUsername();
        currentUsername = username;
        selectedUsername = username;
        typing = false;
        applyOfflineSession(username);
        AltManagerConfig.saveAccounts(username);
    }

    private void removeAccount(Alt alt) {
        String key = alt.getUsername().toLowerCase();
        Animation animation = entryAnimations.computeIfAbsent(key, missed -> createEntryAnimation(Direction.FORWARDS, true));
        animation.setDirection(Direction.BACKWARDS);
        animation.reset();
        removingEntries.add(key);
    }

    private void finalizeRemoval(Alt alt) {
        String username = alt.getUsername();
        entryAnimations.remove(username.toLowerCase());

        if (selectedUsername != null && selectedUsername.equalsIgnoreCase(username)) {
            if (!ALTS.isEmpty()) {
                selectAccount(ALTS.get(0));
            } else {
                selectedUsername = null;
                currentUsername = "";
                typing = false;
                AltManagerConfig.saveAccounts("");
            }
        } else {
            AltManagerConfig.saveAccounts(selectedUsername != null ? selectedUsername : "");
        }

        float visibleHeight = listHeight - 8;
        float maxScroll = Math.max(0, ALTS.size() * (ENTRY_HEIGHT + ENTRY_GAP) - visibleHeight);
        if (targetScrollOffset > maxScroll) {
            targetScrollOffset = maxScroll;
        }
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    private void computeLayout() {
        panelWidth = 280;
        panelHeight = 240;
        panelX = this.width / 2F - panelWidth / 2F;
        panelY = this.height / 2F - panelHeight / 2F;

        float padding = 14;
        float gap = 4;
        inputHeight = 20;
        applySize = inputHeight;
        inputWidth = panelWidth - padding * 2 - gap - applySize;
        inputX = panelX + padding;
        inputY = panelY + padding;

        applyX = inputX + inputWidth + gap;
        applyY = inputY;

        randomWidth = inputWidth + gap + applySize;
        randomHeight = inputHeight;
        randomX = inputX;
        randomY = inputY + inputHeight + gap;

        listX = randomX;
        listY = randomY + randomHeight + gap;
        listWidth = randomWidth;
        listHeight = panelHeight - (listY - panelY) - padding;
        if (listHeight < 40) {
            listHeight = 40;
        }
    }

    @Override
    public void close() {
        startClosing();
    }

    private void startClosing() {
        if (closing) {
            return;
        }
        closing = true;
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();
    }

    public boolean isClosed() {
        return closing && openAnimation.isFinished(Direction.BACKWARDS);
    }

    private Animation createEntryAnimation(Direction direction, boolean resetOnCreate) {
        Animation animation = new DecelerateAnimation().setMs(200).setValue(1);
        animation.setDirection(direction);
        if (resetOnCreate) {
            animation.reset();
        }
        return animation;
    }
}
