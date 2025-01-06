package dev.skydynamic.litematicaboxitempicker.mixin;

import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import dev.skydynamic.litematicaboxitempicker.config.Configs;
import dev.skydynamic.litematicaboxitempicker.network.LSBPClientHandler;
import dev.skydynamic.litematicaboxitempicker.network.LSBPPacket;
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
        Utils.info("LitematicaInventoryUtilsMixin getStack start,{}" , Configs.Generic.ENABLE_LSBP.getBooleanValue());
        if (Configs.Generic.ENABLE_LSBP.getBooleanValue()) {
            ClientPlayerEntity player = mc.player;
            int slotId = findSlotWithBoxWithItem(player.currentScreenHandler, stack, false);
            Utils.info("LitematicaInventoryUtilsMixin findSlotWithBoxWithItem slot {}", slotId);
            int maxMoveCount = Configs.Generic.LSBP_COUNT.getIntegerValue();
            if (slotId != -1 && PlayerSlotUtils.getPlayerSlotHaveEmpty(player)) {
                ItemStack boxStack = player.playerScreenHandler.slots.get(slotId).getStack();
                if (mc.getCurrentServerEntry() == null) {
                    Utils.info("LitematicaInventoryUtilsMixin getStack client start");
                    ServerPlayerEntity serverPlayer = mc.getServer().getPlayerManager().getPlayer(player.getUuid());
                    PlayerSlotUtils.moveBoxItem(serverPlayer, stack, boxStack, maxMoveCount, slotId);
                    setPickedItemToHand(stack, mc);
                    Utils.info("LitematicaInventoryUtilsMixin getStack client end");
                    ci.cancel();
                    return;
                }
                Utils.info("LitematicaInventoryUtilsMixin getStack server start");
                LSBPPacket packet = LSBPPacket.
                        moveItemRequest(maxMoveCount, slotId, stack, boxStack);
                LSBPClientHandler.getInstance().
                        sendPlayPayload(new LSBPPacket.Payload(packet));
                Utils.info("LitematicaInventoryUtilsMixin getStack server end");
            }
        }
    }
}

