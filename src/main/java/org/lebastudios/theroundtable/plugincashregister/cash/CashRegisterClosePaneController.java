package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.plugincashregister.config.CashRegisterStateData;
import org.lebastudios.theroundtable.printers.OpenCashDrawer;
import org.lebastudios.theroundtable.printers.PrinterManager;

import java.io.IOException;
import java.time.LocalDateTime;

public class CashRegisterClosePaneController extends PaneController<CashRegisterClosePaneController>
{
    @FXML
    public void openCashRegister(ActionEvent actionEvent)
    {
        var cashRegisterState = new CashRegisterStateData().load();

        cashRegisterState.open = true;
        cashRegisterState.openTime = LocalDateTime.now().toString();

        cashRegisterState.save();

        CashRegisterPaneController.showInterface();
    }
    
    @FXML public void openCashRegisterDrawer(ActionEvent actionEvent)
    {
        try
        {
            EscPos escPos = new EscPos(new PrinterOutputStream(PrinterManager.getInstance().getDefaultPrintService()));
            new OpenCashDrawer().print(escPos);
            escPos.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
