package org.nem.core.model;

import java.util.ArrayList;

/**
 *TODO: still trying to decide if this class is needed
 */
public class HistoricalOutlink implements Comparable<HistoricalOutlink> {
	private final BlockHeight height;
	private ArrayList<AccountLink> outlinks; //Outlinks at blockheight
	/**
	 * @param height
	 * @param outlinks
	 */
	public HistoricalOutlink(BlockHeight height, ArrayList<AccountLink> outlinks) {
		super();
		this.height = height;
		this.outlinks = outlinks;
	}
	@Override
	public int compareTo(HistoricalOutlink o) {
		// TODO Auto-generated method stub
		return 0;
	}
}
