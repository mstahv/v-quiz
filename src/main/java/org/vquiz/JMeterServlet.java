package org.vquiz;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.vaadin.server.ClientConnector;
import com.vaadin.server.Constants;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;

/**
 * @author Marcus Hellberg (marcus@vaadin.com) Further modified by Johannes
 *         Tuikkala (johannes@vaadin.com)
 */
public class JMeterServlet extends VaadinServlet {
	private static final long serialVersionUID = 898354532369443197L;

	private int sessionTimeoutInSeconds = 10 * 60;

	public JMeterServlet() {
		System.setProperty(getPackageName() + "."
				+ Constants.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION, "true");
		System.setProperty(getPackageName() + "."
				+ Constants.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS, "true");
	}

	@Override
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Logger.getLogger(JMeterServlet.class.getName()).warning(
				"----->" + request.getRequestURI());

		HttpSession session = request.getSession();
		if (session != null
				&& session.getMaxInactiveInterval() != sessionTimeoutInSeconds) {
			session.setMaxInactiveInterval(sessionTimeoutInSeconds);
		}

		super.service(request, response);
	}

	@Override
	protected VaadinServletService createServletService(
			DeploymentConfiguration deploymentConfiguration)
			throws ServiceException {
		JMeterService service = new JMeterService(this, deploymentConfiguration);
		service.init();

		return service;
	}

	private String getPackageName() {
		String pkgName;
		final Package pkg = this.getClass().getPackage();
		if (pkg != null) {
			pkgName = pkg.getName();
		} else {
			final String className = this.getClass().getName();
			pkgName = new String(className.toCharArray(), 0,
					className.lastIndexOf('.'));
		}
		return pkgName;
	}

	public static class JMeterService extends VaadinServletService {
		private static final long serialVersionUID = -5874716650679865909L;

		public JMeterService(VaadinServlet servlet,
				DeploymentConfiguration deploymentConfiguration)
				throws ServiceException {
			super(servlet, deploymentConfiguration);
		}

		@Override
		protected VaadinSession createVaadinSession(VaadinRequest request)
				throws ServiceException {
			return new JMeterSession(this);
		}

		// @Override
		// protected List<RequestHandler> createRequestHandlers()
		// throws ServiceException {
		// List<RequestHandler> handlers = super.createRequestHandlers();
		// handlers.add(0, new ServletBootstrapHandler());
		// handlers.add(new ServletUIInitHandler());
		// try {
		// handlers.add(new PushRequestHandler(this) {
		//
		// @Override
		// public boolean handleRequest(VaadinSession session,
		// VaadinRequest request, VaadinResponse response)
		// throws IOException {
		// Field f = null;
		// try {
		// f = super.getClass().getDeclaredField("atmosphere");
		// f.setAccessible(true);
		// AtmosphereFramework atmosphereX = (AtmosphereFramework) f.get(this);
		// Logger.getLogger(JMeterServlet.class.getName()).warning(
		// "--X-->" + atmosphereX.toString());
		// } catch (NoSuchFieldException e) {
		// e.printStackTrace();
		// } catch (SecurityException e) {
		// e.printStackTrace();
		// } catch (IllegalArgumentException e) {
		// e.printStackTrace();
		// } catch (IllegalAccessException e) {
		// e.printStackTrace();
		// }
		//
		//
		//
		// return super.handleRequest(session, request, response);
		// }
		// });
		// } catch (ServiceException e) {
		// // Atmosphere init failed. Push won't work but we don't throw a
		// // service exception as we don't want to prevent non-push
		// // applications from working
		// Logger.getLogger(JMeterServlet.class.getName())
		// .log(Level.WARNING,
		// "Error initializing Atmosphere. Push will not work.",
		// e);
		//
		// }
		// return handlers;
		// }

	}

	public static class JMeterSession extends VaadinSession {
		private static final long serialVersionUID = 4596901275146146127L;

		public JMeterSession(VaadinService service) {
			super(service);
		}

		@Override
		public String createConnectorId(ClientConnector connector) {
			if (connector instanceof Component) {
				Component component = (Component) connector;
				return component.getId() == null ? super
						.createConnectorId(connector) : component.getId();
			}
			return super.createConnectorId(connector);
		}
	}

}
