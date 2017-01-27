package apollo.server;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.PointerByReference;
import java.nio.ByteBuffer;
import apollo.iface.DataStoreException;
import apollo.iface.ConnectionHandle;


public class Blob {
	public static SQLITE_API api;
	static {
		api=(SQLITE_API)Native.loadLibrary("sqlite3",SQLITE_API.class,W32APIOptions.DEFAULT_OPTIONS);
	}

	public static class Handle extends PointerType {
		public Handle(Pointer p) {
			super(p);
		}
	}

	private Handle blobHandle;

	//db is almost always "main", so I won't pass it in.  If it is different,
	//then make another constructor
	//flags are either 0 for read-only or 1 for read/write
	public Blob(ConnectionHandle ch,String table, String col, long row) throws DataStoreException {
		PointerByReference ppBlob=new PointerByReference();
		int rc = api.sqlite3_blob_open(ch.getPointer(),"main",table,col,row,1,ppBlob);

		if (rc==0) {
			//System.out.println("success");
			Pointer pblob=ppBlob.getValue();
			blobHandle=new Handle(pblob);
		} else {
			throw new DataStoreException("error opening blob", rc);
		}
	}

	public void setRow(long row) throws DataStoreException {
		int rc = api.sqlite3_blob_reopen(blobHandle.getPointer(), row);
		if (rc!=0) {
			throw new DataStoreException("blob error", rc);
		}
	}

	public void close() {
		api.sqlite3_blob_close(blobHandle.getPointer());
	}

	/**
	* Return the size of the bytes in the blob.
	*/
	public int size() {
		return api.sqlite3_blob_bytes(blobHandle.getPointer());
	}

	/**
	* read data into a byte array FROM the blob.
	*	n is the number of bytes to copy
	*	iOffset is the offset within the blob
	*
	* Check the size() before calling this because if iOffset + n > blob.size() then SQLITE_ERROR is thrown
	*
	* If the blob is really big, then you can call this several times.  This could be wrapped into an inputstream.
	*/
	public void read(byte[] buffer, int n, int iOffset) throws DataStoreException {
		if (buffer==null || buffer.length==0) {
			throw new DataStoreException("invalid byte buffer",0);
		}

		//create a byte buffer
		ByteBuffer z=ByteBuffer.wrap(buffer);
		int rc = api.sqlite3_blob_read(blobHandle.getPointer(), z, n, iOffset);
		if (rc!=0) {
			throw new DataStoreException("blob error", rc);
		}
	}

	/**
	* Write data TO the blob from the given byte array
	*/
	public void write(byte[] buffer, int n, int iOffset) throws DataStoreException {
		if (buffer==null || buffer.length==0) {
			throw new DataStoreException("invalid byte buffer",0);
		}

		//create a byte buffer
		ByteBuffer z=ByteBuffer.wrap(buffer);
		int rc = api.sqlite3_blob_write(blobHandle.getPointer(), z, n, iOffset);
		if (rc!=0) {
			throw new DataStoreException("blob error", rc);
		}
	}

	//=============================================
	public interface SQLITE_API extends Library {
		/**
		* This interfaces opens a handle to the BLOB located in row iRow, column zColumn, table zTable in database zDb;
		* in other words, the same BLOB that would be selected by:
		*
		* SELECT zColumn FROM zDb.zTable WHERE rowid = iRow;
		* Parameter zDb is not the filename that contains the database, but rather the symbolic name of the database.
		* For attached databases, this is the name that appears after the AS keyword in the ATTACH statement. For the
		*	main database file, the database name is "main". For TEMP tables, the database name is "temp".
		*
		* If the flags parameter is non-zero, then the BLOB is opened for read and write access. If the flags parameter is zero,
		* the BLOB is opened for read-only access.
		*
		* On success, SQLITE_OK is returned and the new BLOB handle is stored in *ppBlob. Otherwise an error code is returned and,
		* unless the error code is SQLITE_MISUSE, *ppBlob is set to NULL. This means that, provided the API is not misused,
		* it is always safe to call sqlite3_blob_close() on *ppBlob after this function it returns.
		* int sqlite3_blob_open(
		*  sqlite3*,
		*  const char *zDb,
		*  const char *zTable,
		*  const char *zColumn,
		*  sqlite3_int64 iRow,
		*  int flags,
		*  sqlite3_blob **ppBlob
		* );
		*/
		//note, this can be mapped to a string because it is declared "const"
		//if it wasn't, then it should be a byte array
		public int sqlite3_blob_open(
			Pointer psqlite3,
			String zdb,		//byte[] zDb,
			String zTable,	//,byte[] zTable,
			String zColumn,	//byte[] zColumn,
			long row,
			int flags,
			PointerByReference ppBlob
		);

		/**
		* This function is used to move an existing blob handle so that it points to a different row of the same database table.
		* The new row is identified by the rowid value passed as the second argument. Only the row can be changed. The database,
		* table and column on which the blob handle is open remain the same. Moving an existing blob handle to a new row can be
		* faster than closing the existing handle and opening a new one.
		*
		* int sqlite3_blob_reopen(sqlite3_blob *, sqlite3_int64);
		*/
		public int sqlite3_blob_reopen(Pointer pblob, long row);

		/**
		* This function closes an open BLOB handle.
		* int sqlite3_blob_close(sqlite3_blob *);
		*/
		public int sqlite3_blob_close(Pointer pblob);

		/*
		* Returns the size in bytes of the BLOB accessible via the successfully opened BLOB handle in its only argument.
		* int sqlite3_blob_bytes(sqlite3_blob *);
		*/
		public int sqlite3_blob_bytes(Pointer pblob);

		/**
		* This function is used to read data from an open BLOB handle into a caller-supplied buffer.
		* N bytes of data are copied into buffer Z from the open BLOB, starting at offset iOffset.
		*
		* int sqlite3_blob_read(sqlite3_blob *, void *Z, int N, int iOffset);
		*/
		public int sqlite3_blob_read(Pointer pblob, ByteBuffer z, int n, int iOffset);

		/**
		* This function is used to write data into an open BLOB handle from a caller-supplied buffer. N bytes of data are
		* copied from the buffer Z into the open BLOB, starting at offset iOffset.
		*
		* int sqlite3_blob_write(sqlite3_blob *, const void *z, int n, int iOffset);
		*/
		public int sqlite3_blob_write(Pointer pblob, ByteBuffer z, int n, int iOffset);
	}
}