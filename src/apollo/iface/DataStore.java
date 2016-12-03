package apollo.iface;
import java.rmi.*;

/**
* A DataStore is a simplified view of a Database
*/
public interface DataStore extends Remote {
	/**
	* Get the version of the underlying SQLite library.
	*/
	public int getLibVersionNumber() throws RemoteException;

	/**
	* Returns the name of the DB file on the server.
	*/
	public String getDatabaseFileName() throws RemoteException;

	/**
	* Connect to the Database and create a Transaction.  You can change multiple things at once in the transaction.
	*/
	public Transaction createTransaction() throws RemoteException,DataStoreException;

	/**
	* list all tables in the database and return a String array
	* The top level interface will hold a Connection to do the listTables and get methods.
	* It will hold the database only 5 minutes and then reopen it so the connection doesn't get stale.
	*
	* If the list is empty, this will return an emptry String array, not null
	*/
	public String[] listTables() throws RemoteException,DataStoreException;

	/**
	* Get an object by the Key
	*/
	public DataObject get(Key k) throws RemoteException,DataStoreException;

	/**
	* This has limited capabilities on purpose.  It returns every DataObject in the database
	* in the default sort order.  Of course, more options will be needed, but
	* this will work for simple applications.
	* This uses its own Connection and Statement
	*/
	public Cursor selectAll(String tableName,int limit,int offset) throws RemoteException,
		DataStoreException;

	/**
	* Return the data specified by the view.
	*/
	public Cursor view(ViewObject v) throws RemoteException, DataStoreException;

}