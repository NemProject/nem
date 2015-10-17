package org.nem.core.model;

/**
 * Types of NEM nodes.
 * TODO 20151017 J-B: this enum doesn't seem to be used in core, can probably move to node rewards branch
 */
public enum NemNodeType {

	/**
	 * A NIS node.
	 */
	NIS,

	/**
	 * A NCC node.
	 */
	NCC,

	/**
	 * A Servant node.
	 */
	Servant
}
