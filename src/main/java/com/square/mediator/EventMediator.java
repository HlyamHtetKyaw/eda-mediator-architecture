package com.square.mediator;

import com.square.events.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class EventMediator {
    private final Map<Class<? extends Event>, Set<Consumer<Event>>> subscribers;
    private ExecutorService executorService;

    public EventMediator(int poolSize){
        this.subscribers = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    public <T extends Event> void subscribe(Class<T> eventType,Consumer<T> handler){
        subscribers.computeIfAbsent(eventType,k->ConcurrentHashMap.newKeySet()).add((Consumer<Event>)handler);
        System.out.println(Thread.currentThread().getName()+": Subscribed handler for "+eventType.getSimpleName());
    }

    public void publish(Event event){
        System.out.println("\n"+Thread.currentThread()+": Publishing event: "+event.getClass().getSimpleName());
        Set<Consumer<Event>> handlers = subscribers.getOrDefault(event.getClass(),ConcurrentHashMap.newKeySet());
        if(handlers.isEmpty()){
            System.out.println(Thread.currentThread().getName()+": No handlers subscribed for "+event.getClass().getSimpleName());
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for(Consumer<Event> handler : handlers){
            CompletableFuture<Void> future = CompletableFuture.runAsync(()->handler.accept(event),executorService);
            futures.add(future);
        }
    }

    public void shutdown(){
        executorService.shutdown();
        try{
            if(!executorService.awaitTermination(10, TimeUnit.SECONDS)){
                executorService.shutdown();
            }
        }catch (InterruptedException e){
            executorService.shutdown();
            Thread.currentThread().interrupt();
        }
        System.out.println("EventMediator shut down.");
    }
}
