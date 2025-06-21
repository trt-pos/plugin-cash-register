package org.lebastudios.theroundtableplugins.cr.entities;

import jakarta.persistence.*;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lebastudios.theroundtable.apparience.ImageManager;
import org.lebastudios.theroundtable.database.PluginTable;
import org.lebastudios.theroundtable.locale.Translator;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@NoArgsConstructor
@Entity
@PluginTable(name = "receipt")
public class Receipt
{
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "table_name", nullable = false)
    @Setter private String tableName;

    /**
     * The amount of money that the client paid. Not the total amount of the receipt.
     */
    @Column(name = "payment_amount", nullable = false)
    @Setter private BigDecimal paymentAmount;

    @Column(name = "taxes_amount", nullable = false)
    @Setter private BigDecimal taxesAmount;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_identifier")
    private String clientIdentifier;

    @OneToMany(mappedBy = "receipt", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @Setter private Set<Product_Receipt> products;

    @OneToOne(mappedBy = "receipt", optional = false, cascade = CascadeType.PERSIST)
    @Setter private Transaction transaction = new Transaction();

    /// When the receipt has been modified, this field will be set.
    @OneToOne(mappedBy = "superReceipt")
    private ReceiptModification modifiedBy;

    /// When this receipt is a modification of another receipt, this field will be set.
    @OneToOne(mappedBy = "newReceipt")
    private ReceiptModification modifies;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Setter private Status status = Status.DEFAULT;

    public void setClient(String name, String identifier)
    {
        clientName = name;
        clientIdentifier = identifier;
    }

    public BigDecimal getTaxedTotal()
    {
        return transaction.getAmount();
    }

    public BigDecimal getNotTaxedTotal()
    {
        return getTaxedTotal().subtract(taxesAmount);
    }

    public String getClientString()
    {
        if (clientName == null)
        {
            return Translator.getInstance().t("core:phrase.generalpublicclient");
        }
        else
        {
            return clientName + " - " + clientIdentifier;
        }
    }
    
    public Image getStatusIcon()
    {
        return ImageManager.getInstance().get(status.getIconName(), ImageManager.ImageType.ICON);
    }

    @Override
    public final boolean equals(Object o)
    {
        if (!(o instanceof Receipt receipt)) return false;

        return id == receipt.id;
    }

    @Override
    public int hashCode()
    {
        return id;
    }

    public enum Status
    {
        DELETED,
        DEFAULT,
        MODIFIED;

        public String getIconName()
        {
            return switch (this)
            {
                case DELETED, MODIFIED -> "cr:deleted-bill.png";
                case DEFAULT -> "cr:default-bill.png";
                default -> throw new RuntimeException("Unknown receipt status: " + this);
            };
        }
    }
}
