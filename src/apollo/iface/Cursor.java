package apollo.iface;
import java.rmi.*;

/** This is called Cursor, because I like it better than ResultSet. Also, this only
* returns complete DataObjects, not individual fields. It is based on java.util.Iterator,
* but it uses RemoteException.
*
* Compared to java.sql.ResultSet.  This is a little simpler.
*
* You have to call open() to open the cursor
*/
public interface Cursor extends Remote {
	//return the select sql statement that this is running
	public String getSql() throws RemoteException;
	public void open() throws RemoteException, DataStoreException;

	//the hasNext() method actually loads the object, so don't skip it
	public boolean hasNext() throws RemoteException, DataStoreException;

	//this retrieves the next object.  use in confunction with hasNext();
	public DataObject next() throws RemoteException, DataStoreException;

	public void close() throws RemoteException;
}