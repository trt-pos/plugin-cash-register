package org.lebastudios.theroundtableplugins.cr.reports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.hibernate.Session;
import org.lebastudios.theroundtable.config.EstablishmentConfigData;
import org.lebastudios.theroundtable.config.GlobalPreferencesConfigData;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtable.plugins.PluginLoader;
import org.lebastudios.theroundtableplugins.cr.PluginCashRegisterEvents;
import org.lebastudios.theroundtableplugins.cr.entities.Receipt;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class ReceiptReportGenerator
{
    public JasperPrint generate(int receiptId)
    {
        return Database.getInstance().connectQuery(session ->
        {
            return generate(receiptId, session);
        });
    }

    public JasperPrint generate(int receiptId, Session session)
    {
        return generate(
            session.get(Receipt.class, receiptId)
        );
    }
    
    public JasperPrint generate(Receipt receipt)
    {
        try (InputStream inputStream = ReceiptReportGenerator.class.getResourceAsStream("receipt.jasper"))
        {
            Thread.currentThread().setContextClassLoader(PluginLoader.getInstance().getPluginsClassLoader());
            
            HashMap<String, Object> params = new HashMap<>();
            EstablishmentConfigData establishmentDat = new EstablishmentConfigData().load();
            GlobalPreferencesConfigData globalPreferencesDat = new GlobalPreferencesConfigData().load();
            
            params.put("establishment_name", establishmentDat.name);
            params.put("establishment_id", establishmentDat.id);
            params.put("establishment_address", establishmentDat.address + " " + establishmentDat.city + " " + establishmentDat.zipCode);
            params.put("establishment_tlf", establishmentDat.phone);
            
            params.put("receipt_date", DateTimeFormatter.ofPattern(
                    new GlobalPreferencesConfigData().load().dateTimeFormatter
            ).format(receipt.getTransaction().getDate()));
            
            params.put("t.simplifiedreceipt", Translator.getInstance().t("cr:phrase.simplifiedreceipt"));
            params.put("t.table", Translator.getInstance().t("cr:phrase.tablename"));
            params.put("t.client", Translator.getInstance().t("cr:word.client"));
            params.put("t.attendedby", Translator.getInstance().t("cr:phrase.attendedby"));
            params.put("t.qty", Translator.getInstance().t("cr:word.qty"));
            params.put("t.product", Translator.getInstance().t("cr:word.product"));
            params.put("t.price", Translator.getInstance().t("cr:word.price"));
            params.put("t.import", Translator.getInstance().t("cr:word.import"));
            params.put("t.total", "TOTAL");
            params.put("t.method", Translator.getInstance().t("cr:word.method"));
            params.put("t.amount", Translator.getInstance().t("cr:word.amount"));
            params.put("t.change", Translator.getInstance().t("cr:word.change"));
            
            StringBuffer receipt_id = new StringBuffer();
            PluginCashRegisterEvents.onRequestReceiptBillNumber.invoke(
                    new PluginCashRegisterEvents.BillNumberRequestData(receipt.getId(), receipt_id)
            );
            params.put("receipt_id", receipt_id.isEmpty() ? String.valueOf(receipt.getId()) : receipt_id.toString());
            
            params.put("currency_symbol", String.valueOf(globalPreferencesDat.currency.symbol()));
            params.put("currency_abrv", globalPreferencesDat.currency.abbreviation());
            
            params.put("receipt", receipt);
            JRDataSource dataSource = new JRBeanCollectionDataSource(receipt.getProducts());
            
            return JasperFillManager.fillReport(
                inputStream,
                params,
                dataSource
            );
        }
        catch (IOException | JRException e)
        {
            throw new RuntimeException(e);
        }
    }
}
