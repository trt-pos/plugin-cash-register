package org.lebastudios.theroundtableplugins.cr.entities;

import jakarta.persistence.*;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lebastudios.theroundtable.database.PluginTable;
import org.lebastudios.theroundtable.entities.Account;
import org.lebastudios.theroundtable.entities.AppInstallation;
import org.lebastudios.theroundtable.locale.Translator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@PluginTable(name = "transaction")
public class Transaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "date", nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "description", nullable = false, length = 99999)
    private String description = "";

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "receipt_id", referencedColumnName = "id")
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trt_uuid", referencedColumnName = "uuid", nullable = false)
    private AppInstallation appInstallation;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;
    
    ///  Represents the total amount of cash in the register at the moment of the transaction.
    @Column(name = "total_cash", nullable = false)
    private BigDecimal totalCash = BigDecimal.ZERO;

    @Column(name = "method", nullable = false)
    @Enumerated(EnumType.STRING)
    @Setter private PaymentMethod method;
    
    public String getDescription()
    {
        if (description == null || description.isBlank()) 
        {
            return "Transaction #" + id;
        }
        
        return description;
    }

    public enum PaymentMethod
    {
        CASH, CARD;
    
        public String translate()
        {
            return Translator.getInstance().t("cr:word." +
                    switch (this)
                    {
                        case CASH -> "cash";
                        case CARD -> "card";
                        default -> throw new IllegalArgumentException("Unknown payment method");
                    }
            );
        }
    
        public static final StringConverter<PaymentMethod> STRING_CONVERTER = new StringConverter<>()
        {
            @Override
            public String toString(PaymentMethod object)
            {
                return object.translate();
            }
    
            @Override
            public PaymentMethod fromString(String string)
            {
                String cashTranslation = CASH.translate();
                String cardTranslation = CARD.translate();
                
                
                if (string.equals(cashTranslation)) return CASH;
                if (string.equals(cardTranslation)) return CARD;
                
                throw new IllegalArgumentException("Unknown payment method: " + string);
            }
        };
    
    }
}
