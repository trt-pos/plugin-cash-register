package org.lebastudios.theroundtable.plugincashregister.sessions;

import com.github.anastaciocintra.escpos.EscPos;
import javafx.event.ActionEvent;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.plugincashregister.printers.BasicSessionOverviewPrinter;
import org.lebastudios.theroundtable.printers.PrintTask;
import org.lebastudios.theroundtable.printers.PrinterManager;

import java.io.IOException;

public class SessionLabelController extends PaneController<SessionLabelController>
{
    private CashSession cashSession;

    public void updateView(CashSession cashSession)
    {
        this.cashSession = cashSession;
    }

    // TODO: This is printing twice
    public void printSession(ActionEvent actionEvent)
    {
        new PrintTask(PrinterManager.getInstance().getDefaultPrintService()) {

            @Override
            protected EscPos print(EscPos escpos) throws IOException
            {
                return new BasicSessionOverviewPrinter(
                        cashSession,
                        new BasicSessionOverviewPrinter.Settings(false, false)
                ).print(escpos);
            }
        }.execute();
    }
}
