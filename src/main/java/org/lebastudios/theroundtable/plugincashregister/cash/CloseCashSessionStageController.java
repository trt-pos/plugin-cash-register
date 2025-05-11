package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.dialogs.ExceptionDialogController;
import org.lebastudios.theroundtable.dialogs.InformationTextDialogController;
import org.lebastudios.theroundtable.logs.Logs;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.entities.Transaction;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.printers.BasicSessionOverviewPrinter;
import org.lebastudios.theroundtable.printers.InLinePrinter;
import org.lebastudios.theroundtable.printers.LineFiller;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.printers.Styles;
import org.lebastudios.theroundtable.ui.StageBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;

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
        return LangFileLoader.getTranslation("plugincashregister.phrase.closeCashRegister");
    }

    @FXML
    public void acceptAndPrint(ActionEvent actionEvent)
    {
        boolean result = Database.getInstance().connectTransactionWithBool(session ->
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
