package org.lebastudios.theroundtable.plugincashregister;

import org.lebastudios.theroundtable.events.*;
import org.lebastudios.theroundtable.plugincashregister.cash.Order;
import org.lebastudios.theroundtable.plugincashregister.entities.Product;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;

public class PluginCashRegisterEvents
{
    public static final Event1<Order> showOrder = new Event1<>() {};
    public static final Event1<Product> onProductModify = new Event1<>() {};
    public static final Event1<Receipt> onReceiptEmitted = new Event1<>() {};
    public static final Event2<Integer, StringBuffer> onRequestReceiptBillNumber = new SingleListenerEvent2<>();
    public static final Event2<Integer, StringBuffer> onRequestNewReceiptBillNumber = new SingleListenerEvent2<>();
    public static final Event2<Integer, StringBuffer> onRequestNewRectificationBillNumber = new SingleListenerEvent2<>();
    public static final Event2<Receipt, String> onReceiptBilled = new Event2<>();
    public static final Event2<Receipt, String> onModifiedReceiptBilled = new Event2<>();
    
    private static class SingleListenerEvent2<T, P> extends Event2<T, P>
    {
        private boolean hasListener = false;
        
        @Override
        public void addListener(IEventMethod2<T, P> listener)
        {
            if (hasListener) 
            {
                System.err.println("This SingleListenerEvent already has an assigned listener (PlguinCashRegisterEvents)");
                return;
            }
            
            hasListener = true;
            
            super.addListener(listener);
        }

        @Override
        public void removeListener(IEventMethod2<T, P> listener)
        {
            if (getActiveListeners().contains(listener)) 
            {
                return;
            }
            
            super.removeListener(listener);
            hasListener = false;
        }
    }
}
