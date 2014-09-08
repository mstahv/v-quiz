package org.vquiz;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import org.vaadin.maddon.button.PrimaryButton;
import org.vaadin.maddon.fields.MTextField;
import org.vaadin.maddon.form.AbstractForm;
import org.vaadin.maddon.label.RichText;
import org.vaadin.maddon.layouts.MVerticalLayout;
import org.vquiz.domain.User;

public class LoginWindow extends AbstractForm<User> {

    TextField username = new MTextField("Username");

    @Override
    protected Component createContent() {
        return new MVerticalLayout(
                new RichText().withMarkDown("You need a username to join the "
                        + "quiz. Username must have at least *4 letters*."),
                new FormLayout(
                        username
                ),
                getToolbar()
        ).withSpacing(false);
    }

    @Override
    protected Button createSaveButton() {
        return new PrimaryButton("Login");
    }

    @Override
    protected Button createCancelButton() {
        return new Button("Reset");
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
