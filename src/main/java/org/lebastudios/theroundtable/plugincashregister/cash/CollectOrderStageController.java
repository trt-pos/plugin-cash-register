package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import lombok.AllArgsConstructor;
import org.lebastudios.theroundtable.accounts.AccountManager;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.dialogs.ExceptionDialogController;
import org.lebastudios.theroundtable.dialogs.InformationTextDialogController;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegisterEvents;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.printers.CashRegisterPrinters;
import org.lebastudios.theroundtable.printers.OpenCashDrawer;
import org.lebastudios.theroundtable.printers.PrintTask;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.tasks.Task;
import org.lebastudios.theroundtable.ui.BigDecimalField;
import org.lebastudios.theroundtable.ui.StageBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class CollectOrderStageController extends StageController<CollectOrderStageController>
{
    private final Order order;
    private final Consumer<Receipt> onDone;
    @FXML public BigDecimalField amountPaidField;
    @FXML public TextField clientNameField;
    @FXML public TextField clientIdentifierField;
    @FXML public RadioButton cashRadioButton;
    @FXML public RadioButton cardRadioButton;
    @FXML public CheckBox defineClientOption;
    @FXML public GridPane clientDataContainer;
    private ToggleGroup paymentMethodToggleGroup;
    @FXML public Label orderTimeLabel;
    @FXML public Label orderDateLabel;
    @FXML public Label orderTotalLabel;
    @FXML public HBox amountPaidBox;

    public CollectOrderStageController(Order order, Consumer<Receipt> onDone)
    {
        this.order = order;
        this.onDone = onDone;
    }

    @FXML
    @Override
    protected void initialize()
    {
        LocalDateTime date = LocalDateTime.now();
        orderTimeLabel.setText(date.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString());
        orderDateLabel.setText(date.toLocalDate().toString());
        orderTotalLabel.setText("Total: " + BigDecimalOperations.toString(order.getTotal()) + " €");

        paymentMethodToggleGroup = new ToggleGroup();
        cashRadioButton.setToggleGroup(paymentMethodToggleGroup);
        cardRadioButton.setToggleGroup(paymentMethodToggleGroup);

        amountPaidField.setValue(BigDecimalOperations.round(order.getTotal()));

        paymentMethodToggleGroup.selectedToggleProperty().addListener((_, _, newValue) ->
        {
            if (newValue == null) return;

            if (newValue == cashRadioButton)
            {
                amountPaidBox.setDisable(false);
            }
            else
            {
                amountPaidBox.setDisable(true);
                amountPaidField.setValue(BigDecimalOperations.round(order.getTotal()));
            }
        });

        defineClientOption.selectedProperty().addListener((_, _, newValue) ->
        {
            if (newValue == null) return;

            clientDataContainer.setDisable(!newValue);
        });
    }

    @Override
    protected void customizeStageBuilder(StageBuilder stageBuilder)
    {
        stageBuilder.setResizeable(true)
                .setModality(Modality.APPLICATION_MODAL);
    }

    @FXML
    public void submitAndPrint(ActionEvent actionEvent)
    {
        saveReceiptInDatabase(receipt ->
        {
            try
            {
                EscPos escpos = CashRegisterPrinters.getInstance().printReceipt(
                        receipt, 
                        PrinterManager.getInstance().getDefaultPrintService()
                );
                
                return new PrintTask(escpos)
                {
                    @Override
                    protected EscPos print(EscPos escpos) throws IOException
                    {
                        return escpos;
                    }
                };
            }
            catch (IOException e)
            {
                new ExceptionDialogController(e).instantiate(true);
                return null;
            }
        });
    }

    @FXML
    public void submit(ActionEvent actionEvent)
    {
        try (PrinterOutputStream outputStream = new PrinterOutputStream(
                PrinterManager.getInstance().getDefaultPrintService()
        ))
        {
            saveReceiptInDatabase(_ -> new PrintTask(new EscPos(outputStream))
            {
                @Override
                protected EscPos print(EscPos escpos) throws IOException
                {
                    return new OpenCashDrawer().print(escpos);
                }
            });
        }
        catch (IOException e)
        {
            new ExceptionDialogController(e).instantiate(true);
        }
    }

    private void saveReceiptInDatabase(Function<Receipt, PrintTask> printerAction)
    {
        new SaveReceiptAndPrintTask(printerAction).execute(true);
    }

    private Receipt generateReceiptObject()
    {
        if (!validateInputData()) return null;

        var paymentMethod = paymentMethodToggleGroup.getSelectedToggle() == cashRadioButton
                ? PaymentMethod.CASH.name()
                : PaymentMethod.CARD.name();

        Receipt receipt = new Receipt();
        receipt.setPaymentAmount(amountPaidField.getValue());
        receipt.setPaymentMethod(paymentMethod);

        if (defineClientOption.isSelected())
        {
            receipt.setClient(clientNameField.getText(), clientIdentifierField.getText());
        }

        receipt.setAccount(AccountManager.getInstance().getCurrentLogged());
        return receipt;
    }

    private boolean validateInputData()
    {
        clientNameField.setText(clientNameField.getText().trim());
        clientIdentifierField.setText(clientIdentifierField.getText().trim());

        if (amountPaidField.getValue() == null || amountPaidField.getValue().compareTo(order.getTotal()) < 0)
        {
            UIEffects.shakeNode(amountPaidField);
            return false;
        }

        if (paymentMethodToggleGroup.getSelectedToggle() == null)
        {
            UIEffects.shakeNode(cashRadioButton);
            UIEffects.shakeNode(cardRadioButton);
            return false;
        }

        if (defineClientOption.isSelected())
        {
            if (clientNameField.getText().isBlank())
            {
                UIEffects.shakeNode(clientNameField);
                return false;
            }

            if (clientIdentifierField.getText().isBlank())
            {
                UIEffects.shakeNode(clientIdentifierField);
                return false;
            }
        }

        return true;
    }

    @Override
    public String getTitle()
    {
        return "Collect Order";
    }

    @AllArgsConstructor
    private class SaveReceiptAndPrintTask extends Task<Void>
    {
        private final Function<Receipt, PrintTask> printerAction;

        @Override
        protected Void call() throws Exception
        {
            updateTitle("Saving and printing");

            updateMessage("Generating the receipt");
            updateProgress(0, 1);
            Receipt receipt = generateReceiptObject();

            if (receipt == null) return null;

            receipt.setOrder(order);
            
            updateMessage("Requesting bill number");
            updateProgress(0.25, 1);
            StringBuffer billNumber = new StringBuffer();
            PluginCashRegisterEvents.onRequestNewReceiptBillNumber.invoke(receipt.getId(), billNumber);

            updateMessage("Calling the printer");
            updateProgress(0.50, 1);
            PrintTask printTask = printerAction.apply(receipt);
            
            printTask.setOnTaskComplete(_ ->
            {
                updateMessage("Saving the receipt");
                updateProgress(0.75, 1);
                boolean success = Database.getInstance().connectTransactionWithBool(session ->
                {
                    session.persist(receipt);
                });

                if (!success)
                {
                    new InformationTextDialogController(
                            LangFileLoader.getTranslation("plugincashregister.textblock.errorsavingreceipt")
                    ).instantiate(true);
                    return;
                }

                updateMessage("Billing the receipt");
                updateProgress(0.85, 1);
                if (!billNumber.isEmpty())
                {
                    PluginCashRegisterEvents.onReceiptBilled.invoke(receipt, billNumber.toString());
                }
                
                updateMessage("Finishing the process");
                updateProgress(0.95, 1);
                PluginCashRegisterEvents.onReceiptEmitted.invoke(receipt);

                CollectOrderStageController.this.close();
                onDone.accept(receipt);
            });
            
            executeSubtask(printTask);
            return null;
        }
    }
}
