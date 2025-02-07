package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.cash.Order;
import org.lebastudios.theroundtable.plugincashregister.cash.OrderItem;
import org.lebastudios.theroundtable.printers.InLinePrinter;
import org.lebastudios.theroundtable.printers.LineFiller;

import java.io.IOException;

@Deprecated
final class DefaultOrderPrinter implements IOrderPrinter
{
    @Override
    public EscPos print(EscPos escpos, Order order) throws IOException
    {
        // Top Label
        new InLinePrinter()
                .concatLeft(LangFileLoader.getTranslation("word.qty"), 6)
                .concatLeft(" ")
                .concatLeft(LangFileLoader.getTranslation("word.product"), 22)
                .concatRight(LangFileLoader.getTranslation("word.price"), 8, EscPosConst.Justification.Left_Default)
                .concatRight(LangFileLoader.getTranslation("word.import"), 10, EscPosConst.Justification.Left_Default)
                .print(escpos);

        new LineFiller("-").print(escpos);

        for (OrderItem orderItem : order.getOrderItems())
        {
            var total = BigDecimalOperations.toString(orderItem.getTotalPrice());
            var unitPrice = BigDecimalOperations.toString(orderItem.getBaseProduct().getPrice());
            var productQty = BigDecimalOperations.toString(orderItem.getQuantity());
            var productName = orderItem.getBaseProduct().getName();

            new InLinePrinter()
                    .concatLeft(productQty, 6)
                    .concatLeft(" ")
                    .concatLeft(productName, 22)
                    .concatRight(unitPrice, 8, EscPosConst.Justification.Left_Default)
                    .concatRight(total, 10, EscPosConst.Justification.Left_Default).print(escpos);
        }

        new LineFiller("-").print(escpos);

        new InLinePrinter(Style.FontSize._2).concatLeft("TOTAL")
                .concatRight(BigDecimalOperations.toString(order.getTotal()))
                .concatRight("EUR").print(escpos);

        return escpos;
    }
}
