package wtf.dettex.api.ai.training;

import net.minecraft.client.MinecraftClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DatasetLogger {

    private static BufferedWriter writer;
    private static Path logFile;

    private DatasetLogger() {}

    private static void ensureOpen() throws IOException {
        if (writer != null) return;
        Path gameDir = MinecraftClient.getInstance().runDirectory.toPath();
        Path dir = gameDir.resolve("Dettex").resolve("datasets");
        Files.createDirectories(dir);
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        logFile = dir.resolve("ai_" + ts + ".csv");
        boolean newFile = !Files.exists(logFile);
        writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (newFile) {
            writer.write("prevYaw,prevPitch,lerpYaw,lerpPitch,labelYaw,labelPitch\n");
            writer.flush();
        }
    }

    public static synchronized void append(float prevYaw, float prevPitch,
                                           float lerpYaw, float lerpPitch,
                                           float labelYaw, float labelPitch) {
        try {
            ensureOpen();
            writer.write(String.format("%.6f,%.6f,%.6f,%.6f,%.6f,%.6f\n",
                    prevYaw, prevPitch, lerpYaw, lerpPitch, labelYaw, labelPitch));
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    public static synchronized Path currentFile() {
        return logFile;
    }
}

