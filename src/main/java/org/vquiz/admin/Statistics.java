package org.vquiz.admin;

import java.io.Serializable;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
public class Statistics implements Serializable {

    private final long start = System.currentTimeMillis();
    private long end;

    private int answers = 0;

    public void reportAnswer() {
        answers++;
    }

    public int getAnswers() {
        return answers;
    }

    public void close() {
        end = System.currentTimeMillis();
    }

    public double answersPerSecond() {
        return answers / ((end - start) / 1000.0);
    }

    @Override
    public String toString() {
        if (end != 0) {
            return answersPerSecond() + "answers per second";
        }
        return "Not closed statistics";
    }

}
