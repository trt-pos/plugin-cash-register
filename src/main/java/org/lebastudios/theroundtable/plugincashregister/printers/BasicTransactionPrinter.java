package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.Style;
import org.lebastudios.theroundtable.config.GlobalPreferencesConfigData;
import org.lebastudios.theroundtable.plugincashregister.entities.Transaction;
import org.lebastudios.theroundtable.printers.InLinePrinter;
import org.lebastudios.theroundtable.printers.LineFiller;
import org.lebastudios.theroundtable.printers.OpenCashDrawer;
import org.lebastudios.theroundtable.printers.Styles;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class BasicTransactionPrinter extends TransactionPrinter
{
    public BasicTransactionPrinter(Transaction transaction)
    {
        super(transaction);
    }

    @Override
    public EscPos print(EscPos escpos) throws IOException
    {
        final var preferences = new GlobalPreferencesConfigData().load();

        escpos.writeLF(Styles.TITLE, "Transaction #" + transaction.getId());
        
        escpos.feed(1);

        new LineFiller("-").print(escpos);

        final var dateTimeFormatter = DateTimeFormatter.ofPattern(preferences.dateTimeFormatter);
        escpos.writeLF(Styles.CENTERED, dateTimeFormatter.format(transaction.getDate()));

        escpos.feed(1);

        escpos.writeLF(transaction.getDescription());

        escpos.feed(1);

        new InLinePrinter(Style.FontSize._2)
                .concatLeft("TOTAL:")
                .concatRight(transaction.getAmount().toString())
                .concatRight(" ")
                .concatRight(preferences.currency.abbreviation())
                .print(escpos);

        new OpenCashDrawer().print(escpos);

        return escpos;
    }
}
