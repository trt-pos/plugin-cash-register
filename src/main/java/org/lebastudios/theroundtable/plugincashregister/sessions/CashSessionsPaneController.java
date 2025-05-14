package org.lebastudios.theroundtable.plugincashregister.sessions;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.entities.AppInstallation;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.components.PaginableListView;

import java.time.LocalDate;
import java.util.List;

public class CashSessionsPaneController extends PaneController<CashSessionsPaneController>
{
    @FXML public DatePicker fromDatePicker;
    @FXML public DatePicker toDatePicker;
    @FXML public ChoiceBox<String> installationNameChoiceBox;
    @FXML public PaginableListView<CashSession> sessionsListView;

    PaginableListView.ItemsGenerator<CashSession> itemsGenerator = new PaginableListView.ItemsGenerator<>()
    {
        @Override
        public List<CashSession> generateItems(int from, int to)
        {
            return Database.getInstance().connectQuery(session ->
            {
                String installationNamePattern = installationNameChoiceBox.getSelectionModel().getSelectedIndex() == 0
                        ? "%" : installationNameChoiceBox.getValue();

                return session.createQuery("from CashSession s " +
                                        "where s.appInstallation.name like :installationNamePattern " +
                                        "and s.openingDate >= :fromDate " +
                                        "and s.closingDate <= :toDate " +
                                        "order by s.openingDate desc",
                                CashSession.class)
                        .setParameter("installationNamePattern", installationNamePattern)
                        .setParameter("fromDate", fromDatePicker.getValue().atStartOfDay())
                        .setParameter("toDate", toDatePicker.getValue().atTime(23, 59, 59))
                        .setFirstResult(from)
                        .setMaxResults(to)
                        .list();
            });
        }

        @Override
        public long count()
        {
            return Database.getInstance().connectQuery(session ->
            {
                String installationNamePattern = installationNameChoiceBox.getSelectionModel().getSelectedIndex() == 0
                        ? "%" : installationNameChoiceBox.getValue();

                return session.createQuery("select count(*) from CashSession s " +
                                        "where s.appInstallation.name like :installationNamePattern " +
                                        "and s.openingDate >= :fromDate " +
                                        "and s.closingDate <= :toDate",
                                Long.class)
                        .setParameter("installationNamePattern", installationNamePattern)
                        .setParameter("fromDate", fromDatePicker.getValue().atStartOfDay())
                        .setParameter("toDate", toDatePicker.getValue().atTime(23, 59, 59))
                        .uniqueResult();
            });
        }
    };
    
    @Override
    protected void initialize()
    {
        fromDatePicker.setValue(LocalDate.now().minusMonths(1));
        toDatePicker.setValue(LocalDate.now());

        Database.getInstance().connectQuery(session ->
        {
            var installations = session.createQuery("select i.name from AppInstallation i", String.class)
                    .list();

            installationNameChoiceBox.getItems().clear();
            installationNameChoiceBox.getItems().add(Translator.getInstance().t("plugincashregister.sessionspane.all"));
            installationNameChoiceBox.getItems().addAll(installations);
            installationNameChoiceBox.getSelectionModel().select(AppInstallation.thisInstalation(session).getName());
        });

        sessionsListView.setGroupSize(50);
        sessionsListView.setItemsGenerator(itemsGenerator);
        sessionsListView.setReciclablePaneFactory(SessionLabelController::new);
        sessionsListView.refresh();
        
        installationNameChoiceBox.getSelectionModel().selectedItemProperty().addListener((_, _, _) ->
        {
            sessionsListView.refresh();
        });
        
        fromDatePicker.valueProperty().addListener((_, _, newValue) ->
        {
            if (newValue == null) return;
            
            if (newValue.isAfter(toDatePicker.getValue()))
            {
                fromDatePicker.setValue(toDatePicker.getValue());
            }
            
            sessionsListView.refresh();
        });
        
        toDatePicker.valueProperty().addListener((_, _, newValue) ->
        {
            if (newValue == null) return;
            
            if (newValue.isBefore(fromDatePicker.getValue()))
            {
                toDatePicker.setValue(fromDatePicker.getValue());
            }
            
            sessionsListView.refresh();
        });
    }
}
