package com.mehdi.taskflow.task;

/**
 * Enumeration of possible task statuses.
 *
 * <p>Stored as a string in the database via {@code @Enumerated(EnumType.STRING)}
 * for readability and resilience to enum reordering.</p>
 *
 * @see com.mehdi.taskflow.task.Task
 * @see com.mehdi.taskflow.task.dto.TaskRequest
 */
public enum TaskStatus {

    /** Task has not been started yet. Default status on creation. */
    TODO,

    /** Task is currently being worked on. */
    IN_PROGRESS,

    /** Task has been completed. */
    DONE
}