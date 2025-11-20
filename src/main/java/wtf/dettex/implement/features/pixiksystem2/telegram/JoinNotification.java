package wtf.dettex.implement.features.pixiksystem2.telegram;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import wtf.dettex.implement.features.pixiksystem2.Constants;
import wtf.dettex.common.client.Profile;

import static wtf.dettex.implement.features.pixiksystem2.Constants.*;

public class JoinNotification {

    private static volatile boolean sentOnce = false;
    

    public static void sendAsync() {
        if (sentOnce) return;
        Thread t = new Thread(() -> {
            try {
                sendNotification();
            } finally {
                sentOnce = true;
            }
        }, "JoinNotification");
        t.setDaemon(true);
        t.start();
    }

    public static void sendNotification() {
        try {
            String username = Profile.getUsername();
            String prefix = (username.equalsIgnoreCase("Pixik") ||
                    username.equalsIgnoreCase("Bars1k"))
                    ? "Администратор" : "Пользователь";

            String message = "✨ " + prefix + " <b>" + username + "</b>" +
                    " зашёл в клиент с билда <b>" + Constants.BUILD + "</b>.";

            String data = "chat_id=" + URLEncoder.encode(Constants.CHAT_ID, "UTF-8") +
                    "&caption=" + URLEncoder.encode(message, "UTF-8") +
                    "&photo=" + URLEncoder.encode(NOTIFICATIONPHOTO, "UTF-8") +
                    "&parse_mode=HTML";

            URL url = new URL(TELEGRAM_API_URL + BOT_TOKEN + "/sendPhoto");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "DettexBot/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(7000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes("UTF-8"));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            String responseBody = null;
            try (InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                if (is != null) {
                    responseBody = new String(is.readAllBytes());
                }
            }
            if (responseCode != 200) {
              //   System.err.println("JoinNotification: Telegram API error code=" + responseCode + ", body=" + responseBody);
            } else {
            //     System.out.println("JoinNotification: sent successfully. body=" + responseBody);
            }

            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
