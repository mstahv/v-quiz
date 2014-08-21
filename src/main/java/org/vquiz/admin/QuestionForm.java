package org.vquiz.admin;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.vaadin.maddon.fields.MTextField;
import org.vaadin.maddon.form.AbstractForm;
import org.vaadin.maddon.layouts.MVerticalLayout;
import org.vquiz.domain.Question;
import org.vquiz.qualifiers.QuestionRaised;

public class QuestionForm extends AbstractForm<Question> {

    @Inject
    @QuestionRaised
    javax.enterprise.event.Event<Question> questionRaised;

    TextField question = new MTextField("Question");

    TextField answer = new MTextField("Answer (case insensitive)");

    @Override
    protected Component createContent() {
        setCaption("Create new question");
        return new MVerticalLayout(
                new FormLayout(
                        question,
                        answer
                ),
                getToolbar()
        ).withMargin(false);
    }

    @Override
    protected Button createSaveButton() {
        return new Button("Start new quiz");
    }

    @PostConstruct
    void init() {
        setSavedHandler(myData -> {
            questionRaised.fire(myData);
        });
        setEagarValidation(true);
    }

}
