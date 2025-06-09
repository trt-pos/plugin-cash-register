package org.lebastudios.theroundtableplugins.cr.cash;

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
import org.lebastudios.theroundtable.entities.AppInstallation;
import org.lebastudios.theroundtable.locale.LocaleManager;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtableplugins.cr.PluginCashRegisterEvents;
import org.lebastudios.theroundtableplugins.cr.entities.CashSession;
import org.lebastudios.theroundtableplugins.cr.entities.Product_Receipt;
import org.lebastudios.theroundtableplugins.cr.entities.Receipt;
import org.lebastudios.theroundtableplugins.cr.entities.Transaction;
import org.lebastudios.theroundtableplugins.cr.printers.CashRegisterPrinters;
import org.lebastudios.theroundtable.printers.OpenCashDrawer;
import org.lebastudios.theroundtable.printers.PrintTask;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.tasks.Task;
import org.lebastudios.theroundtable.components.BigDecimalField;
import org.lebastudios.theroundtable.components.StageBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
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
                {
                    this.setCut(false);
                }
                
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
                ? Transaction.PaymentMethod.CASH
                : Transaction.PaymentMethod.CARD;

        // Receipt metadata
        Receipt receipt = new Receipt();
        receipt.setPaymentAmount(amountPaidField.getValue());
        receipt.setTableName(order.getOrderName());
        receipt.setTaxesAmount(order.getTotalTaxes());

        // Receipt products relationship
        HashSet<Product_Receipt> products = new HashSet<>();

        for (OrderItem orderItem : order.getOrderItems())
        {
            Product_Receipt productReceipt = new Product_Receipt(orderItem.intoProduct(), orderItem.getQuantity());
            productReceipt.setReceipt(receipt);

            products.add(productReceipt);
        }
        
        receipt.setProducts(products);

        LocalDateTime now = LocalDateTime.now();

        // Transaction represented by the receipt
        Transaction transaction = new Transaction();
        transaction.setMethod(paymentMethod);
        transaction.setAccount(AccountManager.getInstance().getCurrentLogged());
        transaction.setAppInstallation(AppInstallation.thisInstalation());
        transaction.setAmount(order.getTotal());
        transaction.setTotalCash(CashSession.getActualSession().getAmountInDrawer().add(order.getTotal()));
        transaction.setDate(now);
        transaction.setDescription(
                Translator.getInstance().t("cr:word.receipt")
                        + " "
                        + LocaleManager.getInstance().getActualDateTimeFormatter().format(now)
        );

        transaction.setReceipt(receipt);
        receipt.setTransaction(transaction);

        if (defineClientOption.isSelected())
        {
            receipt.setClient(clientNameField.getText(), clientIdentifierField.getText());
        }
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
                boolean success = Database.getInstance().connectTransaction(session ->
                {
                    session.persist(receipt);
                });

                if (!success)
                {
                    new InformationTextDialogController(
                            Translator.getInstance().t("cr:textblock.errorsavingreceipt")
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
                PluginCashRegisterEvents.onTransactionRealized.invoke(receipt.getTransaction());

                CollectOrderStageController.this.close();
                onDone.accept(receipt);
            });
            
            executeSubtask(printTask);
            return null;
        }
    }
}
