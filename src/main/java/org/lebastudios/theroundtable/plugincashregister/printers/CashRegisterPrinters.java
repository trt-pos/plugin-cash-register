package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import lombok.Getter;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegisterEvents;
import org.lebastudios.theroundtable.plugincashregister.cash.Order;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.entities.Transaction;
import org.lebastudios.theroundtable.printers.IPrinter;
import org.lebastudios.theroundtable.printers.Styles;

import javax.print.PrintService;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Getter
public class CashRegisterPrinters
{
    private static CashRegisterPrinters instance;

    public static CashRegisterPrinters getInstance()
    {
        if (instance == null) instance = new CashRegisterPrinters();

        return instance;
    }

    private CashRegisterPrinters() {}

    public EscPos printReceipt(Receipt receipt, PrintService printService) throws IOException
    {
        EscPos escPos = new EscPos(new PrinterOutputStream(printService));
        final var basicReceiptPrinter = new BasicReceiptPrinter(receipt);

        basicReceiptPrinter.setAfterHeader(escpos ->
        {
            StringBuffer billNumber = new StringBuffer();
            PluginCashRegisterEvents.onRequestReceiptBillNumber.invoke(receipt.getId(), billNumber);
            String receiptId = billNumber.isEmpty() ? receipt.getId() + "" : billNumber.toString();
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
 
            escpos.writeLF(Styles.CENTERED,
                    LangFileLoader.getTranslation("word.date") + ": "
                            + receipt.getTransaction().getDate().toLocalDate().format(formatter) + "  " +
                            LangFileLoader.getTranslation("word.time") + ": " +
                            receipt.getTransaction().getDate().toLocalTime().truncatedTo(ChronoUnit.SECONDS)
                                    .toString()
            );
            escpos.writeLF("");

            if (receipt.getModifies() != null)
            {
                final var oldReceipt = receipt.getModifies().getSuperReceipt();

                billNumber = new StringBuffer();
                PluginCashRegisterEvents.onRequestReceiptBillNumber.invoke(oldReceipt.getId(), billNumber);
                String oldReceiptId = billNumber.isEmpty() ? oldReceipt.getId() + "" : billNumber.toString();

                escpos.writeLF(LangFileLoader.getTranslation("phrase.rectificationreceipt") +
                        ": " + receiptId
                );
                escpos.writeLF(LangFileLoader.getTranslation("phrase.modifiesreceipt")
                        + ": " + oldReceiptId
                        + " " + LangFileLoader.getTranslation("phrase.withdate") + " "
                        + oldReceipt.getTransaction().getDate().toLocalDate().format(formatter)
                );
                // TODO: Make a IPrinter class that prints long texts wrapping them
                escpos.writeLF(
                        LangFileLoader.getTranslation("phrase.reason") + ": " + receipt.getModifies().getReason()
                );
            }
            else
            {
                escpos.writeLF(LangFileLoader.getTranslation("phrase.simplifiedreceipt") +
                        ": " + receiptId
                );
            }

            if (receipt.getModifiedBy() != null) 
            {
                final var newReceipt = receipt.getModifiedBy().getNewReceipt();

                billNumber = new StringBuffer();
                PluginCashRegisterEvents.onRequestReceiptBillNumber.invoke(newReceipt.getId(), billNumber);
                String newReceiptId =
                        billNumber.isEmpty() ? newReceipt.getId() + "" : billNumber.toString();

                escpos.writeLF(LangFileLoader.getTranslation("phrase.modifiedbyreceipt")
                        + ": " + newReceiptId 
                        + " " + LangFileLoader.getTranslation("phrase.withdate") + " "
                        + newReceipt.getTransaction().getDate().toLocalDate().format(formatter)
                );
                
                escpos.writeLF(
                        LangFileLoader.getTranslation("phrase.reason") + ": " + receipt.getModifiedBy().getReason()
                );
            }
            
            return escpos;
        });

        return basicReceiptPrinter.print(escPos);
    }

    public EscPos printReceipt(Receipt receipt, PrintService printService, IPrinter afterHeader) throws IOException
    {
        EscPos escPos = new EscPos(new PrinterOutputStream(printService));
        final var basicReceiptPrinter = new BasicReceiptPrinter(receipt);

        basicReceiptPrinter.setAfterHeader(afterHeader);

        return basicReceiptPrinter.print(escPos);
    }

    public EscPos printOrder(Order order, PrintService printService) throws IOException
    {
        EscPos escPos = new EscPos(new PrinterOutputStream(printService));
        return new BasicOrderPrinter(order).print(escPos);
    }

    public EscPos printTransaction(Transaction transaction, PrintService printService) throws IOException
    {
        EscPos escPos = new EscPos(new PrinterOutputStream(printService));
        return new BasicTransactionPrinter(transaction).print(escPos);
    }
}
