package dev.skydynamic.litematicaboxitempicker.utils;


import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.util.Iterator;

public class PlayerSlotUtils {

    public static boolean isPlayerHaveEmptySlot(ServerPlayerEntity player) {
        return player.getInventory().getEmptySlot() != -1;
    }

    // 获得空余的格子槽位id(除去盔甲栏和副手)
    public static int getPlayerEmptySlot(ServerPlayerEntity player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    // 给予玩家物品
    public static void givePlayerItems(ItemStack toGiveStack, ServerPlayerEntity player, int emptySlotId) {
        PlayerInventory inventory = player.getInventory();
        if (inventory.insertStack(emptySlotId, toGiveStack)) {//来自潜影盒的物品的深拷贝
            ItemEntity itemEntity = player.dropItem(toGiveStack, false);
            if (itemEntity != null) {
                itemEntity.setDespawnImmediately();
            }
            player.currentScreenHandler.sendContentUpdates();
        } else {
            Utils.LOGGER.warn("givePlayerItems fails {}", toGiveStack);
        }
    }

    // 将盒子内的物品转移到手上
    // 有空槽位-将原有潜影盒中的物品，取出来，放到空槽位，然后将物品放到主手 -- 会占用一个空槽位
    // 没有空槽位-并且开启强制替换，将原有潜影盒中的物品，取出来，和主手的物品进行替换 --不占用空槽位
    public static void moveBoxItem(ServerPlayerEntity player, ItemStack stack, ItemStack boxStack, int maxMoveCount,
                                   int boxSlotId, boolean replaceWhenNoSlot) {
        int emptySlotId = getPlayerEmptySlot(player);
        // 没有空槽位-并且没有开启满替换
        if (emptySlotId == -1 && !replaceWhenNoSlot) {
            return;
        }
        // 有空槽位或者开启满替换
        // 获得潜影盒的物品列表
        DefaultedList<ItemStack> items = Utils.getStoredItemsWithoutOrder(boxStack);
        String stackItemId = Registries.ITEM.getId(stack.getItem()).toString();
        for (int ind = 0; ind < items.size(); ++ind) {
            ItemStack oneStackInBox = items.get(ind);
            String boxItemId = oneStackInBox.getItem().toString();
            if (!boxItemId.equals(stackItemId)) {
                continue;
            }
            int itemCount = oneStackInBox.getCount();// 潜影盒物品的个数
            // 确定给玩家的物品，以及新的潜影盒中的物品
            ItemStack itemToGive;
            if (itemCount <= maxMoveCount) {// 物品不足，全部给玩家，增加空槽位
                itemToGive = oneStackInBox.copy();
                if (emptySlotId != -1) {
                    // 将潜影盒取出的 物品放到玩家空槽位（在下一次玩家右键时，投影的轻松放置会自动取物）
                    givePlayerItems(itemToGive, player, emptySlotId);// todo 是否销毁 itemToGive
                    items.remove(ind);
                } else {// 没有空槽，但是开启满替换,将玩家主手上的物品，放到潜影盒，将给玩家的物品放到主手
                    ItemStack mainHandStack = player.getMainHandStack();
                    int mainHandSlot = player.getInventory().selectedSlot;
                    // 将潜影盒取出的 物品放到玩家主手
                    givePlayerItems(itemToGive, player, mainHandSlot);
                    items.set(ind, mainHandStack);// 将主手的物品放到潜影盒中
                }
                // 不用给予玩家新的潜影盒，因为潜影盒中的物品已经更新
                // 给予玩家新的潜影盒
//                ItemStack newBox = createShulkerBoxWithNewItems(boxStack, items);
//                player.playerScreenHandler.slots.get(boxSlotId).setStack(newBox);
//                // 丢弃原来的潜影盒
//                ItemEntity itemEntity = player.dropItem(boxStack, false);
//                if (itemEntity != null) {
//                    itemEntity.setDespawnImmediately();
//                }
            } else { // 物品足够，潜影盒不增加空槽位
                oneStackInBox.setCount(itemCount - maxMoveCount);// 更新潜影盒中的物品数量
                itemToGive = oneStackInBox.copy();
                itemToGive.setCount(maxMoveCount);

                if (emptySlotId != -1) {// 有空槽位，放到空槽位，下次投影自己替换主手
                    givePlayerItems(itemToGive, player, emptySlotId);
                } else { // 没有空槽位，开启满替换,
                    // todo
                    Utils.LOGGER.error("no empty slot, can not fetch {}", stackItemId);
                }
//                ItemStack newBox = createShulkerBoxWithNewItems(boxStack, items);
//                player.playerScreenHandler.slots.get(boxSlotId).setStack(newBox);
            }

//                if (itemCount <= maxMoveCount) {
//                    ItemStack itemToGive = oneStackInBox.copy();
//                    givePlayerItems(itemToGive, player);
//                    iterator.remove();
//                    ItemStack newBox = createShulkerBoxWithNewItems(boxStack, items);
//                    player.playerScreenHandler.slots.get(boxSlotId).setStack(newBox);
//                } else {
//                    oneStackInBox.setCount(itemCount - maxMoveCount);
//                    ItemStack itemToGive = oneStackInBox.copy();
//                    itemToGive.setCount(maxMoveCount);
//                    givePlayerItems(itemToGive, player);
//                    ItemStack newBox = createShulkerBoxWithNewItems(boxStack, items);
//                    player.playerScreenHandler.slots.get(boxSlotId).setStack(newBox);
//                }
            break;
        }
    }

    public static ItemStack createShulkerBoxWithNewItems(ItemStack boxStack, DefaultedList<ItemStack> items) {
        ItemStack itemStack = new ItemStack(RegistryEntry.of(boxStack.getItem()), 1);
        ContainerComponent containerComponent = ContainerComponent.fromStacks(items);
        itemStack.set(DataComponentTypes.CONTAINER, containerComponent);
        return itemStack;
    }


}
