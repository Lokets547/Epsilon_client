package wtf.dettex.api.mixins;

import antidaunleak.api.UserProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.MacWindowUtil;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.dettex.event.EventManager;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.other.BufferUtil;
import wtf.dettex.Main;
import wtf.dettex.api.file.exception.FileProcessingException;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.common.util.logger.LoggerUtil;
import wtf.dettex.event.impl.container.SetScreenEvent;
import wtf.dettex.event.impl.player.HotBarUpdateEvent;
import wtf.dettex.modules.impl.combat.NoInteract;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.LocalTime;

import static org.lwjgl.stb.STBImage.stbi_load_from_memory;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements QuickImports {

    @Shadow @Nullable public abstract ClientPlayNetworkHandler getNetworkHandler();

    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    @Shadow @Nullable public ClientPlayerEntity player;

    @Shadow @Final public GameRenderer gameRenderer;

    @Shadow @Nullable public Screen currentScreen;

    @Shadow public abstract Window getWindow();

    @Unique
    private long windowHandle = 0;

    @Unique
    private int currentTitleIndex = 0;

    @Unique
    private boolean isTypingAnimation = true;

    @Unique
    private int typingPosition = 0;

    @Unique
    private boolean isDeleting = false;

    @Unique
    private String currentDisplayTitle = "";

    @Unique
    private int pauseCounter = 0;

    @Unique
    private boolean wasUnHookEnabled = false;

    @Unique
    private String getBabkaTime() {
        LocalTime currentTime = LocalTime.now();
        int hour = currentTime.getHour();

        if (hour >= 5 && hour < 12) {
            return "Good morning";
        } else if (hour >= 12 && hour < 18) {
            return "Good afternoon";
        } else if (hour >= 18 && hour < 22) {
            return "Good evening";
        } else {
            return "Good night";
        }
    }

    @Unique
    private String[] getTitles() {
        UserProfile profile = UserProfile.getInstance();
        return new String[] {
                "Our telegram channel: t.me/DettexDLC",
                "Our website: dettex.space",
                "Build 1.0",
                getBabkaTime() + ", " + profile.profile("username"),
                "Current time: " + getCurrentTime(),
                "Profile: " + profile.profile("username") + ", UID: " + profile.profile("uid") + ", Role: " + profile.profile("role") + ", Subscription: " + profile.profile("subTime")
        };
    }

    @Unique
    private String getCurrentTime() {
        return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Unique
    private boolean isUnHookEnabled() {
        return false;
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    private void onInit(RunArgs args, CallbackInfo ci) {
        Fonts.init();
        initializeWindowTitle();
        setWindowIcon();
        applyWindowStyle();
    }

    @Unique
    private void initializeWindowTitle() {
        MinecraftClient client = (MinecraftClient) (Object) this;
        windowHandle = client.getWindow().getHandle();

        if (isUnHookEnabled()) {
            GLFW.glfwSetWindowTitle(windowHandle, "Minecraft");
        } else {
            GLFW.glfwSetWindowTitle(windowHandle, "Dettex");
        }
    }

    @Unique
    private void setWindowIcon() {
        if (windowHandle == 0) return;

        if (isUnHookEnabled()) {
            setVanillaIcon();
        } else {
            setCustomIcon();
        }
    }

    @Unique
    private void setCustomIcon() {
        final String resourcePath = "/icons/dettex.jpg";

        try (InputStream iconStream = MinecraftClientMixin.class.getResourceAsStream(resourcePath)) {
            if (iconStream != null) {
                setIconFromStream(iconStream);
                return;
            }
        } catch (Exception e) {
            LoggerUtil.error("Failed to load custom window icon (stage 1): " + e.getMessage());
        }

        try (InputStream iconStream = Main.class.getResourceAsStream(resourcePath)) {
            if (iconStream != null) {
                setIconFromStream(iconStream);
                return;
            }
        } catch (Exception e) {
            LoggerUtil.error("Failed to load custom window icon (stage 2): " + e.getMessage());
        }

        try (InputStream iconStream = Main.class.getResourceAsStream(resourcePath)) {
            BufferUtil.setWindowIcon(iconStream, null);
        } catch (Exception ex) {
            LoggerUtil.error("Failed to set custom window icon: " + ex.getMessage());
        }
    }

    @Unique
    private void setVanillaIcon() {
        try {
            InputStream iconStream = MinecraftClient.class.getClassLoader().getResourceAsStream("assets/minecraft/textures/icon.jpg");
            if (iconStream != null) {
                setIconFromStream(iconStream);
                iconStream.close();
            }
        } catch (Exception e) {
            LoggerUtil.error("Failed to set vanilla window icon: " + e.getMessage());
        }
    }

    @Unique
    private void setIconFromStream(InputStream iconStream) throws Exception {
        byte[] iconData = iconStream.readAllBytes();
        ByteBuffer buffer = MemoryUtil.memAlloc(iconData.length);
        buffer.put(iconData);
        buffer.flip();

        int[] width = new int[1];
        int[] height = new int[1];
        int[] channels = new int[1];

        ByteBuffer image = stbi_load_from_memory(buffer, width, height, channels, 4);

        if (image != null) {
            GLFWImage.Buffer icons = GLFWImage.malloc(1);
            GLFWImage icon = icons.get(0);
            icon.set(width[0], height[0], image);
            GLFW.glfwSetWindowIcon(windowHandle, icons);
            MemoryUtil.memFree(image);
            icons.free();
        }

        MemoryUtil.memFree(buffer);
    }

    @Unique
    private void applyWindowStyle() {
    }

    @Inject(at = @At("HEAD"), method = "stop")
    private void stop(CallbackInfo ci) {
        LoggerUtil.info("Stopping for MinecraftClient");
        if (Main.getInstance().isInitialized()) {
            try {
                Main.getInstance().getFileController().saveFiles();
            } catch (FileProcessingException e) {
                LoggerUtil.error("Error occurred while saving files: " + e.getMessage() + " " + e.getCause());
            } finally {
                Main.getInstance().getFileController().stopAutoSave();
            }
        }
    }

    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;"), cancellable = true)
    public void doItemUseHook(CallbackInfo ci) {
        if (NoInteract.getInstance().isState()) {
            for (Hand hand : Hand.values()) {
                if (player.getStackInHand(hand).isEmpty()) continue;
                ActionResult result = interactionManager.interactItem(player, hand);
                if (result.isAccepted()) {
                    if (result instanceof ActionResult.Success success && success.swingSource().equals(ActionResult.SwingSource.CLIENT)) {
                        gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                        player.swingHand(hand);
                    }
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "setScreen", at = @At(value = "HEAD"), cancellable = true)
    public void setScreenHook(Screen screen, CallbackInfo ci) {
        SetScreenEvent event = new SetScreenEvent(screen);
        EventManager.callEvent(event);
        Main.getInstance().getDraggableRepository().draggable().forEach(drag -> drag.setScreen(event));
        Screen eventScreen = event.getScreen();
        if (screen != eventScreen) {
            ((MinecraftClient) (Object) this).setScreen(eventScreen);
            ci.cancel();
        }
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setIcon(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/client/util/Icons;)V"))
    private void onChangeIcon(Window instance, ResourcePack resourcePack, Icons icons) throws IOException {
        if (GLFW.glfwGetPlatform() == 393218) {
            MacWindowUtil.setApplicationIconImage(icons.getMacIcon(resourcePack));
            return;
        }

        setWindowIcon();
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void onWindowTitle(CallbackInfoReturnable<String> cir) {
        if (!isUnHookEnabled()) {
            cir.setReturnValue("Dettex - " + SharedConstants.getGameVersion().getName());
        }
    }

    @Inject(method = "updateWindowTitle", at = @At("HEAD"), cancellable = true)
    private void preventDefaultTitle(CallbackInfo ci) {
        if (!isUnHookEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void onResolutionChanged(CallbackInfo ci) {
        if (windowHandle != 0) {
            setWindowIcon();
            applyWindowStyle();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        boolean currentUnHookState = isUnHookEnabled();
        if (wasUnHookEnabled != currentUnHookState) {
            if (currentUnHookState) {
                GLFW.glfwSetWindowTitle(windowHandle, "Minecraft");
                setVanillaIcon();
                applyWindowStyle();
            } else {
                GLFW.glfwSetWindowTitle(windowHandle, "Dettex");
                setCustomIcon();
                applyWindowStyle();
            }
            wasUnHookEnabled = currentUnHookState;
        }

        if (!isUnHookEnabled()) {
            animateTitle();
        }
    }

    @Unique
    private void animateTitle() {
        if (windowHandle == 0) return;

        String[] titles = getTitles();
        String targetTitle = titles[currentTitleIndex];

        if (isTypingAnimation) {
            if (typingPosition < targetTitle.length()) {
                currentDisplayTitle = "Dettex - " + targetTitle.substring(0, typingPosition + 1);
                typingPosition++;
            } else {
                int pauseDuration = 10;
                if (pauseCounter++ >= pauseDuration) {
                    isDeleting = true;
                    isTypingAnimation = false;
                    pauseCounter = 0;
                }
            }
        } else if (isDeleting) {
            if (typingPosition > 0) {
                currentDisplayTitle = "Dettex - " + targetTitle.substring(0, typingPosition - 1);
                typingPosition--;
            } else {
                isDeleting = false;
                isTypingAnimation = true;
                currentTitleIndex = (currentTitleIndex + 1) % titles.length;
                typingPosition = 0;
            }
        }

        GLFW.glfwSetWindowTitle(windowHandle, currentDisplayTitle);
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"), cancellable = true)
    public void handleInputEventsHook(CallbackInfo ci) {
        HotBarUpdateEvent event = new HotBarUpdateEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) ci.cancel();
    }
}