package org.lebastudios.theroundtable.plugincashregister.config.data;

import org.lebastudios.theroundtable.config.data.FileRepresentator;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;

import java.io.File;

public class CashRegisterStateData implements FileRepresentator
{
    public boolean open = false;
    public String openTime = null;

    @Override
    public File getFile()
    {
        return new File(PluginCashRegister.getInstance().getPluginFolder(), "cash-register-state.json");
    }
}
