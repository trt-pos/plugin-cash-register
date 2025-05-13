package org.lebastudios.theroundtable.plugincashregister.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lebastudios.theroundtable.camelot.FromBytes;
import org.lebastudios.theroundtable.camelot.IntoBytes;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.maths.BigDecimalOperations;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pr_product")
public class Product implements Cloneable, FromBytes<Product>, IntoBytes
{
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name", nullable = false)
    private String name = "Unknown Product";

    @Column(name = "price", nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "img_path", nullable = false, length = 999999)
    private String imgPath = "";
    
    @Transient
    private BigDecimal taxes = new BigDecimal("0.10");
    
    @ManyToOne
    @JoinColumn(name = "taxes_type", referencedColumnName = "id")
    private TaxType taxType;

    @Column(name = "taxes_included", nullable = false)
    private Boolean taxesIncluded = true;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "category_name", referencedColumnName = "category_name")
    @JoinColumn(name = "sub_category_name", referencedColumnName = "name")
    private SubCategory subCategory;

    @Column(name = "category_name", insertable = false, updatable = false)
    private String categoryName;
    @Column(name = "sub_category_name", insertable = false, updatable = false)
    private String subCategoryName;
    
    /// If no taxType is set, the tax field is used.
    /// Usefull when creating dynamically a product that is not  
    /// intended to be saved in the database.
    public BigDecimal getTaxes()
    {
        return taxType == null ? taxes : taxType.getValue();
    }
    
    public void setTaxes(BigDecimal taxes)
    {
        if (taxes.compareTo(BigDecimal.ZERO) < 0 || taxes.compareTo(BigDecimal.ONE) > 0)
        {
            throw new IllegalArgumentException("The value must be between 0 and 1 (Both included).");
        }
        
        this.taxes = taxes;
    }
    
    /**
     * Returns the price of the product with taxes.
     * @return The price of the product with taxes.
     */
    public BigDecimal getPrice()
    {
        return taxesIncluded ? price : price.add(price.multiply(getTaxes()));
    }
    
    public void setTaxedPrice(BigDecimal price)
    {
        this.price = taxesIncluded ? price : BigDecimalOperations.dividePrecise(price, getTaxes().add(BigDecimal.ONE));
    }
    
    public BigDecimal getNotTaxedPrice()
    {
        return taxesIncluded ? BigDecimalOperations.dividePrecise(price, getTaxes().add(BigDecimal.ONE)) : price;
    }

    @Override
    public int hashCode()
    {
        int result = id;
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(price);
        return result;
    }

    @Override
    public final boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;

        return id == product.id && Objects.equals(name, product.name) &&
                Objects.equals(price, product.price);
    }

    @Override
    public Product clone()
    {
        try
        {
            return (Product) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new AssertionError();
        }
    }

    @Override
    public Product fromBytes(byte[] bytes) throws ParseException
    {
        return Database.getInstance().connectQuery(session ->
        {
            return session.get(Product.class, ByteBuffer.wrap(bytes).getInt());
        });
    }

    @Override
    public byte[] intoBytes()
    {
        return ByteBuffer.allocate(4).putInt(this.id).array();
    }
}
