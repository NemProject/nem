package org.nem.nis.cache;

public class DefaultAccountStateRepositoryTest extends AccountStateRepositoryTest<DefaultAccountStateRepository> {

	@Override
	protected DefaultAccountStateRepository createCache() {
		return new DefaultAccountStateRepository();
	}
}
