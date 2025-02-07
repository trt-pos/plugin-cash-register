package org.lebastudios.theroundtable.plugincashregister.products;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.plugincashregister.entities.TaxType;
import org.lebastudios.theroundtable.ui.StageBuilder;

import java.math.BigDecimal;

public class TaxTypeCreatorStageController extends StageController<TaxTypeCreatorStageController>
{
    @FXML private TextField nameField;
    @FXML private TextField taxField;
    @FXML private TextArea descriptionField;

    @FXML
    private void saveButtonAction(ActionEvent actionEvent)
    {
        if (!validateData()) return;

        TaxType taxType = new TaxType(
                nameField.getText(),
                BigDecimalOperations.dividePrecise(new BigDecimal(taxField.getText()), BigDecimal.valueOf(100)),
                descriptionField.getText()
        );

        Database.getInstance().connectTransaction(session -> session.persist(taxType));
        
        cancelButtonAction(actionEvent);
    }

    @FXML
    private void cancelButtonAction(ActionEvent actionEvent)
    {
        close();
    }

    private boolean validateData()
    {
        nameField.setText(nameField.getText().trim());
        descriptionField.setText(descriptionField.getText().trim());
        taxField.setText(taxField.getText().trim());

        if (nameField.getText().isBlank() || !TaxType.isNameAvailable(nameField.getText()))
        {
            UIEffects.shakeNode(nameField);
            return false;
        }

        try
        {
            new BigDecimal(taxField.getText());
        }
        catch (Exception exception)
        {
            UIEffects.shakeNode(taxField);
            return false;
        }

        return true;
    }

    @Override
    protected void customizeStageBuilder(StageBuilder stageBuilder)
    {
        stageBuilder.setModality(Modality.APPLICATION_MODAL);
    }

    @Override
    public String getTitle()
    {
        return "Taxes type creator";
    }

    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }
}
