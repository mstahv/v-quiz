package org.vquiz.admin;

import org.vquiz.domain.Question;

import org.vaadin.maddon.fields.MTable;

/**
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
public class RecentQuestions extends MTable<Question> {

	public RecentQuestions() {
		super(Question.class);
        setCaption("Recent questions");
		withFullWidth();
	}

}
