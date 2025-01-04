package dev.skydynamic.litematicaboxitempicker;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.Iterator;

public class Utils {
    public static DefaultedList<ItemStack> getBundleItems(ItemStack stackIn)
    {
        BundleContentsComponent bundleContainer = stackIn.getComponents().getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);

        if (bundleContainer != null && bundleContainer.equals(BundleContentsComponent.DEFAULT) == false)
        {
            int maxSlots = bundleContainer.size();
            DefaultedList<ItemStack> items = DefaultedList.ofSize(maxSlots);
            Iterator<ItemStack> iter = bundleContainer.stream().iterator();

            while (iter.hasNext())
            {
                ItemStack slot = iter.next();

                if (slot.isEmpty() == false)
                {
                    items.add(slot);
                }
            }

            return items;
        }

        return DefaultedList.of();
    }
}
