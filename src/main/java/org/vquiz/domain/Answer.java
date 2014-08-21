
package org.vquiz.domain;

import java.io.Serializable;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
public class Answer implements Serializable {

    private String answer;
    private User user;
    
    public Answer() {
    }

    public Answer(String answer, User user) {
        this.answer = answer;
        this.user = user;
    }
    
    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    
    

}
