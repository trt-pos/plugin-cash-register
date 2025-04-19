package org.lebastudios.theroundtable.plugincashregister;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import lombok.SneakyThrows;
import org.lebastudios.theroundtable.MainStageController;
import org.lebastudios.theroundtable.config.SettingsItem;
import org.lebastudios.theroundtable.dialogs.EntityFormDialogController;
import org.lebastudios.theroundtable.dialogs.InformationTextDialogController;
import org.lebastudios.theroundtable.events.Event1;
import org.lebastudios.theroundtable.events.Event2;
import org.lebastudios.theroundtable.events.PluginEvents;
import org.lebastudios.theroundtable.fxml2java.CompileFxml;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.cash.CashRegister;
import org.lebastudios.theroundtable.plugincashregister.cash.CashRegisterPaneController;
import org.lebastudios.theroundtable.plugincashregister.config.CashRegisterStateData;
import org.lebastudios.theroundtable.plugincashregister.config.ReceiptPrintingConfigPaneController;
import org.lebastudios.theroundtable.plugincashregister.config.TaxesTypesConfigPaneController;
import org.lebastudios.theroundtable.plugincashregister.entities.*;
import org.lebastudios.theroundtable.plugincashregister.products.ProductFormPaneController;
import org.lebastudios.theroundtable.plugincashregister.products.ProductPaneController;
import org.lebastudios.theroundtable.plugincashregister.products.ProductsUIController;
import org.lebastudios.theroundtable.plugins.IPlugin;
import org.lebastudios.theroundtable.ui.IconButton;

import java.util.ArrayList;
import java.util.List;

@CompileFxml(
        directories = {
                "org/lebastudios/theroundtable/plugincashregister/cash",
                "org/lebastudios/theroundtable/plugincashregister/config",
                "org/lebastudios/theroundtable/plugincashregister/products",
        }
)
public class PluginCashRegister implements IPlugin
{
    private static PluginCashRegister instance;
    private static final int DATABASE_VERSION = 5;

    public static PluginCashRegister getInstance()
    {
        if (instance == null) throw new IllegalStateException("This plugin has to be instantiated");

        return instance;
    }

    @SneakyThrows
    @FXML
    @Override
    public void initialize()
    {
        instance = this;

        PluginCashRegisterEvents.showOrder.addListener(order ->
        {
            var cashRegisterState = new CashRegisterStateData().load();

            if (!cashRegisterState.open)
            {
                new InformationTextDialogController(LangFileLoader.getTranslation("plugincashregister.phrase.cashregisterisclosed"))
                        .instantiate(true);
                return;
            }

            CashRegister.getInstance().swapOrder(order);
            CashRegister.getInstance().showInterface();
        });

        PluginEvents.registerPluginEvent(
                "plugin-cash-register",
                "showOrder",
                Event1.class.getMethod("invoke", Object.class),
                Event1.class.getMethod("addListener", Object.class),
                PluginCashRegisterEvents.showOrder
        );

        PluginEvents.registerPluginEvent(
                "plugin-cash-register",
                "onProductModify",
                Event1.class.getMethod("invoke", Object.class),
                Event1.class.getMethod("addListener", Object.class),
                PluginCashRegisterEvents.onProductModify
        );

        PluginEvents.registerPluginEvent(
                "plugin-cash-register",
                "onReceiptEmitted",
                Event1.class.getMethod("invoke", Object.class),
                Event1.class.getMethod("addListener", Object.class),
                PluginCashRegisterEvents.onReceiptEmitted
        );

        PluginEvents.registerPluginEvent(
                "plugin-cash-register",
                "onRequestReceiptBillNumber",
                Event2.class.getMethod("invoke", Object.class, Object.class),
                Event2.class.getMethod("addListener", Object.class),
                PluginCashRegisterEvents.onRequestReceiptBillNumber
        );

        PluginEvents.registerPluginEvent(
                "plugin-cash-register",
                "onRequestNewReceiptBillNumber",
                Event2.class.getMethod("invoke", Object.class, Object.class),
                Event2.class.getMethod("addListener", Object.class),
                PluginCashRegisterEvents.onRequestNewReceiptBillNumber
        );

        PluginEvents.registerPluginEvent(
                "plugin-cash-register",
                "onRequestNewRectificationBillNumber",
                Event2.class.getMethod("invoke", Object.class, Object.class),
                Event2.class.getMethod("addListener", Object.class),
                PluginCashRegisterEvents.onRequestNewRectificationBillNumber
        );

        PluginEvents.registerPluginEvent(
                "plugin-cash-register",
                "onReceiptBilled",
                Event2.class.getMethod("invoke", Object.class, Object.class),
                Event2.class.getMethod("addListener", Object.class),
                PluginCashRegisterEvents.onReceiptBilled
        );

        PluginEvents.registerPluginEvent(
                "plugin-cash-register",
                "onModifiedReceiptBilled",
                Event2.class.getMethod("invoke", Object.class, Object.class),
                Event2.class.getMethod("addListener", Object.class),
                PluginCashRegisterEvents.onModifiedReceiptBilled
        );
    }

    @Override
    public List<Button> getRightButtons()
    {
        var buttonsList = new ArrayList<Button>();

        buttonsList.add(loadCashRegestryButton());
        buttonsList.add(loadProductsButton());

        return buttonsList;
    }

    public static Button loadCashRegestryButton()
    {
        var newButton = new IconButton("cash-register.png");

        newButton.setOnAction(_ -> CashRegisterPaneController.showInterface());

        return newButton;
    }

    public static Button loadProductsButton()
    {
        var newButton = new IconButton("restaurant-menu.png");

        newButton.setOnMouseClicked(_ ->
        {
            ProductPaneController.onAction = product -> new EntityFormDialogController<>(
                    new ProductFormPaneController(), 
                    product
            ).setOwner(MainStageController.getInstance().getStage()).instantiate();
            
            MainStageController.getInstance().setCentralNode(new ProductsUIController(true));
        });

        return newButton;
    }

    @Override
    public TreeItem<SettingsItem> getSettingsRootTreeItem()
    {
        var cashRegisterConfigSection = new TreeItem<>(
                new SettingsItem(LangFileLoader.getTranslation("plugincashregister.word.cashregister"),
                        "cash-register.png", null)
        );
        cashRegisterConfigSection.setExpanded(false);

        cashRegisterConfigSection.getChildren().add(
                new TreeItem<>(new SettingsItem(new ReceiptPrintingConfigPaneController())
                )
        );

        cashRegisterConfigSection.getChildren().add(
                new TreeItem<>(new SettingsItem(new TaxesTypesConfigPaneController()))
        );

        return cashRegisterConfigSection;
    }

    @Override
    public List<Class<?>> getPluginEntities()
    {
        List<Class<?>> entities = new ArrayList<>();

        entities.add(Category.class);
        entities.add(SubCategory.class);
        entities.add(Product.class);
        entities.add(TaxType.class);

        entities.add(Product_Receipt.class);

        entities.add(Receipt.class);
        entities.add(Transaction.class);
        entities.add(ReceiptModification.class);

        return entities;
    }

    @Override
    public int getDatabaseVersion()
    {
        return DATABASE_VERSION;
    }
}
