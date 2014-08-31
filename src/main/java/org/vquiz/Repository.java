package org.vquiz;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.vquiz.admin.AdminUI;
import org.vquiz.domain.Answer;
import org.vquiz.domain.Password;
import org.vquiz.domain.Question;
import org.vquiz.domain.User;

/**
 * This class stores all shared data in application. Instead of RDBMS we use
 * Infinispan.
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
@Singleton
@Listener
public class Repository {

    private static final String PASSWORD_KEY = "_password";
    private static final String CURRENT_QUESTION_KEY = "_currentQuestion";

    @Resource(lookup = "java:jboss/infinispan/container/myCache")
    CacheContainer cc;

    Collection<AbstractQuizUI> uis = new HashSet<>();

    Cache<String, Serializable> settings;
    Cache<String, User> users;
    Cache<Date, Answer> answers;
    Cache<Date, String> messages;

    @PostConstruct
    void init() {
        System.err.println("Repository instantiated");
        this.settings = cc.getCache();
        if (!settings.containsKey(PASSWORD_KEY)) {
            settings.put("_password", new Password("admin"));
        }
        users = cc.getCache("users");
        answers = cc.getCache("answers");
        messages = cc.getCache("hints");

        settings.addListener(this);
        users.addListener(this);
        answers.addListener(this);
        messages.addListener(this);
    }

    public Question getCurrent() {
        return (Question) settings.get(CURRENT_QUESTION_KEY);
    }

    public void save(Question data) {
        settings.put(CURRENT_QUESTION_KEY, data);
    }

    public Serializable findByName(String key) {
        return settings.get(key);
    }

    public boolean isReserved(String username) {
        return findByName(username) != null;
    }

    public void save(User user) {
        users.put(user.getUsername(), user);
    }

    public void removeUser(User user) {
        users.remove(user.getUsername());
    }

    public boolean adminPasswordMatches(String value) {
        Password pw = (Password) settings.get(PASSWORD_KEY);
        if (pw == null) {
            pw = new Password(("admin"));
        }
        return pw.getPw().equals(value);
    }

    public void save(Answer answer) {
        answers.put(new Date(), answer);
    }

    public void save(String hint) {
        messages.put(new Date(), hint);
    }

    public void setAdminPassword(String value) {
        settings.put(PASSWORD_KEY, value);
    }

    public Cache<String, Serializable> getCache() {
        return settings;
    }

    public void addListener(AbstractQuizUI listener) {
        uis.add(listener);
        listener.addDetachListener(e -> {
            uis.remove(listener);
        });
    }

    @CacheEntryCreated
    public void onNewData(CacheEntryCreatedEvent event) {
        // Only react to post events
        if (!event.isPre()) {
            Object value = event.getValue();
            for (AbstractQuizUI ui : uis) {
                ui.access(() -> {
                    if (value instanceof Question) {
                        ui.questionChanged((Question) value);
                    } else if (value instanceof String) {
                        ui.showMessage(value.toString());
                    }
                    if (ui instanceof AdminUI) {
                        if (value instanceof Answer) {
                            ui.answerSuggested((Answer) value);
                        } else if (value instanceof User) {
                            User user = (User) value;
                            ui.showMessage(
                                    "User " + user.getUsername() + " joined");
                        }
                    }
                });

            }
        }
    }

}
