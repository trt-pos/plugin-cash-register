package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import org.lebastudios.theroundtable.config.data.JSONFile;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.cash.OrderItem;
import org.lebastudios.theroundtable.plugincashregister.cash.PaymentMethod;
import org.lebastudios.theroundtable.plugincashregister.config.data.ReceiptPrintingConfigData;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.printers.InLinePrinter;
import org.lebastudios.theroundtable.printers.LineFiller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.TreeMap;

public class BasicReceiptBodyPrinter extends ReceiptPrinter
{
    public BasicReceiptBodyPrinter(Receipt receipt)
    {
        super(receipt);
    }

    @Override
    public EscPos print(EscPos escpos) throws IOException
    {
        var printerConfig = new JSONFile<>(ReceiptPrintingConfigData.class).get();
        
        if (!printerConfig.hideReceiptData)
        {
            escpos.writeLF(LangFileLoader.getTranslation("phrase.tablename")
                    + ": " + receipt.getTableName());

            escpos.writeLF(LangFileLoader.getTranslation("word.client")
                    + ": " + receipt.getClientString());

            escpos.writeLF(LangFileLoader.getTranslation("phrase.attendedby")
                    + receipt.getAttendantName());
        }

        escpos.feed(1);

        new OrderItemsTablePrinter(receipt.getProducts().stream()
                .map(pr -> new OrderItem(pr.getProduct(), pr.getQuantity()))
                .toList()
        ).print(escpos);

        escpos.feed(1);

        // Taxes

        if (!printerConfig.hideTaxesDesglose)
        {
            TreeMap<BigDecimal, BigDecimal> taxes = new TreeMap<>();

            receipt.getProducts().forEach((pr) ->
            {
                var product = pr.getProduct();
                var qty = pr.getQuantity();

                taxes.put(product.getTaxes(),
                        taxes.getOrDefault(product.getTaxes(), BigDecimal.ZERO)
                                .add(qty.multiply(product.getPrice()))
                );
            });

            new LineFiller("-").print(escpos);
            taxes.forEach((percen, total) ->
            {
                try
                {
                    printTaxesGroup(escpos, percen, total);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            });
            new LineFiller("-").print(escpos);
            escpos.feed(1);
        }

        // Payment Info
        if (!printerConfig.hidePaymentInfo)
        {
            escpos.writeLF(LangFileLoader.getTranslation("word.method") + ": "
                    + PaymentMethod.valueOf(receipt.getPaymentMethod()).translate()
            );

            escpos.writeLF(LangFileLoader.getTranslation("word.amount") + ": "
                    + BigDecimalOperations.toString(receipt.getPaymentAmount()) + " "
                    + LangFileLoader.getTranslation("word.change") + ": " +
                    BigDecimalOperations.toString(receipt.getPaymentAmount().subtract(receipt.getTransaction().getAmount()))
            );

            escpos.feed(1);
        }
        
        return escpos;
    }

    /**
     * @param percentage beetween 0 and 1
     * @param total with taxes
     */
    private void printTaxesGroup(EscPos escpos, BigDecimal percentage, BigDecimal total) throws IOException
    {
        var percentageOver100 = percentage.multiply(new BigDecimal(100));
        var base = BigDecimalOperations.divide(total, percentage.add(BigDecimal.ONE));
        var taxes = total.subtract(base);

        new InLinePrinter().concatLeft(BigDecimalOperations.toString(percentageOver100), 6)
                .concatLeft(" % " + LangFileLoader.getTranslation("word.iva") + " ")
                .concatLeft(LangFileLoader.getTranslation("word.over"))
                .concatRight(BigDecimalOperations.toString(base), 8, EscPosConst.Justification.Right)
                .concatRight(BigDecimalOperations.toString(taxes), 8, EscPosConst.Justification.Right).print(escpos);
    }
}
