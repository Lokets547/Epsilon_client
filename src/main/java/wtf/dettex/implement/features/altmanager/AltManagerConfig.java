package wtf.dettex.implement.features.altmanager;

import com.google.gson.*;
import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.other.StringUtil;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AltManagerConfig implements QuickImports {
    private static final File FILE = new File(MinecraftClient.getInstance().runDirectory, "dettex/accounts/alt.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void saveAccounts(String selectedUsername) {
        if (!FILE.exists() && FILE.getParentFile() != null) {
            FILE.getParentFile().mkdirs();
        }
        try (FileWriter fileWriter = new FileWriter(FILE)) {
            JsonArray accountsArray = new JsonArray();

            JsonObject currentAccountObject = new JsonObject();
            currentAccountObject.addProperty("selected", selectedUsername);
            accountsArray.add(currentAccountObject);

            for (Alt alt : AltManagerScreen.ALTS) {
                JsonObject accountObject = new JsonObject();
                accountObject.addProperty("username", alt.getUsername());
                accountsArray.add(accountObject);
            }

            JsonObject object = new JsonObject();
            object.add("accounts", accountsArray);

            fileWriter.write(GSON.toJson(object));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static String loadAccounts(List<Alt> alts) {
        if (!FILE.exists()) {
            return null;
        }

        alts.clear();
        String selected = null;
        try (FileReader fileReader = new FileReader(FILE)) {
            JsonObject object = GSON.fromJson(fileReader, JsonObject.class);
            if (object != null && object.has("accounts")) {
                JsonArray accountsArray = object.getAsJsonArray("accounts");
                for (JsonElement element : accountsArray) {
                    if (!element.isJsonObject()) continue;
                    JsonObject accountObject = element.getAsJsonObject();
                    if (accountObject.has("selected")) {
                        selected = accountObject.get("selected").getAsString();
                    } else if (accountObject.has("username")) {
                        alts.add(new Alt(accountObject.get("username").getAsString()));
                    }
                }
            }
        } catch (IOException | JsonParseException e) {
            e.printStackTrace();
        }
        return selected;
    }

    public static String loadAccountsAndApply(List<Alt> alts) {
        String selected = loadAccounts(alts);
        if (selected != null && !selected.isEmpty()) {
            applyOfflineSession(selected);
        }
        return selected;
    }

    public static void applyOfflineSession(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        try {
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(UTF_8));
            Session session = new Session(username, uuid, "", Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
            StringUtil.setSession(session);
        } catch (AuthenticationException exception) {
            exception.printStackTrace();
        }
    }
}
