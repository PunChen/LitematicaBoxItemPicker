package dev.skydynamic.litematicaboxitempicker.network;

import dev.skydynamic.litematicaboxitempicker.Utils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.network.ServuxLitematicaHandler;
import fi.dy.masa.litematica.network.ServuxLitematicaPacket;
import fi.dy.masa.malilib.network.IClientPayloadData;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import static dev.skydynamic.litematicaboxitempicker.network.LitematicaShulkerBoxPickerPacket.Type.PACKET_MOVE_ITEM;
import static dev.skydynamic.litematicaboxitempicker.network.LitematicaShulkerBoxPickerPacket.Type.PACKET_SET_ITEM;

public class LitematicaShulkerBoxPickerPacket {
    /**
     * 发送数据包类型
     */
    private LitematicaShulkerBoxPickerPacket.Type packetType;
    /**
     * 缓存
     */
    private PacketByteBuf buffer;
    /**
     * 最大移动数量
     */
    private int maxMoveCount = -1;
    /**
     * 有指定物品的潜影盒所在槽位
     */
    private int hasItemBoxSlot = -1;
    /**
     * 目标物品
     */
    private ItemStack targetStack = null;
    /**
     * 潜影盒
     */
    private ItemStack boxStack = null;
    public static final int PROTOCOL_VERSION = 1;

    public ItemStack getTargetStack() {
        return targetStack;
    }

    public PacketByteBuf getBuffer() {
        return buffer;
    }


    public static LitematicaShulkerBoxPickerPacket moveItemRequest(int maxMoveCount, int hasItemBoxSlot,
                                                                   ItemStack targetStack, ItemStack boxStack) {
        LitematicaShulkerBoxPickerPacket packet = new LitematicaShulkerBoxPickerPacket(PACKET_MOVE_ITEM);
        packet.maxMoveCount = maxMoveCount;
        packet.boxStack = boxStack;
        packet.hasItemBoxSlot = hasItemBoxSlot;
        packet.targetStack = targetStack;
        return packet;
    }

    public static LitematicaShulkerBoxPickerPacket setItemResponse(
            ItemStack targetStack) {
        LitematicaShulkerBoxPickerPacket packet = new LitematicaShulkerBoxPickerPacket(PACKET_SET_ITEM);
        packet.targetStack = targetStack;
        return packet;
    }

    private LitematicaShulkerBoxPickerPacket(LitematicaShulkerBoxPickerPacket.Type type) {
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
    public static LitematicaShulkerBoxPickerPacket fromPacket(PacketByteBuf input) {
        int i = input.readVarInt();
        LitematicaShulkerBoxPickerPacket.Type type = getType(i);
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
                } catch (Exception e) {
                    Utils.LOGGER.error("LitematicaShulkerBoxPickerPacket#fromPacket: error PACKET_MOVE_ITEM from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_SET_ITEM -> {
                // Read Packet Buffer
                try {
                    // todo 转换设置物品相关信息

                } catch (Exception e) {
                    Utils.LOGGER.error("LitematicaShulkerBoxPickerPacket#fromPacket: error reading PACKET_SET_ITEM from packet: [{}]", e.getLocalizedMessage());
                }
            }
            default -> Utils.LOGGER.error("LitematicaShulkerBoxPickerPacket#fromPacket: Unknown packet type!");
        }

        return null;
    }

    public void toPacket(PacketByteBuf output) {
        output.writeVarInt(this.packetType.get());
    }


    public static LitematicaShulkerBoxPickerPacket.Type getType(int input) {
        for (LitematicaShulkerBoxPickerPacket.Type type : LitematicaShulkerBoxPickerPacket.Type.values()) {
            if (type.get() == input) {
                return type;
            }
        }

        return null;
    }

    public enum Type {
        PACKET_MOVE_ITEM(1),
        PACKET_SET_ITEM(2);
        private final int type;

        Type(int type) {
            this.type = type;
        }

        int get() {
            return this.type;
        }
    }

    public record Payload(LitematicaShulkerBoxPickerPacket data) implements CustomPayload {
        public static final Id<Payload> ID = new Id<>(LitematicaShulkerBoxPickerHandler.CHANNEL_ID);


        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public static final PacketCodec<PacketByteBuf, LitematicaShulkerBoxPickerPacket.Payload> CODEC =
                CustomPayload.codecOf(Payload::write, LitematicaShulkerBoxPickerPacket.Payload::new);

        public Payload(PacketByteBuf input) {
            this(fromPacket(input));
        }

        private void write(PacketByteBuf output) {
            data.toPacket(output);
        }

    }


}
