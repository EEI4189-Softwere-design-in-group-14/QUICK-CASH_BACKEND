
-- QuickCash Module 25
-- Daily Fixed Total Repayment Schema Migration
--
-- Run ONCE on a database that has the Module 24 schema.
-- Do NOT rerun on a database where these columns exist.
-- Backup the database before applying this migration.
--
-- Existing loans remain monthly by default.
-- No existing loan balances or payments are changed.

ALTER TABLE loans
    ADD COLUMN repayment_plan_type
        VARCHAR(30) NOT NULL
        DEFAULT 'MONTHLY_PRINCIPAL_ONLY',
    ADD COLUMN total_repayment_amount
        DECIMAL(12,2) NULL,
    ADD COLUMN interest_amount
        DECIMAL(12,2) NULL,
    ADD COLUMN repayment_days
        INT NULL;

ALTER TABLE loan_installments
    ADD COLUMN interest_due
        DECIMAL(15,2) NOT NULL DEFAULT 0.00;
