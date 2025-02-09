package org.lebastudios.theroundtable.plugincashregister.config;

import com.github.anastaciocintra.escpos.EscPos;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import org.lebastudios.theroundtable.config.SettingsPaneController;
import org.lebastudios.theroundtable.config.data.JSONFile;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.plugincashregister.config.data.ReceiptPrintingConfigData;
import org.lebastudios.theroundtable.plugincashregister.entities.*;
import org.lebastudios.theroundtable.plugincashregister.printers.CashRegisterPrinters;
import org.lebastudios.theroundtable.printers.PrinterManager;

import java.math.BigDecimal;
import java.util.Set;

public class ReceiptPrintingConfigPaneController extends SettingsPaneController
{
    @FXML private CheckBox hideEstablishmentLogo;
    @FXML private CheckBox hideReceiptData;
    @FXML private CheckBox hideTaxesDesglose;
    @FXML private CheckBox hidePaymentInfo;
    @FXML private ChoiceBox<LogoSize> logoSize;

    private enum LogoSize
    {
        TINY, SMALL, MEDIUM, LARGE;

        @Override
        public String toString()
        {
            return LangFileLoader.getTranslation(switch (this)
            {
                case TINY -> LangFileLoader.getTranslation("word.tiny");
                case SMALL -> LangFileLoader.getTranslation("word.small");
                case MEDIUM -> LangFileLoader.getTranslation("word.medium");
                case LARGE -> LangFileLoader.getTranslation("word.large");
            });
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
    protected void initialize()
    {
        logoSize.getItems().clear();
        logoSize.getItems().addAll(LogoSize.values());
        
        ReceiptPrintingConfigData receiptPrinterConf = new JSONFile<>(ReceiptPrintingConfigData.class).get();
        
        hideEstablishmentLogo.setSelected(receiptPrinterConf.hideEstablishmentLogo);
        hideReceiptData.setSelected(receiptPrinterConf.hideReceiptData);
        hideTaxesDesglose.setSelected(receiptPrinterConf.hideTaxesDesglose);
        hidePaymentInfo.setSelected(receiptPrinterConf.hidePaymentInfo);
        logoSize.setValue(LogoSize.fromInt(receiptPrinterConf.imageSize));
    }

    @Override
    public void apply()
    {
        JSONFile<ReceiptPrintingConfigData> receiptPrinterConf = new JSONFile<>(ReceiptPrintingConfigData.class);
        
        receiptPrinterConf.get().hideEstablishmentLogo = hideEstablishmentLogo.isSelected();
        receiptPrinterConf.get().hideTaxesDesglose = hideTaxesDesglose.isSelected();
        receiptPrinterConf.get().hidePaymentInfo = hidePaymentInfo.isSelected();
        receiptPrinterConf.get().hideReceiptData = hideReceiptData.isSelected();
        receiptPrinterConf.get().imageSize = logoSize.getValue().toInt();
        
        receiptPrinterConf.save();
    }

    @FXML
    private void printTestReceipt()
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
        
        receipt.setTransaction(transaction);
        
        receipt.setClient("Client", "ABCD123");
        receipt.setPaymentAmount(new BigDecimal("1003"));
        receipt.setPaymentMethod("CASH");
        
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
    
    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }
}
