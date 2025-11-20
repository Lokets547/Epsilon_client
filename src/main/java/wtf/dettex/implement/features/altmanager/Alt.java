package wtf.dettex.implement.features.altmanager;

public class Alt {
    private final String username;

    public Alt(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Alt other)) return false;
        return username.equalsIgnoreCase(other.username);
    }

    @Override
    public int hashCode() {
        return username.toLowerCase().hashCode();
    }
}

