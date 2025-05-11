package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.Style;
import org.lebastudios.theroundtable.config.GlobalPreferencesConfigData;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.locale.Currency;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.entities.Transaction;
import org.lebastudios.theroundtable.printers.InLinePrinter;
import org.lebastudios.theroundtable.printers.LineFiller;
import org.lebastudios.theroundtable.printers.Styles;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

public class BasicSessionOverviewPrinter extends SessionPrinter
{
    private final Settings settings;
    
    public BasicSessionOverviewPrinter(CashSession session, Settings settings)
    {
        super(session);
        this.settings = settings;
    }

    @Override
    public EscPos print(EscPos escpos) throws IOException
    {
        IOException ex = Database.getInstance().connectQuery(session ->
        {
            try
            {
                CashSession loadedCashSession = session.find(CashSession.class, this.session.getId());

                var transactions = session.createQuery(
                                "from Transaction t " +
                                        "where t.date >= :startDate " +
                                        "and t.date <= :endDate " +
                                        "and t.appInstallation.trtUuid = :trtUuid " +
                                        "and t.receipt not in (select rm.superReceipt from ReceiptModification rm) " +
                                        "order by t.date",
                                Transaction.class)
                        .setParameter("startDate", loadedCashSession.getOpeningDate())
                        .setParameter("endDate", loadedCashSession.getClosingDate())
                        .setParameter("trtUuid", loadedCashSession.getAppInstallation().getTrtUuid())
                        .getResultList();

                printHeader(escpos, loadedCashSession);

                if (settings.includeProducts)
                {
                    var receipts = session.createQuery("from Receipt r " +
                                            "where r.transaction in :transactions " +
                                            "order by r.transaction.date",
                                    Receipt.class)
                            .setParameter("transactions", transactions)
                            .getResultList();

                    printProducts(escpos, receipts);
                }

                if (settings.includeTransaction) printTransactions(escpos, transactions);

                var totalBruto = BigDecimal.ZERO;
                var totalNet = BigDecimal.ZERO;

                for (var transaction : transactions)
                {
                    totalBruto = totalBruto.add(transaction.getAmount());
                    totalNet = totalNet.add(transaction.getReceipt() == null
                            ? transaction.getAmount()
                            : BigDecimalOperations.round(transaction.getReceipt().getNotTaxedTotal()));
                }

                Currency currency = new GlobalPreferencesConfigData().load().currency;
                
                new InLinePrinter(Style.FontSize._2).concatLeft("TOTAL BRUTO")
                        .concatRight(BigDecimalOperations.toString(totalBruto))
                        .concatRight(" ")
                        .concatRight(currency.abbreviation()).print(escpos);

                new InLinePrinter(Style.FontSize._2).concatLeft("TOTAL NETO")
                        .concatRight(BigDecimalOperations.toString(totalNet))
                        .concatRight(" ")
                        .concatRight(currency.abbreviation()).print(escpos);
            } 
            catch (IOException e)
            {
                return e;
            }
            
            return null;
        });
        
        if (ex != null)
        {
            throw ex;
        }
        
        return escpos;
    }

    private void printHeader(EscPos escPos, CashSession cashSession) throws IOException
    {
        escPos.feed(1);

        escPos.writeLF(Styles.TITLE, LangFileLoader.getTranslation("plugincashregister.printer.cashsession.overviewheader"));
        
        escPos.feed(2);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(new GlobalPreferencesConfigData().load().dateTimeFormatter);
        
        escPos.writeLF(Styles.CENTERED, 
                "    " + LangFileLoader.getTranslation("plugincashregister.word.from")
                + " " + formatter.format(cashSession.getOpeningDate()));
        escPos.writeLF(Styles.CENTERED, 
                "    " + LangFileLoader.getTranslation("plugincashregister.word.to")
                + " " + formatter.format(cashSession.getClosingDate()));

        new InLinePrinter()
                .concatLeft(LangFileLoader.getTranslation("plugincashregister.printer.cashsession.installationname"))
                .concatLeft(":")
                .concatRight(cashSession.getAppInstallation().getName()).print(escPos);
        new InLinePrinter()
                .concatLeft(LangFileLoader.getTranslation("plugincashregister.printer.cashsession.accountname"))
                .concatLeft(":")
                .concatRight(cashSession.getAccount().getName())
                .print(escPos);
        escPos.feed(1);
    }

    private void printTransactions(EscPos escPos, List<Transaction> transactions) throws IOException
    {
        escPos.feed(1);
        new LineFiller("-").print(escPos);

        new InLinePrinter().concatLeft("    " + LangFileLoader.getTranslation("plugincashregister.word.transactions"))
                .concatRight("Total: " + transactions.size()).print(escPos);
        new LineFiller("-").print(escPos);

        Currency currency = new GlobalPreferencesConfigData().load().currency;
        
        for (var transaction : transactions)
        {
            new InLinePrinter().concatLeft(transaction.getDescription(), 30)
                    .concatRight(BigDecimalOperations.toString(transaction.getAmount()))
                    .concatRight(" ")
                    .concatRight(currency.abbreviation()).print(escPos);
        }

        new LineFiller("-").print(escPos);
        escPos.feed(1);
    }

    private void printProducts(EscPos escPos, List<Receipt> receipts) throws IOException
    {
        new LineFiller("-").print(escPos);

        var productsQty = new HashMap<Product, Integer>();
        int count = 0;
        for (var receipt : receipts)
        {
            for (var product : receipt.getProducts())
            {
                int productQty = product.getQuantity().intValue();
                if (productsQty.containsKey(product.getProduct()))
                {
                    productsQty.put(product.getProduct(), productsQty.get(product.getProduct()) + productQty);
                }
                else
                {
                    productsQty.put(product.getProduct(), productQty);
                }

                count += productQty;
            }
        }

        new InLinePrinter().concatLeft("    " + LangFileLoader.getTranslation("plugincashregister.word.products"))
                .concatRight(" Total: " + count).print(escPos);
        new LineFiller("-").print(escPos);

        for (var entry : productsQty.entrySet())
        {
            new InLinePrinter().concatLeft(entry.getKey().getName())
                    .concatRight(entry.getValue().toString()).print(escPos);
        }

        new LineFiller("-").print(escPos);
        escPos.feed(1);
    }
    
    public record Settings(
            boolean includeTransaction,
            boolean includeProducts
    ) {}
}
