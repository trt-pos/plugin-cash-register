package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.dialogs.ConfirmationTextDialogController;
import org.lebastudios.theroundtable.plugincashregister.entities.Transaction;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.plugincashregister.printers.CashRegisterPrinterManager;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.ui.StageBuilder;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;

public class TransactionCreatorStageController extends StageController<TransactionCreatorStageController>
{
    private final LocalDateTime localDateTime;
    private final TransactionType transactionType;
    @FXML private TextField amountTextField;
    @FXML private TextArea descriptionTextArea;

    public TransactionCreatorStageController(TransactionType transactionType)
    {
        this.localDateTime = LocalDateTime.now();
        this.transactionType = transactionType;
    }

    @Override
    protected void customizeStageBuilder(StageBuilder stageBuilder)
    {
        stageBuilder.setModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public URL getFXML()
    {
        return TransactionCreatorStageController.class.getResource("transactionCreatorPane.fxml");
    }

    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }

    @Override
    public String getTitle()
    {
        var titleKey = transactionType == TransactionType.ADD
                ? "word.put"
                : "word.get";

        return LangFileLoader.getTranslation(titleKey);
    }

    @FXML
    private void mainAction()
    {
        var transaction = new Transaction();

        BigDecimal amount;

        try
        {
            amount = new BigDecimal(amountTextField.getText());

            if (amount.compareTo(BigDecimal.ZERO) <= 0
                    || amount.scale() > 2)
            {
                UIEffects.shakeNode(amountTextField);
                return;
            }
        }
        catch (NumberFormatException exception)
        {
            UIEffects.shakeNode(amountTextField);
            return;
        }

        if (transactionType == TransactionType.REMOVE)
        {
            amount = amount.negate();
        }

        transaction.setAmount(amount);
        transaction.setDate(localDateTime);
        transaction.setDescription(descriptionTextArea.getText().trim());

        Database.getInstance().connectTransaction(session ->
        {
            session.persist(transaction);
            session.flush();
            
            try
            {
                EscPos escPos = new EscPos(new PrinterOutputStream(PrinterManager.getInstance().getDefaultPrintService()));
                CashRegisterPrinterManager.getInstance().getTransactionPrinter().print(escPos, transaction);

                escPos.feed(5).cut(EscPos.CutMode.PART).close();
                cancel();
            }
            catch (Exception e)
            {
                new ConfirmationTextDialogController(LangFileLoader.getTranslation("textblock.printingerror"), response ->
                {
                    if (!response) {
                        session.getTransaction().rollback();
                        return;
                    }

                    cancel();
                }).instantiate();
            }
        });
    }
    
    @FXML
    private void cancel()
    {
        ((Stage) amountTextField.getScene().getWindow()).close();
    }

    public enum TransactionType
    {
        ADD,
        REMOVE
    }
}
