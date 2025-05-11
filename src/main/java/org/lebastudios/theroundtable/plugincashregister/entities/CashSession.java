package org.lebastudios.theroundtable.plugincashregister.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lebastudios.theroundtable.database.entities.Account;
import org.lebastudios.theroundtable.database.entities.AppInstallation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "cr_cash_session")
public class CashSession
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trt_uuid", referencedColumnName = "uuid", nullable = false)
    private AppInstallation appInstallation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    @Column(name = "opening_amount", nullable = false)
    private BigDecimal openingAmount;

    @Column(name = "closing_amount")
    private BigDecimal closingAmount;

    @Column(name = "opening_date", nullable = false)
    private LocalDateTime openingDate;
    
    @Column(name = "closing_date")
    private LocalDateTime closingDate;
    
    public Status getSessionStatus()
    {
        assert (closingDate != null) == (closingAmount != null);
        
        return closingDate == null ? Status.OPEN : Status.CLOSED;
    }
    
    public enum Status
    {
        OPEN,
        CLOSED
    }
}
