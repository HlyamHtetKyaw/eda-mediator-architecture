package com.square.workflow;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WorkflowInstance {
    private final String workflowId;
    private final ConcurrentHashMap<String,WorkflowTask> tasks;
    private final List<String> taskExecutionOrder;

    public WorkflowInstance(String workflowId,List<String> predefinedTaskOrder){
        this.workflowId = workflowId;
        this.tasks = new ConcurrentHashMap<>();
        this.taskExecutionOrder = predefinedTaskOrder;
        predefinedTaskOrder.forEach(t->tasks.put(t,new WorkflowTask(t,TaskStatus.PENDING,null)));
    }

    public String getWorkflowId(){
        return workflowId;
    }
    public WorkflowTask getTask(String taskId){
        return tasks.get(taskId);
    }
    public List<String> getTaskExecutionOrder(){
        return taskExecutionOrder;
    }
    public boolean isCompleted(){
        return tasks.values().stream().anyMatch(e->e.getStatus()==TaskStatus.COMPLETED);
    }
    public boolean isFailed(){
        return tasks.values().stream().anyMatch(e->e.getStatus()==TaskStatus.FAILED);
    }
    public String getOverallStatus(){
        if(isCompleted()){
            return "COMPLETED";
        }
        if(isFailed()){
            return "FAILED";
        }
        if(tasks.values().stream().anyMatch(e->e.getStatus()==TaskStatus.IN_PROGRESS)){
            return "IN_PROGRESS";
        }
        return "PENDING";
    }

    @Override
    public String toString(){
        return String.format("WorkflowInstance: workflowId: %s, overallStatus: %s, tasks: %s/n",
                workflowId,getOverallStatus(),tasks.values().stream().map(WorkflowTask::toString).collect(Collectors.joining("/")));
    }
}
