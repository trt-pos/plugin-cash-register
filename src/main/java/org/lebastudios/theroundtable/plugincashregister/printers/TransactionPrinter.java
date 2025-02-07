package org.lebastudios.theroundtable.plugincashregister.printers;

import org.lebastudios.theroundtable.plugincashregister.entities.Transaction;
import org.lebastudios.theroundtable.printers.IPrinter;

public abstract class TransactionPrinter implements IPrinter
{
    protected final Transaction transaction;
    
    public TransactionPrinter(Transaction transaction)
    {
        this.transaction = transaction;
    }
}
