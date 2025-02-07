package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import org.lebastudios.theroundtable.plugincashregister.cash.Order;

import java.io.IOException;

public class BasicOrderPrinter extends OrderPrinter
{
    public BasicOrderPrinter(Order order)
    {
        super(order);
    }

    @Override
    public EscPos print(EscPos escpos) throws IOException
    {
        return new OrderItemsTablePrinter(order.getOrderItems()).print(escpos);
    }
}
