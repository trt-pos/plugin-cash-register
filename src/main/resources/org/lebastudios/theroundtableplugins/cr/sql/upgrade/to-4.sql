create table cr_receipt_modification
(
    id             integer primary key autoincrement,
    new_receipt_id integer not null,
    reason         text,
    constraint fk_cr_receipt_modification_receipt foreign key (id) references cr_receipt (id),
    constraint fk_cr_receipt_modification_modified_receipt foreign key (new_receipt_id) references cr_receipt (id)
);