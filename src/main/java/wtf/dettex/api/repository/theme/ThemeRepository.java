package wtf.dettex.api.repository.theme;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import wtf.dettex.modules.impl.render.Hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static lombok.AccessLevel.PRIVATE;

@Getter
@FieldDefaults(level = PRIVATE)
public class ThemeRepository {
    final List<Theme> themes = new ArrayList<>();
    Theme active;

    public ThemeRepository() {
        // default theme seeded from current HUD color and sane defaults
        Theme def = new Theme(
                "default",
                Hud.getInstance().colorSetting.getColor(),
                0xFF8C7FFF,
                0x1A1A1F,
                0xFFFFFF,
                0xCCCCCC,
                0xE6E6E6
        );
        themes.add(def);
        active = def;
    }

    public void apply(@NonNull Theme theme) {
        this.active = theme;
        // update accent color in HUD
        Hud.getInstance().colorSetting.setColor(theme.getPrimaryColor());
    }

    public Theme findByName(String name) {
        for (Theme t : themes) if (t.getName().equalsIgnoreCase(name)) return t;
        return null;
    }

    public Theme create(String name) {
        String finalName = uniqueName(name == null || name.isBlank() ? "theme" : name);
        Theme base = active != null ? copyOf(active, finalName) : new Theme(finalName, 0xFF6C9AFD, 0xFF8C7FFF, 0x1A1A1F, 0xFFFFFF, 0xCCCCCC, 0xE6E6E6);
        themes.add(base);
        return base;
    }

    private Theme copyOf(Theme src, String name) {
        return new Theme(name, src.getPrimaryColor(), src.getSecondaryColor(), src.getBackgroundColor(), src.getModuleColor(), src.getSettingColor(), src.getTextColor());
    }

    public void delete(String name) {
        Theme t = findByName(name);
        if (t != null) {
            themes.remove(t);
            if (Objects.equals(active, t)) active = themes.isEmpty() ? null : themes.get(0);
        }
    }

    public void rename(String oldName, String newName) {
        Theme t = findByName(oldName);
        if (t != null && newName != null && !newName.isBlank()) {
            t.setName(uniqueName(newName));
        }
    }

    private String uniqueName(String base) {
        String candidate = base;
        int i = 1;
        while (findByName(candidate) != null) candidate = base + "-" + (i++);
        return candidate;
    }
}
