package dev.skydynamic.litematicaboxitempicker.network;

import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static fi.dy.masa.litematica.util.InventoryUtils.setPickedItemToHand;

@Environment(EnvType.CLIENT)
public abstract class LSBPClientHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T> {

    public static final Identifier CHANNEL_ID = Identifier.of("lsbp", "move_count_item");
    private int failures = 0;
    private static final int MAX_FAILURES = 4;
    private static final LSBPClientHandler<LSBPPacket.Payload> INSTANCE =
            new LSBPClientHandler<>() {
                @Override
                public void receive(LSBPPacket.Payload payload, ClientPlayNetworking.Context context) {
                    LSBPClientHandler.INSTANCE.receivePlayPayload(payload, context);
                }
            };

    public static LSBPClientHandler<LSBPPacket.Payload> getInstance() {
        return INSTANCE;
    }

    @Override
    public Identifier getPayloadChannel() {
        return CHANNEL_ID;
    }

    private boolean payloadRegistered = false;

    @Override
    public boolean isPlayRegistered(Identifier channel) {
        if (channel.equals(CHANNEL_ID)) {
            return this.payloadRegistered;
        }
        return false;
    }

    @Override
    public void setPlayRegistered(Identifier channel) {
        if (channel.equals(CHANNEL_ID)) {
            this.payloadRegistered = true;
        }
    }

    @Override
    public void reset(Identifier channel) {
        // todo whether need to reset
    }

    // 收到服务端数据
    @Override
    public void receivePlayPayload(T payload, ClientPlayNetworking.Context ctx) {
        if (payload.getId().id().equals(CHANNEL_ID)) {
            LSBPPacket packet = ((LSBPPacket.Payload) payload).data();
            switch (packet.getPacketType()) {
                case LSBPPacket.Type.PACKET_MOVE_ITEM_SUCCESS:
                    ItemStack stack = packet.getTargetStack();
                    setPickedItemToHand(stack, ctx.client());
                    break;
                case LSBPPacket.Type.PACKET_MOVE_ITEM_FAILURE:
                    Utils.info("LSBPClientHandler receivePlayPayload move item fail");
                    break;
                default:
                    Utils.info("LSBPClientHandler receivePlayPayload unexpected packet type");
            }
        }
    }
    @Override
    public void encodeWithSplitter(PacketByteBuf buffer, ClientPlayNetworkHandler handler) {
        // todo whether need to encodeWithSplitter

    }


    @Override
    public <P extends IClientPayloadData> void encodeClientData(P data) {

//        IPluginClientPlayHandler.super.encodeClientData(data);
    }
}
