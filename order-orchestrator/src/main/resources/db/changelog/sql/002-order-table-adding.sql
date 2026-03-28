--liquibase formatted sql

--changeset romaRacoon:002-01
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'client_estimate'
ALTER TABLE orders ADD COLUMN client_estimate NUMERIC(19,2);
--rollback ALTER TABLE orders DROP COLUMN client_estimate

--changeset romaRacoon:002-02
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'final_amount'
ALTER TABLE orders ADD COLUMN final_amount NUMERIC(19,2);
--rollback ALTER TABLE orders DROP COLUMN final_amount

--changeset romaRacoon:002-03
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'authorized_amount'
ALTER TABLE orders ADD COLUMN authorized_amount NUMERIC(19,2);
--rollback ALTER TABLE orders DROP COLUMN authorized_amount

--changeset romaRacoon:002-04
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'captured_amount'
ALTER TABLE orders ADD COLUMN captured_amount NUMERIC(19,2);
--rollback ALTER TABLE orders DROP COLUMN captured_amount

--changeset romaRacoon:002-05
--precondition-sql-check expectedResult:false SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status')
CREATE TYPE payment_status AS ENUM ('NEW', 'AUTHORIZATION_FAILED', 'PRICE_CHANGED_FAILED', 'SUCCEED_PAID');
--rollback DROP TYPE payment_status

--changeset romaRacoon:002-06
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'payment_status'
ALTER TABLE orders ADD COLUMN payment_status payment_status NOT NULL;
--rollback ALTER TABLE orders DROP COLUMN payment_status