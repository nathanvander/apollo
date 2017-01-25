package apollo.kernel;
import apollo.util.Credentials;
import apollo.iface.ConnectionHandle;
import apollo.iface.Unauthorized;
import apollo.iface.DataStoreException;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.ptr.PointerByReference;
import java.math.BigInteger;
import java.util.Random;
import java.util.Hashtable;

/**
* Kernel is the heart of the system.  This is used to get a ConnectionHandle.
* This repeats some code used elsewhere.  This has the security built-in.
*
* The rule is that compliant code must use this to get a connection handle
*/
public class Kernel {
	static Kernel k;
	String filename;
	String public_key;

	//this is a cache of username to password to speed up validation
	Hashtable cred=new Hashtable();

	/** Return the singleton object.
	*/
	public static Kernel instance() throws DataStoreException {
		if (k==null) {
			k=new Kernel();
		}
		return k;
	}

	//used internally to wrap a statement pointer
	static class StatementHandle extends PointerType {
		StatementHandle(Pointer p) {
			super(p);
		}
	}


	//adds a null byte to the end of the string
	public static byte[] getByteArray(String str) {
		byte[] stringBytes=null;
		try {stringBytes=str.getBytes("ISO-8859-1");} catch (Exception x) {x.printStackTrace();}
		byte[] ntBytes=new byte[stringBytes.length+1];
		System.arraycopy(stringBytes, 0, ntBytes, 0, stringBytes.length);
		return ntBytes;
	}

	public Kernel() throws DataStoreException {
		//hard-code the filename in
		//for better or worse, this isn't a generic database anymore
		filename="apollo.sqlite";

        init();
	}

	//==========================
	//PART 1 - set up database if it doesn't exist
	private void init() throws DataStoreException {
		ConnectionHandle conn=open(filename);
		exec(conn,"BEGIN IMMEDIATE TRANSACTION");

		//create _system and _user
		String sql1="CREATE TABLE IF NOT EXISTS _system (rowid INTEGER PRIMARY KEY,property TEXT, value TEXT)";
		exec(conn,sql1);

		//this has 3 extra rows for firstname,lastname and email which are not used
		String sql2="CREATE TABLE IF NOT EXISTS _user (rowid INTEGER PRIMARY KEY, username TEXT, firstname TEXT, lastname TEXT, email TEXT, password TEXT)";
		exec(conn,sql2);

		//see if we need to do security
		String sql3="SELECT value FROM _system WHERE property ='public_key'";
		StatementHandle stmt=prepare(conn,sql3);
		if (step(stmt)) {
			//public_key is an object field
			public_key=getString(stmt,0);
		}
		finalize_statement(stmt);

		if (public_key==null) {
			//generate public_key
			BigInteger P = BigInteger.probablePrime(16,new Random());
			BigInteger Q = BigInteger.probablePrime(17,new Random());
			//this would be very easy to factor
			//I am just trying to have "some" security, not "good" security
			BigInteger PQ = P.multiply(Q);
			public_key=PQ.toString(16);
			//we don't calculate private_key because we don't need it
			//but we have the information to do so
			//store them
			String sql4="INSERT INTO _system (property, value) VALUES ('p_prime','"+P.toString(16)+"')";
			exec(conn,sql4);
			String sql5="INSERT INTO _system (property, value) VALUES ('q_prime','"+Q.toString(16)+"')";
			exec(conn,sql5);
			String sql6="INSERT INTO _system (property, value) VALUES ('public_key','"+public_key+"')";
			exec(conn,sql6);
		}

		//now see if we need to add a root user
		String sql7="SELECT username FROM _user WHERE username='root'";
		StatementHandle stmt2=prepare(conn,sql7);
		String root=null;
		if (step(stmt2)) {
			root=getString(stmt2,0);
		}
		finalize_statement(stmt2);

		if (root==null) {
			//we need to add it
			//the default password is 1234. the admin needs to change it right away
			Credentials croot=Credentials.encrypt(public_key,"root",null,1234);
			String sql8="INSERT INTO _user (username, password) VALUES ('root','"+croot.password+"')";
			exec(conn,sql8);
		}
		exec(conn,"COMMIT");
		close(conn);
	}

	//==========================
	//PART 2 - public interface methods
	//this is the public interface. I'm not going to make it into
	//a formal interface unless it gets bigger

	public String getPublicKey() {
		//public_key was either loaded or calculated during init, just return it
		return public_key;
	}

	public String getDatabaseFileName() {
		return filename;
	}

	//I don't know if this is needed but it will clear the credentials
	//cache, forcing a lookup from the database
	public void clearCache() {
		cred.clear();
	}

	/**
	* This will return false if the Credentials are invalid,
	* it won't throw an exception.
	*/
	public boolean validate(Credentials c) throws DataStoreException {
		//first check if it is null
		if (c==null) {
			return false;
		}

		//next, try to find it in the cache
		String pw=(String)cred.get(c.username);
		if (pw!=null) {
			if (c.password.equals(pw)) {
				return true;
			} else {
				//wrong password
				return false;
			}
		}

		//ok, it isn't in the cache.  try to find it in the database
		//maybe do some checking to see if there are multiple attempts to guess
		boolean valid=false;
		String sql="SELECT username,password FROM _user WHERE username='"+c.username+"' AND password='"+c.password+"'";
		ConnectionHandle conn=open(filename);
		StatementHandle stmt=prepare(conn,sql);
		boolean success=false;
		if (step(stmt)) {
			valid=true;
			//it matches
			//cache it
			cred.put(c.username,c.password);
		}
		//close the stmt
		finalize_statement(stmt);
		//close the connection
		close(conn);

		return valid;
	}

	public boolean validateRoot(Credentials root) throws DataStoreException {
		if (root==null) {return false;}
		if (!root.username.equals("root")) {
			return false; //failure
		} else {
			//validate admin
			return validate(root);
		}
	}


	public ConnectionHandle login(Credentials c) throws DataStoreException, Unauthorized {
		if (validate(c)) {
			return open(filename);
		} else {
			throw new Unauthorized("wrong password");
		}
	}

	//this doesn't check for duplicate entries
	public boolean addUser(Credentials admin,Credentials user) throws DataStoreException {
		if (!validateRoot(admin)) {
			//you have to be root to do this
			return false;
		}
		String sql="INSERT INTO _user (username, password) VALUES ('"+user.username+"','"+user.password+"')";
		ConnectionHandle conn=open(filename);
		exec(conn,sql);
		close(conn);

		return true;
	}

	public boolean changePassword(Credentials old,Credentials nu) throws DataStoreException {
		if (!validate(old)) {
			return false;
		}
		if (!old.username.equals(nu.username)) {
			return false;
		}
		String sql="UPDATE _user SET PASSWORD='"+nu.password+"' WHERE username='"+nu.username+"'";
		ConnectionHandle conn=open(filename);
		exec(conn,sql);
		//see the number of rows affected
		int changes=getChanges(conn);
		close(conn);

		//also update cache
		if (changes==0) {
			//this shouldn't happen, but since we checked for it
			throw new DataStoreException("internal error - the password was not actually changed",0);
		} else {
			cred.put(nu.username,nu.password);
			return true;
		}
	}

	//obviously this has two credentials, one for the admin and one for the user
	public boolean changePasswordForUser(Credentials admin, Credentials user) throws DataStoreException {
		if (!validate(admin)) {
			return false;
		}
		//this is more a preventative measure, so the admin doesn't accidently
		//change his own password
		if (admin.username.equals(user.username)) {
			System.out.println("use changePassword to changeAdmin password");
		}

		String sql="UPDATE _user SET PASSWORD='"+user.password+"' WHERE username='"+user.username+"'";
		ConnectionHandle conn=open(filename);
		exec(conn,sql);
		close(conn);

		//update cache.  There is a minuscule change the update didn't actually occur
		//see changePassword() but I am not going to worry about it
		cred.put(user.username,user.password);

		return true;
	}

	public String[] listUsers(Credentials admin) throws DataStoreException {
		if (!validate(admin)) {
			return null;
		}

		String sql="SELECT COUNT(*) FROM _user";
		ConnectionHandle conn=open(filename);
		StatementHandle stmt=prepare(conn,sql);
		int rows=0;
		if (step(stmt)) {
			rows=getInt(stmt,0);
		}
		finalize_statement(stmt);

		//now we know how many there are
		String[] users=new String[rows];
		String sql2="SELECT username FROM _user";
		StatementHandle stmt2=prepare(conn,sql2);
		int i=0;
		while (step(stmt2)) {
			String username=getString(stmt2,0);
			users[i]=username;
			i++;
		}

		return users;
	}

	//this doesn't have a way to delete or disable a user, but you can just change the
	//password

	//===============================================================
	//part 3 - sqlite interface
	//this duplicates some code elsewhere.  I include it here to make the kernel
	//self-sufficient.
	//
	static SQLITE_API api;
	//class initializer
	static {
		api=(SQLITE_API)Native.loadLibrary("sqlite3",SQLITE_API.class,W32APIOptions.DEFAULT_OPTIONS);
	}
	//we need enough the api exposed to do a select and insert
	interface SQLITE_API extends Library {
		final static int SQLITE_ROW=         100;  /* sqlite3_step() has another row ready */
		final static int SQLITE_DONE=        101;  /* sqlite3_step() has finished executing */

		//int sqlite3_open(
		//  const char *filename,   /* Database filename (UTF-8) */
		//  sqlite3 **ppDb          /* OUT: SQLite db handle */
		int sqlite3_open(byte[] filename,PointerByReference ppDb);

		//int sqlite3_close_v2(sqlite3*);
		int sqlite3_close_v2(Pointer pSqlite3);

		//The sqlite3_exec() interface is a convenience wrapper around sqlite3_prepare_v2(), sqlite3_step(),
		//and sqlite3_finalize(), that allows an application to run multiple statements of SQL without having
		//to use a lot of C code.
		int sqlite3_exec(
  			Pointer psqlite3,                               /* An open database */
  			byte[] sql, 									/* SQL to be evaluated */
  			Object onull,									/* Callback function */
   			Object onull2,
   			PointerByReference errmsg
		);

		//int sqlite3_prepare_v2(
		//  sqlite3 *db,            /* Database handle */
		//  const char *zSql,       /* SQL statement, UTF-8 encoded */
		//  int nByte,              /* Maximum length of zSql in bytes. */
		//  sqlite3_stmt **ppStmt,  /* OUT: Statement handle */
		//  const char **pzTail     /* OUT: Pointer to unused portion of zSql */
		int sqlite3_prepare_v2(
			Pointer psqlite3,
			byte[] sql,
			int nByte,
			PointerByReference ppStmt,
			PointerByReference pzTail
		);

		int sqlite3_changes(Pointer psqlite3);

		//int sqlite3_step(sqlite3_stmt*);
		int sqlite3_step(Pointer pStmt);

		//int sqlite3_finalize(sqlite3_stmt *pStmt);
		int sqlite3_finalize(Pointer pStmt);

		int sqlite3_column_int(Pointer pstmt, int iCol);
		Pointer sqlite3_column_text(Pointer pstmt, int iCol);
	}
	//-------------------------------------------------

	//open a connection handle
	static ConnectionHandle open(String filename) throws DataStoreException {
		byte[] fn=getByteArray(filename);
		PointerByReference ppDb=new PointerByReference();
		int rc=api.sqlite3_open(fn,ppDb);
		if (rc!=0) {
			throw new DataStoreException("unable to open database file: "+filename, rc);
		} else {
			Pointer pdb=ppDb.getValue();
			return new ConnectionHandle(pdb);
		}
	}

	static void close(ConnectionHandle ch) {
		int rc=api.sqlite3_close_v2(ch.getPointer());
		if (rc!=0) System.out.println("Warning: error "+rc+" when closing connection");
	}

	/** This is the logical equivalent of
	* prepare, step, close
	* This still uses the Statement object under the hood in the C code.
	*/
	static void exec(ConnectionHandle ch,String sql) throws DataStoreException {
		PointerByReference pbr=new PointerByReference();
		byte[] basql=getByteArray(sql);
		int rc=api.sqlite3_exec(ch.getPointer(),basql,null,null,pbr);
		if (rc!=0) {
			//commenting out because we dont need it
			//Pointer perr=pbr.getValue();
			//String err=perr.getString(0);
			throw new DataStoreException("error in "+sql, rc);
		}
	}

	//return the number of rows affected by the last update or delete statement
	static int getChanges(ConnectionHandle ch) {
		return api.sqlite3_changes(ch.getPointer());
	}

	//prepare a statement
	static StatementHandle prepare(ConnectionHandle ch,String sql) throws DataStoreException {
		PointerByReference ppStmt=new PointerByReference();
		PointerByReference pzTail=new PointerByReference();
		byte[] basql=getByteArray(sql);

		int rc=api.sqlite3_prepare_v2(
			ch.getPointer(),
			basql,
			basql.length,
			ppStmt,
			pzTail
		);

		if (rc==0) {
			Pointer pstmt=ppStmt.getValue();
			return new StatementHandle(pstmt);
		} else {
			throw new DataStoreException("error preparing statement with "+sql, rc);
		}
	}

	static void finalize_statement(StatementHandle stmtHandle) {
		int rc=api.sqlite3_finalize(stmtHandle.getPointer());
		if (rc!=0) System.out.println("Warning: error "+rc+" when finalizing statement");
	}

	/** This is notorious for throwing SQLITE_BUSY errors
	*/
	static boolean step(StatementHandle stmtHandle) throws DataStoreException {
		int rc=api.sqlite3_step(stmtHandle.getPointer());
		if (rc==api.SQLITE_ROW) {return true;}
		else if (rc==api.SQLITE_DONE) {return false;}
		else {
			throw new DataStoreException("error in step()", rc);
		}
	}

	//this doesn't have the full set of get() methods because we don't need them
	//this is just a stripped down version for the Kernel
	static int getInt(StatementHandle stmtHandle,int columnIndex) {
		return api.sqlite3_column_int(stmtHandle.getPointer(), columnIndex);
	}

	static String getString(StatementHandle stmtHandle,int columnIndex) {
		Pointer p=api.sqlite3_column_text(stmtHandle.getPointer(),columnIndex);
		//int i=api.sqlite3_column_bytes(pstmt,columnIndex);
		if (p==null) {
			return null;
			//literally a null pointer
		} else {
			String s=p.getString(0);
			if (s==null) {return null;} else {return s.trim();}
		}
	}
}