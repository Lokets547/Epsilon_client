package wtf.dettex.modules.setting.implement;

import lombok.Getter;
import wtf.dettex.modules.setting.Setting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Getter
public class SelectSetting extends Setting {
    private String selected;
    private List<String> list = Collections.emptyList();

    public SelectSetting(String name, String description) {
        super(name, description);
    }

    public SelectSetting value(String... values) {
        List<String> list = Arrays.asList(values);

        this.list = Collections.unmodifiableList(list);
        this.selected = this.list.isEmpty() ? null : this.list.getFirst();

        return this;
    }

    public SelectSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public SelectSetting selected(String value) {
        setSelected(value);
        return this;
    }

    public boolean isSelected(String name) {
        if (selected == null) return false;
        return selected.equals(name);
    }

    public void setSelected(String value) {
        if (list.isEmpty()) {
            this.selected = null;
            return;
        }

        Optional<String> exact = list.stream().filter(entry -> Objects.equals(entry, value)).findFirst();
        this.selected = exact.orElseGet(() -> list.getFirst());
    }

}
