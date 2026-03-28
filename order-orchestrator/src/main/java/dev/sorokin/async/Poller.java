package dev.sorokin.async;

import dev.sorokin.api.TaskStatus;
import dev.sorokin.domain.task.TaskEntity;
import dev.sorokin.domain.task.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class Poller {
    private final TaskService taskService;
    private final TransactionTemplate transactionTemplate;
    private final TaskProcessor taskProcessor;

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        taskProcessor.processTasks(getTasks());
    }

    private List<TaskEntity> getTasks() {
        return transactionTemplate.execute(transaction -> {
            List<TaskEntity> tasks = taskService.getAllByStatus(List.of(TaskStatus.NEW, TaskStatus.FAILED_RETRYABLE));

            log.info("Polled tasks: {}", tasks);
            tasks.forEach(task -> {
                task.setTaskStatus(TaskStatus.IN_PROGRESS);
            });

            taskService.saveAll(tasks);

            return tasks;
        });
    }
}
