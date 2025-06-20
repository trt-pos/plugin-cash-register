package org.lebastudios.theroundtableplugins.cr;

import org.lebastudios.theroundtable.camelot.CamelotEvent;
import org.lebastudios.theroundtable.events.LocalEvent;
import org.lebastudios.theroundtable.events.SingleListenerLocalEvent;
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
    
    public static final LocalEvent<Order> showOrder = new LocalEvent<>() {};
    public static final LocalEvent<Receipt> onReceiptEmitted = new LocalEvent<>() {};
    public static final LocalEvent<Transaction> onTransactionRealized = new LocalEvent<>();
    
    public static final SingleListenerLocalEvent<BillNumberRequestData> onRequestReceiptBillNumber = new SingleListenerLocalEvent<>();
    public static final SingleListenerLocalEvent<BillNumberRequestData> onRequestNewReceiptBillNumber = new SingleListenerLocalEvent<>();
    public static final SingleListenerLocalEvent<BillNumberRequestData> onRequestNewRectificationBillNumber = new SingleListenerLocalEvent<>();
    
    public record BillNumberRequestData(int receiptId, StringBuffer billNumberOutput) {}
    
    public static final SingleListenerLocalEvent<ReceiptBilledData> onReceiptBilled = new SingleListenerLocalEvent<>();
    public static final SingleListenerLocalEvent<ReceiptBilledData> onModifiedReceiptBilled = new SingleListenerLocalEvent<>();
    
    public record ReceiptBilledData(Receipt receipt, String billNumber) {}
}
