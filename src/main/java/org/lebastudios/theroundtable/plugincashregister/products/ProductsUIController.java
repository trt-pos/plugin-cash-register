package org.lebastudios.theroundtable.plugincashregister.products;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.hibernate.query.Query;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.events.IEventMethod1;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegisterEvents;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.ui.LoadingPaneController;
import org.lebastudios.theroundtable.ui.SearchBox;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class ProductsUIController extends PaneController<ProductsUIController>
{
    @FXML private SearchBox searchBox;
    @FXML private TabPane mainTabPane;
    private final boolean showDiabledProducts;

    private final IEventMethod1<Product> onProductModifyListener = _ -> loadProducts(searchBox.getText());
    
    private static final Map<String, Map<String, List<Product>>> products = new LinkedHashMap<>();

    public ProductsUIController(boolean showDisabled)
    {
        this.showDiabledProducts = showDisabled;
    }

    @FXML
    @Override
    public void initialize()
    {
        PluginCashRegisterEvents.onProductModify.addWeakListener(onProductModifyListener);

        searchBox.setOnSearch(this::searchProducts);

        loadProducts("");
    }

    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }

    @Override
    public URL getFXML()
    {
        return ProductsUIController.class.getResource("productsUI.fxml");
    }

    private void showProducts(String filterText)
    {
        var selectedCategory = mainTabPane.getSelectionModel().getSelectedItem();

        mainTabPane.getTabs().clear();

        products.forEach((mainCategory, subCategories) ->
        {
            var tabName = mainCategory.isBlank()
                    ? LangFileLoader.getTranslation("word.generic")
                    : mainCategory;

            Tab tab = new Tab(tabName);
            
            tab.setOnSelectionChanged(_ ->
            {
                if (tab.isSelected())
                {
                    new Thread(() ->
                    {
                        final var loadingNode = new LoadingPaneController().getRoot();
                        Platform.runLater(() -> tab.setContent(loadingNode));
                        final var node = generateSubCategoriesNode(filterText, subCategories);
                        Platform.runLater(() -> tab.setContent(node));
                    }).start();
                }
                else
                {
                    tab.setContent(null);
                }
            });

            mainTabPane.getTabs().add(tab);
        });

        if (selectedCategory != null)
        {
            Tab tab = mainTabPane.getTabs().stream()
                    .filter(t -> t.getText().equals(selectedCategory.getText()))
                    .findFirst()
                    .orElse(mainTabPane.getTabs().getFirst());
            
            mainTabPane.getSelectionModel().select(tab);
        }
    }

    private Node generateSubCategoriesNode(String filterText, Map<String, List<Product>> subCategories)
    {
        VBox subcategoriesContainer = new VBox(10);
        subcategoriesContainer.setPadding(new Insets(10, 10, 0, 20));

        ScrollPane scrollPane = new ScrollPane(subcategoriesContainer);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        subCategories.forEach((subCategory, products) ->
        {
            VBox subCategoryPane = new VBox(10);
            Label subCategoryLabel = new Label(subCategory.isBlank()
                    ? LangFileLoader.getTranslation("word.generic")
                    : subCategory
            );
            subCategoryLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            FlowPane productsFlowPane = new FlowPane(5, 5);
            subCategoryPane.getChildren().addAll(subCategoryLabel, productsFlowPane);

            products.stream()
                    .filter(product -> product.getName().toLowerCase().contains(filterText.toLowerCase()))
                    .forEach(product -> productsFlowPane.getChildren()
                            .add(new ProductPaneController(product).getRoot()));

            if (!productsFlowPane.getChildren().isEmpty())
            {
                Platform.runLater(() -> subcategoriesContainer.getChildren().add(subCategoryPane));
            }
        });

        return scrollPane;
    }

    private synchronized void loadProducts(String filterText)
    {
        List<Product> productsList = new ArrayList<>();

        Database.getInstance().connectQuery(session ->
        {
            String hql = "FROM Product p WHERE 1=1";

            if (!showDiabledProducts)
            {
                hql += " AND p.enabled = true";
            }

            hql += " ORDER BY p.subCategory.id.categoryName, p.subCategory.id.name, p.name";

            Query<Product> query = session.createQuery(hql, Product.class);

            productsList.addAll(query.getResultList());
        });

        products.clear();

        for (var product : productsList)
        {
            var mainCategory = product.getSubCategory().getId().categoryName();
            var subCategory = product.getSubCategory().getId().name();

            if (!products.containsKey(mainCategory))
            {
                products.put(mainCategory, new LinkedHashMap<>());
            }

            if (!products.get(mainCategory).containsKey(subCategory))
            {
                products.get(mainCategory).put(subCategory, new ArrayList<>());
            }

            products.get(mainCategory).get(subCategory).add(product);
        }

        showProducts(filterText);
    }

    private void searchProducts(String value)
    {
        showProducts(value);
    }

    @FXML
    private void createNewProductWindow()
    {
        new NewProductStageController().instantiate();
    }
}
