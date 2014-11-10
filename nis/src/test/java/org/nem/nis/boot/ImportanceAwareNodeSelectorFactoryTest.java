package org.nem.nis.boot;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.math.ColumnVector;
import org.nem.core.node.*;
import org.nem.nis.poi.PoiFacade;
import org.nem.peer.*;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.NodeExperiences;

import java.util.HashSet;

public class ImportanceAwareNodeSelectorFactoryTest {

	@Test
	public void createNodeSelectorReturnsNonNull() {
		// Arrange:
		final Config config = createConfig();
		final ImportanceAwareNodeSelectorFactory factory = new ImportanceAwareNodeSelectorFactory(
				15,
				config.getTrustProvider(),
				new PeerNetworkState(config, new NodeExperiences(), new NodeCollection()),
				this.createPoiFacade());

		// Assert:
		Assert.assertThat(factory.createNodeSelector(), IsNull.notNullValue());
	}

	private static Config createConfig() {
		// Arrange:
		final TrustProvider trustProvider = Mockito.mock(TrustProvider.class);
		Mockito.when(trustProvider.computeTrust(Mockito.any())).thenReturn(new ColumnVector(1));

		final Config config = Mockito.mock(Config.class);
		Mockito.when(config.getTrustProvider()).thenReturn(trustProvider);
		Mockito.when(config.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));
		Mockito.when(config.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));
		Mockito.when(config.getPreTrustedNodes()).thenReturn(new PreTrustedNodes(new HashSet<>()));
		return config;
	}

	private PoiFacade createPoiFacade() {
		return Mockito.mock(PoiFacade.class);
	}
}
