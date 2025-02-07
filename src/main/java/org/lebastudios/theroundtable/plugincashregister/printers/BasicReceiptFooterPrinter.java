package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.printers.IPrinter;

import java.io.IOException;

public class BasicReceiptFooterPrinter implements IPrinter
{
    @Override
    public EscPos print(EscPos escpos) throws IOException
    {
        var centered = new Style().setBold(true).setJustification(EscPosConst.Justification.Center);
        escpos.writeLF(centered, LangFileLoader.getTranslation("ticket.footerbye"));
        return escpos;
    }
}
