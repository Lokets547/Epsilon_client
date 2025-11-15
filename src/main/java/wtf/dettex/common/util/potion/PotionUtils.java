package wtf.dettex.common.util.potion;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PotionUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private int previousSlot = -1;

    public void changeItemSlot(boolean shouldChange) {
        if (shouldChange && previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = previousSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            previousSlot = -1;
        }
    }

    public void setPreviousSlot(int slot) {
        this.previousSlot = slot;
    }

    public static void useItem(Hand hand) {
        if (mc.player == null || mc.interactionManager == null) return;

        ItemStack stack = mc.player.getStackInHand(hand);
        if (!stack.isEmpty()) {
            mc.interactionManager.interactItem(mc.player, hand);
        }
    }

    public int getPreviousSlot() {
        return previousSlot;
    }
}
