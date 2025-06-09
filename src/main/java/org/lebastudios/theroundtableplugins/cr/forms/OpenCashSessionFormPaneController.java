package org.lebastudios.theroundtableplugins.cr.forms;

import javafx.fxml.FXML;
import lombok.Setter;
import org.lebastudios.theroundtable.accounts.AccountManager;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.FormPaneController;
import org.lebastudios.theroundtable.entities.AppInstallation;
import org.lebastudios.theroundtableplugins.cr.entities.CashSession;
import org.lebastudios.theroundtable.components.BigDecimalField;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class OpenCashSessionFormPaneController extends FormPaneController<CashSession>
{
    @FXML public BigDecimalField cashInDrawerBigDecimalField;

    @Setter public Consumer<CashSession> onSessionSaved = _ -> {};
    
    @Override
    protected void updateUI(CashSession object)
    {
        cashInDrawerBigDecimalField.setValue(object.getAmountInDrawer());
    }

    @Override
    public boolean validate()
    {
        if (cashInDrawerBigDecimalField.getValue() == null 
                || cashInDrawerBigDecimalField.getValue().compareTo(BigDecimal.ZERO) < 0) 
        {
            UIEffects.shakeNode(cashInDrawerBigDecimalField);
            return false;
        }
        
        return true;
    }

    @Override
    public CashSession buildObject(CashSession cashSession)
    {
        cashSession.setAmountInDrawer(cashInDrawerBigDecimalField.getValue());
        cashSession.setAccount(AccountManager.getInstance().getCurrentLogged());
        cashSession.setOpeningDate(LocalDateTime.now());
        cashSession.setAppInstallation(AppInstallation.thisInstalation());
        return cashSession;
    }

    @Override
    public boolean onSaveAction(CashSession object)
    {
        onSessionSaved.accept(object);
        return true;
    }
}
