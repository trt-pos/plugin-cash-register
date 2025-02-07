create table cr_receipt
(
    id                     integer,
    client_identifier      varchar(255),
    client_name            varchar(255),
    employee_name          varchar(255),
    payment_amount         numeric(38, 2) not null,
    payment_method         varchar(255)   not null,
    table_name             varchar(255)   not null,
    taxes_amount           numeric(38, 2) not null,
    constraint pk_receipt primary key (id)
);

create table cr_product_receipt
(
    id             integer,
    product_name   varchar(255)   not null,
    product_value  numeric(38, 2) not null,
    quantity       numeric(38, 2) not null,
    taxes          numeric(38, 2),
    taxes_included boolean        not null,
    total_value    numeric(38, 2) not null,
    receipt_id     integer,
    constraint receipt_line foreign key (receipt_id) references cr_receipt (id),
    constraint pk_product_receipt primary key (id)

);

create table cr_transaction
(
    id          integer,
    amount      numeric(38, 2) not null,
    date        timestamp      not null,
    description varchar(99999) not null,
    receipt_id  integer,
    constraint transaction_receipt foreign key (receipt_id) references cr_receipt (id),
    constraint u_transactrion_receipt_id unique (receipt_id),
    constraint pk_transaction primary key (id)
);

create table pr_category
(
    name varchar(255) not null,
    constraint pk_category primary key (name)
);

create table pr_sub_category
(
    category_name varchar(255) not null,
    name          varchar(255) not null,
    constraint pk_sub_category primary key (category_name, name)
);

create table pr_product
(
    id                integer,
    enabled           boolean         not null,
    img_path          varchar(999999) not null,
    name              varchar(255)    not null,
    price             numeric(38, 2)  not null,
    taxes             numeric(38, 2),
    taxes_included    boolean         not null,
    category_name     varchar(255),
    sub_category_name varchar(255),
    taxes_type        integer,
    constraint fk_prduct_subcategory foreign key (category_name, sub_category_name) references pr_sub_category (category_name, name),
    constraint pk_product primary key (id)
);

create table pr_tax_type
(
    id          integer,
    description varchar(255),
    name        varchar(255) not null,
    value       numeric(38, 2),
    constraint u_tax_type_name unique (name),
    constraint pk_tax_type primary key (id)
);

