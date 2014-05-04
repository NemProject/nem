package org.nem.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 *
 */
public class HistoricalOutlinks {
	private final ArrayList<HistoricalOutlink> outlinks = new ArrayList<HistoricalOutlink>();
	
	/**
	 * Add an outlink at a given block height.
	 * Add the outlink to all historical outlinks with a bigger height.
	 * 
	 * @param height the height where the outlink is inserted
	 * @param outlink the outlink to add
	 */
	public void add(final BlockHeight height, final AccountLink outlink) {
		//TODO: write this.
	}
	
	/**
	 * Remove an outlink at a given block height.
	 * Remove the outlink to all historical outlinks with a bigger height.
	 * 
	 * @param height the height where the outlink is to be removed
	 * @param outlink the outlink to remove
	 */
	public void remove(final BlockHeight height, final AccountLink outlink) {
		//TODO: write this.
	}
}
