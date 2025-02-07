package org.lebastudios.theroundtable.plugincashregister.products;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.plugincashregister.entities.TaxType;

import java.math.BigDecimal;
import java.net.URL;

public class ModifyTaxTypeStageController extends StageController<ModifyTaxTypeStageController>
{
    @FXML private TextField nameField;
    @FXML private TextField taxField;
    @FXML private TextArea descriptionField;
    
    private final TaxType taxType;
    
    public ModifyTaxTypeStageController(TaxType taxType)
    {
        this.taxType = taxType;
    }
    
    @Override
    protected void initialize()
    {
        nameField.setText(taxType.getName());
        taxField.setText(taxType.getValue().multiply(BigDecimal.valueOf(100)).toPlainString());
        descriptionField.setText(taxType.getDescription());
    }

    @FXML
    private void saveButtonAction(ActionEvent actionEvent)
    {
        if (!validateData()) return;

        Database.getInstance().connectTransaction(session ->
        {
            taxType.setProperties(
                    nameField.getText(), 
                    BigDecimalOperations.dividePrecise(new BigDecimal(taxField.getText()), new BigDecimal("100")), 
                    descriptionField.getText()
            );
            
            session.merge(taxType);
        });

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

        if (nameField.getText().isBlank())
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
    public String getTitle()
    {
        return "Modify Tax Type";
    }

    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }

    @Override
    public URL getFXML()
    {
        return TaxTypeCreatorStageController.class.getResource("taxTypeCreatorStage.fxml");
    }
}
