package org.lebastudios.theroundtableplugins.cr.config;

import com.github.anastaciocintra.escpos.EscPos;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import org.lebastudios.theroundtable.config.ConfigPaneController;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtableplugins.cr.entities.*;
import org.lebastudios.theroundtableplugins.cr.printers.CashRegisterPrinters;
import org.lebastudios.theroundtable.printers.PrinterManager;

import java.math.BigDecimal;
import java.util.Set;

public class ReceiptPrintingConfigPaneController extends ConfigPaneController<ReceiptPrintingConfigData>
{
    @FXML public CheckBox hideEstablishmentLogo;
    @FXML public CheckBox hideReceiptData;
    @FXML public CheckBox hideTaxesDesglose;
    @FXML public CheckBox hidePaymentInfo;
    @FXML public ChoiceBox<LogoSize> logoSize;

    public ReceiptPrintingConfigPaneController()
    {
        super(new ReceiptPrintingConfigData(), Translator.getInstance().t("cr:phrase.receiptprinterconfig"), "cr:print.png");
    }

    private enum LogoSize
    {
        TINY, SMALL, MEDIUM, LARGE;

        @Override
        public String toString()
        {
            return switch (this)
            {
                case TINY -> Translator.getInstance().t("cr:word.tiny");
                case SMALL -> Translator.getInstance().t("cr:word.small");
                case MEDIUM -> Translator.getInstance().t("cr:word.medium");
                case LARGE -> Translator.getInstance().t("cr:word.large");
            };
        }
        
        public int toInt()
        {
            return switch (this)
            {
                case TINY -> 200;
                case SMALL -> 300;
                case MEDIUM -> 400;
                case LARGE -> 500;
            };
        }
        
        public static LogoSize fromInt(int value)
        {
            return switch (value)
            {
                case 200 -> TINY;
                case 300 -> SMALL;
                case 500 -> LARGE;
                default -> MEDIUM;
            };
        }
    }

    @Override
    public void updateConfigData(ReceiptPrintingConfigData configData)
    {
        configData.hideEstablishmentLogo = hideEstablishmentLogo.isSelected();
        configData.hideTaxesDesglose = hideTaxesDesglose.isSelected();
        configData.hidePaymentInfo = hidePaymentInfo.isSelected();
        configData.hideReceiptData = hideReceiptData.isSelected();
        configData.imageSize = logoSize.getValue().toInt();
    }

    @Override
    public void updateUI(ReceiptPrintingConfigData configData)
    {
        logoSize.getItems().clear();
        logoSize.getItems().addAll(LogoSize.values());

        hideEstablishmentLogo.setSelected(configData.hideEstablishmentLogo);
        hideReceiptData.setSelected(configData.hideReceiptData);
        hideTaxesDesglose.setSelected(configData.hideTaxesDesglose);
        hidePaymentInfo.setSelected(configData.hidePaymentInfo);
        logoSize.setValue(LogoSize.fromInt(configData.imageSize));
    }

    @Override
    public ValidationResult validate()
    {
        return ValidationResult.valid();
    }

    @FXML
    public void printTestReceipt(ActionEvent actionEvent)
    {
        Product p1 = new Product();
        Product p2 = new Product();

        p1.setName("Product 1");
        p1.setPrice(new BigDecimal("2.90"));
        p1.setTaxType(new TaxType("", new BigDecimal("0.10"), ""));

        p2.setName("Product 2");
        p2.setPrice(new BigDecimal("100"));
        p2.setTaxType(new TaxType("", new BigDecimal("0.21"), ""));

        Receipt receipt = new Receipt();

        receipt.setProducts(
                Set.of(
                        new Product_Receipt(p1, new BigDecimal(1)),
                        new Product_Receipt(p2, new BigDecimal(10))
                )
        );
        
        receipt.setTableName("Table 1");
        
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("1002.90"));
        transaction.setMethod(Transaction.PaymentMethod.CASH);

        receipt.setTransaction(transaction);

        receipt.setClient("Client", "ABCD123");
        receipt.setPaymentAmount(new BigDecimal("1003"));
        
        try (EscPos escPos = CashRegisterPrinters.getInstance().printReceipt(receipt, PrinterManager.getInstance().getDefaultPrintService()))
        {
            escPos.feed(5);
            escPos.cut(EscPos.CutMode.PART);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }
}
