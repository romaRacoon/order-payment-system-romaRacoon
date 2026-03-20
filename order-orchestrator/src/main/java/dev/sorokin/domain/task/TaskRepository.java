package dev.sorokin.domain.task;

import dev.sorokin.api.task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findAllByTaskStatus(TaskStatus status);
}
