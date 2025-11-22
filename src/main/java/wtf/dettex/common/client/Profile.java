package wtf.dettex.common.client;

public class Profile {

    public static String getUsername() {
        String USERNAME = switch (System.getProperty("user.name").toLowerCase()) {
            case "admin" -> "Pixik";
            case "root" -> "Bars1k";
            default -> "user";
        };

        return USERNAME;
    }
}

