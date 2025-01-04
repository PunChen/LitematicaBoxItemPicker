package dev.skydynamic.litematicaboxitempicker;


import dev.skydynamic.litematicaboxitempicker.model.SetPickedItemPayload;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fi.dy.masa.litematica.util.InventoryUtils.setPickedItemToHand;

public class LitematicaShulkerBoxPickerClient implements ClientModInitializer {
    public static final Logger logger = LoggerFactory.getLogger(Reference.MOD_ID);

    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
        ClientPlayNetworking.registerGlobalReceiver(SetPickedItemPayload.SET_PICKED_ITEM, (payload, context) -> {
            ItemStack stack = payload.getTargetStack();
            setPickedItemToHand(stack, context.client());
        });
    }
}
