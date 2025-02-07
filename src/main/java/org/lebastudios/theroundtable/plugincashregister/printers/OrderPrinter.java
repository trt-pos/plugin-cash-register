package org.lebastudios.theroundtable.plugincashregister.printers;

import org.lebastudios.theroundtable.plugincashregister.cash.Order;
import org.lebastudios.theroundtable.printers.IPrinter;

public abstract class OrderPrinter implements IPrinter
{
    protected final Order order;
    
    public OrderPrinter(Order order)
    {
        this.order = order;
    }
}
