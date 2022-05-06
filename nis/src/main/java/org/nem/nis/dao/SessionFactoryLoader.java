package org.nem.nis.dao;

import org.hibernate.SessionFactory;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.TransactionRegistry;
import org.nem.specific.deploy.appconfig.NisAppConfig;
import org.springframework.orm.hibernate4.LocalSessionFactoryBuilder;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * Helper class for loading a SessionFactory.
 */
public class SessionFactoryLoader {

	/**
	 * Loads a session factory given a data source.
	 *
	 * @param dataSource The data source.
	 * @return The session factory.
	 * @throws java.io.IOException if properties could not be loaded.
	 */
	public static SessionFactory load(final DataSource dataSource) throws IOException {
		final LocalSessionFactoryBuilder localSessionFactoryBuilder = new LocalSessionFactoryBuilder(dataSource);
		localSessionFactoryBuilder.addProperties(getDbProperties(entry -> entry.startsWith("hibernate")));
		localSessionFactoryBuilder.addAnnotatedClasses(DbAccount.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbBlock.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbNamespace.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbMosaicDefinition.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbMosaicProperty.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbMosaic.class);

		localSessionFactoryBuilder.addAnnotatedClasses(DbMultisigModification.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbMultisigMinCosignatoriesModification.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbMultisigSignatureTransaction.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbMultisigSend.class);
		localSessionFactoryBuilder.addAnnotatedClasses(DbMultisigReceive.class);
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			localSessionFactoryBuilder.addAnnotatedClasses(entry.dbModelClass);
		}

		return localSessionFactoryBuilder.buildSessionFactory();
	}

	private static Properties getDbProperties(final Predicate<String> filter) throws IOException {
		final Properties dbProperties = new Properties();
		final Properties properties = new Properties();
		dbProperties.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));
		dbProperties.stringPropertyNames().stream().filter(filter)
				.forEach(entry -> properties.setProperty(entry, dbProperties.getProperty(entry)));

		return properties;
	}
}
