package org.lebastudios.theroundtable.plugincashregister.sessions;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import lombok.NonNull;
import org.lebastudios.theroundtable.controllers.PaneController;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtable.plugincashregister.entities.CashSession;
import org.lebastudios.theroundtable.ui.MultipleItemsListView;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

public class CashSessionsPaneController extends PaneController<CashSessionsPaneController>
{
    @FXML public DatePicker fromDatePicker;
    @FXML public DatePicker toDatePicker;
    @FXML public ChoiceBox<String> installationNameChoiceBox;
    @FXML public MultipleItemsListView<CashSession> sessionsListView;

    MultipleItemsListView.ItemsGenerator<CashSession> itemsGenerator = new MultipleItemsListView.ItemsGenerator<>()
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
                                        "and s.closingDate <= :toDate",
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

    Function<MultipleItemsListView<CashSession>, MultipleItemsListView.ICellRecicler<CashSession>> cellReciclerFunction = new Function<MultipleItemsListView<CashSession>, MultipleItemsListView.ICellRecicler<CashSession>>() {
        @Override
        public MultipleItemsListView.ICellRecicler<CashSession> apply(MultipleItemsListView<CashSession> listView)
        {
            return new MultipleItemsListView.ICellRecicler<>()
            {
                private final SessionLabelController sessionLabelController = new SessionLabelController();
                
                @Override
                public void update(@NonNull CashSession item)
                {
                    sessionLabelController.updateView(item);
                }

                @Override
                public Node getGraphic()
                {
                    return sessionLabelController.getRoot();
                }

                @Override
                public String getText()
                {
                    return null;
                }
            };
        }
    };
    
    @Override
    protected void initialize()
    {
        fromDatePicker.setValue(LocalDate.now().minusMonths(1));
        toDatePicker.setValue(LocalDate.now());

        List<String> installations = Database.getInstance().connectQuery(session ->
        {
            return session.createQuery("select i.name from AppInstallation i", String.class)
                    .list();
        });

        installationNameChoiceBox.getItems().clear();
        installationNameChoiceBox.getItems().add(Translator.getInstance().t("plugincashregister.sessionspane.all"));
        installationNameChoiceBox.getItems().addAll(installations);
        installationNameChoiceBox.getSelectionModel().selectFirst();

        sessionsListView.setGroupSize(50);
        sessionsListView.setItemsGenerator(itemsGenerator);
        sessionsListView.setCellReciclerGenerator(cellReciclerFunction);
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
