package com.square.events.impl;

import com.square.events.Event;

public class OrderFulfilledEvent implements Event {
    private final String orderId;
    public OrderFulfilledEvent(String orderId){
        this.orderId = orderId;
    }
    public String getOrderId(){
        return orderId;
    }
    @Override
    public String toString(){
        return String.format("OrderFulfilledEvent: orderId: %s/n",orderId);
    }
}
