package org.lebastudios.theroundtable.plugincashregister.products;

import javafx.fxml.FXML;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegisterEvents;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.locale.LangFileLoader;

public class NewProductStageController extends ProductStageController
{
    @Override
    public String getTitle()
    {
        return LangFileLoader.getTranslation("title.createnewproduct");
    }

    @Override
    @FXML
    protected void initialize()
    {
        super.initialize();

        productIcon.setIconName("no-product-img.png");
    }

    @FXML
    private void mainButtonAction()
    {
        if (!isProductDataValid()) return;

        // Create new product
        Database.getInstance().connectTransaction(session -> saveProductInfo(session, new Product()));
        
        PluginCashRegisterEvents.onProductModify.invoke(null);

        close();
    }
}
