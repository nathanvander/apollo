package apollo.iface;
import java.rmi.*;

/**
* A DataStore is a simplified view of a Database.
*
* VERSION added as of 1/12/17.  This will not change for minor bug fixes.  It will
* be incremented for any changes to interfaces or functionality which code relies
* on this should be aware of.
*
* Recent which require incrementing this are:
*	- changing internal sequence numbering to base 8.  This is for personal, artistic
*	reasons - base 8 is almost as human-friendly as base 10, but it is more useful to
*   the computer.
*  - change to DataObject interface to require displayNames() on fields.  This is so
* 	dynamic dialogs can be created.
*  - new types are allowed on DataObject fields.  See the notes there.
*/
public interface DataStore extends Remote {
	//this is the version
	public final static float VERSION = 1.10F;

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
	* Return the number of rows in the specified table
	*/
	public int rows(String tableName) throws RemoteException,DataStoreException;


	public Cursor selectAll(DataObject d) throws RemoteException,
		DataStoreException;
	/**
	* This has limited capabilities on purpose.  It returns every DataObject in the database
	* in the default sort order.  Of course, more options will be needed, but
	* this will work for simple applications.
	* This uses its own Connection and Statement
	*/
	public Cursor selectAll(DataObject d,int limit,int offset) throws RemoteException,
		DataStoreException;

	/**
	* Return the data specified by the view.  The view object must be created first
	*/
	public Cursor view(ViewObject v) throws RemoteException, DataStoreException;

	/**
	*  Select all objects from the table matching the whereClause.
	*  The where clause should also specify the order and the limit, if any
	*/
	public Cursor selectWhere(DataObject d,String whereClause) throws RemoteException, DataStoreException;

	/**
	* Get a sequence number from the database.  The standard one is "_key".  Use this if you need
	* a database generated sequence before an insert is made.  The id is in base-8.
	*/
	public String nextId(String seqName) throws RemoteException, DataStoreException;


}