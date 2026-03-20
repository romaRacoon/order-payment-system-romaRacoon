package dev.sorokin.domain.task;

import dev.sorokin.api.task.TaskStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;

    public List<TaskEntity> getAllByStatus(TaskStatus status) {
        return taskRepository.findAllByTaskStatus(status);
    }
}
