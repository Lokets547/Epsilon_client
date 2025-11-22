package wtf.dettex.api.file.impl;

import com.google.gson.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import wtf.dettex.api.file.ClientFile;
import wtf.dettex.api.file.exception.FileLoadException;
import wtf.dettex.api.file.exception.FileSaveException;
import wtf.dettex.api.repository.theme.Theme;
import wtf.dettex.api.repository.theme.ThemeRepository;
import wtf.dettex.Main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ThemeFile extends ClientFile {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ThemeFile() {
        super("themes");
    }

    private ThemeRepository repo() {
        return Main.getInstance().getThemeRepository();
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        saveToFile(path, getName() + ".json");
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        loadFromFile(path, getName() + ".json");
    }

    @Override
    public void saveToFile(File path, String fileName) throws FileSaveException {
        File file = new File(path, fileName);
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (Theme t : repo().getThemes()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", t.getName());
            o.addProperty("primary", t.getPrimaryColor());
            o.addProperty("secondary", t.getSecondaryColor());
            o.addProperty("background", t.getBackgroundColor());
            o.addProperty("module", t.getModuleColor());
            o.addProperty("setting", t.getSettingColor());
            o.addProperty("text", t.getTextColor());
            arr.add(o);
        }
        root.add("themes", arr);
        Theme active = repo().getActive();
        if (active != null) root.addProperty("active", active.getName());
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(root, writer);
        } catch (IOException e) {
            throw new FileSaveException("Failed to save themes", e);
        }
    }

    @Override
    public void loadFromFile(File path, String fileName) throws FileLoadException {
        File file = new File(path, fileName);
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            repo().getThemes().clear();
            String activeName = root.has("active") && root.get("active").isJsonPrimitive() ? root.get("active").getAsString() : null;
            if (root.has("themes") && root.get("themes").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("themes")) {
                    JsonObject o = el.getAsJsonObject();
                    Theme t = new Theme(
                            o.get("name").getAsString(),
                            o.get("primary").getAsInt(),
                            o.get("secondary").getAsInt(),
                            o.get("background").getAsInt(),
                            o.get("module").getAsInt(),
                            o.get("setting").getAsInt(),
                            o.get("text").getAsInt()
                    );
                    repo().getThemes().add(t);
                }
            }
            Theme active = activeName != null ? repo().findByName(activeName) : (repo().getThemes().isEmpty() ? null : repo().getThemes().get(0));
            if (active != null) repo().apply(active);
        } catch (IOException | IllegalStateException e) {
            throw new FileLoadException("Failed to load themes", e);
        }
    }
}
