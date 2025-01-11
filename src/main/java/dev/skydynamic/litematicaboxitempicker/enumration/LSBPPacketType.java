package dev.skydynamic.litematicaboxitempicker.enumration;

public enum LSBPPacketType {
    PACKET_MOVE_ITEM_START(111),
    PACKET_MOVE_ITEM_DATA(112),
    PACKET_MOVE_ITEM_SUCCESS(113),
    PACKET_MOVE_ITEM_FAILURE(114);
    private final int type;

    LSBPPacketType(int type) {
        this.type = type;
    }

    public int get() {
        return this.type;
    }

    public static LSBPPacketType getPacketType(int input) {
        for (LSBPPacketType type : LSBPPacketType.values()) {
            if (type.get() == input) {
                return type;
            }
        }

        return null;
    }
}