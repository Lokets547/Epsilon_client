package wtf.dettex.common.util.world;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.*;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.modules.setting.implement.SelectSetting;

@Getter
@UtilityClass
public class ServerUtil implements QuickImports {
    private final StopWatch pvpWatch = new StopWatch();
    public String server = "Vanilla";
    public float TPS = 20;
    public long timestamp;
    @Getter
    public int anarchy;
    @Getter
    public boolean pvpEnd;
    
    public static SelectSetting manualServerSelection = new SelectSetting("Manual Server Selection", "Manually select server type")
            .value("FunTime", "SpookyTime", "CopyTime", "HolyWorld", "ReallyWorld")
            .selected("FunTime");

    public void tick() {
        anarchy = getAnarchyMode();
        server = getServer();
        pvpEnd = inPvpEnd();
        if (inPvp()) pvpWatch.reset();
    }

    public void packet(PacketEvent e) {
        switch (e.getPacket()) {
            case WorldTimeUpdateS2CPacket time -> {
                long nanoTime = System.nanoTime();

                float maxTPS = 20;
                float rawTPS = maxTPS * (1e9f / (nanoTime - timestamp));

                TPS = MathHelper.clamp(rawTPS, 0, maxTPS);
                timestamp = nanoTime;
            }
            default -> {}
        }
    }

    private String getServer() {
        if (PlayerIntersectionUtil.nullCheck() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null || mc.getNetworkHandler().getBrand() == null) return "Vanilla";
        String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
        String brand = mc.getNetworkHandler().getBrand().toLowerCase();

        if (brand.contains("botfilter")) return "FunTime";
        else if (brand.contains("§6spooky§ccore")) return "SpookyTime";
        else if (serverIp.contains("funtime") || serverIp.contains("skytime") || serverIp.contains("space-times") || serverIp.contains("funsky")) return "CopyTime";
        else if (brand.contains("holyworld") || brand.contains("vk.com/idwok")) return "HolyWorld";
        else if (serverIp.contains("reallyworld")) return "ReallyWorld";
        else if (serverIp.contains("aresmine") || serverIp.contains("craftyou")) return "AresMine";
        else if (serverIp.contains("stickshield")) return "stickshield";
        return "Vanilla";
    }

    private int getAnarchyMode() {
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        switch (server) {
            case "FunTime" -> {
                if (objective != null) {
                    String[] string = objective.getDisplayName().getString().split("-");
                    if (string.length > 1) return Integer.parseInt(string[1]);
                }
            }
            case "HolyWorld" -> {
                for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(objective)) {
                    String text = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
                    if (!text.isEmpty()) {
                        String string = StringUtils.substringBetween(text, "#", " -◆-");
                        if (string != null && !string.isEmpty()) return Integer.parseInt(string.replace(" (1.20)", ""));
                    }
                }
            }
        }
        return -1;
    }

    public boolean isPvp() {
        return !pvpWatch.finished(500);
    }

    private boolean inPvp() {
        return mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase()).anyMatch(s -> s.contains("pvp") || s.contains("пвп"));
    }

    private boolean inPvpEnd() {
        return mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase())
                .anyMatch(s -> (s.contains("pvp") || s.contains("пвп")) && (s.contains("0") || s.contains("1")));
    }

    public String getWorldType() {
        return mc.world.getRegistryKey().getValue().getPath();
    }

    public boolean isCopyTime() {
        return manualServerSelection.isSelected("CopyTime") || manualServerSelection.isSelected("SpookyTime") || manualServerSelection.isSelected("FunTime");
    }
    
    public boolean isFunTime() {
        return manualServerSelection.isSelected("FunTime");
    }
    
    public boolean isReallyWorld() {
        return manualServerSelection.isSelected("ReallyWorld");
    }
    
    public boolean isHolyWorld() {
        return manualServerSelection.isSelected("HolyWorld");
    }
    
    public boolean isVanilla() {
        return false;
    }
    
    public boolean isAresMine() {
        return false;
    }
    
    public boolean isStickShield() {
        return false;
    }
}
