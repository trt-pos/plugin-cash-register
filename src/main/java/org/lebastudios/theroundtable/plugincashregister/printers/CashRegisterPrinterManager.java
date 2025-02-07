package org.lebastudios.theroundtable.plugincashregister.printers;

import lombok.Getter;
import lombok.Setter;

@Deprecated
@Getter
@Setter
public class CashRegisterPrinterManager
{
    private static CashRegisterPrinterManager instance;
    
    private IReceiptPrinter receiptPrinter = new DefaultReceiptPrinter();
    private IOrderPrinter orderPrinter = new DefaultOrderPrinter();
    private ITransactionPrinter transactionPrinter = new DefaultTransactionPrinter();
    
    
    public static CashRegisterPrinterManager getInstance()
    {
        if (instance == null) instance = new CashRegisterPrinterManager();
        
        return instance;
    }
    
    private CashRegisterPrinterManager() {}
}
