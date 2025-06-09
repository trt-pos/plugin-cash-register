package org.lebastudios.theroundtableplugins.cr.printers;

import org.lebastudios.theroundtableplugins.cr.entities.Transaction;
import org.lebastudios.theroundtable.printers.IPrinter;

public abstract class TransactionPrinter implements IPrinter
{
    protected final Transaction transaction;
    
    public TransactionPrinter(Transaction transaction)
    {
        this.transaction = transaction;
    }
}
