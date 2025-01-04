package dev.skydynamic.litematicaboxitempicker;

import dev.skydynamic.litematicaboxitempicker.model.MoveItemCountPayload;
import dev.skydynamic.litematicaboxitempicker.model.SetPickedItemPayload;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import static dev.skydynamic.litematicaboxitempicker.utils.PlayerSlotUtils.*;

public class LitematicaShulkerBoxPickerServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(
                MoveItemCountPayload.MOVE_ITEM_COUNT_ID, (payload, context) -> {
                    int maxCount = payload.getMaxMoveCount();
                    int boxSlot = payload.getHasItemBoxSlot();
                    ItemStack targetStack = payload.getTargetStack();
                    ItemStack boxStack = payload.getBoxStack();

                    ServerPlayerEntity serverPlayer = context.player();
                    if (!isPlayerHaveEmptySlot(serverPlayer)) {
                        return;
                    }
                    moveBoxItem(serverPlayer, targetStack, boxStack, maxCount, boxSlot);
                    System.out.println("执行完成");
                    SetPickedItemPayload newPayload = new SetPickedItemPayload();
                    newPayload.setTargetStack(targetStack.copy());
                    ServerPlayNetworking.send(serverPlayer, newPayload);
                });
    }

}
