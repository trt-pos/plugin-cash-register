package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import lombok.Getter;
import lombok.SneakyThrows;
import org.lebastudios.theroundtable.events.Event1;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.events.Event;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.printers.CashRegisterPrinterManager;
import org.lebastudios.theroundtable.printers.PrinterManager;

import java.io.IOException;
import java.math.BigDecimal;

@Getter
public class CashRegister
{
    /// The actual order is considered modified when, a new label is added, removed, or a new order is set to show.
    public static Event onActualOrderModified = new Event();
    /// An order item is modified when the qty or the unit price is changed.
    /// If the qty goes to 0, the item is removed and the event onActualOrderModified is called instead of this.
    public static Event1<OrderItem> onOrderItemModified = new Event1<>();

    private static CashRegister instance;
    
    private final Order cashRegisterOrder;
    
    private Order actualOrder;
    
    private CashRegister()
    {
        cashRegisterOrder = new Order();
        cashRegisterOrder.setOrderName(LangFileLoader.getTranslation("word.cashregister"));
        actualOrder = cashRegisterOrder;
    }

    public static CashRegister getInstance()
    {
        if (instance == null) instance = new CashRegister();

        return instance;
    }

    public void addProduct(Product product, BigDecimal quantity)
    {
        var orderItems = actualOrder.getOrderItems();
        
        var orderItem = orderItems.stream()
                .filter(item -> item.getBaseProduct().equals(product))
                .findFirst()
                .orElse(null);
        
        if (orderItem == null) 
        {
            orderItems.add(new OrderItem(product.clone(), quantity));
            CashRegister.onActualOrderModified.invoke();
        }
        else
        {
            orderItem.setQuantity(orderItem.getQuantity().add(quantity));
            CashRegister.onOrderItemModified.invoke(orderItem);
        }
    }
    
    public void resetActualOrder()
    {
        actualOrder.reset();

        CashRegister.onActualOrderModified.invoke();
    }

    public void printOrder()
    {
        try
        {
            EscPos escpos = new EscPos(new PrinterOutputStream(PrinterManager.getInstance().getDefaultPrintService()));
            CashRegisterPrinterManager.getInstance().getOrderPrinter().print(escpos, actualOrder);
            escpos.feed(5);
            escpos.cut(EscPos.CutMode.PART);
            escpos.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void swapOrder(Order order)
    {
        if (actualOrder == order) return;

        actualOrder = order;
        CashRegister.onActualOrderModified.invoke();
    }

    @SneakyThrows
    public void showInterface()
    {
        CashRegisterPaneController.showInterface();
    }
}
