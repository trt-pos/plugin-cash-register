package org.lebastudios.theroundtable.plugincashregister.cash;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lombok.Getter;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class OrderItemLabelController extends PaneController<OrderItemLabelController>
{
    public static boolean editMode = false;
    private final OrderItem orderItem;
    private final Map<Label, Boolean> hasDefaultText = new HashMap<>();
    
    @FXML @Getter public Label unitPriceLabel;
    @FXML @Getter private Label quantityLabel;
    @FXML @Getter private Label productNameLabel;
    @FXML @Getter private Label totalPriceLabel;
    @Getter private Label actualEditting;

    public OrderItemLabelController(OrderItem orderItem)
    {
        this.orderItem = orderItem;
    }

    @FXML @Override protected void initialize()
    {
        hasDefaultText.put(quantityLabel, true);
        hasDefaultText.put(unitPriceLabel, true);

        quantityLabel.setText(orderItem.getQuantity().toString());
        productNameLabel.setText(orderItem.intoProduct().getName());
        unitPriceLabel.setText(BigDecimalOperations.toString(orderItem.intoProduct().getPrice()));
        totalPriceLabel.setText(BigDecimalOperations.toString(orderItem.getTotalPrice()));
        
        quantityLabel.setOnMouseClicked(_ -> setActualEditting(quantityLabel));
        unitPriceLabel.setOnMouseClicked(_ -> setActualEditting(unitPriceLabel));
    }

    public Product getRepresentingProduct()
    {
        return orderItem.intoProduct();
    }
    
    public BigDecimal getQuantity()
    {
        return orderItem.getQuantity();
    }
    
    public void edit(String textToConcat)
    {
        if (actualEditting == null) return;

        if (hasDefaultText.get(actualEditting))
        {
            actualEditting.setText("");
        }

        var text = actualEditting.getText() + textToConcat;

        try
        {
            if (text.equals(".")) text = "0.";
            
            new BigDecimal(text);
            actualEditting.setText(text);
        }
        catch (NumberFormatException exception)
        {
            UIEffects.shakeNode(actualEditting);
            return;
        }

        hasDefaultText.put(actualEditting, false);
    }

    public void setActualEditting(Label newActualEditting)
    {
        if (actualEditting != null) actualEditting.setStyle("-fx-underline: false;");
        this.actualEditting = newActualEditting;
        if (newActualEditting != null) actualEditting.setStyle("-fx-underline: true;");
    }

    public void removeLast()
    {
        String text;

        if (actualEditting == null) return;
        
        if (actualEditting.getText().length() > 1)
        {
            text = actualEditting.getText().substring(0, actualEditting.getText().length() - 1);
        }
        else
        {
            text = actualEditting.equals(quantityLabel)
                    ? "1"
                    : "0.00";
            hasDefaultText.put(actualEditting, true);
        }

        actualEditting.setText(text);
    }

    public void invertNumber() 
    {
        if (actualEditting == null) return;

        var text = actualEditting.getText();

        if (text.startsWith("-"))
        {
            text = text.substring(1);
        }
        else
        {
            text = "-" + text;
        }

        actualEditting.setText(text);
        
        hasDefaultText.put(actualEditting, false);
    }

    public void submitEditting()
    {
        try
        {
            new BigDecimal(quantityLabel.getText());
        }
        catch (NumberFormatException exception)
        {
            UIEffects.shakeNode(quantityLabel);
            return;
        }
        
        try
        {
            new BigDecimal(unitPriceLabel.getText());
        }
        catch (NumberFormatException exception)
        {
            UIEffects.shakeNode(unitPriceLabel);
            return;
        }
        
        this.orderItem.setQuantity(new BigDecimal(quantityLabel.getText()));
        this.orderItem.getBaseProduct().setTaxedPrice(new BigDecimal(unitPriceLabel.getText()));
        
        checkRemoveOrderItemCondition();
        
        CashRegister.getInstance().getActualOrder().collapseEqualItems();
        
        CashRegister.onActualOrderModified.invoke();
        setActualEditting(null);
    }

    @FXML
    private void removeOneButton()
    {
        orderItem.setQuantity(orderItem.getQuantity().subtract(BigDecimal.ONE));
        
        checkRemoveOrderItemCondition();
        
        CashRegister.onActualOrderModified.invoke();
    }

    private void checkRemoveOrderItemCondition()
    {
        if (orderItem.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
        {
            CashRegister.getInstance().getActualOrder().getOrderItems().remove(orderItem);
        }
    }
    
    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }
}
