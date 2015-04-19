package org.nem.core.utils;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.*;
import org.junit.*;

import java.io.*;

public class LockFileTest {

	private static final String WORKING_DIRECTORY = System.getProperty("user.dir");
	private static final File TEST_FILE_DIRECTORY = new File(WORKING_DIRECTORY, "test_files");
	private static final File TEST_EXISTING_FILE = new File(TEST_FILE_DIRECTORY, "test.lock");

	//region BeforeClass / AfterClass

	@BeforeClass
	public static void createTestFiles() throws IOException {
		final boolean result = TEST_FILE_DIRECTORY.mkdir() && TEST_EXISTING_FILE.createNewFile();

		if (!result) {
			throw new RuntimeException("unable to initialize test suite");
		}
	}

	@AfterClass
	public static void removeTestFiles() throws IOException {
		FileUtils.deleteDirectory(TEST_FILE_DIRECTORY);
	}

	//endregion

	//region tryAcquireLock

	@Test
	public void tryAcquireLockReturnsNullWhenLockFileIsInvalid() throws IOException {
		// Act:
		try (final Closeable lock = LockFile.tryAcquireLock(new File("foo\u0000.lock"))) {
			// Assert:
			Assert.assertThat(lock, IsNull.nullValue());
		}
	}

	@Test
	public void tryAcquireLockReturnsLockWhenExistingFileIsNotLocked() throws IOException {
		// Act:
		try (final Closeable lock = LockFile.tryAcquireLock(TEST_EXISTING_FILE)) {
			// Assert:
			Assert.assertThat(lock, IsNull.notNullValue());
		}
	}

	@Test
	public void tryAcquireLockReturnsLockWhenNewFileIsNotLocked() throws IOException {
		// Act:
		try (final Closeable lock = LockFile.tryAcquireLock(new File(TEST_FILE_DIRECTORY, "tryAcquireLock_new.lock"))) {
			// Assert:
			Assert.assertThat(lock, IsNull.notNullValue());
		}
	}

	@Test
	public void tryAcquireLockReturnsNullWhenExistingFileIsLocked() throws IOException {
		// Act:
		try (final Closeable lock1 = LockFile.tryAcquireLock(TEST_EXISTING_FILE)) {
			try (final Closeable lock2 = LockFile.tryAcquireLock(TEST_EXISTING_FILE)) {
				// Assert:
				Assert.assertThat(lock1, IsNull.notNullValue());
				Assert.assertThat(lock2, IsNull.nullValue());
			}
		}
	}

	//endregion

	//region isLocked

	@Test
	public void isLockedReturnsFalseWhenLockFileIsInvalid() throws IOException {
		// Act:
		final boolean isLocked = LockFile.isLocked(new File("foo\u0000.lock"));

		// Assert:
		Assert.assertThat(isLocked, IsEqual.equalTo(false));
	}

	@Test
	public void isLockedReturnsFalseWhenLockFileIsNotLocked() throws IOException {
		// Act:
		final boolean isLocked = LockFile.isLocked(TEST_EXISTING_FILE);

		// Assert:
		Assert.assertThat(isLocked, IsEqual.equalTo(false));
	}

	@Test
	public void isLockedReturnsTrueWhenLockFileIsNotLocked() throws IOException {
		// Arrange:
		try (final Closeable ignored = LockFile.tryAcquireLock(TEST_EXISTING_FILE)) {
			// Act:
			final boolean isLocked = LockFile.isLocked(TEST_EXISTING_FILE);

			// Assert:
			Assert.assertThat(isLocked, IsEqual.equalTo(true));
		}
	}

	@Test
	public void isLockedReleasesLockBeforeReturn() throws IOException {
		// Act:
		final boolean isLocked = LockFile.isLocked(TEST_EXISTING_FILE);

		try (final Closeable lock = LockFile.tryAcquireLock(TEST_EXISTING_FILE)) {
			// Assert:
			Assert.assertThat(isLocked, IsEqual.equalTo(false));
			Assert.assertThat(lock, IsNull.notNullValue());
		}
	}

	//endregion
}