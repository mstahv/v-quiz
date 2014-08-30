package org.vquiz.admin;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.cdi.CDIUI;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Topic;
import org.apache.commons.beanutils.BeanUtils;
import org.vaadin.maddon.button.PrimaryButton;
import org.vaadin.maddon.fields.MTextField;
import org.vaadin.maddon.label.Header;
import org.vaadin.maddon.layouts.MHorizontalLayout;
import org.vaadin.maddon.layouts.MVerticalLayout;
import org.vquiz.domain.Answer;
import org.vquiz.domain.Question;
import org.vquiz.qualifiers.QuestionRaised;
import org.vquiz.AbstractQuizUI;
import org.vquiz.ActiveUIs;
import org.vquiz.MessageBean;
import org.vquiz.MessageList;
import org.vquiz.Repository;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
@CDIUI
@Theme("valo")
@Push
public class AdminUI extends AbstractQuizUI {

    protected static final String NO_ACTIVE_QUESTION = "No active question";

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "jms/topic/mytopic")
    private Topic topic;

    @Inject
    Repository repo;

    @Inject
    RecentQuestions recentQuestions;

    @Inject
    QuestionForm form;
    
    @EJB
    MessageBean mBean;
    
    @Inject
    ActiveUIs activeUIs;

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

        activeUIs.register(this);

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

        setContent(new MVerticalLayout(
                new Header("Quiz view"),
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
                        messageList
                ).withFullWidth(),
                recentQuestions
        ));
    }

    private void postMessage(String msg) {
        if (msg != null && !msg.isEmpty()) {
            msg = "**" + msg + "**"; // emphasis admin messages with markdown
            jmsContext.createProducer().send(topic, msg);
        }
    }

    /* Admin controller */
    void onQuestionRaised(@Observes @QuestionRaised Question question) {
        question.setWinner(null);
        // save the current question into shared memory for users who join 
        // during quiz
        repo.save(question);
        // post it to active users via JMS
        jmsContext.createProducer().send(topic, question);
        activeQuestion = question;
    }

    @Override
    public void showMessage(String text) {
        messageList.addMessage(text);
    }

    @Override
    public void questionChanged(Question question) {
        currentQuestion.setValue(question.getQuestion() + " - " + question.
                getAnswer());
        messageList.addMessage("New quiz started: " + question.getQuestion());
    }

    @Override
    public void answerSuggested(Answer answer) {
        final String message = answer.getUser() + " suggested *" + answer.
                getAnswer() + "*";
        if (activeQuestion != null && activeQuestion.getAnswer().toLowerCase().
                equals(answer.getAnswer().
                        toLowerCase())) {
            activeQuestion.setWinner(answer.getUser().getUsername());
            repo.save(activeQuestion);
            jmsContext.createProducer().send(topic, activeQuestion);
            postMessage(answer.getUser().getUsername() + " WON!");
            try {
                recentQuestions.addBeans((Question) BeanUtils.cloneBean(
                        activeQuestion));
            } catch (Exception ex) {
                Logger.getLogger(AdminUI.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }
        messageList.addMessage(message);
    }

}
