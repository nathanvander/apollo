package apollo.server;
import com.sun.jna.Pointer;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.PointerType;
import com.sun.jna.Memory;
import com.sun.jna.Callback;
import apollo.iface.ConnectionHandle;
import apollo.util.Credentials;
import apollo.kernel.Kernel;

/**
** ^The sqlite3_update_hook() interface registers a callback function
** with the [database connection] identified by the first argument
** to be invoked whenever a row is updated, inserted or deleted in
** a [rowid table].
*
*/

public class UpdateHook {
	public final static int SQLITE_DELETE = 9;
	public final static int SQLITE_INSERT = 18;
	public final static int SQLITE_UPDATE = 23;

	//this assumes that the string is ascii only
	//the pointer will be immutable like the String constant
	public static Pointer pointerFromString(String myString) {
		if (myString==null) {return null;}
		Pointer m = new Memory(myString.length() + 1); // WARNING: assumes ascii-only string
		m.setString(0, myString);
		return m;
	}

	public static String stringFromPointer(Pointer p) {
		if (p==null) {return null;}
		else {
			return p.getString(0);
		}
	}


	public interface FunctionCallback extends Callback {
		/**
		* The first argument is a pointer to a String.  To get the String,
		* call the stringFromPointer.
		*/
		public void callback(Pointer userData, int type, String dbname, String tbl_name,long rowid);
	}

	public static SQLITE_API api;
	static {
		api=(SQLITE_API)Native.loadLibrary("sqlite3",SQLITE_API.class,W32APIOptions.DEFAULT_OPTIONS);
	}

	/**
	* This is how to use it.  This creates the hook
	*/
	public static String hook(ConnectionHandle ch,FunctionCallback fcb, String userData) {
		Pointer u=pointerFromString(userData);
		Pointer o=api.sqlite3_update_hook(ch.getPointer(),fcb,u);
		return stringFromPointer(o);
	}

	//------------------------------------------------------
	public interface SQLITE_API extends Library {
		/**
		* SQLITE_API void *sqlite3_update_hook(
		*  sqlite3*,
		*  void(*)(void *,int ,char const *,char const *,sqlite3_int64),
		*  void*
		* );
		*
		* The return value, if any, is a pointer to the previous udp.
		* I assume the udp will be a String.  To get the pointer to a String, do
		*/
		public Pointer sqlite3_update_hook(Pointer psqlite3,FunctionCallback f,Pointer udp);

	}

	//======================================
	//this is test code from here on down
	static class TestCallback implements FunctionCallback {
		//this is called from C code, into Java!
		public void callback(Pointer userData, int type, String dbname, String tbl_name,long rowid) {
			System.out.println("alert. type of change = "+type+" on table " +tbl_name+", row="+rowid);
		}
	}

	public static void main(String[] args) throws Exception {
		String pk=Kernel.instance().getPublicKey();
		Credentials root=Credentials.encrypt(pk,"root",null,1234);

		Connection c=new Connection(root);
		//create a table
		c.exec("CREATE TEMP TABLE test(rowid INTEGER PRIMARY KEY, word TEXT)");
		//create a callback function
		TestCallback f=new TestCallback();
		//hook it.  I don't think this code is right
		hook(c.getHandle(),f, null);

		//now insert some data into it
		c.exec("INSERT INTO test (word) VALUES('hello')");
		//i guess the callback function should fire
		c.close();
	}

}

