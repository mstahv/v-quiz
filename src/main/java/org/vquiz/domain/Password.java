
package org.vquiz.domain;

import java.io.Serializable;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
public class Password implements Serializable {
    private String pw;

    public Password(String pw) {
        this.pw = pw;
    }

    public String getPw() {
        return pw;
    }

    public void setPw(String pw) {
        this.pw = pw;
    }
    
}
