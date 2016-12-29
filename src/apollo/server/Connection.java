package apollo.server;
import com.sun.jna.Pointer;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.PointerType;
import java.util.concurrent.atomic.AtomicInteger;
import apollo.iface.DataStoreException;
import java.io.Serializable;

public class Connection {
	//class initializer
	static {
		api=(SQLITE_API)Native.loadLibrary("sqlite3",SQLITE_API.class,W32APIOptions.DEFAULT_OPTIONS);
	}


	public static class Handle extends PointerType implements Serializable {
		public Handle(Pointer p) {
			super(p);
		}
	}

	public static SQLITE_API api;
	private static AtomicInteger counter=new AtomicInteger();
	private int id;
	private Handle handle;
	private boolean closed=false;
	private long time_created;

	//=========================================
	//utility function
	//this adds a null byte to the end, the way C likes it
	//this could be moved to a different class.  used by other classes
	public static byte[] getByteArray(String str) {
		byte[] stringBytes=null;
		try {stringBytes=str.getBytes("ISO-8859-1");} catch (Exception x) {x.printStackTrace();}
		byte[] ntBytes=new byte[stringBytes.length+1];
		System.arraycopy(stringBytes, 0, ntBytes, 0, stringBytes.length);
		return ntBytes;
	}
	//==============================================

	public Connection(String filename) throws DataStoreException {
		handle=open(filename);
		id=counter.incrementAndGet();	//equivalent of ++counter;
		time_created=System.currentTimeMillis();
		System.out.println("connection #"+id+" created in thread "+Thread.currentThread().getId());
	}

	protected Handle getHandle() {return handle;}

	//returns a pointer to the connection object
	private static Handle open(String filename) throws DataStoreException {
		byte[] fn=getByteArray(filename);
		PointerByReference ppDb=new PointerByReference();
		int flags= api.SQLITE_OPEN_READWRITE | api.SQLITE_OPEN_CREATE | api.SQLITE_OPEN_NOMUTEX;
		int rc=api.sqlite3_open_v2(fn,ppDb,flags,null);
		if (rc!=0) {
			throw new DataStoreException("unable to open database file: "+filename,rc);
		} else {
			Pointer pdb=ppDb.getValue();
			if (pdb==null) {
				//probably won't happen
				throw new DataStoreException("pointer to connection is null",api.SQLITE_CANTOPEN);
			}
			return new Handle(pdb);
		}
	}

	public int getId() {return id;}

	public long getTimeCreated() {return time_created;}

	/**
	* Used for all sql commands except for SELECT, including DDL statements, inserts, updates and deletes.
	* Returns the number of rows affected.
	*
	* This should be called within a transaction
	*/
	public int exec(String sql) throws DataStoreException {
		System.out.println(sql);
		PointerByReference pbr=new PointerByReference();
		byte[] basql=getByteArray(sql);
		int rc=api.sqlite3_exec(handle.getPointer(),basql,null,null,pbr);
		if (rc==0) {
			//System.out.println("success");
			return getChanges();
		} else {
			Pointer perr=pbr.getValue();
			String err=perr.getString(0);
			System.out.println("error in "+sql);
			//System.out.println("error code "+rc);
			throw new DataStoreException(err+" with "+sql,rc);
		}
	}

	//returns the number of rows modified
	public int getChanges() {
		return api.sqlite3_changes(handle.getPointer());
	}

	public long lastInsertRowID() {
		return api.sqlite3_last_insert_rowid(handle.getPointer());
	}

	public boolean isClosed() {
		return closed;
	}

	public void close() {
		int rc=api.sqlite3_close_v2(handle.getPointer());
		closed=true;
		if (rc!=0) {
			System.out.println("warning: error "+rc+" on close()");
		}
		System.out.println("connection #"+id+" closed");
	}

	public static int libversion_number() {
		return api.sqlite3_libversion_number();
	}

	//for use by garbage collector
	protected void finalize() {
		close();
	}
	//===============================================================
	public interface SQLITE_API extends Library {
		//flags for open
		public final static int SQLITE_OPEN_READONLY=         0x00000001;  /* Ok for sqlite3_open_v2() */
		public final static int SQLITE_OPEN_READWRITE=        0x00000002;  /* Ok for sqlite3_open_v2() */
		public final static int SQLITE_OPEN_CREATE=           0x00000004;  /* Ok for sqlite3_open_v2() */
		public final static int SQLITE_OPEN_URI=              0x00000040;  /* Ok for sqlite3_open_v2() */
		public final static int SQLITE_OPEN_MEMORY=           0x00000080;  /* Ok for sqlite3_open_v2() */
		public final static int SQLITE_OPEN_NOMUTEX=          0x00008000;  /* Ok for sqlite3_open_v2() */

		//error message
		public final static int SQLITE_CANTOPEN=14;


		//SQLITE_API int SQLITE_STDCALL sqlite3_open_v2(
	  	//const char *filename,   /* Database filename (UTF-8) */
	  	//sqlite3 **ppDb,         /* OUT: SQLite db handle */
	  	//int flags,              /* Flags */
	  	//const char *zVfs        /* Name of VFS module to use */
		//);
		public int sqlite3_open_v2(byte[] filename,PointerByReference ppDb,int flags,byte[] zVfs);

		//SQLITE_API int SQLITE_STDCALL sqlite3_close(sqlite3*);
		//public int sqlite3_close(Pointer pSqlite3);

		//The sqlite3_close_v2() interface is intended for use with host languages that are garbage collected,
		//and where the order in which destructors are called is arbitrary.
		//SQLITE_API int SQLITE_STDCALL sqlite3_close_v2(sqlite3*);
		public int sqlite3_close_v2(Pointer pSqlite3);

		//The sqlite3_exec() interface is a convenience wrapper around sqlite3_prepare_v2(), sqlite3_step(),
		//and sqlite3_finalize(), that allows an application to run multiple statements of SQL without having
		//to use a lot of C code.
		public int sqlite3_exec(
  			Pointer psqlite3,                               /* An open database */
  			byte[] sql, 									/* SQL to be evaluated */
  			Object onull,									/* Callback function */
   			Object onull2,
   			PointerByReference errmsg
		);

		//This function returns the number of rows modified, inserted or deleted by the most recently
		//completed INSERT, UPDATE or DELETE statement on the database connection
		public int sqlite3_changes(Pointer psqlite3);

		//sqlite3_int64 sqlite3_last_insert_rowid(sqlite3*);
		public long sqlite3_last_insert_rowid(Pointer psqlite3);

		//int sqlite3_libversion_number(void);
		public int sqlite3_libversion_number();
	}

}