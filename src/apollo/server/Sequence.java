package apollo.server;
import java.util.concurrent.atomic.AtomicInteger;
import apollo.iface.*;

/**
* The Sequence class manages sequence numbers from the Database.  There is only 1
* sequence per JVM, so this is implemented as a Singleton.
* I changed it to use base 8, as a personal preference.
*
* This is called _sequence because it is not a user table.  The name of the sequence is _key.
*/

public class Sequence {
	public final static int BASE8=8; //for emphasis
	private static Sequence seq;

	/**
	* initialize the Singleton Object.  This is only called once upon startup.
	* It is run inside a separate transaction.
	*/
	public synchronized static void init(Connection conn) throws DataStoreException {
		//if already initialized, return
		if (seq!=null) {return;}

		if (!sequenceTableExists(conn)) {
			//this should only be occur once, ever
			createSequenceTable(conn);
			int nid=1;
			String v=Integer.toString(nid,BASE8);
			insertKey(conn,nid,v);
			//create the sequence object
			seq=new Sequence(nid);
		} else {
			int nid=selectNextId(conn);
			if (nid<1) {
				//something is wrong, throw an exception
				throw new DataStoreException("invalid sequence :"+nid,0);
			}
			seq=new Sequence(nid);
		} //end if

	}

	private synchronized static boolean sequenceTableExists(Connection conn) throws DataStoreException {
		String sql="SELECT name FROM sqlite_master WHERE type = 'table' AND name = '_sequence'";
		//System.out.println(sql);
		boolean exists=false;
		Statement stmt = new Statement(conn,sql);
		exists= stmt.step();
		stmt.close();
		return exists;
	}

	private synchronized static void createSequenceTable(Connection conn) throws DataStoreException {
		//create sequence table
		String sql="CREATE TABLE IF NOT EXISTS _sequence (";
		sql+="rowid INTEGER PRIMARY KEY, name TEXT, nextid INT, nextval TEXT)";
		//System.out.println(sql);

		conn.exec(sql);
		//should this have an index on it?
	}

	private synchronized static void insertKey(Connection conn,int id,String val) throws DataStoreException {
		//insert the key sequence
		String sql="INSERT INTO _sequence (name,nextid,nextval) VALUES ('_key',"+id+",'"+val+"')";
		//System.out.println(sql);

		conn.exec(sql);
	}

	/**
	* return the nextid from the row.  If not found return -1
	*/
	private synchronized static int selectNextId(Connection conn) throws DataStoreException {
		String sql="SELECT nextid FROM _sequence WHERE name = '_key'";
		//System.out.println(sql);

		int nid=-1;
		Statement stmt = new Statement(conn,sql);
		boolean b=stmt.step();
		if (b) {
			nid=stmt.getInt(0);
		}
		stmt.close();
		return nid;
	}

	//init must have been called before this
	public static Sequence getInstance() {return seq;}
	//=====================================================
	//instance methods
	private AtomicInteger nextId;
	private String nextVal;

	private Sequence(int i) {
		nextId=new AtomicInteger(i);
		nextVal=Integer.toString(i,BASE8);
	}

	/**
	* Returns the nextVal as a string
	*
	* This method is heavily called, which is everytime an object is inserted
	* into the database.
	*
	* This never retrieves anything from the database, only updates it
	*/
	public synchronized String nextKey(Connection conn) throws DataStoreException {
		String key=nextVal;

		//now get ready for the next call
		nextId.incrementAndGet();
		//set nextVal to the value of the string in Base
		nextVal=Integer.toString(nextId.intValue(),BASE8);
		//store the nextkey in the database
		updateKey(conn,nextId.intValue(),nextVal);

		return key;
	}

	//note that we are already inside a transaction at this point so don't start a new one
	private synchronized void updateKey(Connection conn,int next_id,String next_key) throws DataStoreException {
		String updateSql="UPDATE _sequence SET nextid="+next_id+",nextval='"+next_key+"' WHERE name = '_key'";
		conn.exec(updateSql);
	}
}