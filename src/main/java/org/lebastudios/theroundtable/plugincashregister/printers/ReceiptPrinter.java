package org.lebastudios.theroundtable.plugincashregister.printers;

import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.printers.IPrinter;

public abstract class ReceiptPrinter implements IPrinter
{
    protected final Receipt receipt;
    
    public ReceiptPrinter(Receipt receipt)
    {
        this.receipt = receipt;
    }
}
