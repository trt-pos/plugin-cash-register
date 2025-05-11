package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.dialogs.EntityFormDialogController;
import org.lebastudios.theroundtable.dialogs.ExceptionDialogController;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.printers.OpenCashDrawer;
import org.lebastudios.theroundtable.printers.PrinterManager;

import java.io.IOException;

public class CashRegisterClosePaneController extends PaneController<CashRegisterClosePaneController>
{
    @FXML
    public void openCashRegister(ActionEvent actionEvent)
    {
        OpenCashSessionFormPaneController form = new OpenCashSessionFormPaneController();
        form.setOnSessionSaved(_ -> CashRegisterPaneController.showInterface());
        
        new EntityFormDialogController<>(form, new CashSession())
                .setOwner(this.getStage())
                .instantiate(true);
    }

    @FXML
    public void openCashRegisterDrawer(ActionEvent actionEvent)
    {
        try (EscPos escPos = new EscPos(new PrinterOutputStream(PrinterManager.getInstance().getDefaultPrintService())))
        {
            new OpenCashDrawer().print(escPos);
        }
        catch (IOException e)
        {
            new ExceptionDialogController(e)
                    .setOwner(this.getStage())
                    .instantiate(true);
        }
    }
}
