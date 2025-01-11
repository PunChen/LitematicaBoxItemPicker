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

public class PlayerSlotUtils {

    public static boolean isPlayerHaveEmptySlot(ServerPlayerEntity player) {
        return player.getInventory().getEmptySlot() != -1;
    }

    // 检测是否有空余的格子(除去盔甲栏和副手)
    public static boolean getPlayerSlotHaveEmpty(Inventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

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
    public static void givePlayerItems(ItemStack stack, ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        if (inventory.insertStack(getPlayerEmptySlot(player), stack)) {
            ItemEntity itemEntity = player.dropItem(stack, false);
            if (itemEntity != null) {
                itemEntity.setDespawnImmediately();
            }
            player.currentScreenHandler.sendContentUpdates();
        } else {
            Utils.LOGGER.warn("givePlayerItems fails {}", stack);
        }
    }

    // 将盒子内的物品转移到手上
    public static void moveBoxItem(ServerPlayerEntity player, ItemStack stack, ItemStack boxStack, int maxMoveCount, int boxSlotId) {
        // 获得潜影盒的物品列表
        DefaultedList<ItemStack> items = Utils.getStoredItemsWithoutOrder(boxStack);
        String stackItemId = Registries.ITEM.getId(stack.getItem()).toString();
        for (int i = 0; i < items.size(); ++i) {
            ItemStack oneStackInBox = items.get(i);
            String boxItemId = oneStackInBox.getItem().toString();
            if (boxItemId.equals(stackItemId)) {
                int itemCount = oneStackInBox.getCount();
                if (itemCount <= maxMoveCount) {
                    ItemStack itemToGive = oneStackInBox.copy();
                    givePlayerItems(itemToGive, player);
                    items.remove(i);
                    ItemStack newBox = createShulkerBoxWithNewItems(boxStack, items);
                    player.playerScreenHandler.slots.get(boxSlotId).setStack(newBox);
                } else {
                    oneStackInBox.setCount(itemCount - maxMoveCount);
                    ItemStack itemToGive = oneStackInBox.copy();
                    itemToGive.setCount(maxMoveCount);
                    givePlayerItems(itemToGive, player);
                    ItemStack newBox = createShulkerBoxWithNewItems(boxStack, items);
                    player.playerScreenHandler.slots.get(boxSlotId).setStack(newBox);
                }
                return;
            }
        }
    }

    public static ItemStack createShulkerBoxWithNewItems(ItemStack boxStack, DefaultedList<ItemStack> items) {
        ItemStack itemStack = new ItemStack(RegistryEntry.of(boxStack.getItem()), 1);
        ContainerComponent containerComponent = ContainerComponent.fromStacks(items);
        itemStack.set(DataComponentTypes.CONTAINER, containerComponent);
        return itemStack;
    }


}
