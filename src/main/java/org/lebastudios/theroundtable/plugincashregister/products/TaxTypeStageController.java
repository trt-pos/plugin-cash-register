package org.lebastudios.theroundtable.plugincashregister.products;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import org.hibernate.loader.NonUniqueDiscoveredSqlAliasException;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.plugincashregister.entities.TaxType;
import org.lebastudios.theroundtable.ui.StageBuilder;

import java.math.BigDecimal;

public abstract class TaxTypeStageController extends StageController<TaxTypeStageController>
{
    @FXML public TextField nameField;
    @FXML public TextField taxField;
    @FXML public TextArea descriptionField;

    protected TaxType taxType;
    
    @FXML
    public abstract void saveButtonAction(ActionEvent actionEvent);

    @FXML
    public void cancelButtonAction(ActionEvent actionEvent)
    {
        close();
    }
    
    protected boolean validateData()
    {
        nameField.setText(nameField.getText().trim());
        descriptionField.setText(descriptionField.getText().trim());
        taxField.setText(taxField.getText().trim());

        String oldName = taxType != null ? taxType.getName() : "";
        
        if (taxType != null) 
        {
            if (nameField.getText().isBlank()) 
            {
                UIEffects.shakeNode(nameField);
                return false;
            }
            
            if (!nameField.getText().equals(taxType.getName()) 
                    && !TaxType.isNameAvailable(nameField.getText())) 
            {
                UIEffects.shakeNode(nameField);
                return false;
            }
        }
        else
        {
            if (nameField.getText().isBlank() || !TaxType.isNameAvailable(nameField.getText()))
            {
                UIEffects.shakeNode(nameField);
                return false;
            }
        }
        
        try
        {
            BigDecimal taxValue = new BigDecimal(taxField.getText());
            if (taxValue.compareTo(BigDecimal.ZERO) < 0 || taxValue.compareTo(BigDecimal.valueOf(100)) > 0)
            {
                throw new Exception("Tax value must be between 0 and 100");
            }
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
        stageBuilder.setModality(Modality.WINDOW_MODAL);
    }
    
    @Override
    protected void loadFXML()
    {
        root = new org.lebastudios.theroundtable.plugincashregister.products.TaxTypeStage$View(this);
        this.initialize();
    }
}
