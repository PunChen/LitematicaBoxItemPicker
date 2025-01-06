package dev.skydynamic.litematicaboxitempicker.model;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public class MoveItemCountPayload implements CustomPayload {
    public static final Id<MoveItemCountPayload> MOVE_ITEM_COUNT_ID = new Id<>(Identifier.of("lsbp", "move_item_count"));
    /**
     * 最大移动数量
     */
    private int maxMoveCount = -1;
    /**
     * 有指定物品的潜影盒所在槽位
     */
    private int hasItemBoxSlot = -1;
    /**
     * 目标物品
     */
    private ItemStack targetStack = null;
    /**
     * 潜影盒
     */
    private ItemStack boxStack = null;

    @Override
    public Id<? extends CustomPayload> getId() {
        return MOVE_ITEM_COUNT_ID;
    }


    public int getMaxMoveCount() {
        return maxMoveCount;
    }

    public void setMaxMoveCount(int maxMoveCount) {
        this.maxMoveCount = maxMoveCount;
    }

    public int getHasItemBoxSlot() {
        return hasItemBoxSlot;
    }

    public void setHasItemBoxSlot(int hasItemBoxSlot) {
        this.hasItemBoxSlot = hasItemBoxSlot;
    }

    public ItemStack getTargetStack() {
        return targetStack;
    }

    public void setTargetStack(ItemStack targetStack) {
        this.targetStack = targetStack;
    }

    public ItemStack getBoxStack() {
        return boxStack;
    }

    public void setBoxStack(ItemStack boxStack) {
        this.boxStack = boxStack;
    }
}
