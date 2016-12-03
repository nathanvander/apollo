package apollo.iface;
import java.rmi.*;

/** This is called Cursor, because I like it better than ResultSet. Also, this only
* returns complete DataObjects, not individual fields. It is based on java.util.Iterator,
* but it uses RemoteException.
*
* Compared to java.sql.ResultSet.  This is a little simpler
*/
public interface Cursor extends Remote {
	public boolean hasNext() throws RemoteException;
	public DataObject next() throws RemoteException, DataStoreException;
}