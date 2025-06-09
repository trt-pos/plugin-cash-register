package org.lebastudios.theroundtableplugins.cr.printers;

import org.lebastudios.theroundtableplugins.cr.entities.Receipt;
import org.lebastudios.theroundtable.printers.IPrinter;

public abstract class ReceiptPrinter implements IPrinter
{
    protected final Receipt receipt;
    
    public ReceiptPrinter(Receipt receipt)
    {
        this.receipt = receipt;
    }
}
