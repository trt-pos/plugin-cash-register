package org.lebastudios.theroundtable.plugincashregister.products;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.util.StringConverter;
import lombok.SneakyThrows;
import org.controlsfx.control.textfield.TextFields;
import org.hibernate.Session;
import org.lebastudios.theroundtable.apparience.ImageLoader;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.plugincashregister.entities.Category;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.plugincashregister.entities.SubCategory;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.plugincashregister.entities.TaxType;
import org.lebastudios.theroundtable.ui.BigDecimalField;
import org.lebastudios.theroundtable.ui.IconView;
import org.lebastudios.theroundtable.ui.StageBuilder;

import java.io.File;
import java.net.URL;
import java.util.HashSet;

public abstract class ProductStageController extends StageController<ProductStageController>
{
    @FXML protected CheckBox enabledProduct;
    @FXML protected CheckBox taxesIncluded;
    @FXML protected TextField productName;
    @FXML protected TextField mainCategory;
    @FXML protected TextField subCategory;
    @FXML protected BigDecimalField price;
    @FXML protected ChoiceBox<TaxType> taxes;
    @FXML protected IconView productIcon;
    @FXML protected Button mainButton;

    protected String imgPath = "";

    @FXML
    @Override
    protected void initialize()
    {
        new Thread(() -> Database.getInstance().connectQuery(session ->
        {
            // Loading TaxesTypes from the database
            taxes.getItems().clear();
            taxes.getItems().addAll(session.createQuery("FROM TaxType", TaxType.class).list());
            taxes.setConverter(new StringConverter<>()
            {
                @Override
                public String toString(TaxType object) {return object == null ? "" : object.getName();}

                @Override
                public TaxType fromString(String string) {return null;}
            });

            // Loading Categories and SubCategories from the database
            var categories = session.createQuery("SELECT name FROM Category", String.class).list();
            var subCategories = session.createQuery("SELECT id.name FROM SubCategory", String.class).list();

            TextFields.bindAutoCompletion(mainCategory, new HashSet<>(categories));
            TextFields.bindAutoCompletion(subCategory, new HashSet<>(subCategories));
        })).start();
    }

    @SneakyThrows
    @FXML
    private void openImageSelector()
    {
        var result = ImageLoader.showImageChooser(this.getStage().getOwner());
        
        if (result == null) return;
        
        imgPath = result.imageFile().getAbsolutePath();
        productIcon.setImage(result.image());
    }

    @Override
    public final Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }

    @Override
    protected void customizeStageBuilder(StageBuilder stageBuilder)
    {
        stageBuilder.setModality(Modality.APPLICATION_MODAL)
                .setResizeable(false);
    }

    @Override
    public final URL getFXML()
    {
        return ProductStageController.class.getResource("newProductWindow.fxml");
    }

    protected boolean isProductDataValid()
    {
        productName.setText(productName.getText().trim());
        mainCategory.setText(mainCategory.getText().trim());
        subCategory.setText(subCategory.getText().trim());

        if (productName.getText().isBlank())
        {
            UIEffects.shakeNode(productName);
            return false;
        }

        if (mainCategory.getText().isBlank())
        {
            UIEffects.shakeNode(mainCategory);
            return false;
        }

        if (taxes.getSelectionModel().getSelectedItem() == null)
        {
            UIEffects.shakeNode(taxes);
            return false;
        }

        if (price.getValue() == null) 
        {
            UIEffects.shakeNode(price);
            return false;
        }
        
        return true;
    }

    protected void saveProductInfo(Session session, Product product)
    {
        insertDataIntoProduct(product);

        var category = session.byId(Category.class).load(mainCategory.getText());

        if (category == null)
        {
            category = new Category();
            category.setName(mainCategory.getText());
            session.persist(category);
        }

        var subCategoryId = new SubCategory.SubCategoryId(category.getName(), this.subCategory.getText());
        var subCategory = session.byId(SubCategory.class).load(subCategoryId);

        if (subCategory == null)
        {
            subCategory = new SubCategory();
            subCategory.setCategory(category);
            subCategory.setId(subCategoryId);
            session.persist(subCategory);
        }

        product.setSubCategory(subCategory);

        session.persist(product);
    }

    private void insertDataIntoProduct(Product product)
    {
        if (imgPath.startsWith(ImageLoader.SavedImagesDirectory()))
        {
            product.setImgPath(new File(this.imgPath).getAbsolutePath());
        }
        else
        {
            if (!imgPath.isBlank())
            {
                try
                {
                    product.setImgPath(ImageLoader.saveImageInSpecialFolder(
                            new File(this.imgPath)
                    ).getAbsolutePath());
                }
                catch (Exception exception)
                {
                    System.err.println("Error saving image");
                }
            }
        }

        product.setName(productName.getText());
        product.setPrice(price.getValue());
        product.setEnabled(enabledProduct.isSelected());
        product.setTaxesIncluded(taxesIncluded.isSelected());
        product.setTaxType(taxes.getSelectionModel().getSelectedItem());
    }
}
