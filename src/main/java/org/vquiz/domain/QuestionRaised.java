package org.vquiz.domain;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

@Qualifier
@Target({METHOD, FIELD, PARAMETER, TYPE})
@Retention(RUNTIME)
public @interface QuestionRaised {
    
}
