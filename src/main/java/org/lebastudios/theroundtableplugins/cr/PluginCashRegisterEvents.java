package org.lebastudios.theroundtableplugins.cr;

import org.lebastudios.theroundtable.camelot.CamelotEvent;
import org.lebastudios.theroundtable.events.Event1;
import org.lebastudios.theroundtable.events.Event2;
import org.lebastudios.theroundtable.events.SingleListenerEvent2;
import org.lebastudios.theroundtableplugins.cr.cash.Order;
import org.lebastudios.theroundtableplugins.cr.entities.Product;
import org.lebastudios.theroundtableplugins.cr.entities.Receipt;
import org.lebastudios.theroundtableplugins.cr.entities.Transaction;

public class PluginCashRegisterEvents
{
    public static final CamelotEvent<Product> onProductModify = new CamelotEvent<>(
            PluginCashRegister.class,
            "product-modify",
            new Product()
    );
    
    public static final Event1<Order> showOrder = new Event1<>() {};
    public static final Event1<Receipt> onReceiptEmitted = new Event1<>() {};
    public static final Event1<Transaction> onTransactionRealized = new Event1<>();
    
    public static final Event2<Integer, StringBuffer> onRequestReceiptBillNumber = new SingleListenerEvent2<>();
    public static final Event2<Integer, StringBuffer> onRequestNewReceiptBillNumber = new SingleListenerEvent2<>();
    public static final Event2<Integer, StringBuffer> onRequestNewRectificationBillNumber = new SingleListenerEvent2<>();
    public static final Event2<Receipt, String> onReceiptBilled = new Event2<>();
    public static final Event2<Receipt, String> onModifiedReceiptBilled = new Event2<>();
}
