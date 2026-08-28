package net.fayber.invisibleitemframes;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

// ModMenu integration: registers the config screen so options can be edited
// from the Mods screen in singleplayer. Only loaded when ModMenu is present
// (client-only, dedicated servers never touch this class). Uses the nicer
// Cloth Config screen if it's installed, otherwise falls back to the
// hand-rolled one.
public class InvisibleItemFramesModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return InvisibleItemFramesClothScreen::create;
        }
        return InvisibleItemFramesConfigScreen::new;
    }
}
