package org.vquiz.admin;

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
import com.vaadin.ui.UI;
import javax.ejb.EJB;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.vaadin.maddon.button.PrimaryButton;
import org.vaadin.maddon.fields.MTextField;
import org.vaadin.maddon.layouts.MHorizontalLayout;
import org.vaadin.maddon.layouts.MVerticalLayout;
import org.vquiz.MessageList;
import org.vquiz.Repository;
import org.vquiz.domain.Answer;
import org.vquiz.domain.Question;
import org.vquiz.domain.User;
import org.vquiz.domain.QuestionRaised;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
@CDIUI
@Theme("valo")
@Widgetset("org.vquiz.admin.AdminWidgetSet")
@Push
@Title("Quiz admin panel")
public class AdminUI extends UI {

    protected static final String NO_ACTIVE_QUESTION = "No active question";

    @EJB
    Repository repo;

    @Inject
    RecentQuestions recentQuestions;

    @Inject
    QuestionForm form;

    @Inject
    private Statistics statistics;

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

    @Override
    protected void init(VaadinRequest request) {
        // Show password promt by default
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
        form.setEntity(new Question());

        PasswordField newPasswordField = new PasswordField("Set admin password");
        newPasswordField.addValueChangeListener(e -> {
            repo.setAdminPassword(newPasswordField.getValue());
        });

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
                                statistics,
                                messageList
                        )
                ).withFullWidth(),
                recentQuestions
        ));

        repo.addListener(this);

        setPollInterval(5000);
        addPollListener(e -> {
            updateStatistics();
        });

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
        recentQuestions.addBeans(question);
    }

    public void showMessage(String text) {
        access(() -> {
            messageList.addMessage(text);
        });
    }

    public void questionChanged(Question question) {
        access(() -> {
            currentQuestion.setValue(question.getQuestion() + " - " + question.
                    getAnswer());
            messageList.
                    addMessage("New quiz started: " + question.getQuestion());
        });
    }

    public void answerSuggested(Answer answer) {
        statistics.reportAnswer();
        if (activeQuestion.matches(answer)) {
            access(() -> {
                activeQuestion.setWinner(answer.getUser().getUsername());
                repo.save(activeQuestion);
                postMessage(answer.getUser().getUsername() + " WON!");
            });
        }
    }

    private void updateStatistics() {
        statistics.report();
    }

}
