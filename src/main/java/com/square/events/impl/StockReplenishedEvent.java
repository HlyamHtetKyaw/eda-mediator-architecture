package com.square.events.impl;


import com.square.events.Event;

import java.util.List;

public class StockReplenishedEvent  implements Event {
    private final String orderId;
    private final List<String> itemsReplenished;

    public StockReplenishedEvent(String orderId, List<String> itemsReplenished) {
        this.orderId = orderId;
        this.itemsReplenished = itemsReplenished;
    }

    public String getOrderId() { return orderId; }
    public List<String> getItemsReplenished() { return itemsReplenished; }
    @Override
    public String toString(){
        return String.format("StockReplenishedEvent: orderId: %s,itemsReplenished: %s/n",orderId,itemsReplenished);
    }
}
