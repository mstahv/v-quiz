package org.vquiz;

import java.io.Serializable;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import org.vaadin.msghub.AbstractMessageHub;
import org.vquiz.domain.Answer;
import org.vquiz.domain.Question;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
public class MessageListener extends AbstractMessageHub<AbstractQuizUI> {

    public MessageListener(AbstractQuizUI mainUI) {
        super(mainUI);
    }

    @Override
    protected Class<AbstractQuizUI> getUiClass() {
        return AbstractQuizUI.class;
    }

    @Override
    protected String getTopicName() {
        return Resources.TOPIC_NAME;
    }

    @Override
    protected void handleMessage(AbstractQuizUI ui, Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                ui.access(() -> ui.showMessage(text));
            } else {
                ObjectMessage objectMessage = (ObjectMessage) message;
                Serializable object = objectMessage.getObject();
                if (object instanceof Question) {
                    Question question = (Question) object;
                    ui.access(() -> ui.questionChanged(question));
                } else if (object instanceof Answer) {
                    Answer answer = (Answer) object;
                    ui.access(() -> ui.answerSuggested(answer));
                }
            }
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }

}
