package org.lebastudios.theroundtable.plugincashregister.cash;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.lebastudios.theroundtable.MainStageController;
import org.lebastudios.theroundtable.dialogs.ConfirmationTextDialogController;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.config.data.CashRegisterStateData;
import org.lebastudios.theroundtable.config.data.JSONFile;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.plugincashregister.products.*;
import org.lebastudios.theroundtable.printers.*;
import org.lebastudios.theroundtable.ui.*;

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

        CashRegister.onActualOrderModified.addListener(() ->
        {
            final var cashRegister = CashRegister.getInstance();

            exitOrderButton.setVisible(cashRegister.getActualOrder() != cashRegister.getCashRegisterOrder());
            clearActualOrderButton.setDisable(cashRegister.getActualOrder().getOrderItems().isEmpty());
        });

        CashRegister.onActualOrderModified.addListener(() ->
        {
            orderItemsListView.getItems().clear();
            orderItemsListView.getItems().addAll(CashRegister.getInstance().getActualOrder().getOrderItems());
        });

        CashRegister.onActualOrderModified.addListener(() ->
        {
            boolean empty = CashRegister.getInstance().getActualOrder().getOrderItems().isEmpty();

            collectOrderButton.setDisable(empty);
            splitOrderButton.setDisable(empty);
        });

        CashRegister.onActualOrderModified.addListener(() ->
        {
            totalLabel.setText(CashRegister.getInstance().getActualOrder().getTotalStringRepresentation());
            orderTableNameLabel.setText(CashRegister.getInstance().getActualOrder().getOrderName());
        });

        CashRegister.onOrderItemModified.addListener(_ ->
        {
            totalLabel.setText(CashRegister.getInstance().getActualOrder().getTotalStringRepresentation());
        });
    }

    @FXML
    @Override
    protected void initialize()
    {
        orderTableNameLabel.setText(CashRegister.getInstance().getActualOrder().getOrderName());

        getRoot().addEventHandler(KeyEvent.KEY_PRESSED, this::escape);
        orderItemsListView.addEventHandler(KeyEvent.KEY_PRESSED, this::escape);

        bindKeyboardWithVirtualPad();

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

                        if (empty || orderItem == null)
                        {
                            setText(null);
                            setGraphic(null);
                            return;
                        }
                        
                        if (!itemLabelControllers.containsKey(this)) 
                        {
                            var controller = new OrderItemLabelController();
                            itemLabelControllers.put(this, controller);
                            final var node = controller.getRoot();
                            ((Pane) node).prefWidthProperty().bind(orderItemsListView.widthProperty().subtract(20));
                        }
                        
                        final var orderItemLabelController = itemLabelControllers.get(this);
                        
                        orderItemLabelController.setOrderItem(orderItem);
                        orderItemLabelController.initialize();

                        setGraphic(orderItemLabelController.getRoot());
                    }
                };
            }
        });
    }

    private void escape(KeyEvent event)
    {
        if (event.isConsumed()) return;

        if (event.getCode() == KeyCode.ESCAPE)
        {
            if (actualProduct != null)
            {
                actualProduct.setActualEditting(null);
                actualProduct = null;
            }

            orderItemsListView.getSelectionModel().select(null);
        }

        event.consume();
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

        ProductPaneController.onAction = product -> CashRegister.getInstance().addProduct(product, BigDecimal.ONE);
        MainStageController.getInstance().setCentralNode(instance);
    }

    private void bindKeyboardWithVirtualPad()
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
                
                default -> consumed = false;
            }

            if (consumed) event.consume();
        });
    }

    @FXML
    private void printOrder()
    {
        CashRegister.getInstance().printOrder();
    }

    @FXML
    private void clearActualOrder()
    {
        new ConfirmationTextDialogController(LangFileLoader.getTranslation("textblock.resetorder"), r ->
        {
            if (!r) return;

            CashRegister.getInstance().resetActualOrder();
        }).instantiate();
    }

    @FXML
    private void collectOrder()
    {
        new CollectOrderStageController(CashRegister.getInstance().getActualOrder(), receipt ->
        {
            updateLastCollectedReceipt(receipt);

            CashRegister.getInstance().resetActualOrder();
        }).instantiate();
    }

    @FXML
    private void splitOrder()
    {
        new SeparateOrderController(CashRegister.getInstance().getActualOrder(), this::separateOrder).instantiate();
    }

    private void separateOrder(Order original, Order generated)
    {
        new CollectOrderStageController(generated, receipt ->
        {
            updateLastCollectedReceipt(receipt);
            
            for (OrderItem orderItem : generated.getOrderItems())
            {
                original.removeOrderItem(orderItem);
            }

            CashRegister.onActualOrderModified.invoke();
        }).instantiate();
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
        var separator = new OrderItem.Separator();
        CashRegister.getInstance().getActualOrder().getOrderItems().add(separator);

        // DrageableNode.makeDrageable(productsInsertedVBox, separator, this::updateOrderItemsOrder,
        //         _ -> OrderItemLabelController.editMode);
    }

    @FXML
    private void alterNumericKeyboardVisibility()
    {
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
        new TransactionCreatorStageController(TransactionCreatorStageController.TransactionType.ADD).instantiate();
    }

    @FXML
    private void moneyOut()
    {
        new TransactionCreatorStageController(TransactionCreatorStageController.TransactionType.REMOVE).instantiate();
    }

    @FXML
    private void closeCashRegister()
    {
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
