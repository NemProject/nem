package ed25519

import (
	"bytes"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"testing"

	"golang.org/x/crypto/sha3"
)

// region utils

var EXPECTED_TEST_CASES_COUNT = 10000

type runTestCase[TTestCase any] func(TTestCase) bool

func RunTestVectors[TTestCase any](t *testing.T, filename string, runTestCase runTestCase[TTestCase]) {
	content, err := ioutil.ReadFile(fmt.Sprintf("../tests/vectors/nem/crypto/%s", filename))
	if nil != err {
		t.Fatal("error when opening file: ", err)
	}

	var testCases []TTestCase
	err = json.Unmarshal(content, &testCases)
	if nil != err {
		t.Fatal("error during Unmarshal(): ", err)
	}

	testsCount := 0
	failedTestsCount := 0
	for _, testCase := range testCases {
		if !runTestCase(testCase) {
			failedTestsCount += 1
		}

		testsCount += 1
	}

	if EXPECTED_TEST_CASES_COUNT != testsCount {
		t.Errorf("unexpected number of tests run (%d)", testsCount)
	}

	if 0 != failedTestsCount {
		t.Errorf("%d test(s) failed ", failedTestsCount)
	}
}

// endregion

// region KeyConversion

type KeyConversionTestCase struct {
	PrivateKey string
	PublicKey string
}

func TestVectors_KeyConversion(t *testing.T) {
	hasher := sha3.NewLegacyKeccak512()
	RunTestVectors(t, "1.test-keys.json", func(testCase KeyConversionTestCase) bool {
		// Arrange:
		privateKey, _ := hex.DecodeString(testCase.PrivateKey);
		expectedPublicKey, _ := hex.DecodeString(testCase.PublicKey);

		seed := make([]byte, len(privateKey))
		copy(seed, privateKey)
		ReverseInPlace(seed)

		// Act:
		actualPrivateKey := NewKeyFromSeed(seed, hasher)
		actualPublicKey := actualPrivateKey.Public()

		// Assert:
		return PublicKey(expectedPublicKey).Equal(actualPublicKey)
	})
}

// endregion

// region Sign

type SignTestCase struct {
	PrivateKey string
	Data string
	Signature string
}

func TestVectors_Sign(t *testing.T) {
	hasher := sha3.NewLegacyKeccak512()
	RunTestVectors(t, "2.test-sign.json", func(testCase SignTestCase) bool {
		// Arrange:
		privateKeySeed, _ := hex.DecodeString(testCase.PrivateKey)
		message, _ := hex.DecodeString(testCase.Data)
		expectedSignature, _ := hex.DecodeString(testCase.Signature)

		ReverseInPlace(privateKeySeed)
		privateKey := NewKeyFromSeed(privateKeySeed, hasher)

		// Act:
		actualSignature := Sign(privateKey, message, hasher)

		// Assert:
		return bytes.Equal(expectedSignature, actualSignature)
	})
}

// endregion

// region Verify

type VerifyTestCase struct {
	PublicKey string
	Data string
	Signature string
}

func TestVectors_Verify(t *testing.T) {
	hasher := sha3.NewLegacyKeccak512()
	RunTestVectors(t, "2.test-sign.json", func(testCase VerifyTestCase) bool {
		// Arrange:
		publicKey, _ := hex.DecodeString(testCase.PublicKey)
		message, _ := hex.DecodeString(testCase.Data)
		signature, _ := hex.DecodeString(testCase.Signature)

		// Act:
		isVerified := Verify(publicKey, message, signature, hasher)

		// Assert:
		return isVerified
	})
}

// endregion
