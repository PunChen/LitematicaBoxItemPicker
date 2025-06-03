package dev.skydynamic.litematicaboxitempicker.utils;


import com.google.common.collect.Lists;
import fi.dy.masa.litematica.util.InventoryUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStackSet;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.util.Iterator;

import static fi.dy.masa.litematica.util.InventoryUtils.findSlotWithBoxWithItem;

public class PlayerSlotUtils {

    public static boolean isPlayerHaveEmptySlot(ServerPlayerEntity player) {
        return player.getInventory().getEmptySlot() != -1;
    }

    // 获得空余的格子槽位id(除去盔甲栏和副手)
    public static int getPlayerEmptySlot(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int ind = 0; ind < inventory.size(); ++ind) {
            if (inventory.getStack(ind).isEmpty() && ind < inventory.main.size()) {
                return ind;
            }
        }
        return -1;
    }

    // 给予玩家物品
    public static void givePlayerItems(ItemStack toGiveStack, ServerPlayerEntity player, int emptySlotId) {
        PlayerInventory inventory = player.getInventory();
        if (inventory.insertStack(emptySlotId, toGiveStack)) {
            ItemEntity itemEntity = player.dropItem(toGiveStack, false);
            if (itemEntity != null) {
                itemEntity.setDespawnImmediately();
            }
            player.currentScreenHandler.sendContentUpdates();
        } else {
            Utils.LOGGER.warn("givePlayerItems fails {}", toGiveStack);
        }
    }

    public static boolean collectOneItemToBox(ServerPlayerEntity player, int toCollectSlotId) {
        if (toCollectSlotId == -1) {
            Utils.LOGGER.warn("collectOneItemToBox can not collect for slot:{}", toCollectSlotId);
            return false;
        }
        ItemStack toCollectItem = player.getInventory().getStack(toCollectSlotId);
        boolean success = tryCollectIntoBox(player, toCollectItem);
        if (!success) {
            Utils.LOGGER.warn("collectOneItemToBox unable to collect {} into box", toCollectItem);
            return false;
        }
        // 已经回收到盒子里的物品从玩家手里清掉
        player.getInventory().setStack(toCollectSlotId, ItemStack.EMPTY);
        return true;
    }

    // 有空槽位-将原有潜影盒中的物品，取出来，放到空槽位 -- 会占用一个空槽位，如果整组取出来，则潜影盒多出一格空槽位
    // 没有空槽位-并且开启强制替换，尝试找最后一个非潜影盒的物品位，将原有潜影盒中的物品，取出来
    // 潜影盒取出物品后，留下空槽位，则将找到的非空物品槽位放到潜影盒，然后将取出的物品放到背包，如果没有多于槽位则提示失败，
    // 如果开启全背包检索空槽位，则可以将背包物品转移到其他潜影盒
    public static void moveBoxItem(ServerPlayerEntity player, ItemStack stack, int maxMoveCount,
                                   ItemStack boxStack, int boxSlotId, boolean noSlotCollectIntoBox) {
        int emptySlotId = getPlayerEmptySlot(player);
        Utils.LOGGER.warn("moveBoxItem emptySlotId {} noSlotCollectIntoBox {}", emptySlotId, noSlotCollectIntoBox);
        // 没有空槽位-并且没有开启满替换
        if (emptySlotId == -1 && !noSlotCollectIntoBox) {
            return;
        }
        // 有空槽位或者开启满替换
        // 获得潜影盒的物品列表
        DefaultedList<ItemStack> boxItems = Utils.getStoredItemsWithoutOrder(boxStack);
        ItemStack oneBoxStack = player.getInventory().getStack(boxSlotId);
        Utils.LOGGER.warn("moveBoxItem oneBoxStack {} boxItems {} boxSlotId {}", oneBoxStack, boxItems, boxSlotId);
        String stackItemId = stack.getItem().toString();
        ItemStack boxItem = null;
        for (ItemStack oneStackInBox : boxItems) {
            String boxItemId = oneStackInBox.getItem().toString();
            if (boxItemId.equals(stackItemId)) {
                boxItem = oneStackInBox;
                break;
            }
        }
        if (boxItem == null) {
            return;
        }
        int itemCount = boxItem.getCount();// 潜影盒物品的个数
        ItemStack itemToGive = boxItem.copy();// 确定给玩家的物品，以及新的潜影盒中的物品
        if (maxMoveCount < itemCount) {// 最大移动数量小于盒子里的物品数量
            // 物品足够，潜影盒不增加空槽位
            itemToGive.setCount(maxMoveCount);
        }

        if (emptySlotId != -1) { // 有空槽位，新盒子替代，并且给玩家物品
            ItemStack newBox = Utils.decreaseItemCountOfBox(boxStack, itemToGive.getItem(), itemToGive.getCount());
            if (newBox == null) {
                Utils.LOGGER.warn("moveBoxItem emptySlotId failed to move item {} from box", itemToGive);
                return;
            }
            player.getInventory().setStack(boxSlotId, newBox);
            givePlayerItems(itemToGive, player, emptySlotId);
        } else {
            // 没有空槽位，开启物品回收 noSlotCollectIntoBox=true
            // 开启：没有空槽位时，回收物品到潜影盒
            int toCollectSlotId = getLastNoContainer(player);
            Utils.LOGGER.warn("moveBoxItem getFirstNoContainer toCollectSlotId {}", toCollectSlotId);
            if (collectOneItemToBox(player, toCollectSlotId)) {// 可能会修改潜影盒内容，因此需要重新更新盒子中的内容
                // 回收成功，toCollectSlotId成为空槽位
                boxStack = player.getInventory().getStack(boxSlotId); // 重新获得潜影盒
                ItemStack updateNewBox = Utils.decreaseItemCountOfBox(boxStack, itemToGive.getItem(), itemToGive.getCount());
                if (updateNewBox == null) {
                    Utils.LOGGER.warn("moveBoxItem noSlotCollectIntoBox failed to move item {} from box", itemToGive);
                    return;
                }
                player.getInventory().setStack(boxSlotId, updateNewBox);
                givePlayerItems(itemToGive, player, toCollectSlotId);
            }
        }
    }

    public static int getFirstNoContainer(ServerPlayerEntity player) {
        for (int ind = 0; ind < player.getInventory().main.size(); ++ind) {
            ItemStack oneStack = player.getInventory().getStack(ind);
            ContainerComponent component = oneStack.getComponents().get(DataComponentTypes.CONTAINER);
            if (component == null) {
                return ind;
            }
        }
        return -1;
    }

    public static int getLastNoContainer(ServerPlayerEntity player) {
        for (int ind = player.getInventory().main.size() - 1; ind >= 0; --ind) {
            ItemStack oneStack = player.getInventory().getStack(ind);
            ContainerComponent component = oneStack.getComponents().get(DataComponentTypes.CONTAINER);
            if (component == null) {
                return ind;
            }
        }
        return -1;
    }


    private static boolean tryCollectIntoBox(ServerPlayerEntity player, ItemStack toCollect) {
        //对所有的潜影盒便利，尝试放入，优先合并其次寻找空位
        for (int ind = 0; ind < player.getInventory().main.size(); ++ind) {
            ItemStack boxStack = player.getInventory().getStack(ind);
            if (!Utils.isItShulkerBox(boxStack)) {
                continue;
            }
            ItemStack newBoxStack = Utils.addItemIntoBox(boxStack, toCollect);
            Utils.LOGGER.warn("tryCollectIntoBox boxStack:{} toCollect:{} result:{}", boxStack, toCollect, newBoxStack);
            if (newBoxStack != null) {// 放入成功
                player.getInventory().setStack(ind, newBoxStack);
                return true;
            }
        }
        return false;
    }


}
