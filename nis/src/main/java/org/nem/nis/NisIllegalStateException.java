package org.nem.nis;

/**
 * An exception that is thrown when NIS is in an illegal state.
 */
@SuppressWarnings("serial")
public class NisIllegalStateException extends RuntimeException {
	private final Reason reason;

	/**
	 * The reason NIS is in an illegal state.
	 */
	public enum Reason {
		/**
		 * The operation could not be performed because NIS is not booted.
		 */
		NIS_ILLEGAL_STATE_NOT_BOOTED,

		/**
		 * The operation could not be performed because NIS is loading its block chain.
		 */
		NIS_ILLEGAL_STATE_LOADING_CHAIN,

		/**
		 * The operation could not be performed because NIS has already been booted.
		 */
		NIS_ILLEGAL_STATE_ALREADY_BOOTED
	}

	/**
	 * Creates a new NIS illegal state exception.
	 *
	 * @param reason The reason NIS is in an illegal state.
	 */
	public NisIllegalStateException(final Reason reason) {
		super(reason.toString());
		this.reason = reason;
	}

	/**
	 * Gets the reason NIS is in an illegal state.
	 *
	 * @return The reason NIS is in an illegal state.
	 */
	public Reason getReason() {
		return this.reason;
	}
}
