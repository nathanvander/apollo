package apollo.server;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import apollo.iface.*;
//import java.lang.reflect.Field;
import java.security.Permission;

/**
* This is the main class.  It holds a Connection with a timeout of 5 minutes for use by the listTables() and get() methods.
* createTransaction() and selectAll() have their own connections.
*/
public class DataStoreEngine implements DataStore {
	protected String dbFileName;
	private transient Connection conn;

	//this doesn't throw RemoteException because it can't be accessed remotely
	public DataStoreEngine(String fn) throws DataStoreException {
		dbFileName=fn;
		conn=new Connection(fn);
	}

	public int getLibVersionNumber() throws RemoteException {
		return Connection.libversion_number();
	}

	/**
	* Returns the name of the DB file on the server.
	*/
	public String getDatabaseFileName() throws RemoteException {
		return dbFileName;
	}

	/**
	* Connect to the Database and create a Transaction.  You can change multiple things at once in the transaction.
	*/
	public Transaction createTransaction() throws RemoteException,DataStoreException {
		TransactionObject tx=new TransactionObject(dbFileName);
		//we are not returning the transaction object, just its stub
		Transaction stub =(Transaction)UnicastRemoteObject.exportObject(tx,0);
		return stub;
	}

	//list all tables in the database and return a String array
	//The top level interface will hold a Connection to do the listTables and get methods.
	//It will hold the database only 5 minutes and then reopen it so the connection doesn't get stale
	public String[] listTables() throws RemoteException,DataStoreException {
		//first check the time on the connection
		long now=System.currentTimeMillis();
		//if it is more than 5 minutes old, then recreate it
		if (now - conn.getTimeCreated() > (5 * 60 * 1000)) {
			conn.close();
			conn=new Connection(dbFileName);
		}

		//find number of rows
		int numTables=0;
		String sql="select count(name) from sqlite_master where type='table'";
		Statement stmt=new Statement(conn,sql);
		if (stmt.step()) {
			numTables=stmt.getInt(0);  //look in the first column
		} else {
			//shouldn't happen
			throw new DataStoreException("error in: "+sql,0);
		}
		stmt.close();

		//create the string array
		String[] tables = new String[numTables];
		String sql2="select name from sqlite_master where type='table'";
		Statement stmt2=null;
		if (numTables==0) {
			return tables;
		} else
			//then create a Statement
			stmt2=new Statement(conn,sql2);
			//step through it
			int count=0;
			while (stmt2.step()) {
				//get the table name and add it to the array
				tables[count]=stmt.getString(0);
				count++;
				//it is theoretically possible that this could overrun the array, but we just
				//asked for a count of the tables
			}

			//close the statement, not the connection
			stmt2.close();
			//return result to client
			return tables;
	}

	public DataObject get(Key k) throws RemoteException,DataStoreException {
		return null;
	}

	/**
	* This has limited capabilities on purpose.  It returns every DataObject in the database
	* in the default sort order.  Of course, more options will be needed, but
	* this will work for simple applications.
	* This uses its own Connection and Statement
	*/
	public Cursor selectAll(String tableName,int limit,int offset) throws RemoteException,
		DataStoreException {
			return null;
	}

	/**
	* Return the data specified by the view.
	*/
	public Cursor view(ViewObject v) throws RemoteException, DataStoreException {
		return null;
	}

	//======================================================================
	//start up the Engine and bind it to the registry
    public static void main(String[] args) {
		String filename="vos2.sqlite";  //hard-coded, would be very easy to have it passed in

		//remove all security
		System.setSecurityManager(
			new SecurityManager() {
				public void checkPermission(Permission perm) {}
			}
		);

		//start rmi registry
		try {
			LocateRegistry.createRegistry(1099);
			System.out.println("RMI Registry started on port 1099");
		} catch (Exception x) {
			System.out.println("unable to start RMI Registry");
			//is it already started by another process? if so, this is not fatal
		}

        try {
            String name = "DataStore";
            DataStore ds = new DataStoreEngine(filename);
            DataStore stub =
                (DataStore)UnicastRemoteObject.exportObject(ds, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(name, stub);
            System.out.println("DataStoreEngine bound to RMI Registry as '"+name+"'");
        } catch (Exception e) {
            System.err.println("fatal DatabaseStore exception:");
            e.printStackTrace();
        }
	}

}