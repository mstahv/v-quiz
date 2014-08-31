package org.vquiz.admin;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.AxisType;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.addon.charts.model.DataSeriesItem;
import com.vaadin.addon.charts.model.Marker;
import com.vaadin.addon.charts.model.PlotOptionsLine;
import com.vaadin.addon.charts.model.style.SolidColor;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.cdi.CDIUI;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.apache.commons.beanutils.BeanUtils;
import org.vaadin.maddon.button.PrimaryButton;
import org.vaadin.maddon.fields.MTextField;
import org.vaadin.maddon.label.Header;
import org.vaadin.maddon.layouts.MHorizontalLayout;
import org.vaadin.maddon.layouts.MVerticalLayout;
import org.vquiz.AbstractQuizUI;
import org.vquiz.MessageList;
import org.vquiz.Repository;
import org.vquiz.domain.Answer;
import org.vquiz.domain.Question;
import org.vquiz.domain.User;
import org.vquiz.qualifiers.QuestionRaised;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
@CDIUI
@Theme("valo")
@Widgetset("org.vquiz.admin.AdminWidgetSet")
@Push
@Title("Quiz admin panel")
public class AdminUI extends AbstractQuizUI {

    protected static final String NO_ACTIVE_QUESTION = "No active question";

    @EJB
    Repository repo;

    @Inject
    RecentQuestions recentQuestions;

    @Inject
    QuestionForm form;

    private final Label currentQuestion = new Label(NO_ACTIVE_QUESTION);
    private TextField newHintOrMessage = new MTextField().withInputPrompt(
            "Message or hint to contenders").withWidth("20em");

    private final PrimaryButton send = new PrimaryButton("Send", e -> {
        postMessage(newHintOrMessage.getValue());
        newHintOrMessage.setValue("");
        newHintOrMessage.focus();
    });

    private final MessageList messageList = new MessageList();
    private Question activeQuestion;

    private Chart activeUsers = new Chart(ChartType.COLUMN);
    private DataSeries activeUsersDs = new DataSeries();
    private Chart answersPerSecond = new Chart(ChartType.LINE);
    private DataSeries answersPerSecondDs = new DataSeries();
    private Chart cpuUsage = new Chart(ChartType.LINE);
    private DataSeries cpuUsageDs = new DataSeries();
    private Chart memUsage = new Chart(ChartType.LINE);
    private DataSeries memUsageDs = new DataSeries();

    @Override
    protected void init(VaadinRequest request) {
        setPollInterval(5000);
        addPollListener(e -> {
            updateStatistics();
        });

        PasswordField passwordField = new PasswordField(
                "Password (admin by default)");
        passwordField.addValueChangeListener(e -> {
            if (repo.adminPasswordMatches(passwordField.getValue())) {
                initActualUi();
            } else {
                Notification.show("Ooops!",
                        Notification.Type.HUMANIZED_MESSAGE);
            }
        });
        setContent(new MVerticalLayout(passwordField));
        passwordField.focus();

    }

    protected void initActualUi() {
        // Start receiving JMS messages from the quiz topic
        form.setEntity(new Question());

        PasswordField newPasswordField = new PasswordField("Set admin password");
        newPasswordField.addValueChangeListener(e -> {
            repo.setAdminPassword(newPasswordField.getValue());
        });

        activeUsers.getConfiguration().addSeries(activeUsersDs);
        configureChart(activeUsers, "Active users");
        answersPerSecond.getConfiguration().addSeries(answersPerSecondDs);
        configureChart(answersPerSecond, "Answers per second");
        cpuUsage.getConfiguration().addSeries(cpuUsageDs);
        configureChart(cpuUsage, "CPU (this node)");
        memUsage.getConfiguration().addSeries(memUsageDs);
        configureChart(memUsage, "Heap (MB, this node)");

        setContent(new MVerticalLayout(
                new MHorizontalLayout(
                        new MVerticalLayout(
                                new MVerticalLayout(
                                        currentQuestion,
                                        new MHorizontalLayout(newHintOrMessage,
                                                send)
                                ).withCaption("Current question"),
                                form,
                                newPasswordField
                        ).withMargin(false),
                        new MVerticalLayout(
                                new MHorizontalLayout(
                                        activeUsers,
                                        answersPerSecond
                                ).withFullWidth().withHeight("150px"),
                                new MHorizontalLayout(
                                        cpuUsage,
                                        memUsage
                                ).withFullWidth().withHeight("150px"),
                                messageList
                        )
                ).withFullWidth(),
                recentQuestions
        ));

        repo.addListener(this);
    }

    protected void configureChart(Chart chart, String caption) {
        chart.setSizeFull();
        chart.getConfiguration().getLegend().setEnabled(false);
        chart.getConfiguration().getxAxis().setType(AxisType.DATETIME);
        chart.getConfiguration().getyAxis().setTitle("");
        chart.getConfiguration().setTitle("");
        chart.getConfiguration().getChart().setBackgroundColor(new SolidColor(0,
                0, 0, 0));
        chart.setCaption(caption);
        PlotOptionsLine plotOptionsLine = new PlotOptionsLine();
        plotOptionsLine.setMarker(new Marker(false));
        chart.getConfiguration().setPlotOptions(plotOptionsLine);
    }

    private void postMessage(String msg) {
        if (msg != null && !msg.isEmpty()) {
            msg = "**" + msg + "**"; // emphasis admin messages with markdown
            repo.save(msg);
        }
    }

    /* Admin controller */
    void onQuestionRaised(@Observes @QuestionRaised Question question) {
        question.setWinner(null);
        // save the current question into shared memory for users who join 
        // during quiz
        repo.save(question);
        activeQuestion = question;
    }

    @Override
    public void showMessage(String text) {
        access(() -> {
            messageList.addMessage(text);
        });
    }

    @Override
    public void questionChanged(Question question) {
        access(() -> {
            currentQuestion.setValue(question.getQuestion() + " - " + question.
                    getAnswer());
            messageList.
                    addMessage("New quiz started: " + question.getQuestion());
        });
    }

    @Override
    public void answerSuggested(Answer answer) {
        access(() -> {
            statistics.reportAnswer();
            if (activeQuestion != null && activeQuestion.getAnswer().
                    toLowerCase().
                    equals(answer.getAnswer().
                            toLowerCase())) {
                activeQuestion.setWinner(answer.getUser().getUsername());
                repo.save(activeQuestion);
                postMessage(answer.getUser().getUsername() + " WON!");
                try {
                    recentQuestions.addBeans((Question) BeanUtils.cloneBean(
                            activeQuestion));

                } catch (Exception ex) {
                    Logger.getLogger(AdminUI.class
                            .getName()).
                            log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    @Override
    public void userJoined(User user) {

    }

    private Statistics statistics = new Statistics();

    OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.
            getOperatingSystemMXBean();

    private void updateStatistics() {
        statistics.close();
//        String msg = String.format(
//                "Active users: *%s*\n"
//                + "Answers per second: *%s*",
//                repo.userCount(),
//                statistics.answersPerSecond());
//        messageList.addMessage(msg);

        boolean shift = activeUsersDs.size() > 10;
        final long now = System.currentTimeMillis();
        activeUsersDs.
                add(new DataSeriesItem(now, repo.userCount()), true,
                        shift);
        answersPerSecondDs.add(new DataSeriesItem(now, statistics.
                answersPerSecond()), true, shift);

        cpuUsageDs.add(new DataSeriesItem(now, operatingSystemMXBean.
                getSystemLoadAverage()), true, shift);
        memUsageDs.add(new DataSeriesItem(now, Runtime.getRuntime().totalMemory()/(1024*1024)), true, shift);

        statistics = new Statistics();
    }

}
