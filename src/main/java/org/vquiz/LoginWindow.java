package org.vquiz;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import org.vaadin.maddon.fields.MTextField;
import org.vaadin.maddon.form.AbstractForm;
import org.vaadin.maddon.layouts.MVerticalLayout;
import org.vquiz.domain.User;

public class LoginWindow extends AbstractForm<User> {

    TextField username = new MTextField("Username");

    @Override
    protected Component createContent() {
        return new MVerticalLayout(
                new FormLayout(
                        username
                ),
                getToolbar()
        );
    }

    public LoginWindow() {
        setEagarValidation(true);
    }

    public void showModal(ContenderUI ui) {
        setEntity(ui.getUser());
        Window window = new Window("Your details:");
        window.setWidth("60%");
        window.setClosable(false);
        window.setModal(true);
        window.setContent(this);
        UI.getCurrent().addWindow(window);
        setSavedHandler(e -> {
            try {
                ui.login();
                window.close();
            } catch(Exception ex) {
                Notification.show("Username is already reserved!",
                        Notification.Type.ERROR_MESSAGE);
            }
        });
        setResetHandler(e -> {
            username.setValue(null);
        });
        focusFirst();
    }

}
