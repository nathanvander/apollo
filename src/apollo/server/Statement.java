package apollo.server;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.PointerByReference;
import apollo.iface.DataStoreException;

/**
* Statement.  A Statement is like a program that SQLite creates to run the SQL.  There can only be
* one statement open at a time on a connection.
*
*/
public class Statement {

	static {
		api=(SQLITE_API)Native.loadLibrary("sqlite3",SQLITE_API.class,W32APIOptions.DEFAULT_OPTIONS);
	}

	public static class Handle extends PointerType {
		public Handle(Pointer p) {
			super(p);
		}
	}

	public static SQLITE_API api;
	private Connection conn;
	private Handle stmtHandle;
	private boolean closed=false;

	/**
	* Create a new Statement, given the connection and the sql.
	*/
	public Statement(Connection c,String sql) throws DataStoreException {
		System.out.println(sql);
		conn=c;
		Connection.Handle ch=c.getHandle();
		PointerByReference ppStmt=new PointerByReference();
		PointerByReference pzTail=new PointerByReference();
		byte[] basql=Connection.getByteArray(sql);

		int rc=api.sqlite3_prepare_v2(
			ch.getPointer(),
			basql,
			basql.length,
			ppStmt,
			pzTail
		);

		if (rc==0) {
			//System.out.println("success");
			Pointer pstmt=ppStmt.getValue();
			stmtHandle=new Handle(pstmt);
		} else {
			throw new DataStoreException("error in "+sql, rc);
		}
	}

	/**
	* This closes the associated Statement.  Don't use this after closing it.
	*/
	public void close() {
		int rc=api.sqlite3_finalize(stmtHandle.getPointer());
		closed=true;
		if (rc!=0) {
			//don't throw an exception
			System.out.println("warning: Statement.close() produced the following error: "+rc);
		}
	}

	public boolean isClosed() {
		return closed;
	}

	protected Handle getHandle() {return stmtHandle;}

	/**
	* Returns:
	*	true if there is data (SQLITE_ROW) was returned, false if statement has been completed (SQLITE_DONE)
	* Throws:
	*   DataStoreException - if result code from sqlite3_step was neither SQLITE_ROW nor SQLITE_DONE,
	* or if any other problem occurs
	*
	* If SQLITE_BUSY is returned, that means a connection in another process has a lock on the file.
	* This is not fatal, just try again in a few milliseconds.
	*/
	public boolean step() throws DataStoreException {
		boolean result=false;
		int rc=api.sqlite3_step(stmtHandle.getPointer());
		if (rc==api.SQLITE_ROW) {result=true;}
		else if (rc==api.SQLITE_DONE) {result=false;}
		else if (rc==api.SQLITE_BUSY) {
			throw new DataStoreException("busy, try again()",rc);
		} else {
			//I don't expect this to happen
			throw new DataStoreException("error in step()",rc);
		}
		return result;
	}


	//returns true is the statement has stepped at least once
	//but not run until completion.
	public boolean isBusy() {
		int i=api.sqlite3_stmt_busy(stmtHandle.getPointer());
		return (i==1)?true:false;
	}

	//-----------------------------------------------
	public int getInt(int columnIndex) {
		return api.sqlite3_column_int(stmtHandle.getPointer(), columnIndex);
	}

	public long getLong(int columnIndex)  {
		return api.sqlite3_column_int(stmtHandle.getPointer(), columnIndex);
	}

	public double getDouble(int columnIndex) {
		return api.sqlite3_column_int(stmtHandle.getPointer(), columnIndex);
	}

	public String getString(int columnIndex) {
		Pointer p=api.sqlite3_column_text(stmtHandle.getPointer(),columnIndex);
		//int i=api.sqlite3_column_bytes(pstmt,columnIndex);
		return p.getString(0).trim();
	}

	public int getColumnCount() {
		return api.sqlite3_column_count(stmtHandle.getPointer());
	}

	public String getColumnName(int i) {
		Pointer p=api.sqlite3_column_name(stmtHandle.getPointer(), i);
		return p.getString(0);
	}

	//The returned value is one of:
	//	SQLITE_INTEGER 1
	//	SQLITE_FLOAT 2
	//	SQLITE_TEXT 3
	//	SQLITE_BLOB 4
	//	SQLITE_NULL 5
	public int getColumnType(int i) {
		return api.sqlite3_column_type(stmtHandle.getPointer(),i);
	}

	//for use by garbage collector
	protected void finalize() {
		close();
	}
	//============================================
	public interface SQLITE_API extends Library {
		//The SQLITE_BUSY result code indicates that the database file could not be written (or in some cases read)
		//because of concurrent activity by some other database connection, usually a database connection in a
		//separate process.
		public final static int SQLITE_BUSY=5;
		public final static int SQLITE_ROW=         100;  /* sqlite3_step() has another row ready */
		public final static int SQLITE_DONE=        101;  /* sqlite3_step() has finished executing */

		//sqlite3_prepare_v2
		//SQLITE_API int SQLITE_STDCALL sqlite3_prepare_v2(
		//  sqlite3 *db,            /* Database handle */
		//  const char *zSql,       /* SQL statement, UTF-8 encoded */
		//  int nByte,              /* Maximum length of zSql in bytes. */
		//  sqlite3_stmt **ppStmt,  /* OUT: Statement handle */
		//  const char **pzTail     /* OUT: Pointer to unused portion of zSql */
		//);
		//create an sqlite3_stmt
		public int sqlite3_prepare_v2(
			Pointer psqlite3,
			byte[] sql,
			int nByte,
			PointerByReference ppStmt,
			PointerByReference pzTail
		);

		//int sqlite3_finalize(sqlite3_stmt *pStmt);
		public int sqlite3_finalize(Pointer pStmt);

		public int sqlite3_step(Pointer pStmt);

		public int sqlite3_stmt_busy(Pointer psqlite3_stmt);

		//int sqlite3_column_int(sqlite3_stmt*, int iCol);
		public int sqlite3_column_int(Pointer pstmt, int iCol);

		public long sqlite3_column_int64(Pointer sqlite3_stmt, int iCol);

		public double sqlite3_column_double(Pointer sqlite3_stmt, int iCol);

		//const unsigned char *sqlite3_column_text(sqlite3_stmt*, int iCol);
		//return a pointer to the text
		public Pointer sqlite3_column_text(Pointer pstmt, int iCol);

		//get the column_type
		//int sqlite3_column_type(sqlite3_stmt*, int iCol);
		public int sqlite3_column_type(Pointer pstmt, int iCol);

		public int sqlite3_column_count(Pointer pStmt);

		public Pointer sqlite3_column_name(Pointer pstmt, int N);
	}
}