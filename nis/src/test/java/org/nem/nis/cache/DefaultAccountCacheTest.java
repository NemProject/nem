package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.cache.AccountCache;

import java.util.*;
import java.util.stream.*;

public class DefaultAccountCacheTest extends AccountCacheTest<DefaultAccountCache> {

	@Override
	protected DefaultAccountCache createAccountCache() {
		return new DefaultAccountCache();
	}
}