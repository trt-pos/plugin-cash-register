drop trigger if exists cr_receipt_modification_insert;

-- DELIMITER

CREATE TABLE cr_receipt_temp AS
SELECT id, client_identifier, client_name, employee_name, payment_amount, payment_method, table_name, taxes_amount
FROM cr_receipt;

-- DELIMITER

drop table cr_receipt;

-- DELIMITER

alter table cr_receipt_temp rename to cr_receipt;

