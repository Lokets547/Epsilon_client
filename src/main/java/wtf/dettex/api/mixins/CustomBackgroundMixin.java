package wtf.dettex.api.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import wtf.dettex.implement.screen.mainmenu.CustomMainMenu;

@Mixin({
        TitleScreen.class,
        SelectWorldScreen.class,
        CreateWorldScreen.class,
        MultiplayerScreen.class,
        OptionsScreen.class
})
public class CustomBackgroundMixin extends Screen {

    protected CustomBackgroundMixin(Text title) {
        super(title);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        CustomMainMenu.renderTitleBackground(context, this.width, this.height);
    }
}
