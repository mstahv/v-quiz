package org.vquiz;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
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

    final static Collection<ContenderUI> uis = new CopyOnWriteArrayList<>();
    final static Collection<AdminUI> adminUis = new CopyOnWriteArrayList<>();

    Cache<String, Serializable> settings;
    Cache<String, User> users;
    Cache<Date, Answer> answers;
    Cache<Date, String> messages;

    @PostConstruct
    void init() {
        this.settings = cc.getCache();
        if (!settings.containsKey(PASSWORD_KEY)) {
            settings.put("_password", new Password("admin"));
        }
        users = cc.getCache("users");
        answers = cc.getCache("answers");
        messages = cc.getCache("messages");

        settings.addListener(this);
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

    public void addListener(ContenderUI listener) {
        uis.add(listener);
        listener.addDetachListener(e -> {
            uis.remove(listener);
            if(listener.getUser().getUsername() != null) {
                removeUser(listener.getUser());
            }
        }); 
    }

    public void addListener(AdminUI listener) {
        adminUis.add(listener);
        listener.addDetachListener(e -> {
            adminUis.remove(listener);
        });
    }

    @CacheEntryCreated
    public void onNewData(CacheEntryCreatedEvent event) {
        // Don't react to pre events
        if (!event.isPre()) {
            Object value = event.getValue();
            if (value instanceof Question) {
                for (ContenderUI ui : uis) {
                    ui.questionChanged((Question) value);
                }
                for (AdminUI ui : adminUis) {
                    ui.questionChanged((Question) value);
                }
            } else if (value instanceof String) {
                for (ContenderUI ui : uis) {
                    ui.showMessage(value.toString());
                }
            } else {
                for (AdminUI ui : adminUis) {
                    if (value instanceof Answer) {
                        ui.answerSuggested((Answer) value);
                    }
                }
            }
        }
    }

    public int getUserCount() {
        return users.size();
    }
    
    public int getLocalUserCount() {
        return uis.size();
    }

    public int getSugestionCount() {
        return answers.size();
    }

}
