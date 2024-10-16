package org.nem.core.metadata;

import java.net.URL;
import java.security.CodeSource;
import java.security.cert.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;

public class CodeSourceFacadeTest {

	@Test
	public void canCreateFacadeAroundNullCertificates() throws Exception {
		// Act:
		final URL url = new URL("http://nem.com/foo/n.jar");
		final CodeSourceFacade facade = new CodeSourceFacade(new CodeSource(url, (Certificate[]) null));

		// Assert:
		MatcherAssert.assertThat(facade.getLocation(), IsEqual.equalTo(url));
		MatcherAssert.assertThat(facade.getFirstCertificate(), IsNull.nullValue());
	}

	@Test
	public void canCreateFacadeAroundNoCertificates() throws Exception {
		// Act:
		final URL url = new URL("http://nem.com/foo/n.jar");
		final CodeSourceFacade facade = new CodeSourceFacade(new CodeSource(url, new Certificate[]{}));

		// Assert:
		MatcherAssert.assertThat(facade.getLocation(), IsEqual.equalTo(url));
		MatcherAssert.assertThat(facade.getFirstCertificate(), IsNull.nullValue());
	}

	@Test
	public void canCreateFacadeAroundSingleCertificate() throws Exception {
		// Act:
		final URL url = new URL("http://nem.com/foo/n.jar");
		final Certificate c1 = Mockito.mock(X509Certificate.class);
		final CodeSourceFacade facade = new CodeSourceFacade(new CodeSource(url, new Certificate[]{
				c1
		}));

		// Assert:
		MatcherAssert.assertThat(facade.getLocation(), IsEqual.equalTo(url));
		MatcherAssert.assertThat(facade.getFirstCertificate(), IsSame.sameInstance(c1));
	}

	@Test
	public void canCreateFacadeAroundMultipleCertificates() throws Exception {
		// Act:
		final URL url = new URL("http://nem.com/foo/n.jar");
		final Certificate c1 = Mockito.mock(X509Certificate.class);
		final Certificate c2 = Mockito.mock(X509Certificate.class);
		final Certificate c3 = Mockito.mock(X509Certificate.class);
		final CodeSourceFacade facade = new CodeSourceFacade(new CodeSource(url, new Certificate[]{
				c1, c2, c3
		}));

		// Assert:
		MatcherAssert.assertThat(facade.getLocation(), IsEqual.equalTo(url));
		MatcherAssert.assertThat(facade.getFirstCertificate(), IsSame.sameInstance(c1));
	}
}
