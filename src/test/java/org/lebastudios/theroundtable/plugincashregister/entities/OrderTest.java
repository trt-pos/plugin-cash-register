package org.lebastudios.theroundtable.plugincashregister.entities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;
import org.lebastudios.theroundtable.plugincashregister.cash.Order;
import org.lebastudios.theroundtable.plugincashregister.cash.OrderItem;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest
{
    private static Order order;
    private static Product product;
    
    @BeforeAll
    static void setUp()
    {
        order = new Order();
        
        Product product = new Product();
        product.setPrice(BigDecimal.valueOf(2.60));
        product.setTaxes(BigDecimal.valueOf(0.21));
        product.setTaxesIncluded(false);
        
        order.getOrderItems().add(new OrderItem(product, BigDecimal.valueOf(2)));
        
        product = new Product();
        product.setPrice(BigDecimal.valueOf(1.50));
        product.setTaxes(BigDecimal.valueOf(0.10));
        product.setTaxesIncluded(true);

        order.getOrderItems().add(new OrderItem(product, BigDecimal.valueOf(3)));
    }

    @Test
    void getTotal()
    {
        assertEquals(order.getTotal(), BigDecimal.valueOf(10.792));
    }

    @Test
    void getTotalWithoutTaxes()
    {
        assertEquals(BigDecimalOperations.round(order.getTotalWithoutTaxes()), new BigDecimal("9.30"));
    }

    @Test
    void getTotalTaxes()
    {
        assertEquals(BigDecimalOperations.toString(order.getTotalTaxes()), "1.51");
    }
}