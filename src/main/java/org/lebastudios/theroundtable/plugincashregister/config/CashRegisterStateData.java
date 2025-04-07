package org.lebastudios.theroundtable.plugincashregister.config;

import org.lebastudios.theroundtable.files.JsonFile;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegister;

import java.io.File;

public class CashRegisterStateData extends JsonFile<CashRegisterStateData>
{
    public boolean open = false;
    public String openTime = null;

    @Override
    public File getFile()
    {
        return new File(PluginCashRegister.getInstance().getPluginFolder(), "cash-register-state.json");
    }
}
