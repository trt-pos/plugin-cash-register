package org.lebastudios.theroundtable.plugincashregister.cash;

import javafx.util.StringConverter;
import org.lebastudios.theroundtable.locale.LangFileLoader;

public enum PaymentMethod
{
    CASH, CARD;

    public String translate()
    {
        return LangFileLoader.getTranslation("word." +
                switch (this)
                {
                    case CASH -> "cash";
                    case CARD -> "card";
                    default -> throw new RuntimeException("Unknown payment method");
                }
        );
    }

    public static final StringConverter<PaymentMethod> converter = new StringConverter<>()
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
