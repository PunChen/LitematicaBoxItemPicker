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

    // 有空槽位-将原有潜影盒中的物品，取出来，放到空槽位 -- 会占用一个空槽位，如果整组取出来，则潜影盒多出一格空槽位
    // 没有空槽位-并且开启强制替换，尝试找最后一个非潜影盒的物品位，将原有潜影盒中的物品，取出来
    // 潜影盒取出物品后，留下空槽位，则将找到的非空物品槽位放到潜影盒，然后将取出的物品放到背包，如果没有多于槽位则提示失败，
    // 如果开启全背包检索空槽位，则可以将背包物品转移到其他潜影盒
    public static void moveBoxItem(ServerPlayerEntity player, ItemStack stack, ItemStack boxStack, int maxMoveCount,
                                   int boxSlotId, boolean noSlotCollectIntoBox) {
        int emptySlotId = getPlayerEmptySlot(player);
        // 没有空槽位-并且没有开启满替换
        if (emptySlotId == -1 && !noSlotCollectIntoBox) {
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
                    givePlayerItems(itemToGive, player, emptySlotId);
                    items.remove(ind);
                } else {// 没有空槽，但是开启满替换,将玩家主手上的物品，放到潜影盒，将给玩家的物品放到主手
                    ItemStack mainHandStack = player.getMainHandStack();
                    int mainHandSlot = player.getInventory().selectedSlot;
                    // 将潜影盒取出的 物品放到玩家主手
                    player.getInventory().setStack(mainHandSlot, ItemStack.EMPTY);// 清空主手物品
                    givePlayerItems(itemToGive, player, mainHandSlot);
                    items.set(ind, mainHandStack.copy());// 将主手的物品放到潜影盒中
                }
                // 潜影盒中的物品已经更新，给予玩家新的潜影盒
                ItemStack newBox = Utils.createShulkerBoxWithNewItems(boxStack, items);
                player.playerScreenHandler.slots.get(boxSlotId).setStack(newBox);
            } else { // 物品足够，潜影盒不增加空槽位
                oneStackInBox.setCount(itemCount - maxMoveCount);// 更新潜影盒中的物品数量
                itemToGive = oneStackInBox.copy();
                itemToGive.setCount(maxMoveCount);
                if (emptySlotId != -1) {// 有空槽位，放到空槽位，下次投影自己替换主手
                    ItemStack newBox = Utils.createShulkerBoxWithNewItems(boxStack, items);
                    player.playerScreenHandler.slots.get(boxSlotId).setStack(newBox);
                    givePlayerItems(itemToGive, player, emptySlotId);
                } else { // 没有空槽位，开启物品回收
                    // 开启：没有空槽位时，回收物品到潜影盒
                    // 首先回收到当前潜影盒，如果没空位置，找到第一个装有该物品但是合起来不超过一组的潜影盒，或者第一个带有空槽位的潜影盒
                    // 放置物品的潜影盒
                    int toCollectSlotId = getFirstNoContainer(player);
                    if (toCollectSlotId == -1) {
                        Utils.LOGGER.warn("moveBoxItem not found item inventory to replace into box for: {}", itemToGive);
                        return;
                    }
                    ItemStack toCollectItem = player.getInventory().getStack(toCollectSlotId);
                    boolean success = tryCollectIntoBox(player, toCollectItem);
                    if (!success) {
                        Utils.LOGGER.warn("moveBoxItem unable to collect into box for: {}", toCollectItem);
                        return;
                    }
                    // 将潜影盒中的物品放到回收的空槽位，tryCollectIntoBox已经将新的潜影盒给予玩家
                    givePlayerItems(itemToGive, player, toCollectSlotId);
                }

            }
            break;
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


    private static boolean tryCollectIntoBox(ServerPlayerEntity player, ItemStack toCollect) {
        int slotId = findSlotWithBoxWithItem(player.currentScreenHandler, toCollect, false);
        if (slotId != -1) { // 找到带有该物品的潜影盒
            ItemStack boxStack = player.getInventory().getStack(slotId);
            ItemStack newBoxStack = Utils.addItemIntoBox(boxStack, toCollect);
            if (newBoxStack != null) {// 放入成功
                player.playerScreenHandler.slots.get(slotId).setStack(newBoxStack);
                return true;
            }
        } // 放入失败，对所有的潜影盒便利，尝试放入
        for (int ind = 0; ind < player.getInventory().main.size(); ++ind) {
            ItemStack boxStack = player.getInventory().getStack(ind);
            if (!Utils.isItShulkerBox(boxStack)) {
                continue;
            }
            ItemStack newBoxStack = Utils.addItemIntoBox(boxStack, toCollect);
            if (newBoxStack != null) {// 放入成功
                player.playerScreenHandler.slots.get(slotId).setStack(newBoxStack);
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {

        ItemStack boxStack = new ItemStack(Items.SHULKER_BOX.asItem(), 1);
        ItemStack itemStack = new ItemStack(Items.GLASS, 20);
        ItemStack toAddStack = new ItemStack(Items.BLUE_CONCRETE, 20);
        ContainerComponent containerComponent = ContainerComponent.fromStacks(Lists.newArrayList(itemStack));
        boxStack.set(DataComponentTypes.CONTAINER, containerComponent);
        ItemStack newBoxStack = Utils.addItemIntoBox(boxStack, toAddStack);

        System.out.println(boxStack);
        System.out.println(newBoxStack);
    }


}
