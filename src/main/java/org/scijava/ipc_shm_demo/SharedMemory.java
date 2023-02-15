/*-
 * #%L
 * Interprocess shared memory demo.
 * %%
 * Copyright (C) 2023 SciJava developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.scijava.ipc_shm_demo;

import com.sun.jna.Pointer;
import com.sun.jna.platform.linux.LibRT;
import com.sun.jna.platform.unix.LibCUtil;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;

/**
 * <strong>Unfinished</strong> port of Python's {@code multiprocess.shared_memory.SharedMemory} class.
 * Original source code in Python is <a href="https://github.com/python/cpython/blob/v3.11.2/Lib/multiprocessing/shared_memory.py">here</a>.
 */
public class SharedMemory {

	private static final boolean _USE_POSIX =
		System.getProperty("os.name").indexOf("Windows") < 0;

	public static final int O_RDONLY = 0;
	public static final int O_WRONLY = 1;
	public static final int O_RDWR = 2;
	public static final int O_NONBLOCK = 4;
	public static final int O_APPEND = 8;
	public static final int O_SHLOCK = 16;
	public static final int O_EXLOCK = 32;
	public static final int O_ASYNC = 64;
	public static final int O_SYNC = 128;
	public static final int O_FSYNC = 128;
	public static final int O_NOFOLLOW = 256;
	public static final int O_CREAT = 512;
	public static final int O_TRUNC = 1024;
	public static final int O_EXCL = 2048;

	public static final int O_ACCMODE = 3;
	public static final int O_NDELAY = 4;

	public static final int O_EVTONLY = 32768;
	public static final int O_NOCTTY = 131072;
	public static final int O_DIRECTORY = 1048576;
	public static final int O_SYMLINK = 2097152;
	public static final int O_DSYNC = 4194304;
	public static final int O_CLOEXEC = 16777216;
	public static final int O_NOFOLLOW_ANY = 536870912;

	private static final int _O_CREX = O_CREAT | O_EXCL;

	/** FreeBSD (and perhaps other BSDs) limit names to 14 characters. */
	private static final int _SHM_SAFE_NAME_LENGTH = 14;

	/** Shared memory block name prefix. */
	private static final String _SHM_NAME_PREFIX = _USE_POSIX ? "/psm_" : "wnsm_";

	public static class MemoryView {
		public MemoryView(Pointer _mmap) {
			throw new UnsupportedOperationException("Unimplemented");
		}

		public void release() {
			throw new UnsupportedOperationException("Unimplemented");
		}
	}

	private static String token_hex(long nbytes) {
		StringBuilder sb = new StringBuilder();
		for (int b=0; b<nbytes; b++) {
			String s = Long.toHexString(Double.doubleToLongBits(Math.random()) & 0xff);
			assert s.length() >= 1 && s.length() <= 2;
			if (s.length() == 1) sb.append("0");
			sb.append(s);
		}
		return sb.toString();
	}

	private static long sizeFromFileDescriptor(int fd) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	private Pointer mmap(int i, long size) {
		String tagName = null; // FIXME
		return mmap(i, size, tagName);
	}

	private Pointer mmap(int i, long size, String tagName) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	private void osClose(int _fd) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	private void register(String name) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	private static void unregister(String name) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	/** Creates a random filename for the shared memory object. */
	private static String _make_filename() {
		// number of random bytes to use for name
		long nbytes = (_SHM_SAFE_NAME_LENGTH - _SHM_NAME_PREFIX.length()) / 2;
		assert nbytes >= 2; // '_SHM_NAME_PREFIX too long'
		String name = _SHM_NAME_PREFIX + token_hex(nbytes);
		assert name.length() <= _SHM_SAFE_NAME_LENGTH;
		return name;
	}

	private String _name;
	private long _size;
	private int _fd = -1;
	private Pointer _mmap;
	private MemoryView _buf;
	private int _flags = O_RDWR;
	private int _mode = 0600;
	private boolean _prepend_leading_slash = _USE_POSIX;

	public SharedMemory(String name, boolean create, long size) {
		// NB: Would be great to use LArray for this instead. But it
		// doesn't support an equivalent of Python's shared_memory:
		// https://github.com/xerial/larray/issues/78

		if (size < 0) {
			throw new IllegalArgumentException("'size' must be a positive integer");
		}
		if (create) {
			this._flags = _O_CREX | O_RDWR;
			if (size == 0) {
				throw new IllegalArgumentException("'size' must be a positive number different from zero");
			}
		}
		if (name == null && (this._flags & O_EXCL) != 0) {
			throw new IllegalArgumentException("'name' can only be null if create=true");
		}

		if (_USE_POSIX) {
			// POSIX Shared Memory

			if (name == null) {
				while (true) {
					name = _make_filename();
					this._fd = LibRT.INSTANCE.shm_open(
						name,
						this._flags,
						this._mode
							);
					this._name = name;
					break;
				}
			}
			else {
				name = this._prepend_leading_slash ? "/" + name : name;
				this._fd = LibRT.INSTANCE.shm_open(
					name,
					this._flags,
					_mode
						);
				this._name = name;
			}
			try {
				if (create && size != 0) {
					LibCUtil.ftruncate(this._fd, size);
				}
				size = sizeFromFileDescriptor(this._fd);
				//LibCUtil.mmap(Pointer addr, long length, int prot, int flags, int fd, long offset);
				this._mmap = mmap(this._fd, size);
			}
			finally {
				this.unlink();
			}

			register(this._name);
		}
		else {
			// Windows Named Shared Memory

			if (create) {
				while (true) {
					String temp_name = name == null ? _make_filename() : name;
					// Create and reserve shared memory block with this name
					// until it can be attached to by mmap.
					Kernel32.HANDLE h_map = Kernel32.INSTANCE.CreateFileMapping(
						WinBase.INVALID_HANDLE_VALUE,
						null,
						WinNT.PAGE_READWRITE,
						(int) ((size >> 32) & 0xFFFFFFFF),
						(int) (size & 0xFFFFFFFF),
						temp_name
							);
					try {
						int last_error_code = Kernel32.INSTANCE.GetLastError();
						if (last_error_code == WinError.ERROR_ALREADY_EXISTS) {
							if (name != null) {
								throw new RuntimeException("File already exists: " + name);
							}
							continue;
						}
						//LibCUtil.mmap(Pointer addr, long length, int prot, int flags, int fd, long offset);
						this._mmap = mmap(-1, size, temp_name);
					}
					finally {
						Kernel32.INSTANCE.CloseHandle(h_map);
					}
					this._name = temp_name;
					break;
				}
			}
			else {
				this._name = name;
				// Dynamically determine the existing named shared memory
				// block's size which is likely a multiple of mmap.PAGESIZE.
				Kernel32.HANDLE h_map = Kernel32.INSTANCE.OpenFileMapping(
					WinBase.FILE_MAP_READ,
					false,
					name
						);
				Pointer p_buf;
				try {
					p_buf = Kernel32.INSTANCE.MapViewOfFile(
						h_map,
						WinBase.FILE_MAP_READ,
						0,
						0,
						0
							);
				}
				finally {
					Kernel32.INSTANCE.CloseHandle(h_map);
				}
				try {
					//SIZE_T size = Kernel32.INSTANCE.VirtualQueryEx(HANDLE hProcess, Pointer lpAddress, MEMORY_BASIC_INFORMATION lpBuffer, SIZE_T dwLength);
					if (getClass() == getClass()) throw new UnsupportedOperationException();
					//size = Kernel32.INSTANCE.VirtualQuerySize(p_buf);
				}
				finally {
					Kernel32.INSTANCE.UnmapViewOfFile(p_buf);
				}
				//LibCUtil.mmap(Pointer addr, long length, int prot, int flags, int fd, long offset);
				this._mmap = mmap(-1, size, name);
			}
		}

		this._size = size;
		this._buf = new MemoryView(this._mmap);
	}

	/** A memoryview of contents of the shared memory block. */
	public MemoryView buf() {
		return this._buf;
	}

	/** Unique name that identifies the shared memory block. */
	public String name() {
		String reported_name = this._name;
		if (_USE_POSIX && this._prepend_leading_slash) {
			if (this._name.startsWith("/")) {
				reported_name = this._name.substring(1);
			}
		}
		return reported_name;
	}

	/** Size in bytes. */
	public long size() {
		return this._size;
	}

	/**
	 * Closes access to the shared memory from this instance but does
   * not destroy the shared memory block.
   */
	public void close() {
		if (this._buf != null) {
			this._buf.release();
			this._buf = null;
		}
		if (this._mmap != null ){
			//this._mmap.close();
			this._mmap = null;
		}
		if (_USE_POSIX && this._fd >= 0) {
			osClose(this._fd);
			this._fd = -1;
		}
	}

	/**
	 * Requests that the underlying shared memory block be destroyed.
   * In order to ensure proper cleanup of resources, unlink should be
   * called once (and only once) across all processes which have access
   * to the shared memory block.
   */
	public void unlink() {
		if (_USE_POSIX && this._name != null) {
			LibRT.INSTANCE.shm_unlink(this._name);
			unregister(this._name);
		}
	}

}
