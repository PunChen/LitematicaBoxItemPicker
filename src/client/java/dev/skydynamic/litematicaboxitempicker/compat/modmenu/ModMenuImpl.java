package dev.skydynamic.litematicaboxitempicker.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.skydynamic.litematicaboxitempicker.config.LSBPConfigGui;

public class ModMenuImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (screen) -> {
            LSBPConfigGui gui = new LSBPConfigGui();
            gui.setParent(screen);
            return gui;
        };
    }
}
