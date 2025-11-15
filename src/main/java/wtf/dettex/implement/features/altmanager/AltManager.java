package wtf.dettex.implement.features.altmanager;

import java.util.ArrayList;
import java.util.List;

public class AltManager {
    private static final AltManager INSTANCE = new AltManager();

    private final List<Alt> alts = new ArrayList<>();

    private AltManager() {
    }

    public static AltManager getInstance() {
        return INSTANCE;
    }

    public List<Alt> getAccounts() {
        return alts;
    }

    public void add(Alt alt) {
        alts.add(alt);
    }

    public void delete(Alt alt) {
        alts.remove(alt);
    }
}
