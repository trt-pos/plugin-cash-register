package org.lebastudios.theroundtableplugins.cr.cash;

import com.github.anastaciocintra.escpos.EscPos;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.lebastudios.theroundtable.accounts.AccountManager;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.dialogs.ConfirmationTextDialogController;
import org.lebastudios.theroundtable.entities.AppInstallation;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtableplugins.cr.PluginCashRegisterEvents;
import org.lebastudios.theroundtableplugins.cr.entities.CashSession;
import org.lebastudios.theroundtableplugins.cr.entities.Transaction;
import org.lebastudios.theroundtableplugins.cr.printers.CashRegisterPrinters;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.components.StageBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransactionCreatorStageController extends StageController<TransactionCreatorStageController>
{
    private final TransactionType transactionType;
    @FXML public TextField amountTextField;
    @FXML public TextArea descriptionTextArea;

    public TransactionCreatorStageController(TransactionType transactionType)
    {
        this.transactionType = transactionType;
    }

    @Override
    protected void customizeStageBuilder(StageBuilder stageBuilder)
    {
        stageBuilder.setModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public String getTitle()
    {
        var titleKey = transactionType == TransactionType.ADD
                ? "cr:word.put"
                : "cr:word.get";

        return Translator.getInstance().t(titleKey);
    }

    @FXML
    public void mainAction(ActionEvent actionEvent)
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
        transaction.setDate(LocalDateTime.now());
        transaction.setDescription(descriptionTextArea.getText().trim());
        transaction.setAccount(AccountManager.getInstance().getCurrentLogged());
        transaction.setMethod(Transaction.PaymentMethod.CASH);

        Database.getInstance().connectTransaction(session ->
        {
            transaction.setTotalCash(CashSession.getActualSession(session).getAmountInDrawer());
            transaction.setAppInstallation(AppInstallation.thisInstalation(session));

            session.persist(transaction);

            AtomicBoolean isTransactionCreated = new AtomicBoolean(false);

            try (EscPos escPos = CashRegisterPrinters.getInstance()
                    .printTransaction(transaction, PrinterManager.getInstance().getDefaultPrintService())
            )
            {
                escPos.feed(5).cut(EscPos.CutMode.PART);
                cancel(null);
                isTransactionCreated.set(true);
            }
            catch (Exception e)
            {
                new ConfirmationTextDialogController(
                        Translator.getInstance().t("cr:textblock.printingerror"),
                        isTransactionCreated::set
                ).instantiate(true);
            }
            
            if (isTransactionCreated.get())
            {
                session.getTransaction().commit();
                PluginCashRegisterEvents.onTransactionRealized.invoke(transaction);
                cancel(null);
            }
            else
            {
                session.getTransaction().rollback();
            }
        });
    }

    @FXML
    public void cancel(ActionEvent actionEvent)
    {
        ((Stage) amountTextField.getScene().getWindow()).close();
    }

    public enum TransactionType
    {
        ADD,
        REMOVE
    }
}
