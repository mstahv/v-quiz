package org.vquiz;

import com.vaadin.ui.VerticalLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.vaadin.maddon.label.RichText;

/**
 * A simple component to display n latest text messages.
 * 
 * @author Matti Tahvonen <matti@vaadin.com>
 */
public class MessageList extends VerticalLayout {

    private static final int MAX_LENGHT = 10;

    public MessageList() {
        setCaption("Quiz events and hints:");
    }

    public void addMessage(String message) {
        RichText text = new RichText().withMarkDown(
                "*" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ":* " + message);
        addComponentAsFirst(text);
        trimToMaxLenght();
    }

    private void trimToMaxLenght() {
        while (getComponentCount() > MAX_LENGHT) {
            removeComponent(getComponent(MAX_LENGHT));
        }
    }

}
