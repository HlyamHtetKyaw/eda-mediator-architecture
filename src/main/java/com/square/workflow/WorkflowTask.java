package com.square.workflow;

import com.square.events.Event;

public class WorkflowTask {
    private final String taskId;
    private TaskStatus status;
    private String errorMessage;
    private Event triggerEvent;
    public WorkflowTask(String taskId,TaskStatus status,Event triggerEvent){
        this.taskId = taskId;
        this.status = status;
        this.triggerEvent = triggerEvent;
    }
    public String getTaskId(){
        return taskId;
    }
    public TaskStatus getStatus(){
        return status;
    }
    public String getErrorMessage(){
        return errorMessage;
    }
    public Event getTriggerEvent(){
        return triggerEvent;
    }

    public void setStatus(TaskStatus status){
        this.status = status;
    }
    public void setErrorMessage(String errorMessage){
        this.errorMessage = errorMessage;
    }
    public void setTriggerEvent(Event triggerEvent){
        this.triggerEvent = triggerEvent;
    }
    @Override
    public String toString(){
        return String.format("WorkflowTask: taskId: %s, status: %s, errorMessage: %s, triggerEvent: %s/n",
                taskId,status,(errorMessage==null)?"N/A":errorMessage,triggerEvent);
    }
}
