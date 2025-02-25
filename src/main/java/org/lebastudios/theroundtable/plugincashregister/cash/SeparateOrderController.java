package org.lebastudios.theroundtable.plugincashregister.cash;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import org.lebastudios.theroundtable.apparience.ImageLoader;
import org.lebastudios.theroundtable.controllers.StageController;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.ui.StageBuilder;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SeparateOrderController extends StageController<SeparateOrderController>
{
    private final Order originalOrder;
    private final HashMap<Product, BigDecimal> originalProducts;
    private final BiConsumer<Order, Order> acceptSeparation;

    @FXML private ListView<Map.Entry<Product, BigDecimal>> sourceList;
    @FXML private ListView<Map.Entry<Product, BigDecimal>> targetList;
    @FXML private Button acceptButton;

    public SeparateOrderController(Order order, BiConsumer<Order, Order> acceptSeparation)
    {
        originalOrder = order;
        originalProducts = new HashMap<>();

        for (var item : order.getOrderItems())
        {
            originalProducts.put(item.getBaseProduct(), item.getQuantity());
        }

        this.acceptSeparation = acceptSeparation;
    }

    @FXML
    @Override
    protected void initialize()
    {
        sourceList.setCellFactory(from -> new MoveableItemListCell(from, targetList));
        targetList.setCellFactory(from -> new MoveableItemListCell(from, sourceList));

        sourceList.setItems(FXCollections.observableArrayList(originalProducts.entrySet()));
        sourceList.getItems().sort(Comparator.comparing(o -> o.getKey().getName()));

        acceptButton.setDisable(true);

        targetList.getItems().addListener(
                (ListChangeListener<Map.Entry<Product, BigDecimal>>) _ -> acceptButton.setDisable(
                        targetList.getItems().isEmpty())
        );
    }

    @FXML
    private void accept()
    {
        var generatedOrder = new Order();
        generatedOrder.setOrderName(
                originalOrder.getOrderName() + " (" + LangFileLoader.getTranslation("word.splitted") + ")"
        );

        for (var variable : targetList.getItems())
        {
            generatedOrder.getOrderItems().add(new OrderItem(variable.getKey(), variable.getValue()));
        }

        acceptSeparation.accept(originalOrder, generatedOrder);
        cancel();
    }

    @Override
    protected void customizeStageBuilder(StageBuilder stageBuilder)
    {
        stageBuilder.setModality(Modality.APPLICATION_MODAL);
    }

    @FXML
    private void cancel()
    {
        close();
    }

    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }

    @Override
    public URL getFXML()
    {
        return SeparateOrderController.class.getResource("separateOrder.fxml");
    }

    @Override
    public String getTitle()
    {
        return LangFileLoader.getTranslation("tiltle.separateorderdialog");
    }

    private static class MoveableItemListCell extends ListCell<Map.Entry<Product, BigDecimal>>
    {
        public MoveableItemListCell(
                ListView<Map.Entry<Product, BigDecimal>> from,
                ListView<Map.Entry<Product, BigDecimal>> to
        )
        {
            this.setOnMouseClicked(_ ->
            {
                var item = this.getItem();

                if (item == null) return;

                moveProductQty(item.getKey(), BigDecimal.ONE, from, to);
                from.getItems().sort(Comparator.comparing(o -> o.getKey().getName()));
                to.getItems().sort(Comparator.comparing(o -> o.getKey().getName()));

                from.getSelectionModel().clearSelection();
            });
        }

        @Override
        public void updateItem(Map.Entry<Product, BigDecimal> item, boolean empty)
        {
            super.updateItem(item, empty);

            if (item == null || empty)
            {
                setText(null);
                setGraphic(null);
            }
            else
            {
                setText(item.getValue().intValueExact() + " - " + item.getKey().getName());
                ImageView imageView = new ImageView(ImageLoader.getSavedImage(item.getKey().getImgPath()));
                imageView.setFitWidth(30);
                imageView.setFitHeight(30);
                imageView.setPreserveRatio(true);
                setGraphic(imageView);
            }
        }

        private static void moveProductQty(Product product, BigDecimal quantity,
                ListView<Map.Entry<Product, BigDecimal>> origin,
                ListView<Map.Entry<Product, BigDecimal>> output)
        {
            addProductToList(product, quantity.negate(), origin);
            addProductToList(product, quantity, output);
        }

        private static void addProductToList(Product product, BigDecimal quantity,
                ListView<Map.Entry<Product, BigDecimal>> listView)
        {
            var list = listView.getItems();
            var index = indexOfProduct(product, listView);

            if (index == -1)
            {
                list.add(Map.entry(product, quantity));
                return;
            }

            var entry = list.get(index);
            list.remove(index);

            var newEntry = Map.entry(product, entry.getValue().add(quantity));

            if (newEntry.getValue().compareTo(BigDecimal.ZERO) != 0)
            {
                list.add(newEntry);
            }
        }

        private static int indexOfProduct(Product product,
                ListView<Map.Entry<Product, BigDecimal>> listView)
        {
            var list = listView.getItems();
            for (var i = 0; i < list.size(); i++)
            {
                if (list.get(i).getKey().equals(product)) return i;
            }

            return -1;
        }
    }
}
