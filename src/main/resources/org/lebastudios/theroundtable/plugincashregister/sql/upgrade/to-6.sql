create table cr_cash_session
(
    id               integer        not null primary key autoincrement,
    trt_uuid         varchar(45)    not null,
    account_id       integer        null,
    amount_in_drawer numeric(10, 2) not null default 0,
    opening_date     timestamp      not null,
    closing_date     timestamp      null,
    constraint FK_CR_CASH_SESSION_CORE_APP_INSTALLATION foreign key (trt_uuid) references core_app_installation (uuid),
    constraint FK_CR_CASH_SESSION_CORE_ACCOUNT foreign key (account_id) references core_account (id)
        on delete set null
);
-- DELIMITER
-- MIGRATE
create table cr_transaction
(
    id          integer primary key autoincrement,
    amount      numeric(10, 2) not null,
    date        timestamp      not null,
    description text           not null,
    receipt_id  integer        null,
    -- NEW
    total_cash  numeric(10, 2) not null default 0,
    trt_uuid    varchar(45)    null,
    account_id  integer        null,                    -- Moved from de cr_receipt
    method      varchar(10)    not null default 'CASH', -- Moved from de cr_receipt
    constraint FK_CR_CASH_SESSION_CORE_APP_INSTALLATION foreign key (trt_uuid) references core_app_installation (uuid),
    constraint FK_CR_TRANSACTION_CR_RECEIPT foreign key (receipt_id) references cr_receipt (id),
    constraint UQ_CR_TRANSACTION_CR_RECEIPT unique (receipt_id),
    constraint FK_CR_TRANSACTION_CORE_ACCOUNT foreign key (account_id) references core_account (id)
        on delete set null
);
-- DELIMITER
update cr_transaction
set account_id = (select id
                  from core_account
                  where name = (select employee_name
                                from cr_receipt
                                where cr_receipt.id = receipt_id))
where receipt_id is not null;
-- DELIMITER
update cr_transaction
set method = (select payment_method
              from cr_receipt
              where cr_receipt.id = receipt_id)
where receipt_id is not null;
-- DELIMITER
alter table cr_receipt
    drop column employee_name;
-- DELIMITER
alter table cr_receipt
    drop column payment_method;
-- DELIMITER
-- MIGRATE
create table cr_sub_category
(
    category_name varchar(255) not null,
    name          varchar(255) not null,
    -- NEW
    constraint PK_PR_SUB_CATEGORY primary key (category_name, name),
    constraint FK_PR_SUB_CATEGORY_PR_CATEGORY foreign key (category_name) references cr_category (name)
);
-- DELIMITER
-- MIGRATE
create table cr_product
(
    id                integer primary key autoincrement,
    enabled           boolean        not null,
    img_path          varchar(255)   not null,
    name              varchar(255)   not null,
    price             numeric(10, 2) not null,
    taxes_included    boolean        not null,
    category_name     varchar(255),
    sub_category_name varchar(255),
    taxes_type        integer,
    -- NEW
    constraint FK_PR_PRODUCT_PR_SUB_CATEGORY foreign key (category_name, sub_category_name) references cr_sub_category (category_name, name),
    constraint FK_PR_PRODUCT_PR_TAX_TYPE foreign key (taxes_type) references pr_tax_type (id)
);