package org.lebastudios.theroundtableplugins.cr;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import lombok.SneakyThrows;
import net.sf.jasperreports.engine.JasperPrint;

import org.lebastudios.theroundtable.MainStageController;
import org.lebastudios.theroundtable.accounts.AccountManager;
import org.lebastudios.theroundtable.config.SettingsItem;
import org.lebastudios.theroundtable.dialogs.EntityFormDialogController;
import org.lebastudios.theroundtable.dialogs.InformationTextDialogController;
import org.lebastudios.theroundtable.fxml2java.CompileFxml;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtableplugins.cr.cash.CashRegister;
import org.lebastudios.theroundtableplugins.cr.cash.CashRegisterPaneController;
import org.lebastudios.theroundtableplugins.cr.config.ReceiptPrintingConfigPaneController;
import org.lebastudios.theroundtableplugins.cr.config.TaxesTypesConfigPaneController;
import org.lebastudios.theroundtableplugins.cr.entities.*;
import org.lebastudios.theroundtableplugins.cr.forms.ProductFormPaneController;
import org.lebastudios.theroundtableplugins.cr.products.ProductPaneController;
import org.lebastudios.theroundtableplugins.cr.products.ProductsUIController;
import org.lebastudios.theroundtableplugins.cr.reports.ReceiptReportGenerator;
import org.lebastudios.theroundtableplugins.cr.sessions.CashSessionsPaneController;
import org.lebastudios.theroundtable.plugins.IPlugin;
import org.lebastudios.theroundtable.reports.ReportPaneController;
import org.lebastudios.theroundtable.components.IconButton;
import org.lebastudios.theroundtable.components.LabeledIconButton;

import java.util.ArrayList;
import java.util.List;

@CompileFxml(
        directories = {
                "org/lebastudios/theroundtableplugins/cr/cash",
                "org/lebastudios/theroundtableplugins/cr/config",
                "org/lebastudios/theroundtableplugins/cr/forms",
                "org/lebastudios/theroundtableplugins/cr/products",
                "org/lebastudios/theroundtableplugins/cr/sessions",
        }
)
public class PluginCashRegister implements IPlugin
{
    private static PluginCashRegister instance;
    private static final int DATABASE_VERSION = 6;

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

        JasperPrint print = new ReceiptReportGenerator().generate(new Receipt());
        Node _ = new ReportPaneController(print).getRoot();
        
        PluginCashRegisterEvents.showOrder.addListener(order ->
        {
            if (CashSession.getActualSession() == null)
            {
                new InformationTextDialogController(
                        Translator.getInstance().t("cr:phrase.cashregisterisclosed")
                ).instantiate(true);
                return;
            }

            CashRegister.getInstance().swapOrder(order);
            CashRegister.getInstance().showInterface();
        });
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
        var newButton = new IconButton("cr:cash-register.png");

        newButton.setOnAction(_ -> CashRegisterPaneController.showInterface());

        return newButton;
    }

    public static Button loadProductsButton()
    {
        var newButton = new IconButton("cr:restaurant-menu.png");

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
                new SettingsItem(Translator.getInstance().t("cr:word.cashregister"),
                        "cr:cash-register.png", null)
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
    public List<LabeledIconButton> getHomeButtons()
    {
        ArrayList<LabeledIconButton> buttons = new ArrayList<>();
        
        if (AccountManager.getInstance().isAccountAdmin())
        {
            buttons.add(new LabeledIconButton(
                    Translator.getInstance().t("cr:word.sessions"),
                    "cr:cash-sessions.png",
                    _ -> MainStageController.getInstance().setCentralNode(new CashSessionsPaneController())
            ));
        }
        
        return buttons;
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
        
        entities.add(CashSession.class);

        return entities;
    }

    @Override
    public int getDatabaseVersion()
    {
        return DATABASE_VERSION;
    }
}
