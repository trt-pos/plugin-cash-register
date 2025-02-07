package org.lebastudios.theroundtable.plugincashregister.products;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.lebastudios.theroundtable.apparience.ImageLoader;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;

import java.net.URL;
import java.util.function.Consumer;

public class ProductPaneController extends PaneController<ProductPaneController>
{
    public static Consumer<Product> onAction;

    private final Product product;

    @FXML private Label label;
    @FXML private ImageView imageView;

    public ProductPaneController(Product product)
    {
        this.product = product;
    }

    @FXML @Override protected void initialize()
    {
        String stringBuilder = product.getName() + "\n" 
                + BigDecimalOperations.toString(product.getPrice()) + " €";
        
        label.setText(stringBuilder);
        
        new Thread(() ->
        {
            Image img = ImageLoader.getSavedImage(product.getImgPath());

            Platform.runLater(() -> imageView.setImage(img));
        }).start();
        
        getRoot().setOnMouseClicked(_ -> onAction.accept(product));
    }

    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }

    @Override
    public URL getFXML()
    {
        return ProductPaneController.class.getResource("productPane.fxml");
    }
}
