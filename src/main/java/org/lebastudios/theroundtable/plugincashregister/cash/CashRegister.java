package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import lombok.Getter;
import lombok.SneakyThrows;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.events.Event;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.printers.CashRegisterPrinterManager;
import org.lebastudios.theroundtable.printers.PrinterManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

@Getter
public class CashRegister
{
    public static Event onActualOrderModified = new Event();

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
        }
        else
        {
            orderItem.setQuantity(orderItem.getQuantity().add(quantity));
        }
        
        CashRegister.onActualOrderModified.invoke();
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
