package com.mehdi.taskflow.task;

/**
 * Enumeration of possible task priority levels.
 *
 * <p>Stored as a string in the database via {@code @Enumerated(EnumType.STRING)}
 * for readability and resilience to enum reordering.</p>
 *
 * @see com.mehdi.taskflow.task.Task
 * @see com.mehdi.taskflow.task.dto.TaskRequest
 */
public enum TaskPriority {

    /** Low priority — can be addressed when time permits. */
    LOW,

    /** Medium priority — should be addressed in the normal course of work. Default priority on creation. */
    MEDIUM,

    /** High priority — requires immediate attention. */
    HIGH
}