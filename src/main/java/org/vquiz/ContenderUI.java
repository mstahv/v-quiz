package org.vquiz;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.cdi.CDIUI;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Topic;
import org.vaadin.maddon.button.PrimaryButton;
import org.vaadin.maddon.label.Header;
import org.vaadin.maddon.label.RichText;
import org.vaadin.maddon.layouts.MHorizontalLayout;
import org.vaadin.maddon.layouts.MVerticalLayout;
import org.vquiz.domain.Answer;
import org.vquiz.domain.Question;
import org.vquiz.domain.User;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
@CDIUI("")
@Theme("valo")
@Push
public class ContenderUI extends AbstractQuizUI {

    private static final int PENALTY_SECONDS = 10;
    
    @Resource
    private ManagedExecutorService managedExecutorService;

    @Inject
    private JMSContext jmsContext;

    @Resource(name = Resources.TOPIC_NAME)
    private Topic topic;

    @Inject
    Repository repo;

    @Inject
    MessageList messageList;

    private Question question;

    Header questionLabel = new Header("Waiting for new quiz...").setHeaderLevel(
            3);
    RichText answerLabel = new RichText();
    TextField answerField = new TextField();
    Button suggest = new PrimaryButton("Suggest answer", this::onSuggestClick);

    MHorizontalLayout answeringControls = new MHorizontalLayout(
            answerField,
            suggest).withMargin(false);

    private User user = new User();

    @Inject
    UserForm loginForm;

    @Override
    protected void init(VaadinRequest request) {

    	Logger.getLogger(ContenderUI.class.getName()).info("init");
    	
    	setComponentIds();
    	
        loginForm.showModal(this);

        answerLabel.setVisible(false);
        suggest.setEnabled(false);

        new MessageListener(this).startListening();

        setContent(
                new MVerticalLayout(
                        new RichText().withMarkDownResource("/intro.md")
                ).expand(
                        new MHorizontalLayout(
                                new MVerticalLayout(
                                        questionLabel,
                                        answerLabel,
                                        answeringControls).withCaption(
                                        "Current Quiz:").withMargin(false),
                                messageList
                        ).withFullWidth()
                )
        );

        joinExistingQuiz();

        addDetachListener(e -> {
            postMessage(user.getUsername() + " left");
            repo.removeUser(user);
        });

    }

    private void setComponentIds() {
    	questionLabel.setId("question-label");
    	answerLabel.setId("answer-label");
    	answerField.setId("answer-field");
    	suggest.setId("suggest-field");
    	loginForm.setId("login-form");
	}

	public void onSuggestClick(Button.ClickEvent event) {
        final String answer = answerField.getValue();
        Logger.getLogger(ContenderUI.class.getName()).info("user "+user.getUsername()+" - answer "+answer);
        postAnswer(answer);
        if (!question.getAnswer().toLowerCase().equals(answer.toLowerCase())) {
        	Logger.getLogger(ContenderUI.class.getName()).info("user "+user.getUsername()+" - wrong answer");
            Notification.show("Thats wrong! ",
                    PENALTY_SECONDS + "s penalty started...",
                    Notification.Type.HUMANIZED_MESSAGE);
            suggest.setEnabled(false);
            managedExecutorService.submit(() -> {
                try {
                    Thread.sleep(PENALTY_SECONDS * 1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ContenderUI.class.getName()).
                            log(Level.SEVERE, null, ex);
                }
                access(() -> suggest.setEnabled(true));
            });
        }
    }

    public User getUser() {
        return user;
    }

    public void login() throws Exception {
        if (repo.isReserved(user.getUsername())) {
        	Logger.getLogger(ContenderUI.class.getName()).info("user "+user.getUsername()+" - Username taken");
            throw new Exception("Username taken");
        } else {
            repo.save(user);
            postMessage(user.getUsername() + " joined");
            Logger.getLogger(ContenderUI.class.getName()).info("user "+user.getUsername()+" - joined");
        }
    }

    void joinExistingQuiz() {
        Question current = repo.getCurrent();
        if (current != null) {
            questionChanged(current);
        }
    }

    @Override
    public void showMessage(String text) {
        messageList.addMessage(text);
    }

    @Override
    public void questionChanged(Question question) {
    	Logger.getLogger(ContenderUI.class.getName()).info("user "+user.getUsername()+" - questionChanged: "+question.getQuestion());
        this.question = question;
        if (question.isSolved()) {
            questionLabel.setText(question.getQuestion());
            answerLabel.withMarkDown(
                    "Quiz was solved by " + question.getWinner() + ": *" + question.
                    getAnswer() + "*");
            answerLabel.setVisible(true);
            answeringControls.setVisible(false);
            suggest.setEnabled(false);

            if (question.getWinner().equals(user.getUsername())) {
            	Logger.getLogger(ContenderUI.class.getName()).info("user "+user.getUsername()+" - WON!");
                Notification.show("Congrats!", "You won!",
                        Notification.Type.WARNING_MESSAGE);
            } else {
                Notification.show("Dough!!", "Quiz was solved by " + question.
                        getWinner() + ". Be sharper next time!",
                        Notification.Type.WARNING_MESSAGE);
            }
        } else {
            questionLabel.setText(question.getQuestion());
            answerLabel.setVisible(false);
            Notification.show("New question: " + question.getQuestion());
            answerField.focus();
            suggest.setEnabled(true);
            answeringControls.setVisible(true);
        }
    }

    void postMessage(String message) {
        jmsContext.createProducer().send(topic, message);
    }

    void postAnswer(String answer) {
        jmsContext.createProducer().send(topic, new Answer(answer, user));
    }

    @Override
    public void answerSuggested(Answer answer) {
        messageList.addMessage(answer.getUser() + " suggested *" + answer.
                getAnswer() + "*");
    }

}
