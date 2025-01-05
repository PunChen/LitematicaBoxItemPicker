package dev.skydynamic.litematicaboxitempicker.mixin;

import dev.skydynamic.litematicaboxitempicker.Utils;
import dev.skydynamic.litematicaboxitempicker.config.Configs;
import dev.skydynamic.litematicaboxitempicker.network.LitematicaShulkerBoxPickerHandler;
import dev.skydynamic.litematicaboxitempicker.network.LitematicaShulkerBoxPickerPacket;
import dev.skydynamic.litematicaboxitempicker.utils.PlayerSlotUtils;
import fi.dy.masa.litematica.util.InventoryUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fi.dy.masa.litematica.util.InventoryUtils.findSlotWithBoxWithItem;
import static fi.dy.masa.litematica.util.InventoryUtils.setPickedItemToHand;

@Environment(EnvType.CLIENT)
@Mixin(InventoryUtils.class)
public abstract class LitematicaInventoryUtilsMixin {

    @Inject(
            method = "schematicWorldPickBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/util/InventoryUtils;findSlotWithBoxWithItem(Lnet/minecraft/screen/ScreenHandler;Lnet/minecraft/item/ItemStack;Z)I"
            ),
            cancellable = true
    )
    private static void getStack(ItemStack stack, BlockPos pos, World schematicWorld, MinecraftClient mc, CallbackInfo ci) {
        Utils.LOGGER.error("LitematicaInventoryUtilsMixin getStack start,{}" , Configs.Generic.ENABLE_LSBP.getBooleanValue());
        if (Configs.Generic.ENABLE_LSBP.getBooleanValue()) {
            ClientPlayerEntity player = mc.player;
            int slotId = findSlotWithBoxWithItem(player.currentScreenHandler, stack, false);
            Utils.LOGGER.error("LitematicaInventoryUtilsMixin findSlotWithBoxWithItem slot {}", slotId);
            int maxMoveCount = Configs.Generic.LSBP_COUNT.getIntegerValue();
            if (slotId != -1 && PlayerSlotUtils.getPlayerSlotHaveEmpty(player)) {
                ItemStack boxStack = player.playerScreenHandler.slots.get(slotId).getStack();
                if (mc.getCurrentServerEntry() == null) {
                    Utils.LOGGER.error("LitematicaInventoryUtilsMixin getStack client start");
                    ServerPlayerEntity serverPlayer = mc.getServer().getPlayerManager().getPlayer(player.getUuid());
                    PlayerSlotUtils.moveBoxItem(serverPlayer, stack, boxStack, maxMoveCount, slotId);
                    setPickedItemToHand(stack, mc);
                    Utils.LOGGER.error("LitematicaInventoryUtilsMixin getStack client end");
                    ci.cancel();
                    return;
                }
                Utils.LOGGER.error("LitematicaInventoryUtilsMixin getStack server start");
                LitematicaShulkerBoxPickerPacket packet = LitematicaShulkerBoxPickerPacket.
                        moveItemRequest(maxMoveCount, slotId, stack, boxStack);
                LitematicaShulkerBoxPickerHandler.getInstance().
                        sendPlayPayload(new LitematicaShulkerBoxPickerPacket.Payload(packet));
                Utils.LOGGER.error("LitematicaInventoryUtilsMixin getStack server end");
            }
        }
    }
}

