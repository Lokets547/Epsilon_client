package wtf.dettex.common;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.stream.Stream;

public interface QuickLogger {
    static Text getPrefix() {
        MutableText text = Text.literal("");

        text.append(createGradientText("Dettex"));

        text.append(createGradientText(" » "));
        return text;
    }

    private static MutableText createGradientText(String text) {
        MutableText result = Text.literal("");
        int[] colors = {0x9F9F9F, 0x8F8F8F, 0x808080, 0x707070, 0x616161, 0x515151};

        for (int i = 0; i < text.length() && i < colors.length; i++) {
            char c = text.charAt(i);
            MutableText charText = Text.literal(String.valueOf(c));
            charText.setStyle(Style.EMPTY.withColor(colors[i]));
            result.append(charText);
        }

        return result;
    }

    default void logDirect(Text... components) {
        MutableText component = Text.literal("");
        component.append(getPrefix());
        component.append(Text.literal(" "));
        Arrays.asList(components).forEach(component::append);
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(component);
        }
    }

    default void logDirect(String message, Formatting color) {
        Stream.of(message.split("\n")).forEach(line -> {
            MutableText component = Text.literal(line.replace("\t", "    "));
            component.setStyle(component.getStyle().withColor(color));
            logDirect(component);
        });
    }

    default void logDirect(String message) {
        logDirect(message, Formatting.GRAY);
    }
}