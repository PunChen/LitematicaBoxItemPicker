package dev.skydynamic.litematicaboxitempicker.utils;

import dev.skydynamic.litematicaboxitempicker.registries.ItemStackRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.DefaultedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

import java.util.Iterator;
import java.util.Objects;

public class Utils {
    public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);

    public static final ItemStackRegistry REGISTRY = new ItemStackRegistry();

    public static DefaultedList<ItemStack> getStoredItemsWithoutOrder(ItemStack stackIn) {
        ContainerComponent container = stackIn.getComponents().get(DataComponentTypes.CONTAINER);
        if (container != null) {
            Iterator<ItemStack> iter = container.stream().iterator();
            DefaultedList<ItemStack> items = DefaultedList.ofSize((int) container.stream().count());
            while (iter.hasNext()) {
                ItemStack stack = iter.next();
                if (stack.isEmpty()) {
                    stack = new ItemStack(Items.AIR);
                }
                items.add(stack);
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

    public static ItemStack addItemIntoBox(ItemStack boxStack, ItemStack toAddStack) {
        String toAddId = toAddStack.getItem().toString();
        DefaultedList<ItemStack> boxItems = getStoredItemsWithoutOrder(boxStack);
        int emptyInd = -1;
        for (int i = 0; i < boxItems.size(); ++i) {
            if (boxItems.get(i).isEmpty()) {
                emptyInd = i;
                break;
            }
        }
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
                    boxItems.set(i, toAddStack);
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

        if (success) {
            return createShulkerBoxWithNewItems(boxStack, boxItems);
        }
        return null;
    }

    public static boolean isItShulkerBox(ItemStack boxStack) {
        return boxStack.getItem().toString().contains(Items.SHULKER_BOX.toString());
    }
}
