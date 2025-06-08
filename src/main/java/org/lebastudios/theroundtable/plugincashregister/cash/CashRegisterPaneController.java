package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.dialogs.ConfirmationTextDialogController;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.products.ProductPaneController;
import org.lebastudios.theroundtable.plugincashregister.products.ProductsUIController;
import org.lebastudios.theroundtable.printers.OpenCashDrawer;
import org.lebastudios.theroundtable.printers.PrinterManager;
import org.lebastudios.theroundtable.components.IconButton;
import org.lebastudios.theroundtable.components.IconTextButton;
import org.lebastudios.theroundtable.components.LoadingPaneController;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

public class CashRegisterPaneController extends PaneController<CashRegisterPaneController>
{
    private static CashRegisterPaneController instance;

    @FXML public ListView<OrderItem> orderItemsListView;
    @FXML public VBox cashRegisterKeyboard;
    @FXML public VBox keyboardParent;
    @FXML public Label orderTableNameLabel;
    @FXML public Label totalLabel;
    @FXML public Label lastCollectedTotalLabel;
    @FXML public IconButton alterVisibilityButton;
    @FXML public IconButton exitOrderButton;
    @FXML public IconButton clearActualOrderButton;
    @FXML public IconTextButton collectOrderButton;
    @FXML public IconButton splitOrderButton;

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
            Platform.runLater(() ->
            {
                root.getChildren().set(0, node);
                HBox.setHgrow(node, Priority.ALWAYS);
            });
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

        ObservableList<OrderItem> items = actualOrder.getObservableOrderItems();
        orderItemsListView.setItems(items);

        updateDisableableButtons(items);
        totalLabel.setText(actualOrder.getTotalStringRepresentation());

        items.addListener((ListChangeListener<OrderItem>) _ ->
        {
            updateDisableableButtons(items);
            totalLabel.setText(actualOrder.getTotalStringRepresentation());
        });

        exitOrderButton.setVisible(actualOrder != cashRegister.getCashRegisterOrder());
        orderTableNameLabel.setText(actualOrder.getOrderName());
    }

    private void updateDisableableButtons(List<OrderItem> items)
    {
        boolean empty = items.isEmpty();

        clearActualOrderButton.setDisable(empty);
        collectOrderButton.setDisable(empty);
        splitOrderButton.setDisable(empty);
    }

    private void bindKeyboardActions()
    {
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            if (event.isConsumed()) return;

            boolean consumed = true;

            switch (event.getCode())
            {
                case DIGIT1, NUMPAD1 -> button1(null);
                case DIGIT2, NUMPAD2 -> button2(null);
                case DIGIT3, NUMPAD3 -> button3(null);
                case DIGIT4, NUMPAD4 -> button4(null);
                case DIGIT5, NUMPAD5 -> button5(null);
                case DIGIT6, NUMPAD6 -> button6(null);
                case DIGIT7, NUMPAD7 -> button7(null);
                case DIGIT8, NUMPAD8 -> button8(null);
                case DIGIT9, NUMPAD9 -> button9(null);
                case DIGIT0, NUMPAD0 -> button0(null);
                case BACK_SPACE -> buttonBackspace(null);
                case DECIMAL, PERIOD, COMMA -> buttonDot(null);
                case MINUS, PLUS, ADD, SUBTRACT -> invertNumber(null);
                case ENTER -> submitEditting(null);
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

        if (CashSession.getActualSession() == null)
        {
            MainStageController.getInstance().setCentralNode(new CashRegisterClosePaneController());
            return;
        }

        ProductPaneController.onAction = product ->
        {
            instance.submitEditting(null);
            CashRegister.getInstance().addProduct(product, BigDecimal.ONE);
        };
        MainStageController.getInstance().setCentralNode(instance);
    }

    @FXML
    public void printOrder(ActionEvent actionEvent)
    {
        submitEditting(null);

        CashRegister.getInstance().printOrder();
    }

    @FXML
    public void clearActualOrder(ActionEvent actionEvent)
    {
        submitEditting(null);

        new ConfirmationTextDialogController(Translator.getInstance().t("cr:plugincashregister.textblock.resetorder"),
                r ->
                {
                    if (!r) return;

                    CashRegister.getInstance().resetActualOrder();
                }).instantiate();
    }

    @FXML
    public void collectOrder(ActionEvent actionEvent)
    {
        submitEditting(null);

        final CashRegister cashRegister = CashRegister.getInstance();
        new CollectOrderStageController(cashRegister.getActualOrder(), receipt ->
        {
            updateLastCollectedReceipt(receipt);

            cashRegister.resetActualOrder();
            cashRegister.swapOrder(cashRegister.getCashRegisterOrder());
        }).instantiate();
    }

    @FXML
    public void splitOrder(ActionEvent actionEvent)
    {
        submitEditting(null);

        final Order actualOrder = CashRegister.getInstance().getActualOrder();
        new SeparateOrderController(actualOrder,
                (original, generated) -> new CollectOrderStageController(generated, receipt ->
                {
                    updateLastCollectedReceipt(receipt);

                    for (var item : generated.getOrderItems())
                    {
                        original.removeOrderItem(item);
                    }
                    
                    if (original.getOrderItems().isEmpty()) 
                    {
                        CashRegister.getInstance().swapOrder(CashRegister.getInstance().getCashRegisterOrder());
                    }
                    
                }).instantiate()
        ).instantiate();
    }

    private void updateLastCollectedReceipt(Receipt receipt)
    {
        lastCollectedTotalLabel.setText(Translator.getInstance().t("cr:plugincashregister.phrase.lastcollected") + " "
                + BigDecimalOperations.toString(receipt.getTaxedTotal()) + " €    "
                + Translator.getInstance().t("cr:plugincashregister.word.payment") + " "
                + BigDecimalOperations.toString(receipt.getPaymentAmount()) + " €    "
                + Translator.getInstance().t("cr:plugincashregister.word.change") + ": "
                + BigDecimalOperations.toString(receipt.getPaymentAmount().subtract(receipt.getTaxedTotal())) + " €"
        );
    }

    @FXML
    public void exitOrder(ActionEvent actionEvent)
    {
        submitEditting(null);

        CashRegister.getInstance().swapOrder(CashRegister.getInstance().getCashRegisterOrder());
    }

    @FXML
    public void button1(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit("1");
    }

    @FXML
    public void button2(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit("2");
    }

    @FXML
    public void button3(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit("3");
    }

    @FXML
    public void button4(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit("4");
    }

    @FXML
    public void button5(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit("5");
    }

    @FXML
    public void button6(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit("6");
    }

    @FXML
    public void button7(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit("7");
    }

    @FXML
    public void button8(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit("8");
    }

    @FXML
    public void button9(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit("9");
    }

    @FXML
    public void button0(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit("0");
    }

    @FXML
    public void buttonBackspace(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.removeLast();
    }

    @FXML
    public void buttonDot(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.edit(".");
    }

    @FXML
    public void invertNumber(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.invertNumber();
    }


    @FXML
    public void submitEditting(ActionEvent actionEvent)
    {
        if (actualProduct == null) return;
        actualProduct.submitEditting();
        actualProduct = null;
    }

    @FXML
    public void openCashRegister(ActionEvent actionEvent)
    {
        submitEditting(null);

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
    public void instantiateSeparator(ActionEvent actionEvent)
    {
        submitEditting(null);

        var separator = new OrderItem.Separator();
        CashRegister.getInstance().getActualOrder().getOrderItems().add(separator);

        // DrageableNode.makeDrageable(productsInsertedVBox, separator, this::updateOrderItemsOrder,
        //         _ -> OrderItemLabelController.editMode);
    }

    @FXML
    public void alterNumericKeyboardVisibility(ActionEvent actionEvent)
    {
        submitEditting(null);

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
    public void moneyIn(ActionEvent actionEvent)
    {
        submitEditting(null);

        new TransactionCreatorStageController(TransactionCreatorStageController.TransactionType.ADD).instantiate();
    }

    @FXML
    public void moneyOut(ActionEvent actionEvent)
    {
        submitEditting(null);

        new TransactionCreatorStageController(TransactionCreatorStageController.TransactionType.REMOVE).instantiate();
    }

    @FXML
    public void closeCashRegister(ActionEvent actionEvent)
    {
        submitEditting(null);

        new CloseCashSessionStageController()
                .setOwner(this.getStage())
                .instantiate();
    }
}
