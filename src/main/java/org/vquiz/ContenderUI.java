package org.vquiz;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.cdi.CDIUI;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.ejb.EJB;
import javax.inject.Inject;
import org.vaadin.maddon.button.MButton;
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
@Title("The Quiz")
@Push
public class ContenderUI extends UI {

    private static final int PENALTY_SECONDS = 10;

    // Managed executor service has too strict restrictions, does not allow enough tasks
    private static final ScheduledExecutorService executorService = Executors.
            newScheduledThreadPool(10);

    @EJB
    Repository repo;

    @Inject
    MessageList messageList;

    private Question question;

    Header questionLabel = new Header("Waiting for new quiz...").setHeaderLevel(
            2);
    RichText answerLabel = new RichText();
    TextField answerField = new TextField();
    Button suggest = new PrimaryButton("Suggest answer", this::onSuggestClick);

    Button help = new MButton(FontAwesome.LIFE_RING).withStyleName("link").withListener(e -> {
        Window window = new Window("What it this about?",
                new MVerticalLayout(
                        new RichText().withMarkDownResource("/intro.md")
                )
        );
        window.setWidth("80%");
        window.setHeight("50%");
        window.setModal(true);
        addWindow(window);
    });

    MHorizontalLayout answeringControls = new MHorizontalLayout()
            .expand(answerField)
            .with(suggest)
            .withMargin(false).withFullWidth();

    private final User user = new User();

    @Inject
    LoginWindow loginForm;

    @Override
    protected void init(VaadinRequest request) {

        loginForm.showModal(this);

        answerLabel.setVisible(false);
        suggest.setEnabled(false);

        Panel currentPanel = new Panel("Current Question",
                new MVerticalLayout(
                        questionLabel,
                        answerLabel,
                        answeringControls
                )
        );
        currentPanel.setWidth(LAYOUT_WIDTH);
        currentPanel.setIcon(FontAwesome.GRADUATION_CAP);

        Panel messages = new Panel("Hints and game flow", messageList);
        messages.setIcon(FontAwesome.BELL);
        messages.setStyleName(ValoTheme.PANEL_WELL);
        messages.setWidth(LAYOUT_WIDTH);
        messages.setHeight("300px");

        setContent(
                new MVerticalLayout(
                        currentPanel,
                        messages,
                        help
                ).alignAll(Alignment.TOP_CENTER)
        );

    }
    protected static final String LAYOUT_WIDTH = "600px";

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
            throw new Exception("That username taken, choose another");
        } else {
            repo.save(user);
            repo.addListener(this);
            // Disabled as crafting load test becomes tricky with non-deterministic
            // changes in UI
            // postMessage(user.getUsername() + " joined");
            Notification.show("Welcome " + user.getUsername() + "!",
                    Notification.Type.TRAY_NOTIFICATION);
            messageList.addMessage("You joined");
            joinExistingQuiz();
        }
    }

    void joinExistingQuiz() {
        Question current = repo.getCurrent();
        if (current != null) {
            questionChanged(current);
        }
    }

    public void showMessage(String text) {
        access(() -> {
            messageList.addMessage(text);
        });
    }

    public void questionChanged(Question question) {
        access(() -> {

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
                    Notification.show("Ooops!!",
                            "Quiz was solved by " + question.
                            getWinner() + ". Be sharper next time!",
                            Notification.Type.WARNING_MESSAGE);
                }
            } else {
                questionLabel.setText(question.getQuestion());
                answerLabel.setVisible(false);
                Notification.show("New question: " + question.getQuestion());
                messageList.addMessage("New question raised: *" + question.
                        getQuestion() + "*");
                answerField.focus();
                suggest.setEnabled(true);
                answeringControls.setVisible(true);
            }
        });
    }

    void postMessage(String message) {
        repo.save(message);
    }

    void postAnswer(String answer) {
        repo.save(new Answer(answer, user));
    }

}
