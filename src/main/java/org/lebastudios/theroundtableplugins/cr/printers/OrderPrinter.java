package org.lebastudios.theroundtableplugins.cr.printers;

import org.lebastudios.theroundtableplugins.cr.cash.Order;
import org.lebastudios.theroundtable.printers.IPrinter;

public abstract class OrderPrinter implements IPrinter
{
    protected final Order order;
    
    public OrderPrinter(Order order)
    {
        this.order = order;
    }
}
