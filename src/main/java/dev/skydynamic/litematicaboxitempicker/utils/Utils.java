package dev.skydynamic.litematicaboxitempicker.utils;

import dev.skydynamic.litematicaboxitempicker.registries.ItemStackRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.DefaultedList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Objects;

import static fi.dy.masa.litematica.util.InventoryUtils.doesShulkerBoxContainItem;

public class Utils {
    public static Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);
    public static final ItemStackRegistry REGISTRY = new ItemStackRegistry();
    public static final int SHULKER_BOX_SIZE = 27;

    public static void enableDetailLogging(boolean enable) {
        try {
            System.out.println("enableDetailLogging: " + enable);
            if (enable) {
                Configurator.setLevel(Reference.MOD_ID, Level.ALL);
            } else {
                Configurator.setLevel(Reference.MOD_ID, Level.OFF);
            }
        } catch (Exception e) {
            System.out.println("enableDetailLogging error:" + e);
        }
    }


    public static DefaultedList<ItemStack> getStoredItemsWithoutOrder(ItemStack stackIn) {
        ContainerComponent container = stackIn.getComponents().get(DataComponentTypes.CONTAINER);
        if (container != null) {
            Iterator<ItemStack> iter = container.stream().iterator();// 不去除空格子，物品之间的空格子是 0xAIR
            DefaultedList<ItemStack> items = DefaultedList.ofSize((int) container.stream().count());
            while (iter.hasNext()) {
                ItemStack stack = iter.next();
                items.add(stack);
            }
            for (int i = items.size(); i < SHULKER_BOX_SIZE; ++i) {// 补充潜影盒物品到27格
                items.add(ItemStack.EMPTY);
            }
            return items;
        }
        return DefaultedList.of();
    }

    public static ItemStack createShulkerBoxWithNewItems(ItemStack boxStack, DefaultedList<ItemStack> items) {
        ItemStack itemStack = new ItemStack(RegistryEntry.of(boxStack.getItem()), 1);
        ContainerComponent containerComponent = ContainerComponent.fromStacks(items);
        itemStack.set(DataComponentTypes.CONTAINER, containerComponent);
        return itemStack;
    }

    public static ItemStack removeItemFromBox(ItemStack boxStack, ItemStack toRemoveStack) {
        String toRemoveId = toRemoveStack.getItem().toString();
        DefaultedList<ItemStack> boxItems = getStoredItemsWithoutOrder(boxStack);//27格
        for (int i = 0; i < boxItems.size(); ++i) {
            String itemId = boxItems.get(i).getItem().toString();
            if (Objects.equals(toRemoveId, itemId)) {
                boxItems.set(i, ItemStack.EMPTY);
                return createShulkerBoxWithNewItems(boxStack, boxItems);
            }
        }
        return null;
    }

    public static ItemStack decreaseItemCountOfBox(ItemStack boxStack, Item toUpdateItem, int count) {
        return setItemCountOfBox(boxStack, toUpdateItem, -count);
    }

    public static ItemStack increaseItemCountOfBox(ItemStack boxStack, Item toUpdateItem, int count) {
        return setItemCountOfBox(boxStack, toUpdateItem, count);
    }

    public static ItemStack setItemCountOfBox(ItemStack boxStack, Item toUpdateItem, int addCount) {
        String toUpdateId = toUpdateItem.toString();
        DefaultedList<ItemStack> boxItems = getStoredItemsWithoutOrder(boxStack);//27格
        Utils.LOGGER.warn("setItemCountOfBox toUpdateItem {} addCount {} boxItems:{}", toUpdateItem, addCount, boxItems);
        for (int i = 0; i < boxItems.size(); ++i) {
            String itemId = boxItems.get(i).getItem().toString();
            if (Objects.equals(toUpdateId, itemId)) {
                ItemStack foundStack = boxItems.get(i);
                foundStack.increment(addCount);
                boxItems.set(i, foundStack);
                return createShulkerBoxWithNewItems(boxStack, boxItems);
            }
        }
        return null;
    }


    public static ItemStack addItemIntoBox(ItemStack boxStack, ItemStack toAddStack) {
        String toAddId = toAddStack.getItem().toString();
        DefaultedList<ItemStack> boxItems = getStoredItemsWithoutOrder(boxStack);//27格
        int emptyInd = -1;
        for (int i = 0; i < boxItems.size(); ++i) {
            if (boxItems.get(i).isEmpty()) {
                emptyInd = i;
                break;
            }
        }
        Utils.LOGGER.warn("addItemIntoBox found emptySlot:{}", emptyInd);
        Utils.LOGGER.warn("addItemIntoBox before boxItems:{}", boxItems);
        boolean success = false;
        for (int i = 0; i < boxItems.size(); ++i) {
            ItemStack stack = boxItems.get(i);
            String oneStackId = stack.getItem().toString();
            // 优先合并物品，其次放到空槽位
            if (Objects.equals(oneStackId, toAddId)) {
                int maxCount = stack.getMaxCount();
                int totCount = stack.getCount() + toAddStack.getCount();
                if (totCount <= maxCount) {
                    stack.setCount(totCount);
                    success = true;
                    break;
                } else if (emptyInd != -1) {
                    stack.setCount(maxCount);
                    toAddStack.setCount(totCount - maxCount);
                    boxItems.set(emptyInd, toAddStack);
                    success = true;
                    break;
                } else {
                    // can not merge with same item
                }
            } else if (stack.isEmpty()) {
                boxItems.set(i, toAddStack);
                success = true;
                break;
            } else {
                // can not set in empty slot
            }
        }
        Utils.LOGGER.warn("addItemIntoBox after boxItems:{}", boxItems);
        if (success) {
            Utils.LOGGER.warn("addItemIntoBox success");
            return createShulkerBoxWithNewItems(boxStack, boxItems);
        }
        return null;
    }

    public static boolean isItShulkerBox(ItemStack boxStack) {
        return boxStack.getItem().toString().contains(Items.SHULKER_BOX.toString());
    }

    public static int findSlotWithBoxWithItem(PlayerInventory inventory, ItemStack stackReference) {
        for (int ind = 0; ind < inventory.size(); ind++) {
            ItemStack stack = inventory.getStack(ind);
            if (Utils.isItShulkerBox(stack) && doesShulkerBoxContainItem(stack, stackReference)) {
                return ind;
            }
        }
        return -1;
    }
}
