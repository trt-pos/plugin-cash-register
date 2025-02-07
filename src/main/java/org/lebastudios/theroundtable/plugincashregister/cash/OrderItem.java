package org.lebastudios.theroundtable.plugincashregister.cash;

import lombok.Getter;
import lombok.Setter;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;

import java.math.BigDecimal;

@Getter
public class OrderItem
{
    private final Product baseProduct;
    @Setter private BigDecimal quantity;

    public OrderItem(Product product, BigDecimal qty)
    {
        this.baseProduct = product;
        this.quantity = qty;
    }

    public BigDecimal getTotalPrice()
    {
        return baseProduct.getPrice().multiply(quantity);
    }

    public BigDecimal getNotTaxedTotalPrice()
    {
        return baseProduct.getNotTaxedPrice().multiply(quantity);
    }
    
    public Product intoProduct()
    {
        return baseProduct;
    }

    public OrderItem add(BigDecimal quantity) 
    {
        this.quantity = this.quantity.add(quantity);
        return this;
    }

    public static class Separator extends OrderItem
    {
        public Separator()
        {
            super(new Product(), new BigDecimal(0));
        }
    }
}
