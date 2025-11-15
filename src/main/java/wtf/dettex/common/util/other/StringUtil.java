package wtf.dettex.common.util.other;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.UserApiService;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import wtf.dettex.api.mixins.accessor.MinecraftClientAccessor;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class StringUtil {
    public String randomString(int length) {
        return IntStream.range(0, length)
                .mapToObj(operand -> String.valueOf((char) new Random().nextInt('a', 'z' + 1)))
                .collect(Collectors.joining());
    }

    public String getBindName(int key) {
        if (key < 0) return "N/A";
        return PlayerIntersectionUtil.getKeyType(key).createFromCode(key).getTranslationKey().replace("key.keyboard.", "")
                .replace("key.mouse.", "mouse ").replace(".", " ").toUpperCase();
    }

    public String wrap(String input, int width, int size) {
        String[] words = input.split(" ");
        StringBuilder output = new StringBuilder();
        float lineWidth = 0;
        for (String word : words) {
            float wordWidth = Fonts.getSize(size).getStringWidth(word);
            if (lineWidth + wordWidth > width) {
                output.append("\n");
                lineWidth = 0;
            } else if (lineWidth > 0) {
                output.append(" ");
                lineWidth += Fonts.getSize(size).getStringWidth(" ");
            }
            output.append(word);
            lineWidth += wordWidth;
        }
        return output.toString();
    }

    public static void setSession(Session session) throws AuthenticationException {
        MinecraftClient mc = MinecraftClient.getInstance();
        MinecraftClientAccessor mca = (MinecraftClientAccessor) mc;
        mca.setSession(session);
        UserApiService apiService;
        apiService = mca.getAuthenticationService().createUserApiService(session.getAccessToken());
        mca.setUserApiService(apiService);
        mca.setSocialInteractionsManager(new SocialInteractionsManager(mc, apiService));
        mca.setProfileKeys(ProfileKeys.create(apiService, session, mc.runDirectory.toPath()));
        mca.setAbuseReportContext(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));
    }

    public String getDuration(int time) {
        int mins = time / 60;
        String sec = String.format("%02d", time % 60);
        return mins + ":" + sec;
    }

    public String formatNumber(double value, int decimals) {
        StringBuilder pattern = new StringBuilder("0");
        if (decimals > 0) {
            pattern.append('.');
            pattern.append("0".repeat(decimals));
        }
        DecimalFormat format = new DecimalFormat(pattern.toString());
        return format.format(value);
    }
}
