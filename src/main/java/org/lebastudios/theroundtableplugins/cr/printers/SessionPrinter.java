package org.lebastudios.theroundtableplugins.cr.printers;

import org.lebastudios.theroundtableplugins.cr.entities.CashSession;
import org.lebastudios.theroundtable.printers.IPrinter;

public abstract class SessionPrinter implements IPrinter
{
    protected final CashSession session;
    
    public SessionPrinter(CashSession session)
    {
        this.session = session;
    }
}
