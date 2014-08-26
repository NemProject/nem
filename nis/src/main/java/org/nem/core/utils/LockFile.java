package org.nem.core.utils;

import java.io.*;
import java.nio.channels.*;

/**
 * Static class that exposes functions for interacting with lock files.
 */
public class LockFile {

	/**
	 * Tries to acquire a file lock for the specified file.
	 * TODO: this API is not safe and can leak a file handle, but since it is only called once on boot, it's probably ok for now
	 *
	 * @param lockFile The lock file.
	 * @return The file lock if acquired, or null otherwise.
	 */
	public static FileLock tryAcquireLock(final File lockFile) {
		try {
			final RandomAccessFile file = new RandomAccessFile(lockFile, "rw");

			// try to acquire the lock 5 times
			for (int i = 0; i < 5; ++i) {
				ExceptionUtils.propagateVoid(() -> Thread.sleep(10));
				final FileLock lock = file.getChannel().tryLock();
				if (null != lock) {
					return lock;
				}
			}

			return null;
		} catch (final IOException|OverlappingFileLockException e) {
			return null;
		}
	}

	/**
	 * Determines whether or not the specified file is locked.
	 *
	 * @param lockFile The lock file.
	 * @return true if the file is locked, false otherwise.
	 */
	public static boolean isLocked(final File lockFile) {
		try (final RandomAccessFile file = new RandomAccessFile(lockFile, "rw");
			 final FileLock lock = file.getChannel().tryLock()) {
			return null == lock;
		} catch (final OverlappingFileLockException e) {
			return true;
		} catch (final IOException e) {
			return false;
		}
	}
}
