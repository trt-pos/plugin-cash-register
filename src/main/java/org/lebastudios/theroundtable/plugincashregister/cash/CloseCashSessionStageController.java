package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtable.logs.Logs;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.plugincashregister.printers.BasicSessionOverviewPrinter;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.components.StageBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

public class CloseCashSessionStageController extends StageController<CloseCashSessionStageController>
{
    @FXML public CheckBox includeTransaction;
    @FXML public CheckBox includeProducts;

    @Override
    protected void customizeStageBuilder(StageBuilder stageBuilder)
    {
        stageBuilder.setModality(Modality.WINDOW_MODAL);
    }
    @Override
    public String getTitle()
    {
        return Translator.getInstance().t("cr:plugincashregister.phrase.closeCashRegister");
    }

    @FXML
    public void acceptAndPrint(ActionEvent actionEvent)
    {
        boolean result = Database.getInstance().connectTransaction(session ->
        {
            CashSession actualSession = CashSession.getActualSession(session);
            
            if (actualSession == null)
            {
                throw new IllegalStateException("There is no actual session");
            }
            
            actualSession.setClosingDate(LocalDateTime.now());
            session.merge(actualSession);
            
            new Thread(() -> printDay(actualSession)).start();
        });
        
        if (!result) return;
        
        
        CashRegisterPaneController.showInterface();
        
        cancel(null);
    }

    private void printDay(CashSession session)
    {
        BasicSessionOverviewPrinter.Settings settings = new BasicSessionOverviewPrinter.Settings(
                includeTransaction.isSelected(), 
                includeProducts.isSelected()
        );
        
        try (EscPos escpos = new EscPos(new PrinterOutputStream(PrinterManager.getInstance().getDefaultPrintService())))
        {
            new BasicSessionOverviewPrinter(session, settings).print(escpos);
            escpos.feed(8).cut(EscPos.CutMode.PART);
        } 
        catch (IOException e)
        {
            Logs.getInstance().log(
                    "Error printing cash register session",
                    e
            );
        }
    }

    @FXML
    public void cancel(ActionEvent actionEvent)
    {
        ((Stage) includeProducts.getScene().getWindow()).close();
    }
}
