package dev.skydynamic.litematicaboxitempicker.utils;

import dev.skydynamic.litematicaboxitempicker.Utils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import static fi.dy.masa.litematica.util.InventoryUtils.setPickedItemToHand;

public class PlayerSlotUtils {

    // 检测是否有空余的格子(除去盔甲栏和副手)
    public static boolean getPlayerSlotHaveEmpty(ClientPlayerEntity player) {
        Inventory inventory = player.getInventory();
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
        }
    }

    // 将盒子内的物品转移到手上
    // 将盒子内的物品转移到手上
    public static void moveBoxItem(ServerPlayerEntity player, ItemStack stack, ItemStack boxStack, int maxMoveCount, int boxSlot) {
        // 获得潜影盒的物品列表 todo getStoredItems getBundleItems 区别
        DefaultedList<ItemStack> items = Utils.getBundleItems(boxStack);
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
                    ItemStack newBox = createShulkerBoxWithItems(items);
                    player.getInventory().setStack(boxSlot,newBox);
                    // todo 区别
//                    player.playerScreenHandler.slots.get(boxSlot).getStack().set(boxSlot,newBox);
                } else {
                    oneStackInBox.setCount(itemCount - maxMoveCount);
                    ItemStack itemToGive = oneStackInBox.copy();
                    itemToGive.setCount(maxMoveCount);
                    givePlayerItems(itemToGive, player);
                    ItemStack newBox = createShulkerBoxWithItems(items);
                    player.getInventory().setStack(boxSlot,newBox);
//                    player.playerScreenHandler.slots.get(boxSlot).getStack().setNbt(boxItemsNbtCompound);
                }
                return;
            }
        }
    }

    public static ItemStack createShulkerBoxWithItems(DefaultedList<ItemStack> items) {
        ItemStack itemStack = new ItemStack(RegistryEntry.of(Items.SHULKER_BOX),1);
//        ItemConvertible value = (RegistryEntry.of(Items.SHULKER_BOX).value());
//        itemStack.set(DataComponentTypes.CONTAINER, items);
//        ContainerComponent container = itemStack.getComponents().get(DataComponentTypes.CONTAINER);
//
//        itemStack.set(DataComponentTypes.CONTAINER,container);
//        ComponentMap.builder().addAll().build();

//        if (container != null)
//        {
//            Iterator<ItemStack> iter = container.streamNonEmpty().iterator();
//            container.stream()
//            DefaultedList<ItemStack> items = DefaultedList.ofSize((int) container.streamNonEmpty().count());
//
//            // Using 'container.copyTo(items)' will break Litematica's Material List
//            while (iter.hasNext())
//            {
//                items.add(iter.next());
//            }
//
//            return items;
//        }


        return itemStack;
    }

}
