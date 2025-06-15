package com.square.events.impl;

import com.square.events.Event;

public class OrderCreatedEvent implements Event {
    private final String orderId;
    public OrderCreatedEvent(String orderId){
        this.orderId = orderId;
    }
    public String getOrderId(){
        return orderId;
    }

    @Override
    public String toString(){
        return String.format("OrderCreatedEvent: orderId: %s/n",orderId);
    }
}
