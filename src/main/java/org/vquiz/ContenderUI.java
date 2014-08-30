package org.vquiz;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.cdi.CDIUI;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.ejb.EJB;
import javax.inject.Inject;
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

    // Managed executor service has too strict restrictions, does not allow enough tasks
    private static final ScheduledExecutorService executorService = Executors.
            newScheduledThreadPool(10);

    @Inject
    Repository repo;

    @Inject
    MessageList messageList;

    @Inject
    ActiveUIs activeUIs;

    @EJB
    MessageBean messageBean;

    private Question question;

    Header questionLabel = new Header("Waiting for new quiz...").setHeaderLevel(
            3);
    RichText answerLabel = new RichText();
    TextField answerField = new TextField();
    Button suggest = new PrimaryButton("Suggest answer", this::onSuggestClick);

    MHorizontalLayout answeringControls = new MHorizontalLayout(
            answerField,
            suggest).withMargin(false);

    private final User user = new User();

    @Inject
    UserForm loginForm;
    
    @Override
    protected void init(VaadinRequest request) {

        loginForm.showModal(this);

        answerLabel.setVisible(false);
        suggest.setEnabled(false);

        addDetachListener(e -> {
            repo.removeUser(user);
            // removed to make test more stable, on multiple runs
            // postMessage(user.getUsername() + " left");
        });
        
        activeUIs.register(this);

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

    }

    public void onSuggestClick(Button.ClickEvent event) {
        final String answer = answerField.getValue();
        postAnswer(answer);
        if (!question.getAnswer().toLowerCase().equals(answer.toLowerCase())) {
            Notification.show("Thats wrong! ",
                    PENALTY_SECONDS + "s penalty started...",
                    Notification.Type.HUMANIZED_MESSAGE);
            suggest.setEnabled(false);
            executorService.schedule(() -> {
                access(() -> suggest.setEnabled(true));
            }, PENALTY_SECONDS, TimeUnit.SECONDS);
        }
    }

    public User getUser() {
        return user;
    }

    public void login() throws Exception {
        if (repo.isReserved(user.getUsername())) {
            throw new Exception("Username taken");
        } else {
            repo.save(user);
            // Disabled as crafting load test becomes tricky with non-deterministic
            // changes in UI
            // postMessage(user.getUsername() + " joined");
            Notification.show("Welcome " + user.getUsername() + "!");
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
        messageBean.postMessage(message);
    }

    void postAnswer(String answer) {
        messageBean.postAnswer(new Answer(answer, user));
    }

    @Override
    public void answerSuggested(Answer answer) {
        // Disabled as crafting load test becomes tricky with non-deterministic
        // changes in UI
//        messageList.addMessage(answer.getUser() + " suggested *" + answer.
//                getAnswer() + "*");
    }

}
