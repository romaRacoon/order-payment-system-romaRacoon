package dev.sorokin.async;

import dev.sorokin.api.PaymentStatus;
import dev.sorokin.api.TaskStatus;
import dev.sorokin.api.payment.AuthorizePaymentRequestDto;
import dev.sorokin.api.payment.AuthorizePaymentResponseDto;
import dev.sorokin.api.warehouse.CalculatePricingRequestDto;
import dev.sorokin.domain.order.OrderEntity;
import dev.sorokin.domain.order.OrderJpaRepository;
import dev.sorokin.domain.task.TaskEntity;
import dev.sorokin.domain.task.TaskRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static dev.sorokin.api.payment.AuthorizationStatus.DECLINED;

@Component
public class TaskProcessor {
    private final ExecutorService ioExecutor;
    private final WebClient paymentStubClient;
    private final WebClient warehouseStubClient;
    private final OrderJpaRepository orderRepository;
    private final TaskRepository taskRepository;

    public TaskProcessor(ExecutorService ioExecutor,
                         @Qualifier("paymentStubClient") WebClient paymentStubClient,
                         @Qualifier("warehouseStubClient") WebClient warehouseStubClient,
                         OrderJpaRepository orderRepository,
                         TaskRepository taskRepository) {
        this.ioExecutor = ioExecutor;
        this.paymentStubClient = paymentStubClient;
        this.warehouseStubClient = warehouseStubClient;
        this.orderRepository = orderRepository;
        this.taskRepository = taskRepository;
    }

    @Value("${payment.payment.auth}")
    private String AUTH_URL;

    @Value("${payment.warehouse.calculate-price}")
    private String CALCULATE_PRICE_URL;

    @Value("${retry}")
    private Duration RETRY;

    public void processTasks(List<TaskEntity> tasks) {
        for (TaskEntity taskEntity : tasks) {
            CompletableFuture
                    .supplyAsync(() -> process(taskEntity), ioExecutor)
                    .thenAccept(result -> processExecutedTask(taskEntity))
                    .exceptionally(ex -> {
                        processFailedTask(taskEntity);

                        return null;
                    });
        }
    }

    private TaskStatus process(TaskEntity taskEntity) {
        OrderEntity order = orderRepository.findById(taskEntity.getOrderId())
                .orElse(null);
        if (order == null) {
            return TaskStatus.FAILED_NON_RETRYABLE;
        }

        AuthorizePaymentResponseDto rs = paymentStubClient.post()
                .uri(AUTH_URL)
                .bodyValue(new AuthorizePaymentRequestDto(order.getAuthorizedAmount()))
                .retrieve()
                .bodyToMono(AuthorizePaymentResponseDto.class)
                .block();

        if (DECLINED.equals(rs.status())) {
            order.setPaymentStatus(PaymentStatus.AUTHORIZATION_FAILED);
            orderRepository.save(order);

            return TaskStatus.FAILED_NON_RETRYABLE;
        }

        return compareSum(order, rs, taskEntity);
    }

    private TaskStatus compareSum(OrderEntity order, AuthorizePaymentResponseDto rs, TaskEntity task) {
        if (order.getFinalAmount().compareTo(rs.authorizedAmount()) > 0) {
            order.setPaymentStatus(PaymentStatus.PRICE_CHANGED_FAILED);
            orderRepository.save(order);

            return TaskStatus.FAILED_NON_RETRYABLE;
        }

        return warehouseStubClient.post()
                .uri(CALCULATE_PRICE_URL)
                .bodyValue(new CalculatePricingRequestDto(order.getId()))
                .retrieve()
                .toBodilessEntity()
                .map(response -> {
                    HttpStatusCode statusCode = response.getStatusCode();

                    if (statusCode.is5xxServerError()) {
                        return TaskStatus.FAILED_RETRYABLE;
                    }

                    return TaskStatus.SUCCEEDED;
                })
                .block();
    }

    private void processExecutedTask(TaskEntity taskEntity) {
        switch (taskEntity.getTaskStatus()) {
            case SUCCEEDED:
                taskEntity.setTaskStatus(TaskStatus.SUCCEEDED);
                taskEntity.setNextAttemptAt(null);
            case FAILED_NON_RETRYABLE:
                taskEntity.setTaskStatus(TaskStatus.FAILED_NON_RETRYABLE);
            case FAILED_RETRYABLE:
                taskEntity.setTaskStatus(TaskStatus.FAILED_RETRYABLE);
                taskEntity.setNextAttemptAt(OffsetDateTime.now().plus(RETRY).toLocalDateTime());
        }

        taskRepository.save(taskEntity);
    }

    private void processFailedTask(TaskEntity task) {
        task.setTaskStatus(TaskStatus.FAILED_NON_RETRYABLE);
        task.setNextAttemptAt(null);

        taskRepository.save(task);
    }
}
