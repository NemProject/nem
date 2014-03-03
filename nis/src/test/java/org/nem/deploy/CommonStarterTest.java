package org.nem.deploy;

import org.junit.Test;

public class CommonStarterTest {

	@Test
	public void testOpenStartPage() {
		CommonStarter cs = new CommonStarter();
		
		//Should not throw any exception
		cs.openStartPage();
	}

}
