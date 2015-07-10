package de.ustu.ims.reiter.treeanno.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;

import de.ustu.ims.reiter.treeanno.DataLayer;
import de.ustu.ims.reiter.treeanno.Perm;
import de.ustu.ims.reiter.treeanno.beans.Document;
import de.ustu.ims.reiter.treeanno.beans.Project;
import de.ustu.ims.reiter.treeanno.beans.User;

public class DatabaseIO implements DataLayer {

	DataSource dataSource;
	Dao<User, Integer> userDao;
	Dao<Project, Integer> projectDao;
	Dao<Document, Integer> documentDao;

	public DatabaseIO() throws ClassNotFoundException, NamingException,
			SQLException {
		Context initContext;
		Class.forName("com.mysql.jdbc.Driver");

		initContext = new InitialContext();
		Context envContext = (Context) initContext.lookup("java:/comp/env");
		dataSource = (DataSource) envContext.lookup("jdbc/treeanno");

		DataSourceConnectionSource connectionSource =
				new DataSourceConnectionSource(dataSource,
						"jdbc:mysql://localhost/de.ustu.ims.reiter.treeanno");
		userDao = DaoManager.createDao(connectionSource, User.class);
		projectDao = DaoManager.createDao(connectionSource, Project.class);
		documentDao = DaoManager.createDao(connectionSource, Document.class);
	}

	@Deprecated
	public DatabaseIO(DataSource ds) throws ClassNotFoundException,
	NamingException {
		dataSource = ds;
	}

	@Deprecated
	public boolean isHidden(int documentId) throws SQLException {
		Connection conn = dataSource.getConnection();
		PreparedStatement stmt =
				conn.prepareStatement("SELECT hidden FROM treeanno_documents WHERE id=?");
		stmt.setInt(1, documentId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			boolean b = rs.getBoolean(1);
			rs.close();
			stmt.close();
			conn.close();
			return b;
		}
		rs.close();
		stmt.close();
		conn.close();
		return false;
	}

	public int getAccessLevel(int documentId, User user) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			conn = dataSource.getConnection();
			stmt =
					conn.prepareStatement("SELECT level FROM (SELECT treeanno_projects.id AS pid, treeanno_documents.id AS did FROM treeanno_documents, treeanno_projects WHERE treeanno_documents.project = treeanno_projects.id) proj, treeanno_users_permissions WHERE pid = projectId AND userId=? AND did=?");
			stmt.setInt(1, user.getDatabaseId());
			stmt.setInt(2, documentId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				int r = rs.getInt(1);
				rs.close();
				stmt.close();
				conn.close();
				return r;
			}
		} finally {
			closeQuietly(rs);
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		return Perm.NO_ACCESS;
	}

	@Override
	public int getAccessLevel(Project project, User user) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			conn = dataSource.getConnection();
			stmt =
					conn.prepareStatement("SELECT level FROM treeanno_users_permissions WHERE projectId=? AND userId=?");
			stmt.setInt(1, project.getDatabaseId());
			stmt.setInt(2, user.getDatabaseId());
			rs = stmt.executeQuery();
			if (rs.next()) {
				int r = rs.getInt(1);
				rs.close();
				stmt.close();
				conn.close();
				return r;
			}
		} finally {
			closeQuietly(rs);
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		return Perm.NO_ACCESS;
	}

	public boolean updateJCas(int documentId, JCas jcas) throws SQLException,
	SAXException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XmiCasSerializer.serialize(jcas.getCas(), baos);

		String s = null;
		try {
			s = new String(baos.toByteArray(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// This should not happen.
			e.printStackTrace();
		}

		Connection connection = dataSource.getConnection();

		PreparedStatement stmt =
				connection
				.prepareStatement("UPDATE treeanno_documents SET xmi=? WHERE id=?");
		stmt.setString(1, s);
		stmt.setInt(2, documentId);
		int r = stmt.executeUpdate();
		stmt.close();
		return r == 1;
	}

	@Override
	public Document getDocument(int documentId) throws SQLException {
		Document d = documentDao.queryForId(documentId);
		// projectDao.refresh(d.getProject());
		return d;
	}

	public JCas getJCas(int documentId) throws SQLException, UIMAException,
	SAXException, IOException {
		JCas jcas = null;

		Connection connection = dataSource.getConnection();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt =
					connection
							.prepareStatement("SELECT xmi FROM treeanno_documents WHERE id=?");
			stmt.setInt(1, documentId);
			rs = stmt.executeQuery();

			if (rs.next()) {

				String textXML = rs.getString(1);
				TypeSystemDescription tsd =
						TypeSystemDescriptionFactory
								.createTypeSystemDescription();
				jcas = JCasFactory.createJCas(tsd);
				InputStream is = null;
				try {
					is = new ByteArrayInputStream(textXML.getBytes());
					XmiCasDeserializer.deserialize(is, jcas.getCas(), true);
				} finally {
					IOUtils.closeQuietly(is);
				}
			}
		} finally {
			closeQuietly(rs);
			closeQuietly(stmt);
			closeQuietly(connection);
		}
		return jcas;

	}

	public boolean deleteDocument(int documentId) throws SQLException {
		return (documentDao.deleteById(documentId) == 1);
	}

	public boolean cloneDocument(int documentId) throws SQLException {
		Connection connection = dataSource.getConnection();

		PreparedStatement stmt =
				connection
				.prepareStatement("INSERT INTO treeanno_documents(xmi,typesystemId,project,name) SELECT xmi,typesystemId,project,name FROM treeanno_documents WHERE id=?");
		stmt.setInt(1, documentId);
		int r = stmt.executeUpdate();
		stmt.close();
		connection.close();

		return r == 1;
	}

	@Override
	public List<Project> getProjects() throws SQLException {
		return projectDao.queryForAll();
	}

	public List<Document> getDocuments(int projectId) throws SQLException {
		Map<String, Object> fv = new HashMap<String, Object>();
		fv.put("project", projectId);
		fv.put("hidden", 0);
		return documentDao.queryForFieldValues(fv);

	}

	private void closeQuietly(Connection connection) {
		try {
			if (connection != null) connection.close();
		} catch (Exception e) {};
	}

	private void closeQuietly(Statement statement) {
		try {
			if (statement != null) statement.close();
		} catch (Exception e) {};
	}

	private void closeQuietly(ResultSet resultSet) {
		try {
			if (resultSet != null) resultSet.close();
		} catch (Exception e) {};
	}

	@Override
	public Project getProject(int i) throws SQLException {
		return projectDao.queryForId(i);
	}

	@Override
	public User getUser(int i) throws SQLException {
		return userDao.queryForId(i);
	}

	@Override
	public Collection<Document> getDocuments(Project proj) throws SQLException {
		return getDocuments(proj.getDatabaseId());
	}

	@Override
	public JCas getJCas(Document document) throws SQLException, UIMAException,
	SAXException, IOException {
		return this.getJCas(document.getDatabaseId());
	}

	@Override
	public boolean deleteDocument(Document document) throws SQLException {
		return this.deleteDocument(document.getDatabaseId());

	}

	@Override
	public int cloneDocument(Document document) throws SQLException {
		if (!cloneDocument(document.getDatabaseId())) return -1;

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			if (rs.next()) {
				return rs.getInt(1);
			}
		} finally {
			closeQuietly(rs);
			closeQuietly(stmt);
			closeQuietly(conn);
		}
		return -1;
	}

	@Override
	public boolean updateJCas(Document document, JCas jcas)
			throws SQLException, SAXException {
		return this.updateJCas(document.getDatabaseId(), jcas);
	}
}
