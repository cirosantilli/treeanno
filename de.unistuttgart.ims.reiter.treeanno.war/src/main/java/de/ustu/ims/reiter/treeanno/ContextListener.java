package de.ustu.ims.reiter.treeanno;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
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
		Context envContext = null;
		try {
			envContext =
					(Context) new InitialContext().lookup("java:/comp/env");

			DataSource dataSource =
					(DataSource) envContext.lookup("jdbc/treeanno");
			sc.setAttribute("dataSource", dataSource);
		} catch (NamingException e1) {
			e1.printStackTrace();
		}

		Properties properties = null;
		Properties defaults = new Properties();

		InputStream is = null;
		try {
			// reading of default properties from inside the war
			is = getClass().getResourceAsStream("/project.properties");
			if (is != null) defaults.load(new InputStreamReader(is, "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}

		try {
			// reading additional properties in seperate file, as specified
			// in the context
			if (envContext != null) {
				String path =
						(String) envContext
								.lookup("treeanno/configurationPath");
				is = new FileInputStream(new File(path));
				properties = new Properties(defaults);
				properties.load(new InputStreamReader(is, "UTF-8"));
			} else {
				// if no context is found, we take the defaults
				properties = defaults;
			}
		} catch (IOException | NamingException e) {
			e.printStackTrace();
			// if something goes wrong, we take the defaults
			properties = defaults;
		} finally {
			IOUtils.closeQuietly(is);
		}

		for (String s : properties.stringPropertyNames()) {
			sc.setAttribute(s, properties.getProperty(s));
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
