package com.square.events.impl;

import com.square.events.Event;

public class PaymentAppliedEvent implements Event {
    private final String orderId;
    private final double amount;
    public PaymentAppliedEvent(String orderId,double amount){
        this.orderId = orderId;
        this.amount = amount;
    }
    public String getOrderId(){
        return orderId;
    }
    public double getAmount(){
        return amount;
    }
    @Override
    public String toString(){
        return String.format("PaymentAppliedEvent: orderId: %s,amount: %s/n",orderId,amount);
    }
}
