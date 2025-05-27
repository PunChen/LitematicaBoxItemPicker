package dev.skydynamic.litematicaboxitempicker.mixin;

import dev.skydynamic.litematicaboxitempicker.clientnetwork.LSBPPacket;
import dev.skydynamic.litematicaboxitempicker.config.Configs;
import dev.skydynamic.litematicaboxitempicker.utils.PlayerSlotUtils;
import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.litematica.util.InventoryUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fi.dy.masa.litematica.util.InventoryUtils.findSlotWithBoxWithItem;
import static fi.dy.masa.litematica.util.InventoryUtils.setPickedItemToHand;

@Environment(EnvType.CLIENT)
@Mixin(InventoryUtils.class)
public abstract class LSBPInventoryUtilsMixin {

    @Inject(
            method = "schematicWorldPickBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/util/InventoryUtils;findSlotWithBoxWithItem(Lnet/minecraft/screen/ScreenHandler;Lnet/minecraft/item/ItemStack;Z)I"
            ),
            cancellable = true
    )
    private static void getStack(ItemStack stack, BlockPos pos, World schematicWorld, MinecraftClient mc, CallbackInfo ci) {
        if (Configs.Generic.ENABLE_LSBP.getBooleanValue()) {
            ClientPlayerEntity player = mc.player;
            int slotId = findSlotWithBoxWithItem(player.currentScreenHandler, stack, false);
            int maxMoveCount = Configs.Generic.LSBP_COUNT.getIntegerValue();
            if (slotId != -1 && PlayerSlotUtils.getPlayerSlotHaveEmpty(player.getInventory())) {
                ItemStack boxStack = player.playerScreenHandler.slots.get(slotId).getStack();
                if (!isItShulkerBox(boxStack)) {
                    Utils.LOGGER.warn("LitematicaInventoryUtilsMixin already getStack for this item, return");
                    ci.cancel();
                    return;
                }
                if (mc.getCurrentServerEntry() == null) {
                    Utils.LOGGER.warn("LitematicaInventoryUtilsMixin getStack client start");
                    ServerPlayerEntity serverPlayer = mc.getServer().getPlayerManager().getPlayer(player.getUuid());
                    PlayerSlotUtils.moveBoxItem(serverPlayer, stack, boxStack, maxMoveCount, slotId);
                    setPickedItemToHand(stack, mc);
                    Utils.LOGGER.warn("LitematicaInventoryUtilsMixin getStack client end");
                    ci.cancel();
                    return;
                }
                NbtElement stackNbt = stack.encode(Utils.REGISTRY);
                NbtElement boxStackNbt = boxStack.encode(Utils.REGISTRY);
                LSBPPacket lsbpPacket = LSBPPacket.moveItemRequest(maxMoveCount, slotId, stackNbt, boxStackNbt);
//                LSBPClientHandler.getInstance().encodeClientData(lsbpPacket);
                ClientPlayNetworking.send(new LSBPPacket.Payload(lsbpPacket));
            }
        }
    }

    @Unique
    private static boolean isItShulkerBox(ItemStack boxStack) {
        return boxStack.getItem().toString().contains(Items.SHULKER_BOX.toString());
    }
}

