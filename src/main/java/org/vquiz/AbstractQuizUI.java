package org.vquiz;

import com.vaadin.ui.UI;
import org.vquiz.domain.Answer;
import org.vquiz.domain.Question;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
public abstract class AbstractQuizUI extends UI {

    public abstract void showMessage(String text);

    public abstract void questionChanged(Question question);

    public abstract void answerSuggested(Answer answer);

}