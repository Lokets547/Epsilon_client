package wtf.dettex.modules.impl.render;

import dev.redstones.mediaplayerinfo.IMediaSession;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.keyboard.KeyEvent;
import wtf.dettex.implement.features.draggables.MediaPlayer;
import wtf.dettex.modules.setting.implement.*;
import wtf.dettex.modules.setting.implement.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hud extends Module {
    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Elements", "Customize the interface elements")
                .value("Watermark", "Hot Keys", "Potions", "Staff List", "Target Hud", "Armor", "Cool Downs", "Inventory", "Player Info", "Boss Bars", "Notifications", "Score Board", "Media Player", "HotBar");

    public SelectSetting watermarkStyle = new SelectSetting("Watermark Style", "Select primary watermark block style")
            .value("Client Name", "Client Logo", "None")
            .selected("Client Name")
            .visible(() -> interfaceSettings.isSelected("Watermark"));

    public MultiSelectSetting notificationSettings = new MultiSelectSetting("Notifications", "Choose when the notification will appear")
            .value("Module Switch", "Staff Join", "Item Pick Up", "Auto Armor", "Break Shield").visible(()-> interfaceSettings.isSelected("Notifications"));

    public ColorSetting colorSetting = new ColorSetting("Client Color", "Select your client's color")
            .setColor(0xFF6C9AFD).presets(0xFF6C9AFD, 0xFF8C7FFF, 0xFFFFA576, 0xFFFF7B7B);
    public static SelectSetting hudType = new SelectSetting("Client Style", "Select the client's style").value("Blur", "New");

    public static ValueSetting newHudAlpha = new ValueSetting("Hud Alpha", "Accept you customize the blur's alpha").range(0.1f, 1.0f);

    public static BooleanSetting glassBlur = new BooleanSetting("Glass Blur", "Turn on glass blur").visible(()-> hudType.isSelected("Liquid Glass")).setValue(false);

    public static ValueSetting glassBlurValue = new ValueSetting("Glass Blur", "How much is going to blur the glass").range(0.0f, 4.0f).visible(() -> glassBlur.isValue() && (hudType.isSelected("Liquid Glass"))).setValue(2.5f);


    public static ValueSetting glassOutlineSoftness = new ValueSetting("Outline Softness", "How soft is going to be the outline").range(0.0f, 10.0f).visible(() -> hudType.isSelected("Liquid Glass")).setValue(1.0f);

    public static ValueSetting glassOutlineThickness = new ValueSetting("Outline Thickness", "How thick is going to be the outline").range(0.0f, 2.5f).visible(() -> hudType.isSelected("Liquid Glass")).setValue(1.0f);

    public static GroupSetting glassSettings = new GroupSetting("Glass Settings", "Settings for the glass hud").visible(()-> hudType.isSelected("Liquid Glass")).settings(glassBlur, glassBlurValue, glassOutlineSoftness, glassOutlineThickness);

    BindSetting preSetting = new BindSetting("Previous Audio", "Turn on previous audio")
            .visible(()-> interfaceSettings.isSelected("Media Player"));

    BindSetting playSetting = new BindSetting("Stop/Play Audio",   "Stop/Play current audio")
            .visible(()-> interfaceSettings.isSelected("Media Player"));

    BindSetting nextSetting = new BindSetting("Next Audio","Turn on next audio")
            .visible(()-> interfaceSettings.isSelected("Media Player"));

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        setup(hudType, newHudAlpha, colorSetting, interfaceSettings, watermarkStyle, notificationSettings, preSetting, playSetting, nextSetting);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        IMediaSession session = MediaPlayer.getInstance().session;
        if (interfaceSettings.isSelected("Media Player") && session != null) {
            if (e.isKeyDown(preSetting.getKey(), true)) session.previous();
            if (e.isKeyDown(playSetting.getKey(), true)) session.playPause();
            if (e.isKeyDown(nextSetting.getKey(), true)) session.next();
        }
    }
    private void setup() {
        glassOutlineSoftness.setValue(0.0f);
        glassOutlineThickness.setValue(0.0f);
        return;
    }
}
