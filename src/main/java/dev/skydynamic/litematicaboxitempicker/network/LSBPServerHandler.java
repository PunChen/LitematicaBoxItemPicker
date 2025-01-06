package dev.skydynamic.litematicaboxitempicker.network;

import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import static dev.skydynamic.litematicaboxitempicker.utils.PlayerSlotUtils.isPlayerHaveEmptySlot;
import static dev.skydynamic.litematicaboxitempicker.utils.PlayerSlotUtils.moveBoxItem;

@Environment(EnvType.SERVER)
public abstract class LSBPServerHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T> {
    public static final Identifier CHANNEL_ID = Identifier.of("lsbp", "move_count_item");
    private int failures = 0;
    private static final int MAX_FAILURES = 4;
    private static final LSBPServerHandler<LSBPPacket.Payload> INSTANCE =
            new LSBPServerHandler<>() {
                @Override
                public void receive(LSBPPacket.Payload payload, ServerPlayNetworking.Context context) {
                    LSBPServerHandler.INSTANCE.receivePlayPayload(payload, context);
                }
            };

    public static LSBPServerHandler<LSBPPacket.Payload> getInstance() {
        return INSTANCE;
    }

    @Override
    public void encodeWithSplitter(ServerPlayerEntity player, PacketByteBuf buffer, ServerPlayNetworkHandler networkHandler) {
        // todo Send each PacketSplitter buffer slice
//        LSBPServerHandler.INSTANCE.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.ResponseS2CData(buffer)));
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
//        LSBPServerHandler.INSTANCE.receivePlayPayload(payload, context);
    }

    // 收到客户端数据
    @Override
    public void receivePlayPayload(T payload, ServerPlayNetworking.Context ctx) {
        if (payload.getId().id().equals(CHANNEL_ID)) {
            LSBPPacket packet = ((LSBPPacket.Payload) payload).data();
            ServerPlayerEntity serverPlayer = ctx.player();
            if (packet.getPacketType() != LSBPPacket.Type.PACKET_MOVE_ITEM) {
                Utils.info("LSBPServerHandler unexpected pack type {}", packet.getPacketType());
                ServerPlayNetworking.send(serverPlayer,
                        new LSBPPacket.Payload(LSBPPacket.moveItemResponseFailure()));
                return;
            }
            int maxCount = packet.getMaxMoveCount();
            int hasItemBoxSlotId = packet.getHasItemBoxSlotId();
            ItemStack targetStack = packet.getTargetStack();
            ItemStack boxStack = packet.getBoxStack();
            if (!isPlayerHaveEmptySlot(serverPlayer)) {
                Utils.info("LSBPServerHandler receivePlayPayload player don't have empty slot");
                return;
            }
            moveBoxItem(serverPlayer, targetStack, boxStack, maxCount, hasItemBoxSlotId);
            ServerPlayNetworking.send(serverPlayer, new LSBPPacket
                    .Payload(LSBPPacket.moveItemResponseSuccess(targetStack)));
            Utils.info("LSBPServerHandler receivePlayPayload done");
        }
    }
}
