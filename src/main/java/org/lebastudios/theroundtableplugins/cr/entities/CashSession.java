package org.lebastudios.theroundtableplugins.cr.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Session;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.database.PluginTable;
import org.lebastudios.theroundtable.entities.Account;
import org.lebastudios.theroundtable.entities.AppInstallation;
import org.lebastudios.theroundtable.env.TrtUUIDReader;
import org.lebastudios.theroundtable.logs.Logs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
@PluginTable(name = "cash_session")
public class CashSession
{
    public static CashSession getActualSession()
    {
        return Database.getInstance().connectQuery(session ->
        {
            return getActualSession(session);
        });
    }

    public static CashSession getActualSession(Session session)
    {
        List<CashSession> cashSessions = session.createQuery(
                "from CashSession as cs " +
                        "where cs.appInstallation.uuid = :trtUuid " +
                        "and cs.closingDate is null", 
                        CashSession.class)
                .setParameter("trtUuid", new TrtUUIDReader().getTrtUUID())
                .list();

        if(cashSessions.isEmpty()) return null;

        if (cashSessions.size() > 1)
        {
            Logs.getInstance().log(
                    Logs.LogType.WARNING,
                    "There are more than one open cash sessions. This is not expected. "
            );
        }

        return cashSessions.getFirst();
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @ManyToOne()
    @JoinColumn(name = "trt_uuid", referencedColumnName = "uuid", nullable = false)
    private AppInstallation appInstallation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    @Column(name = "amount_in_drawer", nullable = false)
    private BigDecimal amountInDrawer = BigDecimal.ZERO;

    @Column(name = "opening_date", nullable = false)
    private LocalDateTime openingDate;
    
    @Column(name = "closing_date")
    private LocalDateTime closingDate;
    
    public Status getSessionStatus()
    {
        return closingDate == null ? Status.OPEN : Status.CLOSED;
    }
    
    public enum Status
    {
        OPEN,
        CLOSED
    }
}
