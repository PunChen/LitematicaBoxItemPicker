package dev.skydynamic.litematicaboxitempicker.network;

import fi.dy.masa.litematica.network.ServuxLitematicaHandler;
import fi.dy.masa.litematica.network.ServuxLitematicaPacket;
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
public abstract class LitematicaShulkerBoxPickerHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T> {

    public static final Identifier CHANNEL_ID = Identifier.of("lsbp", "set_picked_item");
    private int failures = 0;
    private static final int MAX_FAILURES = 4;
    private static final LitematicaShulkerBoxPickerHandler<LitematicaShulkerBoxPickerPacket.Payload> INSTANCE =
            new LitematicaShulkerBoxPickerHandler<>() {
                @Override
                public void receive(LitematicaShulkerBoxPickerPacket.Payload payload, ClientPlayNetworking.Context context) {
                    LitematicaShulkerBoxPickerHandler.INSTANCE.receivePlayPayload(payload, context);
                }
            };

    public static LitematicaShulkerBoxPickerHandler<LitematicaShulkerBoxPickerPacket.Payload> getInstance() {
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

    @Override
    public void receivePlayPayload(T payload, ClientPlayNetworking.Context ctx) {
        if (payload.getId().id().equals(CHANNEL_ID)) {
            LitematicaShulkerBoxPickerPacket packet = ((LitematicaShulkerBoxPickerPacket.Payload) payload).data();
            ItemStack stack = packet.getTargetStack();
            setPickedItemToHand(stack, ctx.client());
        }
    }

    @Override
    public void encodeWithSplitter(PacketByteBuf buffer, ClientPlayNetworkHandler handler) {
        // todo whether need to encodeWithSplitter

    }

    @Override
    public <P extends IClientPayloadData> void encodeClientData(P data) {

        IPluginClientPlayHandler.super.encodeClientData(data);
    }
}
