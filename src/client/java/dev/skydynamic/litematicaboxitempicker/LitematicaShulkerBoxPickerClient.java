package dev.skydynamic.litematicaboxitempicker;


import dev.skydynamic.litematicaboxitempicker.handler.InitHandler;
import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;

public class LitematicaShulkerBoxPickerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Utils.LOGGER.error("LitematicaShulkerBoxPickerClient onInitializeClient start");
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
        Utils.LOGGER.error("LitematicaShulkerBoxPickerClient onInitializeClient end");
    }
}
