package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import lombok.Getter;
import lombok.SneakyThrows;
import org.lebastudios.theroundtable.events.Event;
import org.lebastudios.theroundtable.events.Event1;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.plugincashregister.printers.CashRegisterPrinterManager;
import org.lebastudios.theroundtable.printers.PrinterManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class CashRegister
{
    public static Event onActualOrderSwapped = new Event();
    /// An order item is modified when the qty or the unit price is changed. If the qty goes to 0 or less, this event is
    /// not triggered.
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
                .filter(item ->
                        item.getBaseProduct().getName().equals(product.getName())
                                && item.getBaseProduct().getSubCategoryName().equals(product.getSubCategoryName())
                                && item.getBaseProduct().getCategoryName().equals(product.getCategoryName())
                                && item.getBaseProduct().getImgPath().equals(product.getImgPath())
                                && item.getBaseProduct().getPrice().setScale(2, RoundingMode.CEILING)
                                .equals(product.getPrice().setScale(2, RoundingMode.CEILING)))
                .findFirst()
                .orElse(null);

        if (orderItem == null)
        {
            orderItems.add(new OrderItem(product.clone(), quantity));
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
        CashRegister.onActualOrderSwapped.invoke();
    }

    @SneakyThrows
    public void showInterface()
    {
        CashRegisterPaneController.showInterface();
    }
}
