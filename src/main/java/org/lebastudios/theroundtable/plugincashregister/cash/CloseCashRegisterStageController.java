package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.lebastudios.theroundtable.plugincashregister.config.data.CashRegisterStateData;
import org.lebastudios.theroundtable.config.data.JSONFile;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.entities.Transaction;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.printers.InLinePrinter;
import org.lebastudios.theroundtable.printers.LineFiller;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.printers.Styles;
import org.lebastudios.theroundtable.ui.StageBuilder;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;

public class CloseCashRegisterStageController extends StageController<CloseCashRegisterStageController>
{
    private final LocalDateTime from;
    private final LocalDateTime to;
    @FXML private CheckBox includeTransaction;
    @FXML private CheckBox includeProducts;

    public CloseCashRegisterStageController()
    {
        var cashRegisterState = new JSONFile<>(CashRegisterStateData.class).get();

        from = LocalDateTime.parse(cashRegisterState.openTime);
        to = LocalDateTime.now();
    }

    @Override
    protected void customizeStageBuilder(StageBuilder stageBuilder)
    {
        stageBuilder.setModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public URL getFXML()
    {
        return CloseCashRegisterStageController.class.getResource("closeCashRegisterStage.fxml");
    }

    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }

    @Override
    public String getTitle()
    {
        return LangFileLoader.getTranslation("phrase.closeCashRegister");
    }

    @FXML
    private void acceptAndPrint()
    {
        var cashRegisterState = new JSONFile<>(CashRegisterStateData.class);

        printDay(from, to);

        cashRegisterState.get().open = false;
        cashRegisterState.get().openTime = "";

        cashRegisterState.save();

        CashRegisterPaneController.showInterface();

        cancel();
    }

    private void printDay(LocalDateTime from, LocalDateTime to)
    {
        Database.getInstance().connectQuery(session ->
        {
            var transactions = session.createQuery(
                            "from Transaction t " +
                                    "where t.date >= :startDate " +
                                    "and t.date <= :endDate " +
                                    "and t.receipt not in (select rm.superReceipt from ReceiptModification rm) " +
                                    "order by t.date",
                            Transaction.class)
                    .setParameter("startDate", from)
                    .setParameter("endDate", to)
                    .getResultList();

            try
            {
                var escpos = new EscPos(new PrinterOutputStream(PrinterManager.getInstance().getDefaultPrintService()));

                escpos.writeLF(Styles.CENTERED,
                        "    " + LangFileLoader.getTranslation("word.from") + " " + from.toLocalDate().toString()
                                + " " + LangFileLoader.getTranslation("word.at")
                                + " " + from.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString());

                escpos.writeLF(Styles.CENTERED,
                        "    " + LangFileLoader.getTranslation("word.to") + " " + to.toLocalDate().toString()
                                + " " + LangFileLoader.getTranslation("word.at")
                                + " " + to.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString());
                
                escpos.feed(1);
                
                if (includeProducts.isSelected()) 
                {
                    var receipts = session.createQuery("from Receipt r " +
                                            "where r.transaction in :transactions " +
                                            "order by r.transaction.date",
                                    Receipt.class)
                            .setParameter("transactions", transactions)
                            .getResultList();
                    
                    printProducts(escpos, receipts);
                }

                if (includeTransaction.isSelected()) printTransactions(escpos, transactions);

                var totalBruto = BigDecimal.ZERO;
                var totalNet = BigDecimal.ZERO;

                for (var transaction : transactions)
                {
                    totalBruto = totalBruto.add(transaction.getAmount());
                    totalNet = totalNet.add(transaction.getReceipt() == null 
                            ? transaction.getAmount() 
                            : BigDecimalOperations.round(transaction.getReceipt().getNotTaxedTotal()));
                }
                
                new InLinePrinter(Style.FontSize._2).concatLeft("TOTAL BRUTO")
                        .concatRight(BigDecimalOperations.toString(totalBruto))
                        .concatRight("EUR").print(escpos);

                new InLinePrinter(Style.FontSize._2).concatLeft("TOTAL NETO")
                        .concatRight(BigDecimalOperations.toString(totalNet))
                        .concatRight("EUR").print(escpos);
                
                escpos.feed(8).cut(EscPos.CutMode.PART);
                escpos.close();
            }
            catch (Exception exception)
            {
                System.err.println(exception.getMessage());
            }

        });
    }

    @FXML
    private void cancel()
    {
        ((Stage) includeProducts.getScene().getWindow()).close();
    }

    @SneakyThrows
    private void printTransactions(EscPos escPos, List<Transaction> transactions)
    {
        escPos.feed(1);
        new LineFiller("-").print(escPos);

        new InLinePrinter().concatLeft("    " + LangFileLoader.getTranslation("word.transactions"))
                .concatRight("Total: " + transactions.size()).print(escPos);
        new LineFiller("-").print(escPos);

        for (var transaction : transactions)
        {
            new InLinePrinter().concatLeft(transaction.getDescription(), 30)
                    .concatRight(BigDecimalOperations.toString(transaction.getAmount()))
                    .concatRight(" EUR").print(escPos);
        }

        new LineFiller("-").print(escPos);
        escPos.feed(1);
    }

    @SneakyThrows
    private void printProducts(EscPos escPos, List<Receipt> receipts)
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

        new InLinePrinter().concatLeft("    " + LangFileLoader.getTranslation("word.products"))
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
}
