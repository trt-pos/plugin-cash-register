package org.lebastudios.theroundtable.plugincashregister.products;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.entities.TaxType;

import java.math.BigDecimal;

public class ModifyTaxTypeStageController extends TaxTypeStageController
{
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
    @Override
    public void saveButtonAction(ActionEvent actionEvent)
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

    @Override
    public String getTitle()
    {
        return "Modify Tax Type";
    }}
