package ed25519

import (
	"bytes"
	"crypto"
	"crypto/rand"
	"crypto/sha512"
	"encoding/hex"
	"testing"

	"golang.org/x/crypto/sha3"
)

// region constants / utils

var ZERO_PRIVATE_KEY = "0000000000000000000000000000000000000000000000000000000000000000"
var ZERO_PUBLIC_KEY = "3B6A27BCCEB6A42D62A3A8D02A6F0D73653215771DE243A63AC048A18B59DA29"

var DETERMINISTIC_PRIVATE_KEY = "575DBB3062267EFF57C970A336EBBC8FBCFE12C5BD3ED7BC11EB0481D7704CED"
var DETERMINISTIC_PUBLIC_KEY = "D6C3845431236C5A5A907A9E45BD60DA0E12EFD350B970E7F58E3499E2E7A2F0"

func ReverseInPlace(buf []byte) {
	for i, j := 0, len(buf) - 1; i < j; i, j = i + 1, j - 1 {
		buf[i], buf[j] = buf[j], buf[i]
	}
}

// endregion

// region PublicKey / PrivateKey

func TestPublicKey_Equality(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	public1, _, _ := GenerateKey(rand.Reader, hasher)
	public2, _, _ := GenerateKey(rand.Reader, hasher)

	// Act + Assert:
	if !public1.Equal(public1) || !public2.Equal(public2) {
		t.Errorf("public key is not equal to itself")
	}

	if public1.Equal(public2) || public2.Equal(public1) {
		t.Errorf("different public keys are equal")
	}

	public1Copy := make([]byte, len(public1))
	copy(public1Copy, public1)
	if public1.Equal(crypto.PublicKey(public1Copy)) {
		t.Errorf("different public key types are equal")
	}
}

func TestPrivateKey_Equality(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	_, private1, _ := GenerateKey(rand.Reader, hasher)
	_, private2, _ := GenerateKey(rand.Reader, hasher)

	// Act + Assert:
	if !private1.Equal(private1) || !private2.Equal(private2) {
		t.Errorf("private key is not equal to itself")
	}

	if private1.Equal(private2) || private2.Equal(private1) {
		t.Errorf("different private keys are equal")
	}

	private1Copy := make([]byte, len(private1))
	copy(private1Copy, private1)
	if private1.Equal(crypto.PrivateKey(private1Copy)) {
		t.Errorf("different private key types are equal")
	}
}

// region GenerateKey / NewKeyFromSeed

func AssertZeroPrivateKey(t *testing.T, private PrivateKey) {
	expectedPrivate, _ := hex.DecodeString(ZERO_PRIVATE_KEY)
	if !bytes.Equal(expectedPrivate, private[:32]) {
		t.Errorf("private key [private] was not derived correctly")
	}

	expectedPublic, _ := hex.DecodeString(ZERO_PUBLIC_KEY)
	if !bytes.Equal(expectedPublic, private[32:]) {
		t.Errorf("private key [public] was not derived correctly")
	}

	if !bytes.Equal(expectedPrivate, private.Seed()) {
		t.Errorf("private key [Seed()] was not derived correctly")
	}

	if !bytes.Equal(expectedPublic, private.Public().(PublicKey)) {
		t.Errorf("private key [Public()] was not derived correctly")
	}
}

func AssertDeterministicPrivateKey(t *testing.T, private PrivateKey) {
	expectedPrivate, _ := hex.DecodeString(DETERMINISTIC_PRIVATE_KEY)
	if !bytes.Equal(expectedPrivate, private[:32]) {
		t.Errorf("private key [private] was not derived correctly")
	}

	expectedPublic, _ := hex.DecodeString(DETERMINISTIC_PUBLIC_KEY)
	if !bytes.Equal(expectedPublic, private[32:]) {
		t.Errorf("private key [public] was not derived correctly")
	}

	if !bytes.Equal(expectedPrivate, private.Seed()) {
		t.Errorf("private key [Seed()] was not derived correctly")
	}

	if !bytes.Equal(expectedPublic, private.Public().(PublicKey)) {
		t.Errorf("private key [Public()] was not derived correctly")
	}
}

type zeroReader struct{}

func (zeroReader) Read(buf []byte) (int, error) {
	clear(buf)
	return len(buf), nil
}

type deterministicReader struct{}

func (deterministicReader) Read(buf []byte) (int, error) {
	seed, _ := hex.DecodeString(DETERMINISTIC_PRIVATE_KEY)

	copy(buf, seed)
	return len(buf), nil
}

func TestGenerateKey_Zero(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	var reader zeroReader

	// Act:
	public, private, _ := GenerateKey(reader, hasher)

	// Assert:
	AssertZeroPrivateKey(t, private)

	if !PublicKey(public).Equal(private.Public())  {
		t.Errorf("public key returned does not match public key derived from private key")
	}
}

func TestGenerateKey_Deterministic(t *testing.T) {
	// Arrange:
	hasher := sha3.NewLegacyKeccak512()
	var reader deterministicReader

	// Act:
	public, private, _ := GenerateKey(reader, hasher)

	// Assert:
	AssertDeterministicPrivateKey(t, private)

	if !PublicKey(public).Equal(private.Public())  {
		t.Errorf("public key returned does not match public key derived from private key")
	}
}

func TestNewKeyFromSeed_Zero(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	seed, _ := hex.DecodeString(ZERO_PRIVATE_KEY)

	// Act:
	private := NewKeyFromSeed(seed, hasher)

	// Assert:
	AssertZeroPrivateKey(t, private)
}

func TestNewKeyFromSeed_Deterministic(t *testing.T) {
	// Arrange:
	hasher := sha3.NewLegacyKeccak512()
	seed, _ := hex.DecodeString(DETERMINISTIC_PRIVATE_KEY)

	// Act:
	private := NewKeyFromSeed(seed, hasher)

	// Assert:
	AssertDeterministicPrivateKey(t, private)
}

// endregion

// region Sign

func TestSign_FillsSignature(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	seed, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000000")
	private := NewKeyFromSeed(seed, hasher)

	message, _ := hex.DecodeString("1234567890")

	// Act:
	signature := Sign(private, message, hasher)

	// Assert:
	zeroSignature := [64]byte {}
	if bytes.Equal(zeroSignature[:], signature) {
		t.Errorf("signature is zero")
	}
}

func TestSign_SameSignaturesForSameKeyPairs(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	seed1, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000000")
	private1 := NewKeyFromSeed(seed1, hasher)

	seed2, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000000")
	private2 := NewKeyFromSeed(seed2, hasher)

	message, _ := hex.DecodeString("1234567890")

	// Act:
	signature1 := Sign(private1, message, hasher)
	signature2 := Sign(private2, message, hasher)

	// Assert:
	if !bytes.Equal(signature1, signature2) {
		t.Errorf("signatures should be equal")
	}
}

func TestSign_DifferentSignaturesForDifferentKeyPairs(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	seed1, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000000")
	private1 := NewKeyFromSeed(seed1, hasher)

	seed2, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000001")
	private2 := NewKeyFromSeed(seed2, hasher)

	message, _ := hex.DecodeString("1234567890")

	// Act:
	signature1 := Sign(private1, message, hasher)
	signature2 := Sign(private2, message, hasher)

	// Assert:
	if bytes.Equal(signature1, signature2) {
		t.Errorf("signatures should be different")
	}
}

// endregion

// region Verify

func TestVerify_CanVerify(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	seed, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000000")
	private := NewKeyFromSeed(seed, hasher)

	message, _ := hex.DecodeString("1234567890")

	signature := Sign(private, message, hasher)

	// Act:
	isVerified := Verify(private.Public().(PublicKey), message, signature, hasher)

	// Assert:
	if !isVerified {
		t.Errorf("signature cannot be verified")
	}
}

func TestVerify_CannotVerifyWithDifferentKeyPair(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	seed1, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000000")
	private1 := NewKeyFromSeed(seed1, hasher)

	seed2, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000001")
	private2 := NewKeyFromSeed(seed2, hasher)

	message, _ := hex.DecodeString("1234567890")

	signature := Sign(private1, message, hasher)

	// Act:
	isVerified := Verify(private2.Public().(PublicKey), message, signature, hasher)

	// Assert:
	if isVerified {
		t.Errorf("signature should not be verified")
	}
}

func TestVerify_CannotVerifyWithDifferentMessage(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	seed, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000000")
	private := NewKeyFromSeed(seed, hasher)

	message1, _ := hex.DecodeString("1234567890")
	message2, _ := hex.DecodeString("1234567890ABCDEF")

	signature := Sign(private, message1, hasher)

	// Act:
	isVerified := Verify(private.Public().(PublicKey), message2, signature, hasher)

	// Assert:
	if isVerified {
		t.Errorf("signature should not be verified")
	}
}

func TestVerify_CannotVerifyWithDifferentSignature(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	seed, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000000")
	private := NewKeyFromSeed(seed, hasher)

	message, _ := hex.DecodeString("1234567890")

	signature := Sign(private, message, hasher)
	signature[0] ^= 0xFF

	// Act:
	isVerified := Verify(private.Public().(PublicKey), message, signature, hasher)

	// Assert:
	if isVerified {
		t.Errorf("signature should not be verified")
	}
}

func TestVerify_CannotVerifyWithZeroS(t *testing.T) {
	// Arrange:
	hasher := sha512.New()
	seed, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000000")
	private := NewKeyFromSeed(seed, hasher)

	message, _ := hex.DecodeString("1234567890")

	signature := Sign(private, message, hasher)
	signatureZeroS := make([]byte, len(signature))
	copy(signatureZeroS, signature[:32])

	// Act:
	isVerified := Verify(private.Public().(PublicKey), message, signature, hasher)
	isVerifiedZeroS := Verify(private.Public().(PublicKey), message, signatureZeroS, hasher)

	// Assert:
	if !isVerified {
		t.Errorf("signature cannot be verified")
	}

	if isVerifiedZeroS {
		t.Errorf("zero S signature should not be verified")
	}
}

func ScalarAddGroupOrder(scalar []byte) []byte {
	// 2^252 + 27742317777372353535851937790883648493, little endian
	groupOrder, _ := hex.DecodeString("EDD3F55C1A631258D69CF7A2DEF9DE1400000000000000000000000000000010")
	remainder := 0

	for i, groupOrderByte := range groupOrder {
		byteSum := int(scalar[i]) + int(groupOrderByte);
		scalar[i] = byte((byteSum + remainder) & 0xFF);
		remainder = (byteSum >> 8) & 0xFF;
	}

	return scalar
}

func TestVerify_CannotVerifyNonCanonicalSignature(t *testing.T) {
	// Arrange: the value 30 in the payload ensures that the encodedS part of the signature is < 2 ^ 253 after adding the group order
	hasher := sha512.New()
	seed, _ := hex.DecodeString("0000000000000000000000000000000000000000000000000000000000000000")
	private := NewKeyFromSeed(seed, hasher)

	message, _ := hex.DecodeString("0102030405060708091D")

	canonicalSignature := Sign(private, message, hasher)
	nonCanonicalSignature := make([]byte, len(canonicalSignature))
	copy(nonCanonicalSignature, canonicalSignature)
	ScalarAddGroupOrder(nonCanonicalSignature[32:])

	// Act:
	isVerifiedCanonical := Verify(private.Public().(PublicKey), message, canonicalSignature, hasher)
	isVerifiedNonCanonical := Verify(private.Public().(PublicKey), message, nonCanonicalSignature, hasher)

	// Assert:
	if !isVerifiedCanonical {
		t.Errorf("canonical signature cannot be verified")
	}

	if isVerifiedNonCanonical {
		t.Errorf("non-canonical signature should not be verified")
	}
}

// endregion
