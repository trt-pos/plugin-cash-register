package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.lebastudios.theroundtable.MainStageController;
import org.lebastudios.theroundtable.config.data.JSONFile;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.dialogs.ConfirmationTextDialogController;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.plugincashregister.config.data.CashRegisterStateData;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.products.ProductPaneController;
import org.lebastudios.theroundtable.plugincashregister.products.ProductsUIController;
import org.lebastudios.theroundtable.printers.OpenCashDrawer;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.ui.IconButton;
import org.lebastudios.theroundtable.ui.IconTextButton;
import org.lebastudios.theroundtable.ui.LoadingPaneController;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;

public class CashRegisterPaneController extends PaneController<CashRegisterPaneController>
{
    private static CashRegisterPaneController instance;

    @FXML private ListView<OrderItem> orderItemsListView;
    @FXML private VBox cashRegisterKeyboard;
    @FXML private VBox keyboardParent;
    @FXML private Label orderTableNameLabel;
    @FXML private Label totalLabel;
    @FXML private Label lastCollectedTotalLabel;
    @FXML private IconButton alterVisibilityButton;
    @FXML private IconButton exitOrderButton;
    @FXML private IconButton clearActualOrderButton;
    @FXML private IconTextButton collectOrderButton;
    @FXML private IconButton splitOrderButton;

    private OrderItemLabelController actualProduct;
    private boolean isVisible = true;
    private int index = -1;

    private CashRegisterPaneController()
    {
        if (instance != null)
        {
            throw new IllegalStateException("Shouldn't be created more than once");
        }
    }

    @FXML
    @Override
    protected void initialize()
    {
        this.bindActualOrderToUi();
        CashRegister.onActualOrderSwapped.addListener(this::bindActualOrderToUi);

        CashRegister.onOrderItemModified.addListener(_ ->
        {
            totalLabel.setText(CashRegister.getInstance().getActualOrder().getTotalStringRepresentation());
        });

        bindKeyboardActions();

        // Adding the products interface to the root
        HBox root = (HBox) getRoot();

        var loadingPane = new HBox(new LoadingPaneController().getRoot());
        HBox.setHgrow(loadingPane, Priority.ALWAYS);
        root.getChildren().addFirst(loadingPane);
        new Thread(() ->
        {
            final var node = new ProductsUIController(false).getRoot();
            Platform.runLater(() -> root.getChildren().set(0, node));
        }).start();

        // ListView row factory
        orderItemsListView.setCellFactory(new Callback<>()
        {
            private final HashMap<ListCell<OrderItem>, OrderItemLabelController> itemLabelControllers = new HashMap<>();

            @Override
            public ListCell<OrderItem> call(ListView<OrderItem> orderItemListView)
            {
                return new ListCell<>()
                {
                    {
                        this.setStyle("-fx-padding: 0;");

                        this.setOnMouseClicked(e ->
                        {
                            if (actualProduct == itemLabelControllers.get(this)) return;

                            if (actualProduct != null)
                            {
                                actualProduct.submitEditting();
                            }

                            if (!this.isEmpty() && this.getItem() != null)
                            {
                                actualProduct = itemLabelControllers.get(this);
                            }
                            else
                            {
                                actualProduct = null;
                            }
                        });
                    }

                    @Override
                    protected void updateItem(OrderItem orderItem, boolean empty)
                    {
                        super.updateItem(orderItem, empty);

                        if (itemLabelControllers.containsKey(this))
                        {
                            itemLabelControllers.get(this).removeListeners();
                            itemLabelControllers.remove(this);
                        }

                        if (empty || orderItem == null)
                        {
                            setText(null);
                            setGraphic(null);
                            return;
                        }

                        OrderItemLabelController controller = new OrderItemLabelController(orderItem);
                        itemLabelControllers.put(this, controller);
                        final var node = controller.getRoot();
                        ((Pane) node).prefWidthProperty().bind(orderItemsListView.widthProperty().subtract(20));

                        setGraphic(controller.getRoot());
                    }
                };
            }
        });
    }

    private void bindActualOrderToUi()
    {
        final CashRegister cashRegister = CashRegister.getInstance();
        final Order actualOrder = cashRegister.getActualOrder();

        ObservableList<OrderItem> items = (ObservableList<OrderItem>) actualOrder.getOrderItems();
        orderItemsListView.setItems(items);

        items.addListener((ListChangeListener<OrderItem>) _ ->
        {
            boolean empty = items.isEmpty();

            clearActualOrderButton.setDisable(items.isEmpty());
            collectOrderButton.setDisable(empty);
            splitOrderButton.setDisable(empty);

            totalLabel.setText(actualOrder.getTotalStringRepresentation());
        });

        exitOrderButton.setVisible(actualOrder != cashRegister.getCashRegisterOrder());
        orderTableNameLabel.setText(actualOrder.getOrderName());
    }

    private void bindKeyboardActions()
    {
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            if (event.isConsumed()) return;

            boolean consumed = true;

            switch (event.getCode())
            {
                case DIGIT1, NUMPAD1 -> button1();
                case DIGIT2, NUMPAD2 -> button2();
                case DIGIT3, NUMPAD3 -> button3();
                case DIGIT4, NUMPAD4 -> button4();
                case DIGIT5, NUMPAD5 -> button5();
                case DIGIT6, NUMPAD6 -> button6();
                case DIGIT7, NUMPAD7 -> button7();
                case DIGIT8, NUMPAD8 -> button8();
                case DIGIT9, NUMPAD9 -> button9();
                case DIGIT0, NUMPAD0 -> button0();
                case BACK_SPACE -> buttonBackspace();
                case DECIMAL, PERIOD, COMMA -> buttonDot();
                case MINUS, PLUS, ADD, SUBTRACT -> invertNumber();
                case ENTER -> submitEditting();
                case ESCAPE ->
                {
                    if (actualProduct != null)
                    {
                        actualProduct.setActualEditting(null);
                        actualProduct = null;
                    }

                    orderItemsListView.getSelectionModel().select(null);
                }

                default -> consumed = false;
            }

            if (consumed) event.consume();
        });
    }

    public static void showInterface()
    {
        if (instance == null)
        {
            instance = new CashRegisterPaneController();
        }

        if (!new JSONFile<>(CashRegisterStateData.class).get().open)
        {
            MainStageController.getInstance().setCentralNode(new CashRegisterClosePaneController());
            return;
        }

        ProductPaneController.onAction = product -> {
            instance.submitEditting();
            CashRegister.getInstance().addProduct(product, BigDecimal.ONE);
        };
        MainStageController.getInstance().setCentralNode(instance);
    }

    @FXML
    private void printOrder()
    {
        submitEditting();

        CashRegister.getInstance().printOrder();
    }

    @FXML
    private void clearActualOrder()
    {
        submitEditting();

        new ConfirmationTextDialogController(LangFileLoader.getTranslation("textblock.resetorder"), r ->
        {
            if (!r) return;

            CashRegister.getInstance().resetActualOrder();
        }).instantiate();
    }

    @FXML
    private void collectOrder()
    {
        submitEditting();

        new CollectOrderStageController(CashRegister.getInstance().getActualOrder(), receipt ->
        {
            updateLastCollectedReceipt(receipt);

            CashRegister.getInstance().resetActualOrder();
        }).instantiate();
    }

    @FXML
    private void splitOrder()
    {
        submitEditting();

        final Order actualOrder = CashRegister.getInstance().getActualOrder();
        new SeparateOrderController(actualOrder,
                (original, generated) -> new CollectOrderStageController(generated, receipt ->
                {
                    updateLastCollectedReceipt(receipt);

                    for (OrderItem orderItem : generated.getOrderItems())
                    {
                        original.removeOrderItem(orderItem);
                    }
                }).instantiate()).instantiate();
    }

    private void updateLastCollectedReceipt(Receipt receipt)
    {
        lastCollectedTotalLabel.setText(LangFileLoader.getTranslation("phrase.lastcollected") + " "
                + BigDecimalOperations.toString(receipt.getTaxedTotal()) + " €    "
                + LangFileLoader.getTranslation("word.payment") + " "
                + BigDecimalOperations.toString(receipt.getPaymentAmount()) + " €    "
                + LangFileLoader.getTranslation("word.change") + ": "
                + BigDecimalOperations.toString(receipt.getPaymentAmount().subtract(receipt.getTaxedTotal())) + " €"
        );
    }

    @FXML
    private void exitOrder()
    {
        submitEditting();

        CashRegister.getInstance().swapOrder(CashRegister.getInstance().getCashRegisterOrder());
    }

    @FXML
    private void button1()
    {
        if (actualProduct == null) return;
        actualProduct.edit("1");
    }

    @FXML
    private void button2()
    {
        if (actualProduct == null) return;
        actualProduct.edit("2");
    }

    @FXML
    private void button3()
    {
        if (actualProduct == null) return;
        actualProduct.edit("3");
    }

    @FXML
    private void button4()
    {
        if (actualProduct == null) return;
        actualProduct.edit("4");
    }

    @FXML
    private void button5()
    {
        if (actualProduct == null) return;
        actualProduct.edit("5");
    }

    @FXML
    private void button6()
    {
        if (actualProduct == null) return;
        actualProduct.edit("6");
    }

    @FXML
    private void button7()
    {
        if (actualProduct == null) return;
        actualProduct.edit("7");
    }

    @FXML
    private void button8()
    {
        if (actualProduct == null) return;
        actualProduct.edit("8");
    }

    @FXML
    private void button9()
    {
        if (actualProduct == null) return;
        actualProduct.edit("9");
    }

    @FXML
    private void button0()
    {
        if (actualProduct == null) return;
        actualProduct.edit("0");
    }

    @FXML
    private void buttonBackspace()
    {
        if (actualProduct == null) return;
        actualProduct.removeLast();
    }

    @FXML
    private void buttonDot()
    {
        if (actualProduct == null) return;
        actualProduct.edit(".");
    }

    @FXML
    private void invertNumber()
    {
        if (actualProduct == null) return;
        actualProduct.invertNumber();
    }


    @FXML
    private void submitEditting()
    {
        if (actualProduct == null) return;
        actualProduct.submitEditting();
        actualProduct = null;
    }

    @FXML
    private void openCashRegister()
    {
        submitEditting();

        try
        {
            EscPos escPos = new EscPos(new PrinterOutputStream(PrinterManager.getInstance().getDefaultPrintService()));
            new OpenCashDrawer().print(escPos);
            escPos.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void instantiateSeparator()
    {
        submitEditting();

        var separator = new OrderItem.Separator();
        CashRegister.getInstance().getActualOrder().getOrderItems().add(separator);

        // DrageableNode.makeDrageable(productsInsertedVBox, separator, this::updateOrderItemsOrder,
        //         _ -> OrderItemLabelController.editMode);
    }

    @FXML
    private void alterNumericKeyboardVisibility()
    {
        submitEditting();

        isVisible = !isVisible;

        if (index == -1) index = keyboardParent.getChildren().indexOf(cashRegisterKeyboard);

        if (!isVisible)
        {
            keyboardParent.getChildren().remove(index);
        }
        else
        {
            keyboardParent.getChildren().add(index, cashRegisterKeyboard);
        }

        alterVisibilityButton.setIconName(isVisible ? "hide-outlined.png" : "show-outlined.png");
    }

    @FXML
    private void moneyIn()
    {
        submitEditting();

        new TransactionCreatorStageController(TransactionCreatorStageController.TransactionType.ADD).instantiate();
    }

    @FXML
    private void moneyOut()
    {
        submitEditting();

        new TransactionCreatorStageController(TransactionCreatorStageController.TransactionType.REMOVE).instantiate();
    }

    @FXML
    private void closeCashRegister()
    {
        submitEditting();

        new CloseCashRegisterStageController().instantiate();
    }

    @Override
    public Class<?> getBundleClass()
    {
        return PluginCashRegister.class;
    }

    @Override
    public URL getFXML()
    {
        return CashRegisterPaneController.class.getResource("cashRegisterPane.fxml");
    }
}
