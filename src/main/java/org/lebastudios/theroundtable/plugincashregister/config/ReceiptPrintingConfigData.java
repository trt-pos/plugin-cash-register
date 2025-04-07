package org.lebastudios.theroundtable.plugincashregister.config;

import org.lebastudios.theroundtable.config.AppConfiguration;
import org.lebastudios.theroundtable.config.ConfigData;
import org.lebastudios.theroundtable.printers.Printer80;

import java.io.File;

public class ReceiptPrintingConfigData extends ConfigData<ReceiptPrintingConfigData>
{
    public boolean hideReceiptData = false;
    public boolean hideTaxesDesglose = false;
    public boolean hidePaymentInfo = false;
    public boolean hideEstablishmentLogo = false;
    public int imageSize = Printer80.MAX_IMG_WIDTH;

    @Override
    public File getFile()
    {
        return new File(AppConfiguration.getGlobalDir() + "/receipt-printing-settings.json");
    }
}
