alter table cr_receipt
    add column status varchar(255) not null default 'DEFAULT';

-- DELIMITER

create trigger cr_receipt_modification_insert
    after insert
    on cr_receipt_modification
    for each row
begin
    update cr_receipt
    set status = 'MODIFIED'
    where id = new.id;
end