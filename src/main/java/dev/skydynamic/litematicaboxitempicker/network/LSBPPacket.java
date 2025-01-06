package dev.skydynamic.litematicaboxitempicker.network;

import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.litematica.Litematica;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;

import static dev.skydynamic.litematicaboxitempicker.network.LSBPPacket.Type.*;

public class LSBPPacket {
    /**
     * 发送数据包类型
     */
    private Type packetType;
    /**
     * 缓存
     */
    private PacketByteBuf buffer;
    /**
     * 最大移动数量
     */
    private int maxMoveCount = -1;
    /**
     * 有指定物品的潜影盒所在槽位Id
     */
    private int hasItemBoxSlotId = -1;
    /**
     * 目标物品
     */
    private ItemStack targetStack = null;
    /**
     * 潜影盒
     */
    private ItemStack boxStack = null;

    /**
     * 目标物品
     */
    private NbtCompound targetStackNbt = null;
    /**
     * 潜影盒
     */
    private NbtCompound boxStackNbt = null;
    public static final int PROTOCOL_VERSION = 1;

    public ItemStack getTargetStack() {
        return targetStack;
    }

    public PacketByteBuf getBuffer() {
        return buffer;
    }

    public void setBuffer(PacketByteBuf buffer) {
        this.buffer = buffer;
    }

    public Type getPacketType() {
        return packetType;
    }

    public void setPacketType(Type packetType) {
        this.packetType = packetType;
    }

    public int getMaxMoveCount() {
        return maxMoveCount;
    }

    public void setMaxMoveCount(int maxMoveCount) {
        this.maxMoveCount = maxMoveCount;
    }

    public int getHasItemBoxSlotId() {
        return hasItemBoxSlotId;
    }

    public void setHasItemBoxSlotId(int hasItemBoxSlotId) {
        this.hasItemBoxSlotId = hasItemBoxSlotId;
    }

    public void setTargetStack(ItemStack targetStack) {
        this.targetStack = targetStack;
    }

    public ItemStack getBoxStack() {
        return boxStack;
    }

    public void setBoxStack(ItemStack boxStack) {
        this.boxStack = boxStack;
    }

    public static LSBPPacket moveItemRequest(int maxMoveCount, int hasItemBoxSlotId,
                                             ItemStack targetStack, ItemStack boxStack) {
        LSBPPacket packet = new LSBPPacket(PACKET_MOVE_ITEM);
        packet.maxMoveCount = maxMoveCount;
        packet.boxStack = boxStack;
        packet.hasItemBoxSlotId = hasItemBoxSlotId;
        packet.targetStack = targetStack;
        return packet;
    }

    public static LSBPPacket moveItemRequest(int maxMoveCount, int hasItemBoxSlotId,
                                             NbtCompound targetStack, NbtCompound boxStack) {
        LSBPPacket packet = new LSBPPacket(PACKET_MOVE_ITEM);
        packet.maxMoveCount = maxMoveCount;
        packet.boxStackNbt = boxStack;
        packet.hasItemBoxSlotId = hasItemBoxSlotId;
        packet.targetStackNbt = targetStack;
        return packet;
    }

    public static LSBPPacket moveItemResponseFailure() {
        LSBPPacket packet = new LSBPPacket(PACKET_MOVE_ITEM_FAILURE);
        packet.targetStack = null;
        return packet;
    }

    public static LSBPPacket moveItemResponseSuccess(
            ItemStack targetStack) {
        LSBPPacket packet = new LSBPPacket(PACKET_MOVE_ITEM_SUCCESS);
        packet.targetStack = targetStack;
        return packet;
    }

    private LSBPPacket(Type type) {
        this.packetType = type;
        this.clearPacket();
    }

    private void clearPacket() {
        if (this.buffer != null) {
            this.buffer.clear();
            this.buffer = new PacketByteBuf(Unpooled.buffer());
        }
    }

    /*
        todo delete 暂时不用
     */
    public static LSBPPacket fromPacket(PacketByteBuf input) {
        int i = input.readVarInt();
        Type type = getType(i);
        if (type == null) {
            // Invalid Type
            Litematica.logger.warn("ServuxEntitiesPacket#fromPacket: invalid packet type received");
            return null;
        }
        switch (type) {
            case PACKET_MOVE_ITEM -> {
                // Read Packet Buffer
                try {
                    // todo 转换移动物品相关信息
                    int maxMoveCount = input.readVarInt();
                    int hasItemSlotId = input.readVarInt();
                    NbtCompound targetStack = input.readNbt();
                    NbtCompound boxStack = input.readNbt();
                    return LSBPPacket.moveItemRequest(maxMoveCount, hasItemSlotId, targetStack, boxStack);
                } catch (Exception e) {
                    Utils.info("LitematicaShulkerBoxPickerPacket#fromPacket: error PACKET_MOVE_ITEM from packet: [{}]", e.getLocalizedMessage());
                }
            }
            default -> Utils.info("LitematicaShulkerBoxPickerPacket#fromPacket: Unknown packet type!");
        }

        return null;
    }

    public void toPacket(PacketByteBuf output) {
        output.writeVarInt(this.packetType.get());
        output.writeVarInt(this.maxMoveCount);
        output.writeVarInt(this.hasItemBoxSlotId);
        output.writeNbt(this.targetStackNbt);
        output.writeNbt(this.boxStackNbt);
    }

    public static Type getType(int input) {
        for (Type type : Type.values()) {
            if (type.get() == input) {
                return type;
            }
        }

        return null;
    }

    public enum Type {
        PACKET_MOVE_ITEM(1),
        PACKET_MOVE_ITEM_SUCCESS(2),
        PACKET_MOVE_ITEM_FAILURE(3);
        private final int type;

        Type(int type) {
            this.type = type;
        }

        int get() {
            return this.type;
        }
    }

    public record Payload(LSBPPacket data) implements CustomPayload {
        public static final Id<Payload> ID = new Id<>(LSBPClientHandler.CHANNEL_ID);
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public static final PacketCodec<PacketByteBuf, Payload> CODEC =
                CustomPayload.codecOf(Payload::write, Payload::new);

        public Payload(PacketByteBuf input) {
            this(fromPacket(input));
        }

        private void write(PacketByteBuf output) {
            data.toPacket(output);
        }
    }


}
