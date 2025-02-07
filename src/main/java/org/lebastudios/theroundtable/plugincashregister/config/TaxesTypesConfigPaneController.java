package org.lebastudios.theroundtable.plugincashregister.config;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.lebastudios.theroundtable.config.SettingsPaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.dialogs.InformationTextDialogController;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.plugincashregister.entities.TaxType;
import org.lebastudios.theroundtable.plugincashregister.products.ModifyTaxTypeStageController;
import org.lebastudios.theroundtable.plugincashregister.products.TaxTypeCreatorStageController;
import org.lebastudios.theroundtable.ui.IconButton;

import java.util.List;

public class TaxesTypesConfigPaneController extends SettingsPaneController
{
    @FXML private VBox taxesTypesContainer;

    @Override
    protected void initialize()
    {
        updateTaxesTypesContainer();
    }

    private void updateTaxesTypesContainer()
    {
        taxesTypesContainer.getChildren().clear();

        new Thread(() -> Database.getInstance().connectQuery(session ->
        {
            List<TaxType> results = session.createQuery("FROM TaxType", TaxType.class).getResultList();
            Platform.runLater(() ->
                    results.forEach(
                            taxesType -> taxesTypesContainer.getChildren().add(createTaxesTypeNode(taxesType))
                    )
            );
        })).start();
    }

    @Override
    public void apply() {}

    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }

    @FXML
    private void plusButtonAction()
    {
        new TaxTypeCreatorStageController().instantiate(true);
        updateTaxesTypesContainer();
    }

    private Node createTaxesTypeNode(TaxType taxType)
    {
        HBox root = new HBox();
        root.spacingProperty().setValue(10);
        root.getStyleClass().add("button");

        VBox left = new VBox();
        left.setSpacing(5);
        Label name = new Label(taxType.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        left.getChildren().add(name);
        Label description = new Label(taxType.getDescription());
        description.setWrapText(true);
        left.getChildren().add(description);

        HBox.setHgrow(left, Priority.ALWAYS);

        IconButton edit = new IconButton("edit.png");
        edit.setOnAction(_ ->
        {
            new ModifyTaxTypeStageController(taxType).instantiate(true);
            updateTaxesTypesContainer();
        });

        final var delete = getDeleteButton(taxType);

        root.getChildren().addAll(left, edit, delete);
        
        return root;
    }

    private IconButton getDeleteButton(TaxType taxType)
    {
        IconButton delete = new IconButton("delete.png");
        delete.setOnAction(_ -> 
        {
            Database.getInstance().connectTransaction(session -> 
            {
                TaxType instance = session.get(TaxType.class, taxType.getId());
                if (instance == null)
                {
                    new InformationTextDialogController("Something went wrong. Please try again.").instantiate();
                    return;
                }
                
                if (!instance.getProducts().isEmpty()) 
                {
                    new InformationTextDialogController(
                            LangFileLoader.getTranslation("textblock.taxestypeisbeingused")
                    ).instantiate();
                    return;
                }
                
                session.remove(instance);
            });

            updateTaxesTypesContainer();
        });
        return delete;
    }
}
