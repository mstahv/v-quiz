package org.vquiz;

import com.vaadin.server.VaadinServlet;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

/**
 * Custom servlet is here just to introducing JMS topic. Otherwise we could deal
 * with the server presented by Vaadin CDI (or if one presents the topic using
 * server admin tools).
 */
@WebServlet(name = "QuizServlet", urlPatterns = {"/*"}, asyncSupported = true,
        initParams = {
            @WebInitParam(name = "uiprovider", value = "com.vaadin.cdi.CDIUIProvider"),
            @WebInitParam(name = "org.atmosphere.cpr.broadcaster.maxProcessingThreads", value = "10"),
            @WebInitParam(name = "org.atmosphere.cpr.broadcaster.maxAsyncWriteThreads", value = "10")
        })
public class Servlet extends VaadinServlet {
}
