package org.lebastudios.theroundtable.plugincashregister.products;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.entities.TaxType;

import java.math.BigDecimal;

public class NewTaxTypeStageController extends TaxTypeStageController
{
    @FXML
    @Override
    public void saveButtonAction(ActionEvent actionEvent)
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

    @Override
    public String getTitle()
    {
        return "Taxes type creator";
    }
}
