package org.vquiz;

import java.io.Serializable;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import org.infinispan.manager.CacheContainer;
import org.vquiz.domain.Question;
import org.vquiz.domain.User;

/**
 * This class stores all shared data in application. Instead of RDBMS we use
 * Infinispan.
 *
 * @author Matti Tahvonen <matti@vaadin.com>
 */
@Stateless
public class Repository {

    private static final String PASSWORD_KEY = "_password";
    private static final String CURRENT_QUESTION_KEY = "_currentQuestion";

    @Resource(lookup = "java:jboss/infinispan/container/myCache")
    CacheContainer cc;

    Map<String, Serializable> cache;

    @PostConstruct
    void init() {
        this.cache = cc.getCache();
        if (!cache.containsKey(PASSWORD_KEY)) {
            cache.put("_password", "admin");
        }
    }

    public Question getCurrent() {
        return (Question) cache.get(CURRENT_QUESTION_KEY);
    }

    public void save(Question data) {
        cache.put(CURRENT_QUESTION_KEY, data);
    }

    public Serializable findByName(String key) {
        return cache.get(key);
    }

    public boolean isReserved(String username) {
        return findByName(username) != null;
    }

    public void save(User user) {
        cache.put(user.getUsername(), user);
    }

    public void removeUser(User user) {
        cache.remove(user.getUsername());
    }

    public boolean adminPasswordMatches(String value) {
        Serializable pw = cache.get(PASSWORD_KEY);
        if (pw == null) {
            pw = "admin";
        }
        return pw.equals(value);
    }

    public void setAdminPassword(String value) {
        cache.put(PASSWORD_KEY, value);
    }

}
