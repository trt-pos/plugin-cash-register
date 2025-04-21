package org.lebastudios.theroundtable.plugincashregister.config;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.lebastudios.theroundtable.apparience.UIEffects;
import org.lebastudios.theroundtable.config.ConfigPaneController;
import org.lebastudios.theroundtable.config.NoConfigFile;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.dialogs.EntityFormDialogController;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.entities.TaxType;
import org.lebastudios.theroundtable.plugincashregister.forms.TaxTypeFormPaneController;
import org.lebastudios.theroundtable.ui.IconButton;

import java.math.BigDecimal;
import java.util.List;

public class TaxesTypesConfigPaneController extends ConfigPaneController<NoConfigFile>
{
    @FXML public VBox taxesTypesContainer;
    @FXML public IconButton plusButton;

    public TaxesTypesConfigPaneController()
    {
        super(new NoConfigFile(), LangFileLoader.getTranslation("plugincashregister.phrase.taxestypes"), "taxes.png");
    }

    @Override
    public void updateConfigData(NoConfigFile configData) {}

    @Override
    public void updateUI(NoConfigFile configData)
    {
        updateTaxesTypesContainer();
    }

    @Override
    public ValidationResult validate()
    {
        if (taxesTypesContainer.getChildren().isEmpty())
        {
            UIEffects.shakeNode(plusButton);
            return ValidationResult.invalid("You must have at least one tax type");
        }

        return ValidationResult.valid();
    }

    private void updateTaxesTypesContainer()
    {
        new Thread(() -> Database.getInstance().connectQuery(session ->
        {
            List<TaxType> results = session.createQuery("FROM TaxType", TaxType.class).getResultList();
            Platform.runLater(() ->
            {
                taxesTypesContainer.getChildren().clear();
                results.forEach(
                        taxesType -> taxesTypesContainer.getChildren().add(createTaxesTypeNode(taxesType))
                );
            });
        })).start();
    }

    @FXML
    public void plusButtonAction(ActionEvent actionEvent)
    {
        new EntityFormDialogController<>(new TaxTypeFormPaneController(), new TaxType())
                .setOwner(this.getStage())
                .instantiate(true);

        updateTaxesTypesContainer();
    }

    private Node createTaxesTypeNode(TaxType taxType)
    {
        HBox root = new HBox();
        root.setSpacing(25);
        root.getStyleClass().add("button");
        root.setAlignment(Pos.CENTER_LEFT);
        
        Label value = new Label(
                taxType.getValue().multiply(BigDecimal.valueOf(100)) + " %"
        );
        value.setStyle("-fx-font-weight: bold; -fx-font-size: 20px;");
        
        VBox namesAndDescription = new VBox();
        namesAndDescription.setSpacing(5);
        Label name = new Label(taxType.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        namesAndDescription.getChildren().add(name);
        Label description = new Label(taxType.getDescription());
        description.setWrapText(true);
        namesAndDescription.getChildren().add(description);

        HBox.setHgrow(namesAndDescription, Priority.ALWAYS);

        root.setOnMouseClicked(_ ->
        {
            var formController = new EntityFormDialogController<>(new TaxTypeFormPaneController(), taxType)
                    .setOwner(this.getStage());
            formController.getRoot();

            boolean isBeingUse = Database.getInstance().connectQuery(session ->
            {
                return !session.get(TaxType.class, taxType.getId()).getProducts().isEmpty();
            });
            
            formController.deleteButton.setDisable(isBeingUse);
            formController.instantiate(true);

            updateTaxesTypesContainer();
        });

        root.getChildren().addAll(value, namesAndDescription);
        
        return root;
    }
}
