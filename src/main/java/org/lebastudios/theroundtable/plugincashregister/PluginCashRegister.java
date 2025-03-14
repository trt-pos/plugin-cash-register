package org.lebastudios.theroundtable.plugincashregister;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import lombok.SneakyThrows;
import org.lebastudios.theroundtable.MainStageController;
import org.lebastudios.theroundtable.config.SettingsItem;
import org.lebastudios.theroundtable.config.data.JSONFile;
import org.lebastudios.theroundtable.dialogs.InformationTextDialogController;
import org.lebastudios.theroundtable.events.AppLifeCicleEvents;
import org.lebastudios.theroundtable.events.Event1;
import org.lebastudios.theroundtable.events.Event2;
import org.lebastudios.theroundtable.events.PluginEvents;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.cash.CashRegister;
import org.lebastudios.theroundtable.plugincashregister.cash.CashRegisterPaneController;
import org.lebastudios.theroundtable.plugincashregister.config.ReceiptPrintingConfigPaneController;
import org.lebastudios.theroundtable.plugincashregister.config.TaxesTypesConfigPaneController;
import org.lebastudios.theroundtable.plugincashregister.config.data.CashRegisterStateData;
import org.lebastudios.theroundtable.plugincashregister.entities.*;
import org.lebastudios.theroundtable.plugincashregister.products.ModifyProductStageController;
import org.lebastudios.theroundtable.plugincashregister.products.ProductPaneController;
import org.lebastudios.theroundtable.plugincashregister.products.ProductsUIController;
import org.lebastudios.theroundtable.plugins.IPlugin;
import org.lebastudios.theroundtable.ui.IconButton;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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

        AppLifeCicleEvents.OnAppCloseRequest.addListener(windowEvent ->
        {
            if (windowEvent.isConsumed()) return;

            var cashRegisterState = new JSONFile<>(CashRegisterStateData.class).get();
            if (cashRegisterState.open)
            {
                windowEvent.consume();
                new InformationTextDialogController(
                        LangFileLoader.getTranslation("textblock.needtoclosethecashregister")
                ).instantiate();
            }
        });

        PluginCashRegisterEvents.showOrder.addListener(order ->
        {
            var cashRegisterState = new JSONFile<>(CashRegisterStateData.class).get();

            if (!cashRegisterState.open)
            {
                new InformationTextDialogController(LangFileLoader.getTranslation("phrase.cashregisterisclosed"))
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
            ProductPaneController.onAction = product -> new ModifyProductStageController(product).instantiate();
            MainStageController.getInstance().setCentralNode(new ProductsUIController(true));
        });

        return newButton;
    }

    @Override
    public TreeItem<SettingsItem> getSettingsRootTreeItem()
    {
        var cashRegisterConfigSection = new TreeItem<>(
                new SettingsItem(LangFileLoader.getTranslation("word.cashregister"),
                        "cash-register.png", null)
        );
        cashRegisterConfigSection.setExpanded(false);

        cashRegisterConfigSection.getChildren().add(
                new TreeItem<>(new SettingsItem(LangFileLoader.getTranslation("phrase.receiptprinterconfig"),
                        "print.png", new ReceiptPrintingConfigPaneController())
                )
        );

        cashRegisterConfigSection.getChildren().add(
                new TreeItem<>(new SettingsItem(LangFileLoader.getTranslation("phrase.taxestypes"),
                        "taxes.png", new TaxesTypesConfigPaneController())
                )
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

    public void version1(Connection conn) throws SQLException
    {
        Statement statement = conn.createStatement();

        statement.addBatch("""
                create table cr_receipt
                (
                    id                     integer,
                    client_identifier      varchar(255),
                    client_name            varchar(255),
                    employee_name          varchar(255),
                    payment_amount         numeric(38, 2) not null,
                    payment_method         varchar(255)   not null,
                    table_name             varchar(255)   not null,
                    taxes_amount           numeric(38, 2) not null,
                    unknown_products_value numeric(38, 2) not null,
                    constraint pk_receipt primary key (id)
                );""");

        statement.addBatch("""
                create table cr_product_receipt
                (
                    id             integer,
                    product_name   varchar(255)   not null,
                    product_value  numeric(38, 2) not null,
                    quantity       numeric(38, 2) not null,
                    taxes          numeric(38, 2),
                    taxes_included boolean        not null,
                    total_value    numeric(38, 2) not null,
                    receipt_id     integer,
                    constraint receipt_line foreign key (receipt_id) references cr_receipt (id),
                    constraint pk_product_receipt primary key (id)
                
                );""");

        statement.addBatch("""
                create table cr_transaction
                (
                    id          integer,
                    amount      numeric(38, 2) not null,
                    date        timestamp      not null,
                    description text           not null,
                    receipt_id  integer,
                    constraint transaction_receipt foreign key (receipt_id) references cr_receipt (id),
                    constraint u_transactrion_receipt_id unique (receipt_id),
                    constraint pk_transaction primary key (id)
                );""");

        statement.addBatch("""
                create table pr_category
                (
                    name varchar(255) not null,
                    constraint pk_category primary key (name)
                );""");

        statement.addBatch("""
                create table pr_sub_category
                (
                    category_name varchar(255) not null,
                    name          varchar(255) not null,
                    constraint pk_sub_category primary key (category_name, name)
                );""");

        statement.addBatch("""
                create table pr_product
                (
                    id                integer,
                    enabled           boolean         not null,
                    img_path          text            not null,
                    name              varchar(255)    not null,
                    price             numeric(38, 2)  not null,
                    taxes             numeric(38, 2),
                    taxes_included    boolean         not null,
                    category_name     varchar(255),
                    sub_category_name varchar(255),
                    taxes_type        integer,
                    constraint fk_prduct_subcategory foreign key (category_name, sub_category_name) references pr_sub_category (category_name, name),
                    constraint pk_product primary key (id)
                );""");

        statement.addBatch("""
                create table pr_tax_type
                (
                    id          integer,
                    description varchar(255),
                    name        varchar(255) not null,
                    value       numeric(38, 2),
                    constraint u_tax_type_name unique (name),
                    constraint pk_tax_type primary key (id)
                );""");

        statement.executeBatch();
    }

    public void version2(Connection conn) throws SQLException
    {
        Statement statement = conn.createStatement();

        statement.addBatch("""
                alter table cr_receipt
                    drop column unknown_products_value;""");

        statement.executeBatch();
    }

    public void version3(Connection conn) throws SQLException
    {
        Statement statement = conn.createStatement();

        statement.addBatch("""
                update cr_receipt set payment_method = 'CASH' where payment_method = 'Contado' or payment_method = 'Cash';""");

        statement.addBatch("""
                update cr_receipt set payment_method = 'CARD' where payment_method = 'Tarjeta' or payment_method = 'Card';""");

        statement.executeBatch();
    }

    public void version4(Connection conn) throws SQLException
    {
        Statement stat = conn.createStatement();
        // Adding a new table to store the rectification of a receipt
        stat.addBatch("""
                create table cr_receipt_modification
                (
                    id                integer,
                    new_receipt_id integer not null,
                    reason            text,
                    constraint pk_cr_receipt_modification primary key (id),
                    constraint fk_cr_receipt_modification_receipt foreign key (id) references cr_receipt(id),
                    constraint fk_cr_receipt_modification_modified_receipt foreign key (new_receipt_id) references cr_receipt (id)
                );
                """);
        stat.executeBatch();
    }

    public void version5(Connection conn) throws SQLException
    {
        Statement stat = conn.createStatement();
        // Adding a property to the receipt to store the status of the receipt

        stat.addBatch("""
                alter table cr_receipt
                    add column status varchar(255) not null default 'DEFAULT';
                """);

        // Adding a trigger to set the status to 'MODIFIED' when the receipt is inserted into cr_receipt_modification
        stat.addBatch("""
                create trigger cr_receipt_modification_insert
                    after insert on cr_receipt_modification
                    for each row
                    begin
                    update cr_receipt
                    set status = 'MODIFIED'
                    where id = new.id;
                    end
                """);

        stat.executeBatch();
    }
}
