package com.square.events.impl;

import com.square.events.Event;

import java.util.List;

public class PlaceOrderEvent implements Event {
    private final String orderId;
    private final String customerEmail;
    private final List<String> items;

    public PlaceOrderEvent(String orderId,String customerEmail,List<String> items){
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.items = items;
    }
    public String getOrderId(){
        return orderId;
    }
    public String getCustomerEmail(){
        return customerEmail;
    }
    public List<String> getItems(){
        return items;
    }

    @Override
    public String toString(){
        return String.format("PlaceOrderEvent: orderId: %s, customerEmail: %s, items: %s/n",orderId,customerEmail,items);
    }
}
