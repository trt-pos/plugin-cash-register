package org.lebastudios.theroundtable.plugincashregister.printers;

import com.github.anastaciocintra.escpos.EscPos;
import lombok.Setter;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.printers.IPrinter;
import org.lebastudios.theroundtable.printers.OpenCashDrawer;

import java.io.IOException;

@Setter
public class BasicReceiptPrinter extends ReceiptPrinter
{
    private IPrinter afterHeader = p -> p;
    
    public BasicReceiptPrinter(Receipt receipt)
    {
        super(receipt);
    }

    @Override
    public EscPos print(EscPos escpos) throws IOException
    {
        new BasicReceiptHeaderPrinter().print(escpos);

        escpos.feed(1);

        afterHeader.print(escpos);
        
        escpos.feed(1);

        new BasicReceiptBodyPrinter(receipt).print(escpos);

        new BasicReceiptFooterPrinter().print(escpos);

        new OpenCashDrawer().print(escpos);

        return escpos;
    }
}
