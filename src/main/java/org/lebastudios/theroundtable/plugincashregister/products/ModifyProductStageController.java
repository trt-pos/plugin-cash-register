package org.lebastudios.theroundtable.plugincashregister.products;

import javafx.fxml.FXML;
import lombok.SneakyThrows;
import org.lebastudios.theroundtable.apparience.ImageLoader;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegisterEvents;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.dialogs.ConfirmationTextDialogController;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.ui.IconButton;

public class ModifyProductStageController extends ProductStageController
{
    private final Product product;
    @FXML private IconButton deleteButton;

    public ModifyProductStageController(Product product)
    {
        this.product = product;
    }

    @Override
    public String getTitle()
    {
        return LangFileLoader.getTranslation("title.modifyproduct");
    }

    @Override
    @SneakyThrows
    @FXML
    protected void initialize()
    {
        super.initialize();

        productName.setText(product.getName());
        enabledProduct.setSelected(product.isEnabled());
        taxesIncluded.setSelected(product.getTaxesIncluded());
        price.setValue(product.getTaxesIncluded()
                ? product.getPrice()
                : product.getNotTaxedPrice()
        );
        taxes.getSelectionModel().select(product.getTaxType());
        mainCategory.setText(product.getSubCategory().getId().categoryName());
        subCategory.setText(product.getSubCategory().getId().name());
        mainButton.setText(LangFileLoader.getTranslation("word.save"));

        deleteButton.setVisible(true);
        deleteButton.setOnAction(_ -> deleteButtonAction());

        try
        {
            imgPath = product.getImgPath();
            productIcon.setImage(ImageLoader.getSavedImage(product.getImgPath()));
        }
        catch (Exception exception)
        {
            System.err.println("Error loading image");
            productIcon.setIconName("no-product-img.png");
        }
    }

    private void deleteButtonAction()
    {
        Database.getInstance().connectTransaction(session ->
        {
            var entity = session.get(Product.class, product.getId());

            session.remove(entity);
        });

        PluginCashRegisterEvents.onProductModify.invoke(product);

        close();
    }

    @FXML
    private void mainButtonAction()
    {
        if (!isProductDataValid()) return;

        Database.getInstance().connectTransaction(session ->
                saveProductInfo(session, session.get(Product.class, product.getId()))
        );

        PluginCashRegisterEvents.onProductModify.invoke(product);

        close();
    }
}
