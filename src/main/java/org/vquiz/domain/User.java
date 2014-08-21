package org.vquiz.domain;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
public class User implements Serializable {

    @NotNull
    @Size(min = 4, max = 25)
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return username;
    }

}
