package org.lebastudios.theroundtable.plugincashregister.config.data;

import org.lebastudios.theroundtable.config.Settings;
import org.lebastudios.theroundtable.config.data.FileRepresentator;
import org.lebastudios.theroundtable.printers.Printer80;

import java.io.File;

public class ReceiptPrintingConfigData implements FileRepresentator
{
    public boolean hideReceiptData = false;
    public boolean hideTaxesDesglose = false;
    public boolean hidePaymentInfo = false;
    public boolean hideEstablishmentLogo = false;
    public int imageSize = Printer80.MAX_IMG_WIDTH;

    @Override
    public File getFile()
    {
        return new File(Settings.getGlobalDir() + "/receipt-printing-settings.json");
    }
}
