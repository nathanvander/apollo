package apollo.iface;
import java.rmi.*;

/**
* A Transaction is used to make changes to the database.  This is run in its own connection, so you can have multiple
* transactions at once.
*/
public interface Transaction extends Remote {
	/**
	* Get the transaction Id.  Mostly for testing purposes, but I could see storing it.  This is available right away,
	* before the transaction begins.
	*/
	public long getID() throws RemoteException;

	/**
	* This both opens the connection and begins the transaction
	*/
	public void begin() throws RemoteException, DataStoreException;

	/**
	* This both commits the transaction and closes the connection.
	*/
	public void commit() throws RemoteException, DataStoreException;

	/**
	* This both rollsback the transaction and closes the connection.
	*/
	public void rollback() throws RemoteException, DataStoreException;

	/**
	* Create a table based on the fields in the DataObject.  This is done so that we don't have to check if the
	* table exists every time an insert is done.  It doesn't hurt to call this more than once because
	* it creates the table only if it doesn't exist.
	* This also creates an index on the table.
	*/
	public void createTable(DataObject d) throws RemoteException,DataStoreException;

	/**
	* This will drop the table only if it is empty.  If there are records in it, it will throw an exception
	*/
	public void dropTable(DataObject d) throws RemoteException,DataStoreException;


	public Key insert(DataObject d) throws RemoteException,DataStoreException;

	/**
	* Update the object.  Must provide old state before doing so.
	*/
	public void update(DataObject old,DataObject nu) throws RemoteException,DataStoreException;

	/**
	* Delete the object.  Must provide old state of object before doing so.
	*/
	public void delete(DataObject old)  throws RemoteException,DataStoreException;

	public void createView(ViewObject v) throws RemoteException,DataStoreException;

	public void dropView(ViewObject v) throws RemoteException,DataStoreException;

}