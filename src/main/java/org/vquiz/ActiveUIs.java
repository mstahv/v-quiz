/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vquiz;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import org.vquiz.domain.Answer;
import org.vquiz.domain.Question;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
@ApplicationScoped
public class ActiveUIs {

    Collection<AbstractQuizUI> uis = new LinkedList<>();

    public void register(AbstractQuizUI ui) {
        uis.add(ui);
        ui.addDetachListener(e->{
            deregister(ui);
        });
    }

    public void deregister(AbstractQuizUI ui) {
        uis.remove(ui);
    }

    public void handleMessage(Message message) {
        try {
            for (AbstractQuizUI ui : uis) {
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
            }
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }

    public AbstractQuizUI[] getUis() {
        return uis.toArray(new AbstractQuizUI[uis.size()]);
    }

}
