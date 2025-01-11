package dev.skydynamic.litematicaboxitempicker.handler;

import dev.skydynamic.litematicaboxitempicker.utils.Reference;
import dev.skydynamic.litematicaboxitempicker.config.Configs;
import dev.skydynamic.litematicaboxitempicker.config.LitematicaShulkerBoxPickerConfigGui;
import dev.skydynamic.litematicaboxitempicker.clientnetwork.LBPSPacket;
import dev.skydynamic.litematicaboxitempicker.clientnetwork.LSBPClientHandler;
import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;

public class InitHandler implements IInitializationHandler {

    private final static LSBPClientHandler<LBPSPacket.Payload> HANDLER =
            LSBPClientHandler.getInstance();

    @Override
    public void registerModHandlers() {
        Utils.LOGGER.error("LitematicaShulkerBoxPickerHandler registerModHandlers start");
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        InputEventHandler.getKeybindManager().registerKeybindProvider(LitematicaShulkerBoxPickerInputHandler.getInstance());

        Configs.Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind().setCallback(new KeyCallbackHotkeys());
        Configs.Hotkeys.ENABLE_LSBP.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.ENABLE_LSBP));

        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(LBPSPacket.Payload.ID,
                LBPSPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);//客户端收发的
        Utils.LOGGER.error("LitematicaShulkerBoxPickerHandler registerModHandlers end");
    }

    private static class KeyCallbackHotkeys implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            if (key == Configs.Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind()) {
                GuiBase.openGui(new LitematicaShulkerBoxPickerConfigGui());
            }
            return true;
        }
    }
}
