package apollo.iface;

/**
* Any DataObject can be stored in the DataStore.  Since this is an interface, it will be
* easy to add this to any object.
*
* Note that the first two fields must be:
*	long ROWID (or OID).  This is the primary key, and is unique only in the table.
*   String key.  This is the apollo assigned key which is unique among all objects stored
*
* The only field types allowed are the SQLite types:
*	Text: String
*	integer: int or long
*	real: float or double
* Also booleans are allowed and represented as 0s and 1s.
* Blobs are not used.
*/
public interface DataObject extends java.io.Serializable {
	/**
	* return the name of the table.  It is usually the same name as the object
	* but there may be a reason to have a different name, like to get rid of the dots.
	*/
	public String getTableName();

	/**
	* get a list of all the fields in the object in order.  Reflection is used to get the
	* information, but we need to have an order.
	* Note that if a field in the object is not listed here, it won't be stored.
	* The first two columns (rowid, key) are understood to be on this list.  You don't need
	* to list them but it won't hurt to do so.
	*/
	public String[] fields();

	/**
	* This is the index for the DataObject.  This determines the order that it is returned
	* in.  This is used both to create the index, and in the order by phrase when doing a select.
	*
	* You can specify multiple fields in the index, like "lastname,firstname"
	*/
	public String index();

	/**
	* Return the key of this object.  This is its unique id within the database.  Will be null if it is being inserted.
	*/
	public String getKey();
}