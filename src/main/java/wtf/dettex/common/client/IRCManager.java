package wtf.dettex.common.client;

import antidaunleak.api.annotation.Native;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import wtf.dettex.modules.impl.misc.IRC;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

import static wtf.dettex.common.QuickImports.mc;

public class IRCManager {

    private final Set<String> ignoredNicks = ConcurrentHashMap.newKeySet();
    private final File ignoreFile = new File(MinecraftClient.getInstance().runDirectory, "files/ircChat.irc");

    private volatile Socket socket;
    private volatile BufferedReader in;
    private volatile BufferedWriter out;

    private final String host = "64.188.79.75";
    private final int port = 4004;

    private volatile String nickname;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService readerService = Executors.newSingleThreadExecutor();

    private volatile boolean connecting = false;

    public IRCManager() {
        loadIgnoreList();
    }

    
    public void connect(String username) {
        this.nickname = username;
        scheduleConnect(0);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void scheduleConnect(long delayMillis) {
        if (connecting) return;
        connecting = true;
        scheduler.schedule(this::tryConnect, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void tryConnect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            out.write("Dettex" + ":" + nickname);
            out.newLine();
            out.flush();

            startReading();
            logToClient("Подключено к IRC");
        } catch (IOException e) {
            logToClient("IRC » Не удалось подключиться. Повтор через 15 секунд...");
            scheduleConnect(15000);
        } finally {
            connecting = false;
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void startReading() {
        readerService.submit(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (IRC.getInstance().state) {
                        messageClient(line);
                    }
                }
            } catch (IOException ignored) {
            } finally {
                reconnect();
            }
        });
    }

    
    private void reconnect() {
        closeConnection();
        scheduleConnect(15000);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void closeConnection() {
        closeQuietly(in);
        closeQuietly(out);
        closeQuietly(socket);
        in = null;
        out = null;
        socket = null;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    public void messageHost(String msg) {
        try {
            if (out != null) {
                out.write(msg);
                out.newLine();
                out.flush();
                messageClient("[IRC » " + "Dettex" + "] » Вы: " + msg);
            }
        } catch (IOException e) {
            reconnect();
        }
    }

    
    public void messageClient(String string) {
        if (mc == null || mc.player == null || mc.world == null || mc.inGameHud == null) return;
        String[] parts = string.split(":", 2);
        if (parts.length > 1 && isIgnored(parts[0].trim())) return;

        mc.inGameHud.getChatHud().addMessage(applyGradient(string));
    }

    
    private Text applyGradient(String string) {
        MutableText component = Text.empty();
        int splitIndex = string.indexOf(" » ");
        String prefix = splitIndex != -1 ? string.substring(0, splitIndex) : string;
        String rest = splitIndex != -1 ? string.substring(splitIndex) : "";

        int length = Math.max(prefix.length(), 1);
        for (int i = 0; i < length; i++) {
            int rgb = blendColors(0x808080, 0xFFFFFF, (float) i / (length - 1));
            component.append(Text.literal(String.valueOf(prefix.charAt(i))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        if (!rest.isEmpty()) {
            component.append(Text.literal(rest).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))));
        }
        return component;
    }

    private int blendColors(int c1, int c2, float ratio) {
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - ratio) + ((c2 >> 16) & 0xFF) * ratio);
        int g = (int) (((c1 >> 8) & 0xFF) * (1 - ratio) + ((c2 >> 8) & 0xFF) * ratio);
        int b = (int) ((c1 & 0xFF) * (1 - ratio) + (c2 & 0xFF) * ratio);
        return (r << 16) | (g << 8) | b;
    }

    public Set<String> getIgnoredNicks() {
        return Collections.unmodifiableSet(ignoredNicks);
    }

    public void ignoreNick(String nick) {
        ignoredNicks.add(nick.toLowerCase());
        asyncSaveIgnoreList();
    }

    public void unignoreNick(String nick) {
        ignoredNicks.remove(nick.toLowerCase());
        asyncSaveIgnoreList();
    }

    public boolean isIgnored(String nick) {
        return ignoredNicks.contains(nick.toLowerCase());
    }

    private void asyncSaveIgnoreList() {
        scheduler.execute(this::saveIgnoreList);
    }

    private void saveIgnoreList() {
        try {
            if (!ignoreFile.getParentFile().exists()) ignoreFile.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(ignoreFile), StandardCharsets.UTF_8))) {
                for (String nick : ignoredNicks) {
                    writer.write(nick);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    private void loadIgnoreList() {
        if (!ignoreFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(ignoreFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) ignoredNicks.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logToClient(String msg) {
        System.out.println(msg);
        if (IRC.getInstance().state) messageClient(msg);
    }

    private void closeQuietly(Closeable c) {
        try {
            if (c != null) c.close();
        } catch (IOException ignored) {
        }
    }
}