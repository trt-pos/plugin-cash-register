package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.plugincashregister.cash.Order;
import org.lebastudios.theroundtable.plugincashregister.cash.OrderItem;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.entities.TaxType;
import org.lebastudios.theroundtable.printers.PrinterManager;

import java.io.IOException;
import java.math.BigDecimal;

@Deprecated
public interface IReceiptPrinter
{
    EscPos print(EscPos escPos, Receipt receipt, Order order) throws IOException;
    
    static void printTest() throws IOException
    {
        try (EscPos escPos = new EscPos(new PrinterOutputStream(PrinterManager.getInstance().getDefaultPrintService())))
        {
            Order order = new Order();

            order.setOrderName("Table 1");
            
            Product p1 = new Product();
            Product p2 = new Product();

            p1.setName("Product 1");
            p1.setPrice(new BigDecimal("2.90"));
            p1.setTaxType(new TaxType("", new BigDecimal("0.10"), ""));

            p1.setName("Product 2");
            p1.setPrice(new BigDecimal("100"));
            p1.setTaxType(new TaxType("", new BigDecimal("0.21"), ""));

            order.getOrderItems().add(new OrderItem(p1, new BigDecimal(10)));
            order.getOrderItems().add(new OrderItem(p2, new BigDecimal(1)));

            Receipt receipt = new Receipt();
            
            receipt.setClient("Client", "ABCD123");
            receipt.setPaymentAmount(new BigDecimal("1000"));
            receipt.setPaymentMethod("Cash");

            Database.getInstance().connectTransaction(session ->
            {
                receipt.setOrder(order, session);
                session.getTransaction().rollback();
            });
            
            
            var printer = new DefaultReceiptPrinter().print(escPos, receipt, order);

            printer.feed(5);
            printer.cut(EscPos.CutMode.PART);
            printer.close();
        }
        
    }
}
