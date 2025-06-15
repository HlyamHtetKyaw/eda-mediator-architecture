package com.square.events.impl;

import com.square.events.Event;

import java.util.List;

public class InventoryAdjustedEvent implements Event {
    private final String orderId;
    private final List<String> itemsAdjusted;
    public InventoryAdjustedEvent(String orderId,List<String> itemsAdjusted){
        this.orderId = orderId;
        this.itemsAdjusted = itemsAdjusted;
    }
    public String getOrderId(){
        return orderId;
    }
    public List<String> getItemsAdjusted(){
        return itemsAdjusted;
    }
    @Override
    public String toString(){
        return String.format("InventoryAdjustedEvent: orderId: %s,itemsAdjusted: %s/n",orderId,itemsAdjusted);
    }
}
