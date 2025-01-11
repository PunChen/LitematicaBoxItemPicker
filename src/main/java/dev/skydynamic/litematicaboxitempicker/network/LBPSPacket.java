package dev.skydynamic.litematicaboxitempicker.network;

import dev.skydynamic.litematicaboxitempicker.enumration.LSBPPacketType;
import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.servux.network.IServerPayloadData;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class LBPSPacket implements IServerPayloadData {

    public static final Identifier CHANNEL_ID = Identifier.of("lsbp", "move_count_item");
    public static final int PROTOCOL_VERSION = 1;
    private PacketByteBuf buffer;


    private LBPSPacket() {
        this.clearPacket();
    }

    public static LBPSPacket moveItemRequest(int maxMoveCount, int hasItemBoxSlotId,
                                             NbtElement targetStack, NbtElement boxStack) {
        LBPSPacket packet = new LBPSPacket();
        packet.buffer.writeVarInt(LSBPPacketType.PACKET_MOVE_ITEM_START.get());
        packet.buffer.writeVarInt(maxMoveCount);
        packet.buffer.writeVarInt(hasItemBoxSlotId);
        packet.buffer.writeNbt(targetStack);
        packet.buffer.writeNbt(boxStack);
        return packet;
    }

    public static LBPSPacket moveItemSplitPacketRequest(PacketByteBuf buffer) {
        Utils.LOGGER.error("client moveItemSplitPacketRequest buffer start:{}", buffer.toString());
        LBPSPacket packet = new LBPSPacket();
        packet.buffer.writeBytes(buffer);
        return packet;
    }

    public static LBPSPacket moveItemResponseFailure() {
        LBPSPacket packet = new LBPSPacket();
        packet.buffer.writeVarInt(LSBPPacketType.PACKET_MOVE_ITEM_FAILURE.get());
        return packet;
    }

    public static LBPSPacket moveItemResponseSuccess(NbtElement targetStack) {
        LBPSPacket packet = new LBPSPacket();
        packet.buffer.writeVarInt(LSBPPacketType.PACKET_MOVE_ITEM_SUCCESS.get());
        packet.buffer.writeNbt(targetStack);
        return packet;
    }

    public static LBPSPacket fromPacket(PacketByteBuf input) {
        Utils.LOGGER.error("LBPSPacket#fromPacket server: input:{}", input);
        LBPSPacket packet = new LBPSPacket();
        packet.buffer.writeBytes(input);
        return packet;
    }

    public PacketByteBuf getBuffer() {
        return buffer;
    }

    @Override
    public int getVersion() {
        return PROTOCOL_VERSION;
    }

    @Override
    public int getPacketType() {
        return LSBPPacketType.PACKET_MOVE_ITEM_START.get();
    }

    @Override
    public int getTotalSize() {
        if (this.buffer != null) {
            return this.buffer.readableBytes();
        }
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return this.buffer != null && this.buffer.isReadable();
    }

    private void clearPacket() {
        if (this.buffer == null) {
            this.buffer = new PacketByteBuf(Unpooled.buffer());
        } else {
            this.buffer.clear();
        }
    }

    @Override
    public String toString() {
        return "LBPSPacket{" +
                "buffer=" + buffer +
                '}';
    }

    public void toPacket(PacketByteBuf output) {
        output.writeBytes(this.buffer);
    }

    @Override
    public void clear() {
        this.clearPacket();
    }


    public static class Payload implements CustomPayload {
        public static final Id<Payload> ID = new Id<>(LBPSPacket.CHANNEL_ID);
        public static final PacketCodec<PacketByteBuf, Payload> CODEC =
                CustomPayload.codecOf(Payload::write, Payload::new);
        public final LBPSPacket data;

        public Payload(LBPSPacket data) {
            this.data = data;
        }

        public Payload(PacketByteBuf input) {
            this(fromPacket(input));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        private void write(PacketByteBuf output) {
            data.toPacket(output);
        }

        @Override
        public String toString() {
            return "Payload{" +
                    "data=" + data +
                    '}';
        }
    }


}
