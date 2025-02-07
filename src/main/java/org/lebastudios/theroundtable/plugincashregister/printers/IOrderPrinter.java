package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import org.lebastudios.theroundtable.plugincashregister.cash.Order;

import java.io.IOException;

@Deprecated
public interface IOrderPrinter
{
    EscPos print(EscPos escpos, Order order) throws IOException;
}
