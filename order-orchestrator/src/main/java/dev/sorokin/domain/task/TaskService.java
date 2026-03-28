package dev.sorokin.domain.task;

import dev.sorokin.api.TaskStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;

    public List<TaskEntity> getAllByStatus(List<TaskStatus> statuses) {
        return taskRepository.findAllByTaskStatusIn(statuses);
    }

    public void saveAll(List<TaskEntity> tasks) {
        taskRepository.saveAll(tasks);
    }
}
