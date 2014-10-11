package org.nem.core.crypto;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.crypto.ed25519.Ed25519Engine;
import org.nem.core.test.Utils;

import java.math.BigInteger;

public class SignerTest {

	@Test
	public void canCreateSignerFromKeyPair() {
		// Arrange:
		final SignerContext context = new SignerContext();

		// Assert:
		new Signer(context.keyPair);
	}

	@Test
	public void ctorDelegatesToDefaultEngineCreateDsaSigner() {
		// Arrange:
		final SignerContext context = new SignerContext();

		// Act:
		new Signer(context.keyPair);

		// Assert:
		Mockito.verify(context.engine, Mockito.times(1)).createDsaSigner(context.keyPair);
	}

	@Test
	public void signDelegatesToDsaSigner() {
		// Assert:
		final SignerContext context = new SignerContext();
		final Signer signer = new Signer(context.keyPair);

		// Act:
		signer.sign(context.data);

		// Assert:
		Mockito.verify(context.dsaSigner, Mockito.times(1)).sign(context.data);
	}

	@Test
	public void verifyDelegatesToDsaSigner() {
		// Assert:
		final SignerContext context = new SignerContext();
		final Signer signer = new Signer(context.keyPair);

		// Act:
		signer.verify(context.data, context.signature);

		// Assert:
		Mockito.verify(context.dsaSigner, Mockito.times(1)).verify(context.data, context.signature);
	}

	@Test
	public void isCanonicalSignatureDelegatesToDsaSigner() {
		// Assert:
		final SignerContext context = new SignerContext();
		final Signer signer = new Signer(context.keyPair);

		// Act:
		signer.isCanonicalSignature(context.signature);

		// Assert:
		Mockito.verify(context.dsaSigner, Mockito.times(1)).isCanonicalSignature(context.signature);
	}

	@Test
	public void makeSignatureCanonicalDelegatesToDsaSigner() {
		// Assert:
		final SignerContext context = new SignerContext();
		final Signer signer = new Signer(context.keyPair);

		// Act:
		signer.makeSignatureCanonical(context.signature);

		// Assert:
		Mockito.verify(context.dsaSigner, Mockito.times(1)).makeSignatureCanonical(context.signature);
	}

	private class SignerContext {
		private final Ed25519Engine engine = Mockito.mock(Ed25519Engine.class);
		private final KeyAnalyzer analyzer = Mockito.mock(KeyAnalyzer.class);
		private final DsaSigner dsaSigner = Mockito.mock(DsaSigner.class);
		private final byte[] data = Utils.generateRandomBytes();
		private final Signature signature = new Signature(BigInteger.ONE, BigInteger.ONE);
		private final KeyPair keyPair;

		private SignerContext() {
			CryptoEngines.setDefaultEngine(this.engine);
			Mockito.when(this.analyzer.isKeyCompressed(Mockito.any())).thenReturn(true);
			Mockito.when(this.engine.createKeyAnalyzer()).thenReturn(this.analyzer);
			this.keyPair = new KeyPair(null, new PublicKey(Utils.generateRandomBytes(32)));
			Mockito.when(this.engine.createDsaSigner(this.keyPair)).thenReturn(this.dsaSigner);
			Mockito.when(this.dsaSigner.isCanonicalSignature(this.signature)).thenReturn(true);
			Mockito.when(this.dsaSigner.makeSignatureCanonical(this.signature)).thenReturn(this.signature);
		}
	}
}
