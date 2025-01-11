package dev.skydynamic.litematicaboxitempicker.clientnetwork;

import dev.skydynamic.litematicaboxitempicker.enumration.LSBPPacketType;
import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.PacketSplitter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Optional;

import static fi.dy.masa.litematica.util.InventoryUtils.setPickedItemToHand;

@Environment(EnvType.CLIENT)
public abstract class LSBPClientHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T> {

    private int failures = 0;
    private static final int MAX_FAILURES = 2;
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
        return LSBPPacket.CHANNEL_ID;
    }

    private boolean payloadRegistered = false;
    private boolean serverRegistered = false;

    @Override
    public boolean isPlayRegistered(Identifier channel) {
        if (channel.equals(LSBPPacket.CHANNEL_ID)) {
            return this.payloadRegistered;
        }
        return false;
    }

    @Override
    public void setPlayRegistered(Identifier channel) {
        if (channel.equals(LSBPPacket.CHANNEL_ID)) {
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
        if (payload.getId().id().equals(LSBPPacket.CHANNEL_ID)) {
            LSBPPacket packet = ((LSBPPacket.Payload) payload).data;
            LSBPPacketType type = LSBPPacketType.getPacketType(packet.getBuffer().readVarInt());
            if (type == null) {
                Utils.LOGGER.error("LSBPClientHandler receivePlayPayload unexpected packet type null");
                return;
            }
            switch (type) {
                case LSBPPacketType.PACKET_MOVE_ITEM_SUCCESS:
                    NbtElement stack = packet.getBuffer().readNbt();
                    Optional<ItemStack> targetStack = ItemStack.fromNbt(Utils.REGISTRY, stack);
                    if (targetStack.isEmpty()) {
                        Utils.LOGGER.warn("LSBPClientHandler receivePlayPayload empty stack from server");
                        return;
                    }
                    setPickedItemToHand(targetStack.get(), ctx.client());
                    break;
                case LSBPPacketType.PACKET_MOVE_ITEM_FAILURE:
                    Utils.LOGGER.error("LSBPClientHandler receivePlayPayload move item fail");
                    break;
                default:
                    Utils.LOGGER.error("LSBPClientHandler receivePlayPayload unexpected packet type:{}", type);
            }
        }
    }


    @Override
    public void encodeWithSplitter(PacketByteBuf buffer, ClientPlayNetworkHandler handler) {
        Utils.LOGGER.error("encodeWithSplitter client buffer start: {}", buffer);
        LSBPClientHandler.INSTANCE.sendPlayPayload(new LSBPPacket.Payload(LSBPPacket.moveItemSplitPacketRequest(buffer)));
        Utils.LOGGER.error("encodeWithSplitter client buffer end: {}", buffer);
    }

    @Override
    public <P extends IClientPayloadData> void encodeClientData(P data) {
        LSBPPacket packet = (LSBPPacket) data;
        PacketByteBuf packetByteBuf = packet.getBuffer();
        int ind = packetByteBuf.readVarInt();
        LSBPPacketType type = LSBPPacketType.getPacketType(ind);
        if (type == null) {
            Utils.LOGGER.error("encodeClientData unexpected packet type:{}", packet);
            return;
        }
        if (type == LSBPPacketType.PACKET_MOVE_ITEM_START) {
            Utils.LOGGER.error("encodeClientData client start packet:{}", packet);
            PacketSplitter.send(LSBPClientHandler.INSTANCE, packetByteBuf, MinecraftClient.getInstance().getNetworkHandler());
            Utils.LOGGER.error("encodeClientData client end packet:{}", packet);
        } else if (!LSBPClientHandler.INSTANCE.sendPlayPayload(new LSBPPacket.Payload(packet))) {
            if (this.failures > MAX_FAILURES) {
                Utils.LOGGER.error("LSBPClientHandler encodeClientData(): encountered [{}] sendPayload failures, cancelling any join attempt(s)", MAX_FAILURES);
                this.serverRegistered = false;
                LSBPClientHandler.INSTANCE.unregisterPlayReceiver();
            } else {
                this.failures++;
            }
        }

    }
}
