/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vquiz;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import org.vquiz.domain.Answer;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
@MessageDriven(mappedName = Resources.TOPIC_NAME, activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = Resources.TOPIC_NAME)
})
public class MessageBean implements MessageListener {

    @Inject
    ActiveUIs activeUIs;

    @Inject
    private JMSContext jmsContext;

    @Resource(name = Resources.TOPIC_NAME)
    private Topic topic;

    public MessageBean() {
    }

    @PostConstruct
    public void init() {
        System.err.println("MessageBean INSTANTIATED");
    }

    @PreDestroy
    public void destroy() {
        System.err.println("MessageBean DESTROYED");
    }

    @Override
    public void onMessage(Message message) {
        System.err.println("MSG IN");
        activeUIs.handleMessage(message);
    }

    public void postMessage(String msg) {
        if (msg != null && !msg.isEmpty()) {
            jmsContext.createProducer().send(topic, msg);
        }
    }

    public void postAnswer(Answer answer) {
        jmsContext.createProducer().send(topic, answer);
    }

}
