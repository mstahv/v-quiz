package org.vquiz.domain;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
public class Question implements Serializable {
    
    @NotNull
    @Size(max = 140)
    private String question;

    @NotNull
    @Size(min = 2, max = 20)
    private String answer;
    
    private String winner;
    
    public boolean isSolved() {
        return winner != null;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    /**
     * Get the value of answer
     *
     * @return the value of answer
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Set the value of answer
     *
     * @param answer new value of answer
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * Get the value of question
     *
     * @return the value of question
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Set the value of question
     *
     * @param question new value of question
     */
    public void setQuestion(String question) {
        this.question = question;
    }

}
