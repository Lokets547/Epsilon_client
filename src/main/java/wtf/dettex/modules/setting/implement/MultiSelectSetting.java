package wtf.dettex.modules.setting.implement;

import wtf.dettex.modules.setting.Setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class MultiSelectSetting extends Setting {
    private List<String> list = Collections.emptyList();
    private final List<String> selected = new ArrayList<>();

    public MultiSelectSetting(String name, String description) {
        super(name, description);
    }

    public MultiSelectSetting value(String... settings) {
        list = Collections.unmodifiableList(Arrays.asList(settings));
        selected.removeIf(value -> !list.contains(value));
        return this;
    }

    public MultiSelectSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public List<String> getList() {
        return list;
    }

    public List<String> getSelected() {
        return Collections.unmodifiableList(selected);
    }

    public boolean isSelected(String name) {
        return selected.contains(name);
    }

    public void setSelected(List<String> values) {
        selected.clear();
        if (values == null || list.isEmpty()) return;
        values.stream()
                .filter(value -> list.stream().anyMatch(entry -> Objects.equals(entry, value)))
                .forEachOrdered(this::addInOrder);
    }

    public void toggle(String value) {
        if (list.isEmpty()) return;
        if (list.stream().noneMatch(entry -> Objects.equals(entry, value))) return;

        if (selected.contains(value)) {
            selected.remove(value);
        } else {
            addInOrder(value);
        }
    }

    private void addInOrder(String value) {
        if (selected.contains(value)) return;
        int targetIndex = list.indexOf(value);
        if (targetIndex < 0) return;

        for (int i = 0; i < selected.size(); i++) {
            int currentIndex = list.indexOf(selected.get(i));
            if (targetIndex < currentIndex) {
                selected.add(i, value);
                return;
            }
        }
        selected.add(value);
    }
}

