package org.lebastudios.theroundtable.plugincashregister.printers;

import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.printers.IPrinter;

public abstract class SessionPrinter implements IPrinter
{
    protected final CashSession session;
    
    public SessionPrinter(CashSession session)
    {
        this.session = session;
    }
}
