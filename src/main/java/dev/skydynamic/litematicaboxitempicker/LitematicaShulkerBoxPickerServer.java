package dev.skydynamic.litematicaboxitempicker;


import dev.skydynamic.litematicaboxitempicker.network.LBPSPacket;
import dev.skydynamic.litematicaboxitempicker.network.LSBPServerHandler;
import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import net.fabricmc.api.DedicatedServerModInitializer;

public class LitematicaShulkerBoxPickerServer implements DedicatedServerModInitializer {
    protected final static LSBPServerHandler<LBPSPacket.Payload> HANDLER =
            LSBPServerHandler.getInstance();
    @Override
    public void onInitializeServer() {
        Utils.LOGGER.error("LitematicaShulkerBoxPickerClient onInitializeServer start");

        HANDLER.registerPlayPayload(LBPSPacket.Payload.ID,
                LBPSPacket.Payload.CODEC,
                IPluginServerPlayHandler.BOTH_SERVER);
        HANDLER.registerPlayReceiver(LBPSPacket.Payload.ID, HANDLER::receivePlayPayload);
        Utils.LOGGER.error("LitematicaShulkerBoxPickerClient onInitializeServer end");

    }


}
