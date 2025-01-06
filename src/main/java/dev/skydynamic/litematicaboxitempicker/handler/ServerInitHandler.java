package dev.skydynamic.litematicaboxitempicker.handler;

import dev.skydynamic.litematicaboxitempicker.network.LSBPServerHandler;
import dev.skydynamic.litematicaboxitempicker.network.LSBPPacket;
import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import net.minecraft.server.MinecraftServer;

public class ServerInitHandler implements IServerListener {
    protected final static LSBPServerHandler<LSBPPacket.Payload> HANDLER =
            LSBPServerHandler.getInstance();

    @Override
    public void onServerStarted(MinecraftServer server) {
        HANDLER.registerPlayPayload(LSBPPacket.Payload.ID,
                LSBPPacket.Payload.CODEC,
                IPluginServerPlayHandler.BOTH_SERVER);
        HANDLER.registerPlayReceiver(LSBPPacket.Payload.ID, HANDLER::receivePlayPayload);
    }
}
