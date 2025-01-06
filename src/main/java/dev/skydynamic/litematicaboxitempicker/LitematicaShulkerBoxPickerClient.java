package dev.skydynamic.litematicaboxitempicker;


import dev.skydynamic.litematicaboxitempicker.config.Reference;
import dev.skydynamic.litematicaboxitempicker.handler.InitHandler;
import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LitematicaShulkerBoxPickerClient implements ClientModInitializer {
    public static final Logger logger = LoggerFactory.getLogger(Reference.MOD_ID);

    @Override
    public void onInitializeClient() {
        Utils.info("LitematicaShulkerBoxPickerClient onInitializeClient start");
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
        Utils.info("LitematicaShulkerBoxPickerClient onInitializeClient end");
    }
}
