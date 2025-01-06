package dev.skydynamic.litematicaboxitempicker.model;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public class SetPickedItemPayload implements CustomPayload {
    public static final Id<SetPickedItemPayload> SET_PICKED_ITEM = new Id<>(Identifier.of("lsbp", "set_picked_item"));
    private ItemStack targetStack = null;


    @Override
    public Id<? extends CustomPayload> getId() {
        return SET_PICKED_ITEM;
    }

    public ItemStack getTargetStack() {
        return targetStack;
    }

    public void setTargetStack(ItemStack targetStack) {
        this.targetStack = targetStack;
    }
}
