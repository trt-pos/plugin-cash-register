package org.lebastudios.theroundtableplugins.cr.forms;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.FormPaneController;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtableplugins.cr.entities.TaxType;

import java.math.BigDecimal;

public class TaxTypeFormPaneController extends FormPaneController<TaxType>
{
    @FXML public TextField nameField;
    @FXML public TextField taxField;
    @FXML public TextArea descriptionField;

    @Override
    protected void updateUI(TaxType taxType)
    {
        nameField.setText(taxType.getName());
        taxField.setText(taxType.getValue().multiply(BigDecimal.valueOf(100)).toPlainString());
        descriptionField.setText(taxType.getDescription());
    }

    @Override
    public final boolean validate()
    {
        nameField.setText(nameField.getText().trim());
        descriptionField.setText(descriptionField.getText().trim());
        taxField.setText(taxField.getText().trim());

        if (nameField.getText().isBlank())
        {
            UIEffects.shakeNode(nameField);
            return false;
        }

        if (!nameField.getText().equals(object.getName())
                && !TaxType.isNameAvailable(nameField.getText()))
        {
            UIEffects.shakeNode(nameField);
            return false;
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
    public TaxType buildObject(TaxType taxType)
    {
        taxType.setName(nameField.getText());
        taxType.setValue(BigDecimalOperations.dividePrecise(new BigDecimal(taxField.getText()), BigDecimal.valueOf(100)));
        taxType.setDescription(descriptionField.getText());

        return taxType;
    }
}
