package wtf.dettex.implement.proxy;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

public class ProxyConnection {
    @Getter private ProxyType proxyType = ProxyType.DIRECT;
    @Getter private InetSocketAddress proxyAddr = null;
    @Getter @Setter private boolean enabled = false;
    @Getter @Setter private String username = "";
    @Getter @Setter private String password = "";

    public void setup(ProxyType proxyType, InetSocketAddress proxyAddr) {
        this.proxyType = proxyType;
        this.proxyAddr = proxyAddr;
        this.enabled = proxyType != ProxyType.DIRECT && proxyAddr != null;
    }

    public void reset() {
        proxyType = ProxyType.DIRECT;
        proxyAddr = null;
        enabled = false;
        username = "";
        password = "";
    }
}

