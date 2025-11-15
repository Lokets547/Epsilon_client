package wtf.dettex.implement.screen.proxy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import wtf.dettex.Main;
import wtf.dettex.implement.proxy.ProxyConnection;
import wtf.dettex.implement.proxy.ProxyType;

import java.net.InetSocketAddress;
import java.util.Locale;

public class ProxyScreen extends Screen {
    private TextFieldWidget proxyField;
    private TextFieldWidget usernameField;
    private TextFieldWidget passwordField;
    private ButtonWidget enableButton;
    private boolean proxyEnabled;
    private final ProxyConnection proxyConnection = Main.getInstance().getProxyConnection();

    public ProxyScreen() {
        super(Text.of("Proxy"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        proxyField = new TextFieldWidget(textRenderer, cx - 100, cy + 5, 200, 20, Text.of("proxy"));
        proxyField.setMaxLength(128);
        if (proxyConnection.getProxyAddr() != null) {
            String preset = proxyConnection.getProxyAddr().getHostString() + ":" + proxyConnection.getProxyAddr().getPort();
            proxyField.setText(preset);
        }
        addSelectableChild(proxyField);

        usernameField = new TextFieldWidget(textRenderer, cx - 100, cy + 45, 200, 20, Text.of("username"));
        usernameField.setMaxLength(128);
        usernameField.setText(proxyConnection.getUsername() == null ? "" : proxyConnection.getUsername());
        addSelectableChild(usernameField);

        passwordField = new TextFieldWidget(textRenderer, cx - 100, cy + 85, 200, 20, Text.of("password"));
        passwordField.setMaxLength(128);
        passwordField.setText(proxyConnection.getPassword() == null ? "" : proxyConnection.getPassword());
        addSelectableChild(passwordField);

        proxyEnabled = proxyConnection.isEnabled();
        enableButton = ButtonWidget.builder(Text.of(getEnableLabel()), b -> toggleEnabled())
                .dimensions(cx - 100, cy - 45, 200, 20)
                .build();
        this.addDrawableChild(enableButton);

        addDrawableChild(ButtonWidget.builder(Text.of("Применить"), b -> applyProxy())
                .dimensions(cx - 100 - 5, cy + 115, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.of("Назад"), b -> MinecraftClient.getInstance().setScreen(new MultiplayerScreen(null)))
                .dimensions(cx + 5, cy + 115, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        String cProxy;
        if (proxyConnection != null && proxyConnection.getProxyAddr() != null) {
            cProxy = proxyConnection.getProxyAddr().getHostString() + ":" + proxyConnection.getProxyAddr().getPort();
        } else {
            cProxy = "БЕЗ ПРОКСИ";
        }

        int cx = this.width / 2;
        int cy = this.height / 2;
        context.drawCenteredTextWithShadow(textRenderer, "Активный прокси: " + cProxy, cx, cy - 70, 0xFFFFFF);

        context.drawTextWithShadow(textRenderer, "Прокси (host:port):", cx - 100, cy - 5, 0xCCCCCC);
        proxyField.render(context, mouseX, mouseY, delta);

        context.drawTextWithShadow(textRenderer, "Username:", cx - 100, cy + 35, 0xCCCCCC);
        usernameField.render(context, mouseX, mouseY, delta);

        context.drawTextWithShadow(textRenderer, "Password:", cx - 100, cy + 75, 0xCCCCCC);
        passwordField.render(context, mouseX, mouseY, delta);
    }

    private void applyProxy() {
        String input = proxyField.getText();
        if (input == null) return;
        input = input.trim().toLowerCase(Locale.ROOT);
        try {
            String addr = input.contains("://") ? input.split("://", 2)[1] : input;
            String host = addr.split(":", 2)[0];
            int port = Integer.parseInt(addr.split(":", 2)[1]);
            ProxyType type = inferType(input, port, usernameField.getText(), passwordField.getText());

            if (proxyConnection != null) {
                proxyConnection.setup(type, new InetSocketAddress(host, port));
                proxyConnection.setUsername(usernameField.getText() == null ? "" : usernameField.getText());
                proxyConnection.setPassword(passwordField.getText() == null ? "" : passwordField.getText());
                proxyConnection.setEnabled(proxyEnabled && type != ProxyType.DIRECT);
            }

            if (proxyEnabled) updateSystemProxy(type, host, port); else clearSocksProperties();
        } catch (Exception e) {
            if (proxyConnection != null) {
                proxyConnection.reset();
            }
            clearSocksProperties();
        }
    }

    private ProxyType inferType(String rawInput, int port, String user, String pass) {
        if (rawInput.startsWith("http://")) return ProxyType.HTTP;
        if (rawInput.startsWith("socks4://")) return ProxyType.SOCKS4;
        if (rawInput.startsWith("socks5://")) return ProxyType.SOCKS5;

        if ((user != null && !user.isEmpty()) || (pass != null && !pass.isEmpty())) return ProxyType.SOCKS5;

        if (port == 8080 || port == 3128 || port == 8000 || port == 8888) return ProxyType.HTTP;
        if (port == 1080) return ProxyType.SOCKS5;
        if (port == 1081) return ProxyType.SOCKS4;

        return ProxyType.SOCKS5;
    }

    private void toggleEnabled() {
        proxyEnabled = !proxyEnabled;
        enableButton.setMessage(Text.of(getEnableLabel()));
        if (!proxyEnabled) {
            clearSocksProperties();
            if (proxyConnection != null) proxyConnection.setEnabled(false);
        }
    }

    private String getEnableLabel() {
        return (proxyEnabled ? "[✔] " : "[❌] ") + "Включить прокси";
    }

    private void updateSystemProxy(ProxyType type, String host, int port) {
        switch (type) {
            case SOCKS4:
            case SOCKS5:
                System.setProperty("socksProxyHost", host);
                System.setProperty("socksProxyPort", String.valueOf(port));
                break;
            default:
                clearSocksProperties();
                break;
        }
    }

    private void clearSocksProperties() {
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
    }
}
