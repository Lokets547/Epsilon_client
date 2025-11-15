package wtf.dettex.api.other.command.datatypes;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.util.InputUtil;
import wtf.dettex.api.other.command.exception.CommandException;
import wtf.dettex.api.other.command.helpers.TabCompleteHelper;
import wtf.dettex.common.util.other.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public enum KeyDataType implements IDatatypeFor<Map.Entry<String, Integer>> {
    INSTANCE;

    @Override
    public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
        Stream<String> keys = getKeys()
                .keySet()
                .stream();

        String context = datatypeContext
                .getConsumer()
                .getString();

        return new TabCompleteHelper()
                .append(keys)
                .filterPrefix(context)
                .sortAlphabetically()
                .stream();
    }

    @Override
    public Map.Entry<String, Integer> get(IDatatypeContext datatypeContext) throws CommandException {
        String key = datatypeContext
                .getConsumer()
                .getString();

        return getKeys()
                .entrySet()
                .stream()
                .filter(s -> s.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Integer> getKeys() {
        Map<String, Integer> keys = new HashMap<>();
        for (Int2ObjectMap.Entry<InputUtil.Key> entry : InputUtil.Type.KEYSYM.map.int2ObjectEntrySet()) {
            int keyCode = entry.getIntKey();
            String bindName = StringUtil.getBindName(keyCode).toLowerCase();
            keys.put(bindName, keyCode);
        }
        return keys;
    }
}
