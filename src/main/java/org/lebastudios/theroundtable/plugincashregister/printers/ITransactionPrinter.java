package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import org.lebastudios.theroundtable.plugincashregister.entities.Transaction;

import java.io.IOException;

@Deprecated
public interface ITransactionPrinter
{
    EscPos print(EscPos escpos, Transaction transaction) throws IOException;
}
