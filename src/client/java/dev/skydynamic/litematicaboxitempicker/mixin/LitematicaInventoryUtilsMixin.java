package dev.skydynamic.litematicaboxitempicker.mixin;

import dev.skydynamic.litematicaboxitempicker.clientnetwork.LBPSPacket;
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
        Utils.LOGGER.error("LitematicaInventoryUtilsMixin getStack start, stack {} position:{}, {}",
                Configs.Generic.ENABLE_LSBP.getBooleanValue(), stack, pos);
        if (Configs.Generic.ENABLE_LSBP.getBooleanValue()) {
            ClientPlayerEntity player = mc.player;
            int slotId = findSlotWithBoxWithItem(player.currentScreenHandler, stack, false);
            Utils.LOGGER.error("LitematicaInventoryUtilsMixin findSlotWithBoxWithItem slot {}", slotId);
            int maxMoveCount = Configs.Generic.LSBP_COUNT.getIntegerValue();
            if (slotId != -1 && PlayerSlotUtils.getPlayerSlotHaveEmpty(player.getInventory())) {
                ItemStack boxStack = player.playerScreenHandler.slots.get(slotId).getStack();
                if (!isItShulkerBox(boxStack)) {
                    Utils.LOGGER.warn("LitematicaInventoryUtilsMixin already getStack for this item, return");
                    ci.cancel();
                    return;
                }
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
                NbtElement stackNbt = stack.encode(Utils.REGISTRY);
                NbtElement boxStackNbt = boxStack.encode(Utils.REGISTRY);
                Utils.LOGGER.error("getStack send moveItemRequest {},{},{},{},{}", maxMoveCount, slotId, stack, boxStack,
                        boxStack.encode(Utils.REGISTRY));
                ClientPlayNetworking.send(new LBPSPacket.Payload(LBPSPacket.moveItemRequest(maxMoveCount, slotId, stackNbt, boxStackNbt)));
                Utils.LOGGER.error("LitematicaInventoryUtilsMixin getStack server end");
            }
        }
    }

    @Unique
    private static boolean isItShulkerBox(ItemStack boxStack) {
        return boxStack.getItem().toString().contains(Items.SHULKER_BOX.toString());
    }
}

