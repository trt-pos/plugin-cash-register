package org.lebastudios.theroundtableplugins.cr.products;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.lebastudios.theroundtable.apparience.ImageManager;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtableplugins.cr.entities.Product;

import java.util.function.Consumer;

public class ProductPaneController extends PaneController<ProductPaneController>
{
    public static Consumer<Product> onAction;

    private final Product product;

    @FXML public Label label;
    @FXML public ImageView imageView;

    public ProductPaneController(Product product)
    {
        this.product = product;
    }

    @FXML @Override protected void initialize()
    {
        String stringBuilder = product.getName() + "\n" 
                + BigDecimalOperations.toString(product.getPrice()) + " €";
        
        label.setText(stringBuilder);

        imageView.setImage(ImageManager.getInstance().get(product.getImgPath(), ImageManager.ImageType.PERSISTED));
        
        getRoot().setOnMouseClicked(_ -> onAction.accept(product));
    }}
