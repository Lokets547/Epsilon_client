package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.ColorSetting;
import wtf.dettex.modules.setting.implement.BindSetting;
import wtf.dettex.event.EventHandler;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.task.scripts.Script;
import wtf.dettex.event.impl.chat.ChatReceiveEvent;
import wtf.dettex.event.impl.container.HandledScreenEvent;
import wtf.dettex.common.util.auction.AuctionPriceParser;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.event.impl.keyboard.KeyEvent;

import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuctionHelper extends Module {
    AuctionPriceParser auctionPriceParser = new AuctionPriceParser();
    Script script = new Script();
    @NonFinal
    Slot cheapestSlot, costEffectiveSlot;
    int[] RED_GREEN_COLORS = {0xFF4BFF4B, 0xFFFF4B4B};

    ColorSetting cheapestItemColorSetting = new ColorSetting("Cheapest Item", "Highlight color for the lowest priced item.")
            .setColor(0xFF4BFF4B).presets(RED_GREEN_COLORS);

    ColorSetting costEffectiveItemColorSetting = new ColorSetting("Cost Effective Item", "Highlight color for the best item.")
            .setColor(0xFFFF4B4B).presets(RED_GREEN_COLORS);

    BindSetting searchBind = new BindSetting("Auction Search", "Search item on auction");

    @NonFinal
    String pendingFallbackName;
    @NonFinal
    boolean waitingForFallbackResponse;

    public AuctionHelper() {
        super("AuctionHelper", "Auction Helper", ModuleCategory.RENDER);
        setup(cheapestItemColorSetting, costEffectiveItemColorSetting, searchBind);
    }

    
    @EventHandler
     
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket) script.cleanup().addTickStep(0, () -> {
            if (mc.currentScreen instanceof GenericContainerScreen screen) {
                cheapestSlot = findSlotWithLowestPrice(screen.getScreenHandler().slots);
                costEffectiveSlot = findSlotWithBestPricePerItem(screen.getScreenHandler().slots);
            }
        });
    }

    @EventHandler
    
    public void onTick(TickEvent e) {
        script.update();
    }

    @EventHandler
    
    public void onKey(KeyEvent e) {
        if (mc.player == null || mc.currentScreen != null) return;
        if (!e.isKeyDown(searchBind.getKey())) return;

        var stack = mc.player.getMainHandStack();
        if (stack == null || stack.isEmpty()) return;

        String customName = getCustomItemName(stack);
        if (customName == null || customName.isEmpty()) return;

        String fallbackName = getDefaultItemName(stack);

        pendingFallbackName = null;
        waitingForFallbackResponse = false;

        if (fallbackName != null && !fallbackName.isEmpty() && !fallbackName.equalsIgnoreCase(customName)) {
            pendingFallbackName = fallbackName;
            waitingForFallbackResponse = true;
        }

        sendSearchCommand(customName);
    }

    @EventHandler
    
    public void onChat(ChatReceiveEvent event) {
        if (!waitingForFallbackResponse || pendingFallbackName == null) return;

        String message = event.getMessage().getString();
        if (message == null) return;

        if (message.contains("Такого предмета не существует")) {
            waitingForFallbackResponse = false;
            String fallback = pendingFallbackName;
            pendingFallbackName = null;
            sendSearchCommand(fallback);
        }
    }

    private void sendSearchCommand(String query) {
        if (query == null || query.isEmpty()) return;
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler != null) {
            handler.sendChatCommand("ah search " + query);
        }
    }

    private String getCustomItemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String name = stack.getName().getString();
        return sanitizeItemName(ColorUtil.removeFormatting(name));
    }

    private String getDefaultItemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return sanitizeItemName(stack.getItem().getName(stack).getString());
    }

    private String sanitizeItemName(String input) {
        if (input == null) return null;
        String sanitized = input.replaceAll("[^A-Za-zА-Яа-я ]", " ");
        sanitized = sanitized.trim().replaceAll("\\s+", " ");
        return sanitized;
    }

    /**
     * Draw the highlight for the cheapest and cost effective slots.
     *
     * @param e Handled screen event.
     */
    @EventHandler
    
    public void onHandledScreen(HandledScreenEvent e) {
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            int offsetX = (screen.width - e.getBackgroundWidth()) / 2;
            int offsetY = (screen.height - e.getBackgroundHeight()) / 2;

            int cheapItemColor = getBlinkingColor(cheapestItemColorSetting.getColor());
            int cheapestQuantityColor = getBlinkingColor(costEffectiveItemColorSetting.getColor());

            matrix.push();
            matrix.translate(offsetX, offsetY, 0);
            if (cheapestSlot != costEffectiveSlot) highlightSlot(context, cheapestSlot, cheapItemColor);
            highlightSlot(context, costEffectiveSlot, cheapestQuantityColor);
            matrix.pop();
        }
    }

    
    private int getBlinkingColor(int color) {
        float alpha = (float) Math.abs(Math.sin((double) System.currentTimeMillis() / 10 * Math.PI / 180));
        return ColorUtil.multAlpha(color, alpha);
    }

    
    private Slot findSlotWithLowestPrice(List<Slot> slots) {
        return slots.stream().filter(this::hasValidPrice).min(Comparator.comparingInt(slot -> auctionPriceParser.getPrice(slot.getStack()))).orElse(null);
    }

    
    private Slot findSlotWithBestPricePerItem(List<Slot> slots) {
        return slots.stream().filter(this::isValidMultiItemSlot).min(Comparator.comparingInt(slot -> auctionPriceParser.getPrice(slot.getStack()) / slot.getStack().getCount())).orElse(null);
    }

    
    private boolean hasValidPrice(Slot slot) {
        return auctionPriceParser.getPrice(slot.getStack()) >= 0;
    }

    
    private boolean isValidMultiItemSlot(Slot slot) {
        return hasValidPrice(slot) && slot.getStack().getCount() > 1;
    }

    
    private void highlightSlot(DrawContext context, Slot slot, int color) {
        if (slot != null) rectangle.render(ShapeProperties.create(context.getMatrices(), slot.x, slot.y, 16, 16).color(color).build());
    }
}

