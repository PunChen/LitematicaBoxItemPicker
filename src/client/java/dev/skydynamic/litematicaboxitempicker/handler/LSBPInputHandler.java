package dev.skydynamic.litematicaboxitempicker.handler;

import com.google.common.collect.ImmutableList;
import dev.skydynamic.litematicaboxitempicker.config.Configs;
import dev.skydynamic.litematicaboxitempicker.utils.Reference;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

import java.util.List;

public class LSBPInputHandler implements IKeybindProvider {

    private static final LSBPInputHandler INSTANCE = new LSBPInputHandler();

    private LSBPInputHandler() {
        super();
    }

    public static LSBPInputHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        manager.addKeybindToMap(Configs.Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind());
        manager.addKeybindToMap(Configs.Hotkeys.ENABLE_LSBP.getKeybind());
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        List<? extends IHotkey> hotkeys = ImmutableList.of(
                Configs.Hotkeys.OPEN_GUI_MAIN_MENU,
                Configs.Hotkeys.ENABLE_LSBP
        );
        manager.addHotkeysForCategory(Reference.MOD_NAME, "hotkeys.category.generic_hotkeys", hotkeys);
    }

}
