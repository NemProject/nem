package org.nem.core.crypto.ed25519.arithmetic;

public enum CoordinateSystem {

	/**
	 * Available coordinate systems for a group element:
	 *
	 * AFFINE: Affine coordinate system (x, y).
	 * P2: Projective coordinate system (X:Y:Z) satisfying x=X/Z, y=Y/Z.
	 * P3: Extended projective coordinate system (X:Y:Z:T) satisfying x=X/Z, y=Y/Z, XY=ZT.
	 * P1xP1: Completed coordinate system ((X:Z), (Y:T)) satisfying x=X/Z, y=Y/T.
	 * PRECOMPUTED: Precomputed coordinate system (y+x, y-x, 2dxy).
	 * CACHED: Cached coordinate system (Y+X, Y-X, Z, 2dT)
	 */
	AFFINE,
	P2,
	P3,
	P1xP1,
	PRECOMPUTED,
	CACHED

}
