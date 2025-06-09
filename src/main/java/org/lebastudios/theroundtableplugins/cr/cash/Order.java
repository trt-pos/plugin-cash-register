package org.lebastudios.theroundtableplugins.cr.cash;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import org.lebastudios.theroundtable.config.GlobalPreferencesConfigData;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtableplugins.cr.entities.Product;
import org.lebastudios.theroundtableplugins.cr.entities.Receipt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Order
{
    @Getter @Setter private String orderName;
    private ObservableList<OrderItem> orderItems = new ObservableListWrapper<>(new ArrayList<>());

    /// Legacy method. orderItems is no longer a simple List<>, but an ObservableList<>. Use getObservableOrderItems() instead.
    public List<OrderItem> getOrderItems()
    {
        return orderItems;
    }
    
    public ObservableList<OrderItem> getObservableOrderItems()
    {
        return orderItems;
    }
    
    public static Order fromReceipt(int receiptId)
    {
        return Database.getInstance().connectQuery(session ->
        {
            return fromReceipt(session.get(Receipt.class, receiptId));
        });
    }
    
    public static Order fromReceipt(Receipt receipt)
    {
        var order = new Order();

        order.setOrderName(receipt.getTableName());

        final List<OrderItem> orderItems = new ArrayList<>();

        for (var receiptItem : receipt.getProducts())
        {
            orderItems.add(new OrderItem(receiptItem.getProduct(), receiptItem.getQuantity()));
        }

        order.orderItems = new ObservableListWrapper<>(orderItems);

        return order;
    }
    
    public void removeOrderItem(OrderItem orderItem)
    {
        removeOrderItem(orderItem.getBaseProduct(), orderItem.getQuantity());
    }
    
    public void removeOrderItem(Product product, BigDecimal quantity)
    {
        var orderItem = orderItems.stream()
                .filter(item -> item.getBaseProduct().equals(product))
                .findFirst()
                .orElse(null);
        
        if (orderItem == null) return;
        
        orderItem.setQuantity(orderItem.getQuantity().subtract(quantity));
        
        if (orderItem.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
        {
            orderItems.remove(orderItem);
        }
        else
        {
            CashRegister.onOrderItemModified.invoke(orderItem);
        }
    }
    
    public void collapseEqualItems()
    {
        var products = new LinkedHashMap<Product, BigDecimal>();
        
        for (var orderItem : orderItems)
        {
            var product = orderItem.getBaseProduct();
            var quantity = orderItem.getQuantity();
            
            if (products.containsKey(product))
            {
                products.put(product, products.get(product).add(quantity));
            }
            else
            {
                products.put(product, quantity);
            }
        }
        
        orderItems.clear();
        
        for (var entry : products.entrySet())
        {
            if (entry.getValue().compareTo(BigDecimal.ZERO) <= 0) continue;
            
            orderItems.add(new OrderItem(entry.getKey(), entry.getValue()));
        }
    }
    
    public BigDecimal getTotal()
    {
        BigDecimal total = BigDecimal.ZERO;

        for (var orderItem : orderItems)
        {
            total = total.add(orderItem.getTotalPrice());
        }

        return total;
    }

    /**
     * This Method always returns getTotal() + [actual currency symbol]. The currency should be decided at runtime.
     */
    public String getTotalStringRepresentation()
    {
        return BigDecimalOperations.toString(getTotal()) + " " + new GlobalPreferencesConfigData().load().currency.symbol();
    }
    
    public BigDecimal getTotalWithoutTaxes()
    {
        BigDecimal total = BigDecimal.ZERO;

        for (var orderItem : orderItems)
        {
            total = total.add(orderItem.getNotTaxedTotalPrice());
        }

        return total;
    }
    
    public BigDecimal getTotalTaxes()
    {
        return getTotal().subtract(getTotalWithoutTaxes());
    }

    public void reset()
    {
        orderItems.clear();
    }
}
