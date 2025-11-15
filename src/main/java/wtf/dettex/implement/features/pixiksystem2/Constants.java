package wtf.dettex.implement.features.pixiksystem2;

public class Constants {
    // Profile
    public static boolean devmode = true;
    public static int UID = 1;
    public static String ROLE = "developer";

    // Links
    public static String DOMAIN = "dettex.space";
    public static String SITE = "https://" + DOMAIN;
    public static String STAFF_URl = SITE + "/staff.json";
    public static String CHANGELOGS_URL = SITE + "/version";
    public static String API_LINK = "https://api." + DOMAIN;
    public static String AI_LINK = API_LINK + "/ai/ask";
    public static String TELEGRAM = "https://telegram." + DOMAIN;
    public static String DISCORD = "https://discord." + DOMAIN;

    // Client
    public static String CLIENTNAME = "dettex.space";
    public static String BUILD = "1.0.0";
    public static String BRAND = "1.21.4 edition";
    public static String PREFIX = "dettex.space » ";
    public static String BUILDMODE = "Dev";
    public static String WINDOWNAME = "Dettex " + BUILDMODE + " [v" + BUILD + "] -> " + SITE;

    // RPC
    public static String GIFLINK = "https://ibb.co/wNgch9k8";
    public static String APPID = "1421764995271757836";

    // Authors
    public static String DEVELOPERS = "Pixik, Bars1k";
    public static String SUPPORT = "t.me/Pixik1337";

    // Telegram
    public static String BOT_TOKEN = "8398579014:AAFTLM9vB3sZm8mYUn9RjbU8m1DVCApfH5M";
    public static String CHAT_ID = "-4909559669";
    public static String NOTIFICATIONPHOTO = "https://cdn.discordapp.com/attachments/1376201876365905940/1421764411135103056/photo_2025-09-28_10-43-32.jpg?ex=68da38ca&is=68d8e74a&hm=1aa89a89907f74f12d3af7832c05eba29ba72c430745653db564298be21a1697&";
    public static String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    // Devmode
    private String setDevUsers() {
        return switch (System.getProperty("user.name").toLowerCase()) {
            case "admin" -> "Pixik";
            case "root" -> "Bars1k";
            default -> "НоуНейм";
        };
    }
}