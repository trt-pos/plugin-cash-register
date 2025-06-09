package org.lebastudios.theroundtable.plugincashregister.sessions;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import org.lebastudios.theroundtable.components.DateRangePicker;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.entities.AppInstallation;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.components.PaginableListView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class CashSessionsPaneController extends PaneController<CashSessionsPaneController>
{
    @FXML public ChoiceBox<String> installationNameChoiceBox;
    @FXML public PaginableListView<CashSession> sessionsListView;
    @FXML public DateRangePicker dateRangePicker;

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
                        .setParameter("fromDate", dateRangePicker.getStartDate().getValue().atStartOfDay())
                        .setParameter("toDate", dateRangePicker.getEndDate().getValue().atTime(LocalTime.MAX))
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
                        .setParameter("fromDate", dateRangePicker.getStartDate().getValue().atStartOfDay())
                        .setParameter("toDate", dateRangePicker.getEndDate().getValue().atTime(LocalTime.MAX))
                        .uniqueResult();
            });
        }
    };
    
    @Override
    protected void initialize()
    {
        dateRangePicker.getStartDate().set(LocalDate.now().minusMonths(1));

        Database.getInstance().connectQuery(session ->
        {
            var installations = session.createQuery("select i.name from AppInstallation i", String.class)
                    .list();

            installationNameChoiceBox.getItems().clear();
            installationNameChoiceBox.getItems().add(Translator.getInstance().t("cr:sessionspane.all"));
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

        dateRangePicker.setOnDateChange((_, _) -> sessionsListView.refresh());
    }
}
