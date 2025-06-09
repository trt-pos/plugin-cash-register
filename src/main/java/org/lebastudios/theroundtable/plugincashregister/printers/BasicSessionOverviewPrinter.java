package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.Style;
import lombok.NoArgsConstructor;
import org.lebastudios.theroundtable.config.GlobalPreferencesConfigData;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.locale.Currency;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.entities.Transaction;
import org.lebastudios.theroundtable.printers.IPrinter;
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
                                        "and t.appInstallation.uuid = :trtUuid " +
                                        "and t.receipt not in (select rm.superReceipt from ReceiptModification rm) " +
                                        "order by t.date",
                                Transaction.class)
                        .setParameter("startDate", loadedCashSession.getOpeningDate())
                        .setParameter("endDate", loadedCashSession.getClosingDate())
                        .setParameter("trtUuid", loadedCashSession.getAppInstallation().getUuid())
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

                if (settings.includeTransaction)
                {
                    printTransactions(escpos, transactions);
                }

                new OtherTransactionsOverviewPrinter(transactions).print(escpos);
                new ReceiptsOverviewPrinter(transactions).print(escpos);
                
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

        escPos.writeLF(Styles.TITLE,
                Translator.getInstance().t("cr:printer.cashsession.overviewheader"));

        escPos.feed(2);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(new GlobalPreferencesConfigData().load().dateTimeFormatter);

        escPos.writeLF(Styles.CENTERED,
                "    " + Translator.getInstance().t("cr:word.from")
                        + " " + formatter.format(cashSession.getOpeningDate()));
        escPos.writeLF(Styles.CENTERED,
                "    " + Translator.getInstance().t("cr:word.to")
                        + " " + formatter.format(cashSession.getClosingDate()));

        escPos.feed(1);

        new InLinePrinter()
                .concatLeft(
                        Translator.getInstance().t("cr:printer.cashsession.expectedamountindrawer"))
                .concatRight(BigDecimalOperations.toString(cashSession.getAmountInDrawer()))
                .concatRight(" ")
                .concatRight(new GlobalPreferencesConfigData().load().currency.abbreviation()).print(escPos);

        escPos.feed(1);

        new InLinePrinter()
                .concatLeft(Translator.getInstance().t("cr:printer.cashsession.installationname"))
                .concatLeft(":")
                .concatRight(cashSession.getAppInstallation().getName()).print(escPos);
        new InLinePrinter()
                .concatLeft(Translator.getInstance().t("cr:printer.cashsession.accountname"))
                .concatLeft(":")
                .concatRight(cashSession.getAccount().getName())
                .print(escPos);
        escPos.feed(1);
    }

    private void printTransactions(EscPos escPos, List<Transaction> transactions) throws IOException
    {
        escPos.feed(1);
        new LineFiller("-").print(escPos);

        new InLinePrinter().concatLeft("    " + Translator.getInstance().t("cr:word.transactions"))
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

        new InLinePrinter().concatLeft("    " + Translator.getInstance().t("cr:word.products"))
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
    
    @NoArgsConstructor
    private static class ReceiptsOverviewPrinter implements IPrinter
    {
        private int numberOfPaymentsWithCash;
        private int numberOfPaymentsWithCard;
        private BigDecimal totalWithCash;
        private BigDecimal totalWithCard;
        private BigDecimal totalBruto;
        private BigDecimal totalNet;

        public ReceiptsOverviewPrinter(List<Transaction> transactions)
        {
            numberOfPaymentsWithCash = 0;
            numberOfPaymentsWithCard = 0;
            totalWithCash = BigDecimal.ZERO;
            totalWithCard = BigDecimal.ZERO;
            totalBruto = BigDecimal.ZERO;
            totalNet = BigDecimal.ZERO;
            
            for (Transaction transaction : transactions)
            {
                if (transaction.getReceipt() == null) continue;

                totalBruto = totalBruto.add(transaction.getAmount());
                totalNet = totalNet.add(BigDecimalOperations.round(transaction.getReceipt().getNotTaxedTotal()));

                if (transaction.getMethod() == Transaction.PaymentMethod.CASH)
                {
                    totalWithCash = totalWithCash.add(transaction.getAmount());
                    numberOfPaymentsWithCash++;
                }
                else
                {
                    if (transaction.getMethod() == Transaction.PaymentMethod.CARD)
                    {
                        totalWithCard = totalWithCard.add(transaction.getAmount());
                        numberOfPaymentsWithCard++;
                    }
                }
            }
        }

        @Override
        public EscPos print(EscPos escpos) throws IOException
        {
            Currency currency = new GlobalPreferencesConfigData().load().currency;

            escpos.writeLF(Styles.CENTERED, Translator.getInstance().t("cr:word.receipts"));
            new LineFiller("-").print(escpos);

            new InLinePrinter()
                    .concatLeft(String.valueOf(numberOfPaymentsWithCash))
                    .concatLeft(" ")
                    .concatLeft(Translator.getInstance().t("cr:printer.cashsession.cashpayments"))
                    .concatRight(BigDecimalOperations.toString(totalWithCash))
                    .concatRight(" ")
                    .concatRight(currency.abbreviation()).print(escpos);

            new InLinePrinter()
                    .concatLeft(String.valueOf(numberOfPaymentsWithCard))
                    .concatLeft(" ")
                    .concatLeft(Translator.getInstance().t("cr:printer.cashsession.cardpayments"))
                    .concatRight(BigDecimalOperations.toString(totalWithCard))
                    .concatRight(" ")
                    .concatRight(currency.abbreviation()).print(escpos);

            escpos.feed(1);

            new InLinePrinter(Style.FontSize._2).concatLeft("TOTAL BRUTO")
                    .concatRight(BigDecimalOperations.toString(totalBruto))
                    .concatRight(" ")
                    .concatRight(currency.abbreviation()).print(escpos);

            new InLinePrinter(Style.FontSize._2).concatLeft("TOTAL NETO")
                    .concatRight(BigDecimalOperations.toString(totalNet))
                    .concatRight(" ")
                    .concatRight(currency.abbreviation()).print(escpos);
            
            escpos.feed(1);
            
            return escpos;
        }
    }
    
    private static class OtherTransactionsOverviewPrinter implements IPrinter
    {
        private int numberOfIn;
        private int numberOfOut;
        
        private BigDecimal totalIn;
        private BigDecimal totalOut;
                
        public OtherTransactionsOverviewPrinter(List<Transaction> transactions)
        {
            numberOfIn = 0;
            numberOfOut = 0;
            totalIn = BigDecimal.ZERO;
            totalOut = BigDecimal.ZERO;
            
            for (var transaction : transactions)
            {
                if (transaction.getReceipt() != null) continue;
                
                if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0)
                {
                    totalOut = totalOut.add(transaction.getAmount());
                    numberOfOut++;
                }
                else
                {
                    totalIn = totalIn.add(transaction.getAmount());
                    numberOfIn++;
                }
            }
        }

        @Override
        public EscPos print(EscPos escpos) throws IOException
        {
            Currency currency = new GlobalPreferencesConfigData().load().currency;
            
            escpos.writeLF(Styles.CENTERED, Translator.getInstance().t("cr:word.othertransactions"));
            new LineFiller("-").print(escpos);

            new InLinePrinter()
                    .concatLeft(String.valueOf(numberOfIn))
                    .concatLeft(" ")
                    .concatLeft(Translator.getInstance().t("cr:printer.cashsession.cashin"))
                    .concatRight(BigDecimalOperations.toString(totalIn))
                    .concatRight(" ")
                    .concatRight(currency.abbreviation()).print(escpos);

            new InLinePrinter()
                    .concatLeft(String.valueOf(numberOfOut))
                    .concatLeft(" ")
                    .concatLeft(Translator.getInstance().t("cr:printer.cashsession.cashout"))
                    .concatRight(BigDecimalOperations.toString(totalOut))
                    .concatRight(" ")
                    .concatRight(currency.abbreviation()).print(escpos);

            escpos.feed(1);
            
            new InLinePrinter(Style.FontSize._2)
                    .concatLeft("TOTAL:")
                    .concatRight(BigDecimalOperations.toString(totalIn.add(totalOut)))
                    .concatRight(" ")
                    .concatRight(currency.abbreviation()).print(escpos);
            
            escpos.feed(1);

            return escpos;
        }
    }
}
