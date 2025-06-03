package dev.skydynamic.litematicaboxitempicker.network;

import dev.skydynamic.litematicaboxitempicker.enumration.LSBPPacketType;
import dev.skydynamic.litematicaboxitempicker.utils.Configs;
import dev.skydynamic.litematicaboxitempicker.utils.PlayerSlotUtils;
import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.network.IPluginServerPlayHandler;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.PacketSplitter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Environment(EnvType.SERVER)
public abstract class LSBPServerHandler<T extends CustomPayload> implements IPluginServerPlayHandler<T> {
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
//        LBPSPacket.moveItemResponseSuccess();
//        LSBPServerHandler.INSTANCE.sendPlayPayload(player, new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.ResponseS2CData(buffer)));
    }

    @Override
    public Identifier getPayloadChannel() {
        return LSBPPacket.CHANNEL_ID;
    }

    private boolean payloadRegistered = false;

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
//        LSBPServerHandler.INSTANCE.receivePlayPayload(payload, context);

    }


    // 收到客户端数据
    @Override
    public void receivePlayPayload(T payload, ServerPlayNetworking.Context ctx) {
        if (payload.getId().id().equals(LSBPPacket.CHANNEL_ID)) {
            LSBPPacket packet = ((LSBPPacket.Payload) payload).data;
            Utils.LOGGER.info("receivePlayPayload packet:{}", packet);
            LSBPServerHandler.INSTANCE.decodeServerData(LSBPPacket.CHANNEL_ID, ctx.player(),
                    ((LSBPPacket.Payload) payload).data);
        }
    }

    private final Map<UUID, Long> readingSessionKeys = new HashMap<>();

    @Override
    public <P extends IServerPayloadData> void decodeServerData(Identifier channel, ServerPlayerEntity serverPlayer, P data) {
        LSBPPacket packet = (LSBPPacket) data;
        LSBPPacketType type = LSBPPacketType.getPacketType(packet.getBuffer().readVarInt());
        if (type == null) {
            Utils.LOGGER.error("LSBPServerHandler invalid pack type null");
            return;
        }
        if (type == LSBPPacketType.PACKET_MOVE_ITEM_START) {// 完整数据包
            Utils.LOGGER.warn("LSBPServerHandler decodeServerData pack type {}", type);
            dealWithMoveItemRequest(serverPlayer, packet.getBuffer());
            return;
        }
        if (type == LSBPPacketType.PACKET_MOVE_ITEM_DATA) {// 分包
            UUID uuid = serverPlayer.getUuid();
            long readingSessionKey;
            if (!this.readingSessionKeys.containsKey(uuid)) {
                readingSessionKey = Random.create(Util.getMeasuringTimeMs()).nextLong();
                this.readingSessionKeys.put(uuid, readingSessionKey);
            } else {
                readingSessionKey = this.readingSessionKeys.get(uuid);
            }
            PacketByteBuf fullPacket = PacketSplitter.receive(this, readingSessionKey, packet.getBuffer());
            if (fullPacket != null) {
                try {
                    // 分包
                    // type_data [ size [ type_start [ buffer ]  [ buffer ] ] ]
                    /*
                    {
                        PACKET_MOVE_ITEM_DATA,
                        data_size,
                        PACKET_MOVE_ITEM_START,
                        [
                            buffer1,
                            buffer2
                        ]
                    }
                     */
                    this.readingSessionKeys.remove(uuid);
                    int bufSize = fullPacket.readVarInt();
                    int packetType = fullPacket.readVarInt();
                    Utils.LOGGER.warn("LSBPServerHandler decodeServerData fullPacket size {} packet type {}", bufSize, packetType);
                    LSBPPacketType newType = LSBPPacketType.getPacketType(packetType);
                    if (newType != LSBPPacketType.PACKET_MOVE_ITEM_START) {
                        Utils.LOGGER.warn("LSBPServerHandler decodeServerData expected type data but get {}", newType);
                        return;
                    }
                    Utils.LOGGER.warn("LSBPServerHandler decodeServerData with data:{}", fullPacket);

                    dealWithMoveItemRequest(serverPlayer, fullPacket);
                } catch (Exception e) {
                    Servux.logger.error("ServuxLitematicaHandler#decodeServerData(): Litematic Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                }
            }
        }

    }

    private void dealWithMoveItemRequest(ServerPlayerEntity serverPlayer, PacketByteBuf buffer) {
        int maxCount = buffer.readVarInt();
        int hasItemBoxSlotId = buffer.readVarInt();
        Optional<ItemStack> targetStackOpt = ItemStack.fromNbt(Utils.REGISTRY, buffer.readNbt());
        Optional<ItemStack> boxStackOpt = ItemStack.fromNbt(Utils.REGISTRY, buffer.readNbt());
        if (targetStackOpt.isEmpty() || boxStackOpt.isEmpty()) {
            Utils.LOGGER.warn("LSBPServerHandler receivePlayPayload stackOpt or boxStackOpt empty,{},{}",
                    targetStackOpt, boxStackOpt);
            LSBPPacket packet = LSBPPacket.moveItemResponseFailure();
            ServerPlayNetworking.send(serverPlayer, new LSBPPacket.Payload(packet));
            return;
        }
        ItemStack targetStack = targetStackOpt.get();
        ItemStack boxStack = boxStackOpt.get();
        if (!PlayerSlotUtils.isPlayerHaveEmptySlot(serverPlayer)) {
            Utils.LOGGER.warn("LSBPServerHandler receivePlayPayload player don't have empty slot");
            LSBPPacket packet = LSBPPacket.moveItemResponseFailure();
            ServerPlayNetworking.send(serverPlayer, new LSBPPacket.Payload(packet));
            return;
        }
        boolean noSlotCollectIntoBox = Configs.Generic.ENABLE_NO_SLOT_COLLECT_INTO_BOX.getBooleanValue();
        PlayerSlotUtils.moveBoxItem(serverPlayer, targetStack, maxCount, boxStack, hasItemBoxSlotId, noSlotCollectIntoBox);
        LSBPPacket packet = LSBPPacket.moveItemResponseSuccess(targetStack.encode(Utils.REGISTRY));
        ServerPlayNetworking.send(serverPlayer, new LSBPPacket.Payload(packet));
    }
}
