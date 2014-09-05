package org.vquiz.admin;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.AxisType;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.Credits;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.addon.charts.model.DataSeriesItem;
import com.vaadin.addon.charts.model.Marker;
import com.vaadin.addon.charts.model.PlotOptionsSeries;
import com.vaadin.addon.charts.model.style.SolidColor;
import javax.ejb.EJB;
import org.vaadin.maddon.layouts.MHorizontalLayout;
import org.vaadin.maddon.layouts.MVerticalLayout;
import org.vquiz.Repository;

/**
 * This component uses some charts to display some live data about the quiz game
 * usage.
 * 
 * @author Matti Tahvonen <matti@vaadin.com>
 */
public class Statistics extends MVerticalLayout {

    @EJB
    Repository repo;

    private long start = System.currentTimeMillis();
    private long end;

    private int answers = 0;

    private final Chart activeUsers = new Chart(ChartType.COLUMN);
    private final DataSeries activeUsersDs = new DataSeries("Total");
    private final DataSeries localUsersDs = new DataSeries("This node");
    private final Chart answersPerSecond = new Chart(ChartType.LINE);
    private final DataSeries answersPerSecondDs = new DataSeries("Ansers/second");
    private final Chart totalSuggestion = new Chart(ChartType.AREA);
    private final DataSeries totalSuggestionsDs = new DataSeries(
            "Total suggestions");
    private final Chart memUsage = new Chart(ChartType.AREA);
    private final DataSeries memUsageDs = new DataSeries("Heap size, MB");

    public Statistics() {
        activeUsers.getConfiguration().addSeries(activeUsersDs);
        activeUsers.getConfiguration().addSeries(localUsersDs);
        configureChart(activeUsers, "Active users", false);
        answersPerSecond.getConfiguration().addSeries(answersPerSecondDs);
        configureChart(answersPerSecond, "Answers per second", false);
        totalSuggestion.getConfiguration().addSeries(totalSuggestionsDs);
        configureChart(totalSuggestion, "Suggestion count)", false);
        memUsage.getConfiguration().addSeries(memUsageDs);
        configureChart(memUsage, "Heap (MB, this node)", true);

        setMargin(false);
        add(
                new MHorizontalLayout(
                        activeUsers,
                        answersPerSecond
                ).withFullWidth().withHeight("150px"),
                new MHorizontalLayout(
                        totalSuggestion,
                        memUsage
                ).withFullWidth().withHeight("150px")
        );
    }

    public void reportAnswer() {
        answers++;
    }

    public int getAnswers() {
        return answers;
    }

    public void report() {
        end = System.currentTimeMillis();

        boolean shift = activeUsersDs.size() > 10;
        final long now = System.currentTimeMillis();
        activeUsersDs.
                add(new DataSeriesItem(now, repo.getUserCount()), true,
                        shift);
        localUsersDs.
                add(new DataSeriesItem(now, repo.getLocalUserCount()), true,
                        shift);
        answersPerSecondDs.add(new DataSeriesItem(now,
                answersPerSecond()), true, shift);

        totalSuggestionsDs.
                add(new DataSeriesItem(now, repo.getSugestionCount()), true,
                        shift);

        memUsageDs.add(new DataSeriesItem(now, Runtime.getRuntime().
                totalMemory() / (1024 * 1024)), true, shift);

        start = end;
        answers = 0;
    }

    public double answersPerSecond() {
        return answers / ((end - start) / 1000.0);
    }

    private void configureChart(Chart chart, String caption, boolean credits) {
        chart.setCaption(caption);
        chart.setSizeFull();

        chart.getConfiguration().setCredits(new Credits(credits));
        chart.getConfiguration().getLegend().setEnabled(false);
        chart.getConfiguration().getxAxis().setType(AxisType.DATETIME);
        chart.getConfiguration().getyAxis().setTitle("");
        chart.getConfiguration().setTitle("");
        chart.getConfiguration().getChart().setBackgroundColor(new SolidColor(0,
                0, 0, 0));
        PlotOptionsSeries po = new PlotOptionsSeries();
        po.setMarker(new Marker(false));
        chart.getConfiguration().setPlotOptions(po);
    }

}
