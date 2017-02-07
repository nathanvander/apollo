/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 *
 * NOTE:  Downloaded from https://github.com/gwenn/sqlite-jna.
 */
package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

// TODO JNA/Bridj/JNR/JNI and native libs embedded in JAR.
public final class SQLite implements Library {
	private static final String JNA_LIBRARY_NAME = "sqlite3";

	// public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(SQLite.JNA_LIBRARY_NAME);
	static {
		Native.register(JNA_LIBRARY_NAME);
	}

	public static final int SQLITE_OK = 0;

	public static final int SQLITE_ROW = 100;
	public static final int SQLITE_DONE = 101;

	static final int SQLITE_TRANSIENT = -1;

	static native String sqlite3_libversion(); // no copy needed
	static native int sqlite3_libversion_number();
	static native boolean sqlite3_threadsafe();
	static native boolean sqlite3_compileoption_used(String optName);
	static native String sqlite3_compileoption_get(int n);

	public static final int SQLITE_CONFIG_SINGLETHREAD = 1,
			SQLITE_CONFIG_MULTITHREAD = 2, SQLITE_CONFIG_SERIALIZED = 3,
			SQLITE_CONFIG_MEMSTATUS = 9,
			SQLITE_CONFIG_LOG = 16,
			SQLITE_CONFIG_URI = 17;
	//sqlite3_config(SQLITE_CONFIG_SINGLETHREAD|SQLITE_CONFIG_MULTITHREAD|SQLITE_CONFIG_SERIALIZED)
	static native int sqlite3_config(int op);
	//sqlite3_config(SQLITE_CONFIG_URI, int onoff)
	//sqlite3_config(SQLITE_CONFIG_MEMSTATUS, int onoff)
	static native int sqlite3_config(int op, boolean onoff);
	//sqlite3_config(SQLITE_CONFIG_LOG, void(*)(void *udp, int err, const char *msg), void *udp)
	public static native int sqlite3_config(int op, LogCallback xLog, Pointer udp);
	// Applications can use the sqlite3_log(E,F,..) API to send new messages to the log, if desired, but this is discouraged.
	public static native void sqlite3_log(int iErrCode, String msg);

	static native String sqlite3_errmsg(SQLite3 pDb); // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
	static native int sqlite3_errcode(SQLite3 pDb);

	static native int sqlite3_extended_result_codes(SQLite3 pDb, boolean onoff);
	static native int sqlite3_extended_errcode(SQLite3 pDb);

	static native int sqlite3_initialize();
	static native int sqlite3_shutdown();

	static native int sqlite3_open_v2(String filename, PointerByReference ppDb, int flags, String vfs); // no copy needed
	static native int sqlite3_close(SQLite3 pDb);
	static native int sqlite3_close_v2(SQLite3 pDb); // since 3.7.14
	static native void sqlite3_interrupt(SQLite3 pDb);

	static native int sqlite3_busy_handler(SQLite3 pDb, BusyHandler bh, Pointer pArg);
	static native int sqlite3_busy_timeout(SQLite3 pDb, int ms);
	static native int sqlite3_db_status(SQLite3 pDb, int op, IntByReference pCur, IntByReference pHiwtr, boolean resetFlg);
	static native int sqlite3_db_config(SQLite3 pDb, int op, int v, IntByReference pOk);
	//#if mvn.project.property.sqlite.omit.load.extension == "true"
	static int sqlite3_enable_load_extension(Object pDb, boolean onoff) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
	static int sqlite3_load_extension(Object pDb, String file, String proc, PointerByReference errMsg) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
	//#else
	static native int sqlite3_enable_load_extension(SQLite3 pDb, boolean onoff);
	static native int sqlite3_load_extension(SQLite3 pDb, String file, String proc, PointerByReference errMsg);
	//#endif
	public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
			SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
			SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
			SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10;
	static native int sqlite3_limit(SQLite3 pDb, int id, int newVal);
	static native boolean sqlite3_get_autocommit(SQLite3 pDb);

	static native int sqlite3_changes(SQLite3 pDb);
	static native int sqlite3_total_changes(SQLite3 pDb);
	static native long sqlite3_last_insert_rowid(SQLite3 pDb);

	static native String sqlite3_db_filename(SQLite3 pDb, String dbName); // no copy needed
	static native int sqlite3_db_readonly(SQLite3 pDb, String dbName); // no copy needed

	static native SQLite3Stmt sqlite3_next_stmt(SQLite3 pDb, SQLite3Stmt pStmt);

	static native int sqlite3_table_column_metadata(SQLite3 pDb, String dbName, String tableName, String columnName,
			PointerByReference pzDataType, PointerByReference pzCollSeq,
			IntByReference pNotNull, IntByReference pPrimaryKey, IntByReference pAutoinc); // no copy needed

	static native int sqlite3_exec(SQLite3 pDb, String cmd, Callback c, Pointer udp, PointerByReference errMsg);

	static native int sqlite3_prepare_v2(SQLite3 pDb, Pointer sql, int nByte, PointerByReference ppStmt,
			PointerByReference pTail);
	static native String sqlite3_sql(SQLite3Stmt pStmt); // no copy needed
	static native int sqlite3_finalize(SQLite3Stmt pStmt);
	static native int sqlite3_step(SQLite3Stmt pStmt);
	static native int sqlite3_reset(SQLite3Stmt pStmt);
	static native int sqlite3_clear_bindings(SQLite3Stmt pStmt);
	static native boolean sqlite3_stmt_busy(SQLite3Stmt pStmt);
	static native boolean sqlite3_stmt_readonly(SQLite3Stmt pStmt);

	static native int sqlite3_column_count(SQLite3Stmt pStmt);
	static native int sqlite3_data_count(SQLite3Stmt pStmt);
	static native int sqlite3_column_type(SQLite3Stmt pStmt, int iCol);
	static native String sqlite3_column_name(SQLite3Stmt pStmt, int iCol); // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
	//#if mvn.project.property.sqlite.enable.column.metadata == "true"
	static native String sqlite3_column_origin_name(SQLite3Stmt pStmt, int iCol); // copy needed
	static native String sqlite3_column_table_name(SQLite3Stmt pStmt, int iCol); // copy needed
	static native String sqlite3_column_database_name(SQLite3Stmt pStmt, int iCol); // copy needed
	static native String sqlite3_column_decltype(SQLite3Stmt pStmt, int iCol); // copy needed
	//#else
	static String sqlite3_column_origin_name(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	static String sqlite3_column_table_name(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	static String sqlite3_column_database_name(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	static String sqlite3_column_decltype(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	//#endif

	static native Pointer sqlite3_column_blob(SQLite3Stmt pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
	static native int sqlite3_column_bytes(SQLite3Stmt pStmt, int iCol);
	static native double sqlite3_column_double(SQLite3Stmt pStmt, int iCol);
	static native int sqlite3_column_int(SQLite3Stmt pStmt, int iCol);
	static native long sqlite3_column_int64(SQLite3Stmt pStmt, int iCol);
	static native String sqlite3_column_text(SQLite3Stmt pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
	//const void *sqlite3_column_text16(SQLite3Stmt pStmt, int iCol);
	//sqlite3_value *sqlite3_column_value(SQLite3Stmt pStmt, int iCol);

	static native int sqlite3_bind_parameter_count(SQLite3Stmt pStmt);
	static native int sqlite3_bind_parameter_index(SQLite3Stmt pStmt, String name); // no copy needed
	static native String sqlite3_bind_parameter_name(SQLite3Stmt pStmt, int i); // copy needed

	static native int sqlite3_bind_blob(SQLite3Stmt pStmt, int i, byte[] value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	static native int sqlite3_bind_double(SQLite3Stmt pStmt, int i, double value);
	static native int sqlite3_bind_int(SQLite3Stmt pStmt, int i, int value);
	static native int sqlite3_bind_int64(SQLite3Stmt pStmt, int i, long value);
	static native int sqlite3_bind_null(SQLite3Stmt pStmt, int i);
	static native int sqlite3_bind_text(SQLite3Stmt pStmt, int i, String value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	//static native int sqlite3_bind_text16(SQLite3Stmt pStmt, int i, const void*, int, void(*)(void*));
	//static native int sqlite3_bind_value(SQLite3Stmt pStmt, int i, const sqlite3_value*);
	static native int sqlite3_bind_zeroblob(SQLite3Stmt pStmt, int i, int n);
	static native int sqlite3_stmt_status(SQLite3Stmt pStmt, int op, boolean reset);
	//#if mvn.project.property.sqlite.enable.stmt.scanstatus == "true"
	static native int sqlite3_stmt_scanstatus(SQLite3Stmt pStmt, int idx, int iScanStatusOp, PointerByReference pOut);
	static native void sqlite3_stmt_scanstatus_reset(SQLite3Stmt pStmt);
	//#endif

	static native void sqlite3_free(Pointer p);

	static native int sqlite3_blob_open(SQLite3 pDb, String dbName, String tableName, String columnName,
			long iRow, boolean flags, PointerByReference ppBlob); // no copy needed
	static native int sqlite3_blob_reopen(SQLite3Blob pBlob, long iRow);
	static native int sqlite3_blob_bytes(SQLite3Blob pBlob);
	static native int sqlite3_blob_read(SQLite3Blob pBlob, ByteBuffer z, int n, int iOffset);
	static native int sqlite3_blob_write(SQLite3Blob pBlob, ByteBuffer z, int n, int iOffset);
	static native int sqlite3_blob_close(SQLite3Blob pBlob);

	static native SQLite3Backup sqlite3_backup_init(SQLite3 pDst, String dstName, SQLite3 pSrc, String srcName);
	static native int sqlite3_backup_step(SQLite3Backup pBackup, int nPage);
	static native int sqlite3_backup_remaining(SQLite3Backup pBackup);
	static native int sqlite3_backup_pagecount(SQLite3Backup pBackup);
	static native int sqlite3_backup_finish(SQLite3Backup pBackup);

	// As there is only one ProgressCallback by connection, and it is used to implement query timeout,
	// the method visibility is restricted.
	static native void sqlite3_progress_handler(SQLite3 pDb, int nOps, ProgressCallback xProgress, Pointer pArg);
	static native void sqlite3_trace(SQLite3 pDb, TraceCallback xTrace, Pointer pArg);
	static native void sqlite3_profile(SQLite3 pDb, ProfileCallback xProfile, Pointer pArg);

	// TODO sqlite3_commit_hook, sqlite3_rollback_hook
	static native Pointer sqlite3_update_hook(SQLite3 pDb, UpdateHook xUpdate, Pointer pArg);
	static native int sqlite3_set_authorizer(SQLite3 pDb, Authorizer authorizer, Pointer pUserData);

	/*
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*),
	void(*)(void*)
	*/
	// eTextRep: SQLITE_UTF8 => 1, ...
	static native int sqlite3_create_function_v2(SQLite3 pDb, String functionName, int nArg, int eTextRep,
			Pointer pApp, ScalarCallback xFunc, AggregateStepCallback xStep, AggregateFinalCallback xFinal, Destructor xDestroy);

	static native void sqlite3_result_null(SQLite3Context pCtx);
	static native void sqlite3_result_int(SQLite3Context pCtx, int i);
	static native void sqlite3_result_double(SQLite3Context pCtx, double d);
	static native void sqlite3_result_text(SQLite3Context pCtx, String text, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	static native void sqlite3_result_blob(SQLite3Context pCtx, byte[] blob, int n, long xDel);
	static native void sqlite3_result_int64(SQLite3Context pCtx, long l);
	static native void sqlite3_result_zeroblob(SQLite3Context pCtx, int n);

	static native void sqlite3_result_error(SQLite3Context pCtx, String err, int length);
	static native void sqlite3_result_error_code(SQLite3Context pCtx, int errCode);
	static native void sqlite3_result_error_nomem(SQLite3Context pCtx);
	static native void sqlite3_result_error_toobig(SQLite3Context pCtx);
	//static native void sqlite3_result_subtype(SQLite3Context pCtx, /*unsigned*/ int subtype);

	static native Pointer sqlite3_value_blob(Pointer pValue);
	static native int sqlite3_value_bytes(Pointer pValue);
	static native double sqlite3_value_double(Pointer pValue);
	static native int sqlite3_value_int(Pointer pValue);
	static native long sqlite3_value_int64(Pointer pValue);
	static native String sqlite3_value_text(Pointer pValue);
	static native int sqlite3_value_type(Pointer pValue);
	static native int sqlite3_value_numeric_type(Pointer pValue);

	static native Pointer sqlite3_get_auxdata(SQLite3Context pCtx, int n);
	static native void sqlite3_set_auxdata(SQLite3Context pCtx, int n, Pointer p, Destructor free);
	static native Pointer sqlite3_aggregate_context(SQLite3Context pCtx, int nBytes);
	static native SQLite3 sqlite3_context_db_handle(SQLite3Context pCtx);

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final String UTF_8_ECONDING = UTF_8.name();
	static Pointer nativeString(String sql) {
		final byte[] data = sql.getBytes(UTF_8);
		final Pointer pointer = new Memory(data.length + 1);
		pointer.write(0L, data, 0, data.length);
		pointer.setByte(data.length, (byte) 0);
		return pointer;
	}

	// http://sqlite.org/datatype3.html
	public static int getAffinity(String declType) {
		if (declType == null || declType.isEmpty()) {
			return ColAffinities.NONE;
		}
		declType = declType.toUpperCase();
		if (declType.contains("INT")) {
			return ColAffinities.INTEGER;
		} else if (declType.contains("TEXT") || declType.contains("CHAR") || declType.contains("CLOB")) {
			return ColAffinities.TEXT;
		} else if (declType.contains("BLOB")) {
			return ColAffinities.NONE;
		} else if (declType.contains("REAL") || declType.contains("FLOA") || declType.contains("DOUB")) {
			return ColAffinities.REAL;
		} else {
			return ColAffinities.NUMERIC;
		}
	}

	private SQLite() {
	}

	public static String escapeIdentifier(String identifier) {
		if (identifier == null) {
			return "";
		}
		if (identifier.indexOf('"') >= 0) { // escape quote by doubling them
			identifier = identifier.replaceAll("\"", "\"\"");
		}
		return identifier;
	}

	public static String doubleQuote(String dbName) {
		if (dbName == null) {
			return "";
		}
		if ("main".equals(dbName) || "temp".equals(dbName)) {
			return dbName;
		}
		return '"' + escapeIdentifier(dbName) + '"'; // surround identifier with quote
	}
	public static String qualify(String dbName) {
		if (dbName == null) {
			return "";
		}
		if ("main".equals(dbName) || "temp".equals(dbName)) {
			return dbName + '.';
		}
		return '"' + escapeIdentifier(dbName) + '"' + '.'; // surround identifier with quote
	}

	public interface LogCallback extends Callback {
		@SuppressWarnings("unused")
		default void callback(Pointer udp, int err, String msg) {
			log(err, msg);
		}
		void log(int err, String msg);
	}

	private static final LogCallback LOG_CALLBACK = new LogCallback() {
		@Override
		public void log(int err, String msg) {
			System.out.printf("%d: %s%n", err, msg);
		}
	};

	static {
		if (!System.getProperty("sqlite.config.log", "").isEmpty()) {
			// DriverManager.getLogWriter();
			final int res = sqlite3_config(SQLITE_CONFIG_LOG, LOG_CALLBACK, null);
			if (res != SQLITE_OK) {
				throw new ExceptionInInitializerError("sqlite3_config(SQLITE_CONFIG_LOG, ...) = " + res);
			}
		}
	}

	/**
	 * Query Progress Callback.
	 * @see <a href="http://sqlite.org/c3ref/progress_handler.html">sqlite3_progress_handler</a>
	 */
	public interface ProgressCallback extends Callback {
		/**
		 * @param arg
		 * @return <code>true</code> to interrupt
		 */
		@SuppressWarnings("unused")
		default boolean callback(Pointer arg) {
			return progress();
		}

		boolean progress();
	}

	/**
	 * Database connection handle
	 * @see <a href="http://sqlite.org/c3ref/sqlite3.html">sqlite3</a>
	 */
	public static class SQLite3 extends PointerType {
		public SQLite3() {
		}
		public SQLite3(Pointer p) {
			super(p);
		}
	}

	/**
	 * Prepared statement object
	 * @see <a href="http://sqlite.org/c3ref/stmt.html">sqlite3_stmt</a>
	 */
	public static class SQLite3Stmt extends PointerType {
		public SQLite3Stmt() {
		}
		public SQLite3Stmt(Pointer p) {
			super(p);
		}
	}

	/**
	 * A handle to an open BLOB
	 * @see <a href="http://sqlite.org/c3ref/blob.html">sqlite3_blob</a>
	 */
	public static class SQLite3Blob extends PointerType {
		public SQLite3Blob() {
		}
		public SQLite3Blob(Pointer p) {
			super(p);
		}
	}

	/**
	 * Online backup object
	 * @see <a href="http://sqlite.org/c3ref/backup.html">sqlite3_backup</a>
	 */
	public static class SQLite3Backup extends PointerType {
		public SQLite3Backup() {
		}
		public SQLite3Backup(Pointer p) {
			super(p);
		}
	}

	/**
	 * SQL function context object
	 * @see <a href="http://sqlite.org/c3ref/context.html">sqlite3_context</a>
	 */
	public static class SQLite3Context extends PointerType {
		public SQLite3Context() {
		}
		public SQLite3Context(Pointer p) {
			super(p);
		}

		/**
		 * @return a copy of the pointer to the database connection (the 1st parameter) of
		 * {@link SQLite#sqlite3_create_function_v2(SQLite3, String, int, int, Pointer, ScalarCallback, AggregateStepCallback, AggregateFinalCallback, Destructor)}
		 * @see <a href="http://sqlite.org/c3ref/context_db_handle.html">sqlite3_context_db_handle</a>
         */
		public SQLite3 getDbHandle() {
			return sqlite3_context_db_handle(this);
		}

		/**
		 * Sets the return value of the application-defined function to be the BLOB value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_blob</a>
		 */
		public void setResultBlob(byte[] result) {
			sqlite3_result_blob(this, result, result.length, SQLITE_TRANSIENT);
		}
		/**
		 * Sets the return value of the application-defined function to be the floating point value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_double</a>
		 */
		public void setResultDouble(double result) {
			sqlite3_result_double(this, result);
		}
		/**
		 * Sets the return value of the application-defined function to be the 32-bit signed integer value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_int</a>
		 */
		public void setResultInt(int result) {
			sqlite3_result_int(this, result);
		}
		/**
		 * Sets the return value of the application-defined function to be the 64-bit signed integer value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_int64</a>
		 */
		public void setResultLong(long result) {
			sqlite3_result_int64(this, result);
		}
		/**
		 * Sets the return value of the application-defined function to be NULL.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_null</a>
		 */
		public void setResultNull() {
			sqlite3_result_null(this);
		}
		/**
		 * Sets the return value of the application-defined function to be the text string given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_text</a>
		 */
		public void setResultText(String result) {
			sqlite3_result_text(this, result, -1, SQLITE_TRANSIENT);
		}
		/**
		 * Sets the return value of the application-defined function to be a BLOB containing all zero bytes and N bytes in size.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_zeroblob</a>
		 */
		public void setResultZeroBlob(ZeroBlob result) {
			sqlite3_result_zeroblob(this, result.n);
		}

		/*
		 * Causes the subtype of the result from the application-defined SQL function to be the value given.
		 * @see <a href="http://sqlite.org/c3ref/result_subtype.html">sqlite3_result_subtype</a>
		 */
		/*public void setResultSubType(int subtype) {
			sqlite3_result_subtype(this, subtype);
		}*/

		/**
		 * Causes the implemented SQL function to throw an exception.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error</a>
		 */
		public void setResultError(String errMsg) {
			sqlite3_result_error(this, errMsg, -1);
		}
		/**
		 * Causes the implemented SQL function to throw an exception.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_code</a>
		 */
		public void setResultErrorCode(int errCode) {
			sqlite3_result_error_code(this, errCode);
		}
		/**
		 * Causes SQLite to throw an error indicating that a memory allocation failed.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_nomem</a>
		 */
		public void setResultErrorNoMem() {
			sqlite3_result_error_nomem(this);
		}
		/**
		 * Causes SQLite to throw an error indicating that a string or BLOB is too long to represent.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_toobig</a>
		 */
		public void setResultErrorTooBig() {
			sqlite3_result_error_toobig(this);
		}
	}

	/**
	 * Dynamically typed value objects
	 * @see <a href="http://sqlite.org/c3ref/value.html">sqlite3_value</a>
	 */
	public static class SQLite3Values {
		private static final SQLite3Values NO_ARG = new SQLite3Values(new Pointer[0]);
		private final Pointer[] args;

		public static SQLite3Values build(int nArg, Pointer args) {
			if (nArg == 0) {
				return NO_ARG;
			}
			return new SQLite3Values(args.getPointerArray(0, nArg));
		}

		private SQLite3Values(Pointer[] args) {
			this.args = args;
		}

		/**
		 * @return arg count
		 */
		public int getCount() {
			return args.length;
		}

		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_blob</a>
		 */
		public byte[] getBlob(int i) {
			Pointer arg = args[i];
			Pointer blob = sqlite3_value_blob(arg);
			if (blob == null) {
				return null;
			} else {
				return blob.getByteArray(0L, sqlite3_value_bytes(arg)); // a copy is made...
			}
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_double</a>
		 */
		public double getDouble(int i) {
			return sqlite3_value_double(args[i]);
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_int</a>
		 */
		public int getInt(int i) {
			return sqlite3_value_int(args[i]);
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_int64</a>
		 */
		public long getLong(int i) {
			return sqlite3_value_int64(args[i]);
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_text</a>
		 */
		public String getText(int i) {
			return sqlite3_value_text(args[i]);
		}
		/**
		 * @param i 0...
		 * @return {@link ColTypes}.*
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_type</a>
		 */
		public int getType(int i) {
			return sqlite3_value_type(args[i]);
		}
		/**
		 * @param i 0...
		 * @return {@link ColTypes}.*
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_numeric_type</a>
		 */
		public int getNumericType(int i) {
			return sqlite3_value_numeric_type(args[i]);
		}
	}

	//===================================================================
	//added code starts here

public static abstract class AggregateFinalCallback implements Callback {
		/**
		 * @param pCtx <code>sqlite3_context*</code>
		 */
		@SuppressWarnings("unused")
		public void callback(SQLite3Context pCtx) {
			finalStep(pCtx, getAggregateContext(pCtx));
		}

		protected abstract void finalStep(SQLite3Context pCtx, Pointer aggrCtx);

		/**
		 * Obtain aggregate function context.
		 *
		 * @return <code>null</code> when no rows match an aggregate query.
		 * @see <a href="http://sqlite.org/c3ref/aggregate_context.html">sqlite3_aggregate_context</a>
		 */
		protected Pointer getAggregateContext(SQLite3Context pCtx) {
			// Within the xFinal callback, it is customary to set N=0 in calls to sqlite3_aggregate_context(C,N)
			// so that no pointless memory allocations occur.
			return sqlite3_aggregate_context(pCtx, 0);
		}
}

public static abstract class AggregateStepCallback implements Callback {
	//void (*)(sqlite3_context*,int,sqlite3_value**),
	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param nArg number of arguments
	 * @param args function arguments
	 */
	@SuppressWarnings("unused")
	public void callback(SQLite3Context pCtx, int nArg, Pointer args) {
		final int nBytes = numberOfBytes();
		final Pointer p = sqlite3_aggregate_context(pCtx, nBytes);
		if (p == null && nBytes > 0) {
			sqlite3_result_error_nomem(pCtx);
			return;
		}
		step(pCtx, p, SQLite3Values.build(nArg, args));
	}

	/**
	 * @return number of bytes to allocate.
	 * @see <a href="http://sqlite.org/c3ref/aggregate_context.html">sqlite3_aggregate_context</a>
	 */
	protected abstract int numberOfBytes();

	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param aggrCtx aggregate context
	 * @param args function arguments
	 */
	protected abstract void step(SQLite3Context pCtx, Pointer aggrCtx, SQLite3Values args);
}

public static interface Authorizer extends Callback {
	/**
	 * @param pArg       User data (<code>null</code>)
	 * @param actionCode {@link ActionCodes}.*
	 * @return {@link #SQLITE_OK} or {@link #SQLITE_DENY} or {@link #SQLITE_IGNORE}
	 */
	public default int callback(Pointer pArg, int actionCode, String arg1, String arg2, String dbName, String triggerName) {
		return authorize(actionCode, arg1, arg2, dbName, triggerName);
	}

	/**
	 * @param actionCode {@link ActionCodes}.*
	 * @return {@link #SQLITE_OK} or {@link #SQLITE_DENY} or {@link #SQLITE_IGNORE}
	 */
	int authorize(int actionCode, String arg1, String arg2, String dbName, String triggerName);

	int SQLITE_OK = ErrCodes.SQLITE_OK;
	/**
	 * @see <a href="http://sqlite.org/c3ref/c_deny.html">Authorizer Return Codes</a>
	 */
	int SQLITE_DENY = 1;
	/**
	 * @see <a href="http://sqlite.org/c3ref/c_deny.html">Authorizer Return Codes</a>
	 */
	int SQLITE_IGNORE = 2;
}

public static interface BusyHandler extends Callback {
	/**
	 * @param pArg  User data (<code>null</code>)
	 * @param count the number of times that the busy handler has been invoked previously for the same locking event.
	 * @return <code>true</code> to try again, <code>false</code> to abort.
	 */
	public default boolean callback(Pointer pArg, int count) {
		return busy(count);
	}

	/**
	 * @param count the number of times that the busy handler has been invoked previously for the same locking event.
	 * @return <code>true</code> to try again, <code>false</code> to abort.
	 */
	boolean busy(int count);
}

public static interface ColAffinities {
	int INTEGER = 0;
	int TEXT = 1;
	int NONE = 2;
	int REAL = 3;
	int NUMERIC = 4;
}

public static interface Destructor extends Callback {
	public void callback(Pointer p);
}

public static interface ErrCodes {
	/**
	 * Error in SQLite Wrapper
	 */
	int WRAPPER_SPECIFIC = -1;

	/**
	 * Successful result
	 */
	int SQLITE_OK = 0;

	/**
	 * SQL error or missing database
	 */
	int SQLITE_ERROR = 1;
	/**
	 * Internal logic error in SQLite
	 */
	int SQLITE_INTERNAL = 2;
	/**
	 * Access permission denied
	 */
	int SQLITE_PERM = 3;
	/**
	 * Callback routine requested an abort
	 */
	int SQLITE_ABORT = 4;
	/**
	 * The database file is locked
	 */
	int SQLITE_BUSY = 5;
	/**
	 * A table in the database is locked
	 */
	int SQLITE_LOCKED = 6;
	/**
	 * A malloc() failed
	 */
	int SQLITE_NOMEM = 7;
	/**
	 * Attempt to write a readonly database
	 */
	int SQLITE_READONLY = 8;
	/**
	 * Operation terminated by sqlite_interrupt()
	 */
	int SQLITE_INTERRUPT = 9;
	/**
	 * Some kind of disk I/O error occurred
	 */
	int SQLITE_IOERR = 10;
	/**
	 * The database disk image is malformed
	 */
	int SQLITE_CORRUPT = 11;
	/**
	 * Unknown opcode in sqlite3_file_control()
	 */
	int SQLITE_NOTFOUND = 12;
	/**
	 * Insertion failed because database is full
	 */
	int SQLITE_FULL = 13;
	/**
	 * Unable to open the database file
	 */
	int SQLITE_CANTOPEN = 14;
	/**
	 * Database lock protocol error
	 */
	int SQLITE_PROTOCOL = 15;
	/**
	 * Database table is empty
	 */
	int SQLITE_EMPTY = 16;
	/**
	 * The database schema changed
	 */
	int SQLITE_SCHEMA = 17;
	/**
	 * String or BLOB exceeds size limit
	 */
	int SQLITE_TOOBIG = 18;
	/**
	 * Abort due to constraint violation
	 */
	int SQLITE_CONSTRAINT = 19;
	/**
	 * Data type mismatch
	 */
	int SQLITE_MISMATCH = 20;
	/**
	 * Library used incorrectly
	 */
	int SQLITE_MISUSE = 21;
	/**
	 * Uses OS features not supported on host
	 */
	int SQLITE_NOLFS = 22;
	/**
	 * Authorization denied
	 */
	int SQLITE_AUTH = 23;
	/**
	 * Auxiliary database format error
	 */
	int SQLITE_FORMAT = 24;
	/**
	 * 2nd parameter to sqlite3_bind out of range
	 */
	int SQLITE_RANGE = 25;
	/**
	 * File opened that is not a database file
	 */
	int SQLITE_NOTADB = 26;

	/** sqlite_step() has another row ready */
	int SQLITE_ROW        =  100;
	/** sqlite_step() has finished executing */
	int SQLITE_DONE       =  101;
}

public static interface ProfileCallback extends Callback {
	/**
	 * @param sql SQL statement text.
	 * @param ns time in nanoseconds
	 */
	@SuppressWarnings("unused")
	public default void callback(Pointer arg, String sql, long ns) {
		profile(sql, ns);
	}

	/**
	 * @param sql SQL statement text.
	 * @param ns time in nanoseconds
	 */
	void profile(String sql, long ns);
}

public static abstract class ScalarCallback implements Callback {
	//void (*)(sqlite3_context*,int,sqlite3_value**),
	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param nArg number of arguments
	 * @param args function arguments
	 */
	@SuppressWarnings("unused")
	public void callback(SQLite3Context pCtx, int nArg, Pointer args) {
		func(pCtx, SQLite3Values.build(nArg, args));
	}

	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param args function arguments
	 */
	protected abstract void func(SQLite3Context pCtx, SQLite3Values args);

	/**
	 * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_set_auxdata</a>
	 */
	public void setAuxData(SQLite3Context pCtx, int n, Pointer auxData, Destructor free) {
		sqlite3_set_auxdata(pCtx, n, auxData, free);
	}
	/**
	 * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_get_auxdata</a>
	 */
	public Pointer getAuxData(SQLite3Context pCtx, int n) {
		return sqlite3_get_auxdata(pCtx, n);
	}
}

public static interface TraceCallback extends Callback {
	/**
	 * @param sql SQL statement text.
	 */
	@SuppressWarnings("unused")
	public default void callback(Pointer arg, String sql) {
		trace(sql);
	}

	/**
	 * @param sql SQL statement text.
	 */
	void trace(String sql);
}

public interface UpdateHook extends Callback {
	/**
	 * Data Change Notification Callback
	 * @param pArg <code>null</code>.
	 * @param actionCode org.sqlite.ActionCodes.SQLITE_INSERT | SQLITE_UPDATE | SQLITE_DELETE.
	 * @param dbName database name containing the affected row.
	 * @param tblName table name containing the affected row.
	 * @param rowId id of the affected row.
	 */
	public default void callback(Pointer pArg, int actionCode, String dbName, String tblName, long rowId) {
		update(actionCode, dbName, tblName, rowId);
	}
	/**
	 * Data Change Notification Callback
	 * @param actionCode org.sqlite.ActionCodes.SQLITE_INSERT | SQLITE_UPDATE | SQLITE_DELETE.
	 * @param dbName database name containing the affected row.
	 * @param tblName table name containing the affected row.
	 * @param rowId id of the affected row.
	 */
	void update(int actionCode, String dbName, String tblName, long rowId);
}

public static class ZeroBlob {
	// length of BLOB
	public final int n;

	/**
	 * @param n length of BLOB
	 */
	public ZeroBlob(int n) {
		this.n = n;
	}
}

//end added code
//=====================
}
