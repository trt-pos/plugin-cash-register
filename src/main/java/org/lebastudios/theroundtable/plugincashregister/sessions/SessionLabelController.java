package org.lebastudios.theroundtable.plugincashregister.sessions;

import com.github.anastaciocintra.escpos.EscPos;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.lebastudios.theroundtable.config.GlobalPreferencesConfigData;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.plugincashregister.printers.BasicSessionOverviewPrinter;
import org.lebastudios.theroundtable.printers.PrintTask;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.ui.MultipleItemsListView;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SessionLabelController extends PaneController<SessionLabelController>
        implements MultipleItemsListView.IReciclablePane<CashSession>
{
    @FXML public Label textLabel;

    private CashSession cashSession;

    @Override
    public PaneController<?> updateItem(CashSession item, MultipleItemsListView<CashSession> control)
    {
        this.cashSession = item;

        String name = item.getAppInstallation().getName();
        LocalDateTime opening = item.getOpeningDate();
        LocalDateTime closing = item.getClosingDate();
        BigDecimal amountInDrawer = item.getAmountInDrawer();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(new GlobalPreferencesConfigData().load().dateTimeFormatter);

        textLabel.setText(
                name + "  " + formatter.format(opening)
                        + " -- " + (closing == null ? "__________" : formatter.format(closing))
                        + "    " + BigDecimalOperations.toCurrencyString(amountInDrawer)
        );
        
        return this;
    }

    public void printSession(ActionEvent actionEvent)
    {
        new PrintTask(PrinterManager.getInstance().getDefaultPrintService())
        {
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
