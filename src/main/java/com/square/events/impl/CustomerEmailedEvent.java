package com.square.events.impl;

import com.square.events.Event;

public class CustomerEmailedEvent implements Event {
    private final String customerEmail;
    private final String message;
    private final String orderId;
    public CustomerEmailedEvent(String customerEmail,String message,String orderId){
        this.customerEmail = customerEmail;
        this.message = message;
        this.orderId = orderId;
    }

    public String getCustomerEmail(){
        return customerEmail;
    }

    public String getMessage(){
        return message;
    }

    public String getOrderId(){
        return orderId;
    }

    @Override
    public String toString(){
        return String.format("CustomerEmailedEvent: customerEmail: %s, message: %s, orderId: %s/n",customerEmail,message,orderId);
    }
}
