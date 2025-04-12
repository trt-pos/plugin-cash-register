update cr_receipt
set payment_method = 'CASH'
where payment_method = 'Contado'
   or payment_method = 'Cash';

-- DELIMITER

update cr_receipt
set payment_method = 'CARD'
where payment_method = 'Tarjeta'
   or payment_method = 'Card';