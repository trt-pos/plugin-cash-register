package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.fxml.FXML;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.plugincashregister.config.data.CashRegisterStateData;
import org.lebastudios.theroundtable.config.data.JSONFile;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.printers.OpenCashDrawer;
import org.lebastudios.theroundtable.printers.PrinterManager;

import java.io.IOException;
import java.time.LocalDateTime;

public class CashRegisterClosePaneController extends PaneController<CashRegisterClosePaneController>
{
    @FXML
    private void openCashRegister()
    {
        var cashRegisterState = new JSONFile<>(CashRegisterStateData.class);

        cashRegisterState.get().open = true;
        cashRegisterState.get().openTime = LocalDateTime.now().toString();

        cashRegisterState.save();

        CashRegisterPaneController.showInterface();
    }
    
    @FXML private void openCashRegisterDrawer()
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

    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }
}
