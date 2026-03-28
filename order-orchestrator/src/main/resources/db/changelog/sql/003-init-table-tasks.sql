--liquibase formatted sql

--changeset romaRacoon:003-01
--precondition-sql-check expectedResult:f SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'task_status')
CREATE TYPE task_status AS ENUM ('NEW', 'IN_PROGRESS', 'SUCCEEDED', 'FAILED_RETRYABLE', 'FAILED_NON_RETRYABLE');
--rollback DROP TYPE task_status

--changeset romaRacoon:003-02
--precondition-sql-check expectedResult:f SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'step_status')
CREATE TYPE step_status AS ENUM ('AUTH', 'REPRICE', 'CAPTURE');
--rollback DROP TYPE step_status

--changeset romaRacoon:003-03
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'tasks'
CREATE TABLE IF NOT EXISTS tasks
(
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),
    order_id            UUID NOT NULL,
    task_status         task_status NOT NULL DEFAULT 'NEW',
    step_status         step_status,
    attempts            INTEGER NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tasks_orders
    FOREIGN KEY (order_id)
    REFERENCES orders(id)
    ON DELETE CASCADE
    );

CREATE INDEX idx_tasks_task_status ON tasks(task_status);
--rollback DROP TABLE tasks