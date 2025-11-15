package wtf.dettex.api.mixins;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dettex.Main;
import wtf.dettex.implement.proxy.ProxyConnection;
import wtf.dettex.implement.proxy.ProxyType;

import java.net.InetSocketAddress;

@Mixin(targets = {"net.minecraft.network.ClientConnection$1"})
public class ClientConnectionInit {
    @Inject(method = "initChannel", at = @At("HEAD"))
    private void dettex$injectProxy(Channel channel, CallbackInfo ci) {
        ProxyConnection pc = Main.getInstance().getProxyConnection();
        if (pc == null || !pc.isEnabled() || pc.getProxyAddr() == null || pc.getProxyType() == ProxyType.DIRECT) return;

        InetSocketAddress addr = pc.getProxyAddr();
        ChannelPipeline pipeline = channel.pipeline();
        try {
            switch (pc.getProxyType()) {
                case SOCKS4 -> {
                    if (pipeline.get("proxy") == null) {
                        pipeline.addFirst("proxy", new Socks4ProxyHandler(addr, pc.getUsername().isEmpty() ? null : pc.getUsername()));
                    }
                }
                case SOCKS5 -> {
                    if (pipeline.get("proxy") == null) {
                        String user = pc.getUsername().isEmpty() ? null : pc.getUsername();
                        String pass = pc.getPassword().isEmpty() ? null : pc.getPassword();
                        pipeline.addFirst("proxy", new Socks5ProxyHandler(addr, user, pass));
                    }
                }
                case HTTP -> {
                    if (pipeline.get("proxy") == null) {
                        String user = pc.getUsername().isEmpty() ? null : pc.getUsername();
                        String pass = pc.getPassword().isEmpty() ? null : pc.getPassword();
                        if (user != null || pass != null) {
                            pipeline.addFirst("proxy", new HttpProxyHandler(addr, user, pass));
                        } else {
                            pipeline.addFirst("proxy", new HttpProxyHandler(addr));
                        }
                    }
                }
                default -> {}
            }
        } catch (Throwable ignored) {}
    }
}
