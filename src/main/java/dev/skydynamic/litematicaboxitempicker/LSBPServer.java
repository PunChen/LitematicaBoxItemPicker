package dev.skydynamic.litematicaboxitempicker;


import dev.skydynamic.litematicaboxitempicker.network.LSBPPacket;
import dev.skydynamic.litematicaboxitempicker.network.LSBPServerHandler;
import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import net.fabricmc.api.DedicatedServerModInitializer;

public class LSBPServer implements DedicatedServerModInitializer {
    protected final static LSBPServerHandler<LSBPPacket.Payload> HANDLER =
            LSBPServerHandler.getInstance();

    @Override
    public void onInitializeServer() {
        Utils.LOGGER.warn("LitematicaShulkerBoxPickerServer onInitializeServer start");

        try {
            HANDLER.registerPlayPayload(LSBPPacket.Payload.ID,
                    LSBPPacket.Payload.CODEC,
                    IPluginServerPlayHandler.BOTH_SERVER);
            HANDLER.registerPlayReceiver(LSBPPacket.Payload.ID, HANDLER::receivePlayPayload);
        } catch (Exception e) {
            Utils.LOGGER.error("onInitializeServer error ", e);
        }

        Utils.LOGGER.warn("LitematicaShulkerBoxPickerServer onInitializeServer end");

    }


}
