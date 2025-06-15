package com.square;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// --- 1. Event Definitions (No Change from previous version) ---

/**
 * Base interface for all events in the system.
 */
interface Event {}

/**
 * Event triggered when a new order is initiated (Purchasing Book).
 */
class PlaceOrderEvent implements Event {
    private final String orderId;
    private final String customerEmail;
    private final List<String> items;

    public PlaceOrderEvent(String orderId, String customerEmail, List<String> items) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.items = items;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerEmail() { return customerEmail; }
    public List<String> getItems() { return items; }

    @Override
    public String toString() {
        return "PlaceOrderEvent{" +
                "orderId='" + orderId + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", items=" + items +
                '}';
    }
}

/**
 * Event indicating that an order has been successfully created.
 */
class OrderCreatedEvent implements Event {
    private final String orderId;

    public OrderCreatedEvent(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() { return orderId; }

    @Override
    public String toString() {
        return "OrderCreatedEvent{" +
                "orderId='" + orderId + '\'' +
                '}';
    }
}

/**
 * Event indicating that an email has been sent to the customer.
 */
class CustomerEmailedEvent implements Event {
    private final String customerEmail;
    private final String message;
    private final String orderId;

    public CustomerEmailedEvent(String orderId, String customerEmail, String message) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.message = message;
    }

    public String getCustomerEmail() { return customerEmail; }
    public String getMessage() { return message; }
    public String getOrderId() { return orderId; }

    @Override
    public String toString() {
        return "CustomerEmailedEvent{" +
                "orderId='" + orderId + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

/**
 * Event indicating that payment has been applied to an order.
 */
class PaymentAppliedEvent implements Event {
    private final String orderId;
    private final double amount;

    public PaymentAppliedEvent(String orderId, double amount) {
        this.orderId = orderId;
        this.amount = amount;
    }

    public String getOrderId() { return orderId; }
    public double getAmount() { return amount; }

    @Override
    public String toString() {
        return "PaymentAppliedEvent{" +
                "orderId='" + orderId + '\'' +
                ", amount=" + amount +
                '}';
    }
}

/**
 * Event indicating that inventory has been adjusted for an order.
 */
class InventoryAdjustedEvent implements Event {
    private final String orderId;
    private final List<String> itemsAdjusted;

    public InventoryAdjustedEvent(String orderId, List<String> itemsAdjusted) {
        this.orderId = orderId;
        this.itemsAdjusted = itemsAdjusted;
    }

    public String getOrderId() { return orderId; }
    public List<String> getItemsAdjusted() { return itemsAdjusted; }

    @Override
    public String toString() {
        return "InventoryAdjustedEvent{" +
                "orderId='" + orderId + '\'' +
                ", itemsAdjusted=" + itemsAdjusted +
                '}';
    }
}

/**
 * Event indicating that an order has been fulfilled.
 */
class OrderFulfilledEvent implements Event {
    private final String orderId;

    public OrderFulfilledEvent(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() { return orderId; }

    @Override
    public String toString() {
        return "OrderFulfilledEvent{" +
                "orderId='" + orderId + '\'' +
                '}';
    }
}

/**
 * Event indicating that stock has been replenished for certain items.
 */
class StockReplenishedEvent implements Event {
    private final String orderId; // Associated with the order that triggered replenishment
    private final List<String> itemsReplenished;

    public StockReplenishedEvent(String orderId, List<String> itemsReplenished) {
        this.orderId = orderId;
        this.itemsReplenished = itemsReplenished;
    }

    public String getOrderId() { return orderId; }
    public List<String> getItemsReplenished() { return itemsReplenished; }

    @Override
    public String toString() {
        return "StockReplenishedEvent{" +
                "orderId='" + orderId + '\'' +
                ", itemsReplenished=" + itemsReplenished +
                '}';
    }
}


// --- 2. Workflow Management Classes ---

/**
 * Represents the status of a single task within a workflow.
 */
enum TaskStatus {
    PENDING,      // Task is waiting to be started
    IN_PROGRESS,  // Task is currently running
    COMPLETED,    // Task finished successfully
    FAILED        // Task encountered an error
}

/**
 * Represents a single, trackable unit of work (task) within a larger workflow.
 */
class WorkflowTask {
    private final String taskId; // Unique identifier for this task (e.g., "CreateOrder")
    private TaskStatus status;
    private String errorMessage;
    private Event triggerEvent; // The event that caused this task to start (for resumption)

    public WorkflowTask(String taskId, TaskStatus status, Event triggerEvent) {
        this.taskId = taskId;
        this.status = status;
        this.triggerEvent = triggerEvent;
    }

    public String getTaskId() { return taskId; }
    public TaskStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Event getTriggerEvent() { return triggerEvent; }

    public void setStatus(TaskStatus status) { this.status = status; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setTriggerEvent(Event triggerEvent) { this.triggerEvent = triggerEvent; }

    @Override
    public String toString() {
        return "WorkflowTask{" +
                "taskId='" + taskId + '\'' +
                ", status=" + status +
                ", errorMessage='" + (errorMessage != null ? errorMessage : "N/A") + '\'' +
                '}';
    }
}

/**
 * Represents a running instance of a specific workflow (e.g., an order processing workflow).
 * Contains the state of all tasks belonging to this instance.
 */
class WorkflowInstance {
    private final String workflowId; // Unique ID for this workflow instance (e.g., orderId)
    // Map of taskId to WorkflowTask, storing the current state of each task
    private final ConcurrentHashMap<String, WorkflowTask> tasks;
    // Defines the predefined, sequential order of tasks for this workflow template
    private final List<String> taskExecutionOrder;

    public WorkflowInstance(String workflowId, List<String> predefinedTaskOrder) {
        this.workflowId = workflowId;
        this.tasks = new ConcurrentHashMap<>();
        this.taskExecutionOrder = predefinedTaskOrder;
        // Initialize all tasks for this workflow as PENDING
        predefinedTaskOrder.forEach(taskId -> tasks.put(taskId, new WorkflowTask(taskId, TaskStatus.PENDING, null)));
    }

    public String getWorkflowId() { return workflowId; }
    public WorkflowTask getTask(String taskId) { return tasks.get(taskId); }
    public List<String> getTaskExecutionOrder() { return taskExecutionOrder; }

    /**
     * Checks if all tasks in the workflow are completed.
     * Note: This assumes a linear progression or that parallel branches eventually converge.
     * For complex DAG workflows, a more sophisticated completion check is needed.
     */
    public boolean isCompleted() {
        return tasks.values().stream().allMatch(task -> task.getStatus() == TaskStatus.COMPLETED);
    }

    /**
     * Checks if any task in the workflow has failed.
     */
    public boolean isFailed() {
        return tasks.values().stream().anyMatch(task -> task.getStatus() == TaskStatus.FAILED);
    }

    /**
     * Gets the current overall status of the workflow instance.
     */
    public String getOverallStatus() {
        if (isCompleted()) {
            return "COMPLETED";
        }
        if (isFailed()) {
            return "FAILED";
        }
        // If not completed and not failed, check if any task is in progress
        if (tasks.values().stream().anyMatch(task -> task.getStatus() == TaskStatus.IN_PROGRESS)) {
            return "IN_PROGRESS";
        }
        return "PENDING"; // Or PARTIALLY_COMPLETED if some tasks are done but not all
    }

    @Override
    public String toString() {
        return "WorkflowInstance{" +
                "workflowId='" + workflowId + '\'' +
                ", overallStatus='" + getOverallStatus() + '\'' +
                ", tasks=" + tasks.values().stream().map(WorkflowTask::toString).collect(Collectors.joining(", ")) +
                '}';
    }
}

/**
 * Manages the lifecycle and state of multiple workflow instances.
 * Handles task status updates, error reporting, and workflow resumption.
 * This class simulates persistent storage of workflow state (currently in-memory).
 */
class WorkflowManager {
    private final EventMediator mediator;
    // Stores all active workflow instances, mapped by their workflow ID.
    private final ConcurrentHashMap<String, WorkflowInstance> workflows;

    // Defines the typical task sequence for an order processing workflow.
    // This is a simplified linear sequence for demonstration.
    private static final List<String> ORDER_WORKFLOW_TASKS = List.of(
            "CreateOrder",
            "EmailCustomerPlaced",
            "ApplyPayment",      // Parallel with AdjustInventory
            "AdjustInventory",   // Parallel with ApplyPayment
            "FulfillOrder",
            "ReplenishStock",
            "EmailCustomerFulfilled" // Part of user's Step 4
            // "ShipOrder",             // User's Step 4
            // "EmailCustomerShipped"   // User's Step 5
    );

    public WorkflowManager(EventMediator mediator) {
        this.mediator = mediator;
        this.workflows = new ConcurrentHashMap<>();
    }

    /**
     * Initializes and starts a new order processing workflow.
     * This creates a new WorkflowInstance and publishes the initial event.
     * @param orderId The unique ID for this order (workflow).
     * @param initialEvent The PlaceOrderEvent that kicks off the workflow.
     */
    public void startNewOrderWorkflow(String orderId, PlaceOrderEvent initialEvent) {
        // Create a new workflow instance with the predefined task order
        WorkflowInstance instance = new WorkflowInstance(orderId, ORDER_WORKFLOW_TASKS);
        workflows.put(orderId, instance); // Store the instance

        // The "CreateOrder" task is the first to be triggered by PlaceOrderEvent
        WorkflowTask createOrderTask = instance.getTask("CreateOrder");
        if (createOrderTask != null) {
            createOrderTask.setTriggerEvent(initialEvent); // Store the initial event for potential resumption
            System.out.println(Thread.currentThread().getName() + ": Workflow " + orderId + " initialized. Publishing initial event for CreateOrder.");
            mediator.publish(initialEvent); // Publish to trigger the first task
        } else {
            System.err.println("Error: 'CreateOrder' task not found in workflow definition for " + orderId);
        }
    }

    /**
     * Marks a specific task within a workflow as started (IN_PROGRESS).
     * @param workflowId The ID of the workflow instance.
     * @param taskId The ID of the task.
     * @param triggerEvent The event that triggered this task (important for resumption).
     */
    public void markTaskStarted(String workflowId, String taskId, Event triggerEvent) {
        WorkflowInstance instance = workflows.get(workflowId);
        if (instance != null) {
            WorkflowTask task = instance.getTask(taskId);
            if (task != null) {
                // Only mark as IN_PROGRESS if it's PENDING or FAILED (for resumption)
                if (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.FAILED) {
                    task.setStatus(TaskStatus.IN_PROGRESS);
                    task.setTriggerEvent(triggerEvent); // Store the event that started it
                    System.out.println(Thread.currentThread().getName() + ": Workflow " + workflowId + " - Task " + taskId + " IN_PROGRESS.");
                } else {
                    System.out.println(Thread.currentThread().getName() + ": Workflow " + workflowId + " - Task " + taskId + " already " + task.getStatus() + ". Skipping mark started.");
                }
            } else {
                System.err.println("Warning: Task " + taskId + " not found for workflow " + workflowId);
            }
        } else {
            System.err.println("Warning: Workflow " + workflowId + " not found.");
        }
    }

    /**
     * Marks a specific task within a workflow as completed (COMPLETED).
     * @param workflowId The ID of the workflow instance.
     * @param taskId The ID of the task.
     */
    public void markTaskCompleted(String workflowId, String taskId) {
        WorkflowInstance instance = workflows.get(workflowId);
        if (instance != null) {
            WorkflowTask task = instance.getTask(taskId);
            if (task != null) {
                task.setStatus(TaskStatus.COMPLETED);
                task.setErrorMessage(null); // Clear any old error message
                System.out.println(Thread.currentThread().getName() + ": Workflow " + workflowId + " - Task " + taskId + " COMPLETED.");
                // After completion, check if the entire workflow is now completed
                checkWorkflowOverallStatus(workflowId);
            } else {
                System.err.println("Warning: Task " + taskId + " not found for workflow " + workflowId);
            }
        } else {
            System.err.println("Warning: Workflow " + workflowId + " not found.");
        }
    }

    /**
     * Marks a specific task within a workflow as failed (FAILED).
     * @param workflowId The ID of the workflow instance.
     * @param taskId The ID of the task.
     * @param errorMessage The error message describing the failure.
     */
    public void markTaskFailed(String workflowId, String taskId, String errorMessage) {
        WorkflowInstance instance = workflows.get(workflowId);
        if (instance != null) {
            WorkflowTask task = instance.getTask(taskId);
            if (task != null) {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage(errorMessage);
                System.err.println("\n------------------------------------------------------------------");
                System.err.println(Thread.currentThread().getName() + ": Workflow " + workflowId + " - Task " + taskId + " FAILED: " + errorMessage);
                System.err.println("User Action Required: Please fix the issue for workflow " + workflowId + " and call workflowManager.resumeWorkflow(\"" + workflowId + "\").");
                System.err.println("------------------------------------------------------------------\n");
                checkWorkflowOverallStatus(workflowId);
            } else {
                System.err.println("Warning: Task " + taskId + " not found for workflow " + workflowId);
            }
        } else {
            System.err.println("Warning: Workflow " + workflowId + " not found.");
        }
    }

    /**
     * Retrieves the current status of a specific task within a workflow.
     * @param workflowId The ID of the workflow instance.
     * @param taskId The ID of the task.
     * @return The TaskStatus, or null if workflow/task not found.
     */
    public TaskStatus getTaskStatus(String workflowId, String taskId) {
        WorkflowInstance instance = workflows.get(workflowId);
        if (instance != null) {
            WorkflowTask task = instance.getTask(taskId);
            if (task != null) {
                return task.getStatus();
            }
        }
        return null;
    }

    /**
     * Attempts to resume a workflow that has failed or is partially completed.
     * It finds the first PENDING or FAILED task in the defined execution order
     * and re-publishes its original trigger event to restart it.
     * @param workflowId The ID of the workflow instance to resume.
     */
    public void resumeWorkflow(String workflowId) {
        WorkflowInstance instance = workflows.get(workflowId);
        if (instance == null) {
            System.err.println("Error: Workflow " + workflowId + " not found for resumption.");
            return;
        }

        System.out.println("\n" + Thread.currentThread().getName() + ": Attempting to resume workflow " + workflowId + "...");

        // Iterate through tasks in their predefined execution order
        for (String taskId : instance.getTaskExecutionOrder()) {
            WorkflowTask task = instance.getTask(taskId);
            if (task == null) {
                System.err.println("Warning: Task " + taskId + " defined in order but not found in instance for workflow " + workflowId);
                continue;
            }

            // If a task is FAILED or PENDING, attempt to resume it
            if (task.getStatus() == TaskStatus.FAILED || task.getStatus() == TaskStatus.PENDING) {
                System.out.println(Thread.currentThread().getName() + ": Resuming task: " + taskId + " with current status " + task.getStatus());

                // Crucially, we use the stored trigger event to re-publish
                Event triggerEvent = task.getTriggerEvent();
                if (triggerEvent != null) {
                    System.out.println(Thread.currentThread().getName() + ": Re-publishing event " + triggerEvent.getClass().getSimpleName() + " to re-trigger " + taskId);
                    // Mark as PENDING again so the handler can mark it IN_PROGRESS if it starts
                    task.setStatus(TaskStatus.PENDING);
                    task.setErrorMessage(null); // Clear error for retry
                    mediator.publish(triggerEvent);
                    // Only resume the first encountered pending/failed task in order.
                    // Subsequent tasks will be triggered by successful completion of this one.
                    return;
                } else {
                    System.err.println("Error: Cannot resume task " + taskId + " for workflow " + workflowId + " as no trigger event was stored.");
                    // This scenario should ideally not happen if tasks are properly initialized and markTaskStarted is called.
                    break;
                }
            }
        }
        System.out.println(Thread.currentThread().getName() + ": No pending or failed tasks found to resume for workflow " + workflowId + ". Checking overall status.");
        checkWorkflowOverallStatus(workflowId);
    }

    /**
     * Prints the overall status of the workflow instance (COMPLETED, FAILED, IN_PROGRESS, PENDING).
     * Also lists the status of individual tasks.
     * @param workflowId The ID of the workflow instance.
     */
    public void checkWorkflowOverallStatus(String workflowId) {
        WorkflowInstance instance = workflows.get(workflowId);
        if (instance != null) {
            System.out.println("\n===== Workflow " + workflowId + " Overall Status: " + instance.getOverallStatus() + " =====");
            instance.getTaskExecutionOrder().forEach(taskId -> {
                WorkflowTask task = instance.getTask(taskId);
                if (task != null) {
                    System.out.println("  - Task: " + task.getTaskId() + " -> Status: " + task.getStatus() + (task.getErrorMessage() != null ? " (Error: " + task.getErrorMessage() + ")" : ""));
                }
            });
            System.out.println("==================================================================\n");
        } else {
            System.err.println("Workflow " + workflowId + " not found.");
        }
    }
}


// --- 3. Event Mediator (Slightly modified to provide ExecutorService directly) ---

/**
 * The central Event Mediator that manages event subscriptions and dispatches events.
 * It uses an ExecutorService for asynchronous and parallel event processing.
 */
class EventMediator {
    private final Map<Class<? extends Event>, Set<Consumer<Event>>> subscribers;
    private final ExecutorService executorService; // Provide access to this for handlers if needed

    /**
     * Constructs an EventMediator with a fixed thread pool.
     * @param poolSize The number of threads in the thread pool for event processing.
     */
    public EventMediator(int poolSize) {
        this.subscribers = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Subscribes a consumer (event handler) to a specific event type.
     * @param eventType The class of the event to subscribe to.
     * @param handler The consumer that will handle the event.
     * @param <T> The type of the event.
     */
    public <T extends Event> void subscribe(Class<T> eventType, Consumer<T> handler) {
        subscribers.computeIfAbsent(eventType, k -> ConcurrentHashMap.newKeySet()).add((Consumer<Event>) handler);
        System.out.println(Thread.currentThread().getName() + ": Subscribed handler for " + eventType.getSimpleName());
    }

    /**
     * Publishes an event to all subscribed handlers.
     * Each handler is executed asynchronously on the executor service.
     * Handlers are expected to manage their own success/failure reporting to WorkflowManager.
     * @param event The event to publish.
     */
    public void publish(Event event) {
        System.out.println("\n" + Thread.currentThread().getName() + ": Publishing event: " + event.getClass().getSimpleName() + " - " + event);
        Set<Consumer<Event>> handlers = subscribers.getOrDefault(event.getClass(), ConcurrentHashMap.newKeySet());

        if (handlers.isEmpty()) {
            System.out.println(Thread.currentThread().getName() + ": No handlers subscribed for " + event.getClass().getSimpleName());
            return;
        }

        // Use CompletableFuture to execute each handler asynchronously.
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Consumer<Event> handler : handlers) {
            // Handlers will now handle their own try-catch and report to WorkflowManager
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> handler.accept(event), executorService);
            futures.add(future);
        }

        // We don't block here. WorkflowManager will get updates from handlers.
    }

    /**
     * Shuts down the executor service gracefully.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) { // Increased timeout
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("EventMediator shut down.");
    }
}


// --- 4. Component Event Handlers (Modified to integrate with WorkflowManager) ---

/**
 * Handles the 'Create Order' logic, part of Step 1.
 * Simulates interaction with an Order Placement Component.
 */
class OrderPlacementHandler implements Consumer<PlaceOrderEvent> {
    private final EventMediator mediator;
    private final WorkflowManager workflowManager;
    private static final String TASK_ID = "CreateOrder"; // Unique ID for this task

    public OrderPlacementHandler(EventMediator mediator, WorkflowManager workflowManager) {
        this.mediator = mediator;
        this.workflowManager = workflowManager;
    }

    @Override
    public void accept(PlaceOrderEvent event) {
        String orderId = event.getOrderId();

        // Idempotency check: If task is already completed, skip.
        if (workflowManager.getTaskStatus(orderId, TASK_ID) == TaskStatus.COMPLETED) {
            System.out.println(Thread.currentThread().getName() + ": [OrderPlacementComponent] Task " + TASK_ID + " for workflow " + orderId + " already completed. Skipping.");
            return;
        }

        workflowManager.markTaskStarted(orderId, TASK_ID, event); // Mark task as started

        try {
            System.out.println(Thread.currentThread().getName() + ": [OrderPlacementComponent] Creating order for " + orderId);
            Thread.sleep(500); // Simulate network/DB call

            // Simulate an error for a specific order ID to test error handling
            if (orderId.equals("ORD-ERROR-CREATE")) {
                throw new RuntimeException("Simulated error: Database connection failed during order creation.");
            }

            System.out.println(Thread.currentThread().getName() + ": [OrderPlacementComponent] Order " + orderId + " created successfully.");
            workflowManager.markTaskCompleted(orderId, TASK_ID); // Mark task as completed
            mediator.publish(new OrderCreatedEvent(orderId)); // Publish next event
        } catch (Exception e) {
            workflowManager.markTaskFailed(orderId, TASK_ID, e.getMessage()); // Mark task as failed
        }
    }
}

/**
 * Handles the 'Email Customer (Placed)' logic, part of Step 1.
 * Simulates interaction with a Notification Component.
 */
class NotificationHandler implements Consumer<OrderCreatedEvent> {
    private final EventMediator mediator;
    private final WorkflowManager workflowManager;
    private static final String TASK_ID = "EmailCustomerPlaced";

    public NotificationHandler(EventMediator mediator, WorkflowManager workflowManager) {
        this.mediator = mediator;
        this.workflowManager = workflowManager;
    }

    @Override
    public void accept(OrderCreatedEvent event) {
        String orderId = event.getOrderId();
        if (workflowManager.getTaskStatus(orderId, TASK_ID) == TaskStatus.COMPLETED) {
            System.out.println(Thread.currentThread().getName() + ": [NotificationComponent] Task " + TASK_ID + " for workflow " + orderId + " already completed. Skipping.");
            return;
        }

        workflowManager.markTaskStarted(orderId, TASK_ID, event);
        try {
            System.out.println(Thread.currentThread().getName() + ": [NotificationComponent] Sending order confirmation email for order " + orderId);
            Thread.sleep(300); // Simulate email sending time

            if (orderId.equals("ORD-ERROR-EMAIL-PLACE")) {
                throw new RuntimeException("Simulated error: Email service unavailable for customer email.");
            }

            String customerEmail = "customer-" + orderId + "@example.com";
            String message = "Your order " + orderId + " has been placed.";
            System.out.println(Thread.currentThread().getName() + ": [NotificationComponent] Email sent to " + customerEmail + " for order " + orderId);
            workflowManager.markTaskCompleted(orderId, TASK_ID);
            mediator.publish(new CustomerEmailedEvent(orderId, customerEmail, message));
        } catch (Exception e) {
            workflowManager.markTaskFailed(orderId, TASK_ID, e.getMessage());
        }
    }
}

/**
 * Handles the 'Apply Payment' logic, part of Step 2.
 * Simulates interaction with a Payment Component.
 */
class PaymentHandler implements Consumer<OrderCreatedEvent> {
    private final EventMediator mediator;
    private final WorkflowManager workflowManager;
    private static final String TASK_ID = "ApplyPayment";

    public PaymentHandler(EventMediator mediator, WorkflowManager workflowManager) {
        this.mediator = mediator;
        this.workflowManager = workflowManager;
    }

    @Override
    public void accept(OrderCreatedEvent event) {
        String orderId = event.getOrderId();
        if (workflowManager.getTaskStatus(orderId, TASK_ID) == TaskStatus.COMPLETED) {
            System.out.println(Thread.currentThread().getName() + ": [PaymentComponent] Task " + TASK_ID + " for workflow " + orderId + " already completed. Skipping.");
            return;
        }

        workflowManager.markTaskStarted(orderId, TASK_ID, event);
        try {
            System.out.println(Thread.currentThread().getName() + ": [PaymentComponent] Applying payment for order " + orderId);
            Thread.sleep(700); // Simulate payment processing time

            if (orderId.equals("ORD-ERROR-PAY")) {
                throw new RuntimeException("Simulated error: Payment gateway declined the transaction.");
            }

            double amount = 100.00; // Placeholder amount
            System.out.println(Thread.currentThread().getName() + ": [PaymentComponent] Payment of $" + amount + " applied for order " + orderId);
            workflowManager.markTaskCompleted(orderId, TASK_ID);
            mediator.publish(new PaymentAppliedEvent(orderId, amount));
        } catch (Exception e) {
            workflowManager.markTaskFailed(orderId, TASK_ID, e.getMessage());
        }
    }
}

/**
 * Handles the 'Adjust Inventory' logic, part of Step 2.
 * Simulates interaction with an Inventory Component.
 */
class InventoryAdjustmentHandler implements Consumer<OrderCreatedEvent> {
    private final EventMediator mediator;
    private final WorkflowManager workflowManager;
    private static final String TASK_ID = "AdjustInventory";

    public InventoryAdjustmentHandler(EventMediator mediator, WorkflowManager workflowManager) {
        this.mediator = mediator;
        this.workflowManager = workflowManager;
    }

    @Override
    public void accept(OrderCreatedEvent event) {
        String orderId = event.getOrderId();
        if (workflowManager.getTaskStatus(orderId, TASK_ID) == TaskStatus.COMPLETED) {
            System.out.println(Thread.currentThread().getName() + ": [InventoryComponent] Task " + TASK_ID + " for workflow " + orderId + " already completed. Skipping.");
            return;
        }

        workflowManager.markTaskStarted(orderId, TASK_ID, event);
        try {
            System.out.println(Thread.currentThread().getName() + ": [InventoryComponent] Adjusting inventory for order " + orderId);
            Thread.sleep(600); // Simulate inventory adjustment time

            if (orderId.equals("ORD-ERROR-INV")) {
                throw new RuntimeException("Simulated error: Item out of stock during inventory adjustment.");
            }

            List<String> adjustedItems = List.of("Book A", "Book B"); // Placeholder items
            System.out.println(Thread.currentThread().getName() + ": [InventoryComponent] Inventory adjusted for items: " + adjustedItems + " for order " + orderId);
            workflowManager.markTaskCompleted(orderId, TASK_ID);
            mediator.publish(new InventoryAdjustedEvent(orderId, adjustedItems));
        } catch (Exception e) {
            workflowManager.markTaskFailed(orderId, TASK_ID, e.getMessage());
        }
    }
}

/**
 * Handles the 'Fulfill Order' logic, part of Step 3.
 * Simulates interaction with an Order Fulfillment Component.
 * This handler now explicitly depends on PaymentAppliedEvent to demonstrate sequence.
 */
class OrderFulfillmentHandler implements Consumer<PaymentAppliedEvent> {
    private final EventMediator mediator;
    private final WorkflowManager workflowManager;
    private static final String TASK_ID = "FulfillOrder";

    public OrderFulfillmentHandler(EventMediator mediator, WorkflowManager workflowManager) {
        this.mediator = mediator;
        this.workflowManager = workflowManager;
    }

    @Override
    public void accept(PaymentAppliedEvent event) {
        String orderId = event.getOrderId();
        if (workflowManager.getTaskStatus(orderId, TASK_ID) == TaskStatus.COMPLETED) {
            System.out.println(Thread.currentThread().getName() + ": [OrderFulfillmentComponent] Task " + TASK_ID + " for workflow " + orderId + " already completed. Skipping.");
            return;
        }

        // To ensure FulfillOrder truly waits for InventoryAdjustedEvent as well,
        // you would typically need a more advanced correlation mechanism or
        // a dedicated aggregator that consumes both events before publishing a new one.
        // For this example, we assume PaymentAppliedEvent is the primary trigger.
        // A robust solution might check workflowManager.getTaskStatus("AdjustInventory") == COMPLETED
        // and if not, put this task back to PENDING and wait for InventoryAdjustedEvent.
        // For simplicity, we are assuming it only triggers after Payment is applied.

        workflowManager.markTaskStarted(orderId, TASK_ID, event);
        try {
            System.out.println(Thread.currentThread().getName() + ": [OrderFulfillmentComponent] Fulfilling order " + orderId + " (triggered by Payment Applied)");
            Thread.sleep(800); // Simulate order fulfillment time

            if (orderId.equals("ORD-ERROR-FULFILL")) {
                throw new RuntimeException("Simulated error: Fulfillment system offline.");
            }

            System.out.println(Thread.currentThread().getName() + ": [OrderFulfillmentComponent] Order " + orderId + " fulfilled.");
            workflowManager.markTaskCompleted(orderId, TASK_ID);
            mediator.publish(new OrderFulfilledEvent(orderId));
        } catch (Exception e) {
            workflowManager.markTaskFailed(orderId, TASK_ID, e.getMessage());
        }
    }
}

/**
 * Handles the 'Replenish Stock' logic, part of Step 3.
 * Simulates interaction with a Warehouse Component.
 */
class StockReplenishmentHandler implements Consumer<InventoryAdjustedEvent> {
    private final EventMediator mediator;
    private final WorkflowManager workflowManager;
    private static final String TASK_ID = "ReplenishStock";

    public StockReplenishmentHandler(EventMediator mediator, WorkflowManager workflowManager) {
        this.mediator = mediator;
        this.workflowManager = workflowManager;
    }

    @Override
    public void accept(InventoryAdjustedEvent event) {
        String orderId = event.getOrderId();
        if (workflowManager.getTaskStatus(orderId, TASK_ID) == TaskStatus.COMPLETED) {
            System.out.println(Thread.currentThread().getName() + ": [WarehouseComponent] Task " + TASK_ID + " for workflow " + orderId + " already completed. Skipping.");
            return;
        }

        workflowManager.markTaskStarted(orderId, TASK_ID, event);
        try {
            System.out.println(Thread.currentThread().getName() + ": [WarehouseComponent] Checking stock for replenishment for order " + orderId);
            Thread.sleep(400); // Simulate stock check time

            if (orderId.equals("ORD-ERROR-REPLENISH")) {
                throw new RuntimeException("Simulated error: Supplier integration failed for replenishment.");
            }

            // In a real scenario, this would check if stock is low and trigger replenishment
            List<String> itemsToReplenish = List.of("Book C", "Book D");
            if (!itemsToReplenish.isEmpty()) {
                System.out.println(Thread.currentThread().getName() + ": [WarehouseComponent] Replenishing stock for items: " + itemsToReplenish + " (triggered by order " + orderId + ")");
                workflowManager.markTaskCompleted(orderId, TASK_ID);
                mediator.publish(new StockReplenishedEvent(orderId, itemsToReplenish));
            } else {
                System.out.println(Thread.currentThread().getName() + ": [WarehouseComponent] No stock replenishment needed for order " + orderId);
                workflowManager.markTaskCompleted(orderId, TASK_ID); // Even if nothing to do, the task is "completed"
            }
        } catch (Exception e) {
            workflowManager.markTaskFailed(orderId, TASK_ID, e.getMessage());
        }
    }
}

/**
 * Handles the 'Email Customer (Fulfilled)' logic, part of Step 4.
 * This handler is included for completeness and demonstrates where your implementation would go.
 */
class FulfillmentNotificationHandler implements Consumer<OrderFulfilledEvent> {
    private final EventMediator mediator;
    private final WorkflowManager workflowManager;
    private static final String TASK_ID = "EmailCustomerFulfilled";

    public FulfillmentNotificationHandler(EventMediator mediator, WorkflowManager workflowManager) {
        this.mediator = mediator;
        this.workflowManager = workflowManager;
    }

    @Override
    public void accept(OrderFulfilledEvent event) {
        String orderId = event.getOrderId();
        if (workflowManager.getTaskStatus(orderId, TASK_ID) == TaskStatus.COMPLETED) {
            System.out.println(Thread.currentThread().getName() + ": [NotificationComponent] Task " + TASK_ID + " for workflow " + orderId + " already completed. Skipping.");
            return;
        }

        workflowManager.markTaskStarted(orderId, TASK_ID, event);
        try {
            System.out.println(Thread.currentThread().getName() + ": [NotificationComponent] Sending fulfillment email for order " + orderId);
            Thread.sleep(350); // Simulate email sending time

            if (orderId.equals("ORD-ERROR-EMAIL-FULFILL")) {
                throw new RuntimeException("Simulated error: SMTP server unreachable for fulfillment email.");
            }

            String customerEmail = "customer-" + orderId + "@example.com";
            String message = "Your order " + orderId + " has been fulfilled and is ready for shipment.";
            System.out.println(Thread.currentThread().getName() + ": [NotificationComponent] Fulfillment email sent to " + customerEmail + " for order " + orderId);
            workflowManager.markTaskCompleted(orderId, TASK_ID);
            // You might publish a new event here like OrderReadyForShipmentEvent for your Step 4
            // mediator.publish(new OrderReadyForShipmentEvent(orderId));
        } catch (Exception e) {
            workflowManager.markTaskFailed(orderId, TASK_ID, e.getMessage());
        }
    }
}

// --- Main Application ---

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Initialize the Event Mediator with a pool of 5 threads
        EventMediator mediator = new EventMediator(5);
        // Initialize the Workflow Manager, passing the mediator to it
        WorkflowManager workflowManager = new WorkflowManager(mediator);

        // --- Register Handlers (Simulating component subscriptions) ---
        // Pass the WorkflowManager to each handler so they can report status
        mediator.subscribe(PlaceOrderEvent.class, new OrderPlacementHandler(mediator, workflowManager));
        mediator.subscribe(OrderCreatedEvent.class, new NotificationHandler(mediator, workflowManager));
        mediator.subscribe(OrderCreatedEvent.class, new PaymentHandler(mediator, workflowManager));
        mediator.subscribe(OrderCreatedEvent.class, new InventoryAdjustmentHandler(mediator, workflowManager));
        mediator.subscribe(PaymentAppliedEvent.class, new OrderFulfillmentHandler(mediator, workflowManager));
        mediator.subscribe(InventoryAdjustedEvent.class, new StockReplenishmentHandler(mediator, workflowManager));
        // Handler for part of STEP 4
        mediator.subscribe(OrderFulfilledEvent.class, new FulfillmentNotificationHandler(mediator, workflowManager));


        // --- Scenario 1: Successful Workflow ---
        System.out.println("\n--- Scenario 1: Successful Workflow ---");
        String orderId1 = "ORD-001";
        PlaceOrderEvent event1 = new PlaceOrderEvent(orderId1, "alice@example.com", List.of("Item X", "Item Y"));
        workflowManager.startNewOrderWorkflow(orderId1, event1);
        Thread.sleep(3000); // Give time for initial async tasks

        // --- Scenario 2: Workflow with a simulated error and resumption ---
        System.out.println("\n--- Scenario 2: Workflow with a simulated error and resumption ---");
        String orderId2 = "ORD-ERROR-PAY"; // This order will trigger a payment error
        PlaceOrderEvent event2 = new PlaceOrderEvent(orderId2, "bob@example.com", List.of("Item A", "Item B"));
        workflowManager.startNewOrderWorkflow(orderId2, event2);
        Thread.sleep(2000); // Allow time for the error to occur

        workflowManager.checkWorkflowOverallStatus(orderId2); // Check status after error

        System.out.println("\n--- Simulating user fixing the 'ORD-ERROR-PAY' issue... ---");
        Thread.sleep(1000); // Simulate time taken to fix
        workflowManager.resumeWorkflow(orderId2); // Resume the workflow
        Thread.sleep(3000); // Allow time for resumed tasks to complete

        workflowManager.checkWorkflowOverallStatus(orderId2); // Check final status after resumption

        // --- Scenario 3: Another error scenario, with Create Order failure ---
        System.out.println("\n--- Scenario 3: Workflow with Create Order error and resumption ---");
        String orderId3 = "ORD-ERROR-CREATE"; // This order will trigger a create order error
        PlaceOrderEvent event3 = new PlaceOrderEvent(orderId3, "charlie@example.com", List.of("Item C"));
        workflowManager.startNewOrderWorkflow(orderId3, event3);
        Thread.sleep(1000); // Allow time for the error to occur

        workflowManager.checkWorkflowOverallStatus(orderId3);

        System.out.println("\n--- Simulating user fixing the 'ORD-ERROR-CREATE' issue... ---");
        Thread.sleep(1000);
        workflowManager.resumeWorkflow(orderId3);
        Thread.sleep(3000);

        workflowManager.checkWorkflowOverallStatus(orderId3);


        // Final check for all workflows
        System.out.println("\n--- Final Status Check for all orders ---");
        workflowManager.checkWorkflowOverallStatus(orderId1);
        workflowManager.checkWorkflowOverallStatus(orderId2);
        workflowManager.checkWorkflowOverallStatus(orderId3);

        // Give extra time for any lingering async tasks before shutdown
        Thread.sleep(1000);
        mediator.shutdown();
        System.out.println("\nApplication Finished.");
    }
}
