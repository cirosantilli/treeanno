package de.ustu.ims.reiter.treeanno;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;

import de.ustu.ims.reiter.treeanno.io.DatabaseIO;

/**
 * Application Lifecycle Listener implementation class ContextListener
 *
 */
public class ContextListener implements ServletContextListener {

	DatabaseIO dr;

	/**
	 * Default constructor.
	 * 
	 * @throws NamingException
	 * @throws ClassNotFoundException
	 */
	public ContextListener() {}

	/**
	 * @see ServletContextListener#contextDestroyed(ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {

	}

	/**
	 * @see ServletContextListener#contextInitialized(ServletContextEvent)
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext sc = sce.getServletContext();
		Context envContext;
		try {
			envContext =
					(Context) new InitialContext().lookup("java:/comp/env");
			DataSource dataSource =
					(DataSource) envContext.lookup("jdbc/treeanno");
			sc.setAttribute("dataSource", dataSource);
		} catch (NamingException e1) {
			e1.printStackTrace();
		}

		Properties properties = new Properties();
		InputStream is = null;
		try {
			is = getClass().getResourceAsStream("/project.properties");
			if (is != null)
				properties.load(new InputStreamReader(is, "UTF-8"));
			Enumeration<String> pNames = sc.getInitParameterNames();
			while (pNames.hasMoreElements()) {
				String pName = pNames.nextElement();
				properties.put(pName, sc.getInitParameter(pName));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
		for (String s : properties.stringPropertyNames()) {
			sc.setAttribute(s, properties.get(s));
		}

		try {
			CW.setDataLayer(sc, new DatabaseIO());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		sc.getSessionCookieConfig().setPath("/");
	}

}
