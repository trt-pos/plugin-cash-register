package org.lebastudios.theroundtable.plugincashregister.forms;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import lombok.SneakyThrows;
import org.controlsfx.control.textfield.TextFields;
import org.lebastudios.theroundtable.TheRoundTableApplication;
import org.lebastudios.theroundtable.apparience.ImageLoader;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.config.RequestConfigStageController;
import org.lebastudios.theroundtable.controllers.FormPaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegisterEvents;
import org.lebastudios.theroundtable.plugincashregister.config.TaxesTypesConfigPaneController;
import org.lebastudios.theroundtable.plugincashregister.entities.Category;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.plugincashregister.entities.SubCategory;
import org.lebastudios.theroundtable.plugincashregister.entities.TaxType;
import org.lebastudios.theroundtable.ui.BigDecimalField;
import org.lebastudios.theroundtable.ui.IconView;

import java.io.File;
import java.util.HashSet;
import java.util.List;

public class ProductFormPaneController extends FormPaneController<Product>
{
    @FXML public CheckBox enabledProduct;
    @FXML public CheckBox taxesIncluded;
    @FXML public TextField productName;
    @FXML public TextField mainCategory;
    @FXML public TextField subCategory;
    @FXML public BigDecimalField price;
    @FXML public ChoiceBox<TaxType> taxes;
    @FXML public IconView productIcon;

    protected String imgPath = "";

    @FXML
    @Override
    protected void initialize()
    {
        Database.getInstance().connectQuery(session ->
        {
            taxes.getItems().clear();
            taxes.setConverter(new StringConverter<>()
            {
                @Override
                public String toString(TaxType object) {return object == null ? "" : object.getName();}

                @Override
                public TaxType fromString(String string) {return null;}
            });

            List<TaxType> taxesList = session.createQuery("FROM TaxType", TaxType.class).list();

            if (taxesList.isEmpty())
            {
                TheRoundTableApplication.executeInFxThreadAndWait(() ->
                {
                    new RequestConfigStageController(new TaxesTypesConfigPaneController())
                            .setOwner(this.getStage())
                            .setTitle(LangFileLoader.getTranslation("plugincashregister.reqconfig.notaxtypes"))
                            .instantiate(true);
                });

                taxesList = session.createQuery("FROM TaxType", TaxType.class).list();
            }
            taxes.getItems().addAll(taxesList);
            taxes.selectionModelProperty().get().select(0);
        });

        new Thread(() -> Database.getInstance().connectQuery(session ->
        {
            var categories = session.createQuery("SELECT name FROM Category", String.class).list();
            var subCategories = session.createQuery("SELECT id.name FROM SubCategory", String.class).list();

            TextFields.bindAutoCompletion(mainCategory, new HashSet<>(categories));
            TextFields.bindAutoCompletion(subCategory, new HashSet<>(subCategories));
        })).start();

        super.initialize();
    }

    @Override
    protected void updateUI(Product product)
    {
        productName.setText(product.getName());
        enabledProduct.setSelected(product.isEnabled());
        taxesIncluded.setSelected(product.getTaxesIncluded());
        price.setValue(product.getTaxesIncluded()
                ? product.getPrice()
                : product.getNotTaxedPrice()
        );

        if (product.getTaxType() != null)
        {
            taxes.getSelectionModel().select(product.getTaxType());
        }

        SubCategory productSubCategory = product.getSubCategory();
        if (productSubCategory != null)
        {
            mainCategory.setText(productSubCategory.getId().categoryName());
            subCategory.setText(productSubCategory.getId().name());
        }

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

    @Override
    public final boolean validate()
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

    @Override
    public Product buildObject(Product product)
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

        Category category = new Category();
        category.setName(mainCategory.getText());

        SubCategory.SubCategoryId subCategoryId =
                new SubCategory.SubCategoryId(category.getName(), this.subCategory.getText());
        SubCategory subCategory = new SubCategory();
        subCategory.setCategory(category);
        subCategory.setId(subCategoryId);

        product.setSubCategory(subCategory);

        return product;
    }

    @Override
    public boolean onDeleteAction(Product product)
    {
        PluginCashRegisterEvents.onProductModify.invoke(product);

        Database.getInstance().connectTransaction(session ->
        {
            session.createMutationQuery("delete from SubCategory c where size(c.products) = 0").executeUpdate();
            session.createMutationQuery("delete from Category c where size(c.subCategories) = 0").executeUpdate();
        });

        return true;
    }

    @Override
    public boolean onSaveAction(Product product)
    {
        PluginCashRegisterEvents.onProductModify.invoke(product);
        return true;
    }

    @SneakyThrows
    @FXML
    public void openImageSelector(ActionEvent actionEvent)
    {
        var result = ImageLoader.showImageChooser(this.getStage());

        if (result == null) return;

        imgPath = result.imageFile().getAbsolutePath();
        productIcon.setImage(result.image());
    }
}
