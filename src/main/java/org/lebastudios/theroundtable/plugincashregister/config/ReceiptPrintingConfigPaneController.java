package org.lebastudios.theroundtable.plugincashregister.config;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import org.lebastudios.theroundtable.config.SettingsPaneController;
import org.lebastudios.theroundtable.config.data.JSONFile;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.plugincashregister.config.data.ReceiptPrintingConfigData;
import org.lebastudios.theroundtable.plugincashregister.printers.IReceiptPrinter;

import java.util.Arrays;
import java.util.List;

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
                case TINY -> "Tiny";
                case SMALL -> "Small";
                case MEDIUM -> "Medium";
                case LARGE -> "Large";
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
        try
        {
            IReceiptPrinter.printTest();
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
