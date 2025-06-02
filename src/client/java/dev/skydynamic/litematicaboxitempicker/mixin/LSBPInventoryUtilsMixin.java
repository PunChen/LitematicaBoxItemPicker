package dev.skydynamic.litematicaboxitempicker.mixin;

import dev.skydynamic.litematicaboxitempicker.clientnetwork.LSBPPacket;
import dev.skydynamic.litematicaboxitempicker.utils.Configs;
import dev.skydynamic.litematicaboxitempicker.utils.PlayerSlotUtils;
import dev.skydynamic.litematicaboxitempicker.utils.Utils;
import fi.dy.masa.litematica.util.InventoryUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
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
        try {
            if (!Configs.Generic.ENABLE_LSBP.getBooleanValue()) {
                ci.cancel();
                return;
            }
            ClientPlayerEntity player = mc.player;
            if (player == null) {
                Utils.LOGGER.error("LSBPInventoryUtilsMixin unknown error, player is null");
                ci.cancel();
                return;
            }
            int slotId = findSlotWithBoxWithItem(player.currentScreenHandler, stack, true);
            int maxMoveCount = Configs.Generic.LSBP_COUNT.getIntegerValue();
            if (slotId == -1) {
                Utils.LOGGER.warn("LSBPInventoryUtilsMixin not found for {}", stack);
                ci.cancel();
                return;
            }
            // 开启无空槽位物品回收潜影盒功能
            boolean noSlotCollectIntoBox = Configs.Generic.ENABLE_NO_SLOT_COLLECT_INTO_BOX.getBooleanValue();
            // 获得包含物品的潜影盒
            ItemStack boxStack = player.playerScreenHandler.slots.get(slotId).getStack();
            if (!Utils.isItShulkerBox(boxStack)) {
                Utils.LOGGER.error("LSBPInventoryUtilsMixin not a shulker box");
                ci.cancel();
                return;
            }
            if (mc.world == null) {
                ci.cancel();
                return;
            }
            if (mc.world.isClient) { // 客户端，直接替换
                Utils.LOGGER.warn("LSBPInventoryUtilsMixin getStack client start");
                if (mc.getServer() == null) {
                    Utils.LOGGER.error("LSBPInventoryUtilsMixin can not get server");
                    ci.cancel();
                    return;
                }
                // 有空槽位-将原有潜影盒中的物品，取出来，放到空槽位 -- 会占用一个空槽位，如果整组取出来，则潜影盒多出一格空槽位
                // 没有空槽位-并且开启强制替换，尝试找最后一个非潜影盒的物品位，将原有潜影盒中的物品，取出来
                // 潜影盒取出物品后，留下空槽位，则将找到的非空物品槽位放到潜影盒，然后将取出的物品放到背包，如果没有多于槽位则提示失败，
                // 如果开启全背包检索空槽位，则可以将背包物品转移到其他潜影盒
                ServerPlayerEntity serverPlayer = mc.getServer().getPlayerManager().getPlayer(player.getUuid());
                PlayerSlotUtils.moveBoxItem(serverPlayer, stack, maxMoveCount, slotId, noSlotCollectIntoBox);
                setPickedItemToHand(stack, mc);
                Utils.LOGGER.warn("LSBPInventoryUtilsMixin getStack client end");
                ci.cancel();
                return;
            }
            // 服务端发包出去
            NbtElement stackNbt = stack.encode(Utils.REGISTRY);
            NbtElement boxStackNbt = boxStack.encode(Utils.REGISTRY);
            LSBPPacket lsbpPacket = LSBPPacket.moveItemRequest(maxMoveCount, slotId, stackNbt, boxStackNbt);
//                LSBPClientHandler.getInstance().encodeClientData(lsbpPacket);
            ClientPlayNetworking.send(new LSBPPacket.Payload(lsbpPacket));

        } catch (Exception e) {
            Utils.LOGGER.error("LSBPInventoryUtilsMixin getStack error:", e);
        }
    }

}

