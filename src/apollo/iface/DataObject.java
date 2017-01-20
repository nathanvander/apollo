package apollo.iface;

/**
* Any DataObject can be stored in the DataStore.  Since this is an interface, it will be
* easy to add this to any object.
*
* Note that the first field must be:
*	long rowid.  This is the primary key.
*
* The field types can be any of the following:
*	String (maps to TEXT)
*	int or long (maps to INTEGER)
*	float or double (maps to REAL)
*	boolean (will be stored as String "true" or "false")
*
*	In addition, field types of the following objects are allowed:
*	java.util.Date (will be stored as value of Date.toString())
*	apollo.util.DateYMD (will be stored as String in value YYYY-MM-DD)
*	apollo.util.DateYM (represents the month, will be stored as YYYY-MM)
*	java.math.BigDecimal (use for money fields, will be stored as value of BigDecimal.toPlainString());
*	java.awt.TextField (same as a String, but marks this as needing a TextArea box.  The value is getText()
*	java.awt.Choice (same as String, but only the choices on the list are allowed.  This is used
*		for enumerated values).
*
*/
public interface DataObject extends java.io.Serializable {

	/**
	* return the name of the table.  It is usually the same name as the object
	* but there may be a reason to have a different name, like to get rid of the dots.
	*/
	public String getTableName();

	/**
	* get a list of all the fields in the object in order.  Reflection is used to get the
	* information, but we need to have an order.  This uses Class.getDeclaredFields()
	* instead of Class.getFields, so it can access fields of the DataObject that are not
	* public, but it can't access parent (inherited) fields.
	*
	* Read this from Class.getDeclaredFields():
	*   The elements in the array returned are not sorted and are not in any particular order.

	* Note that if a field in the object is not listed here, it won't be stored.
	* The first column (rowid) is  understood to be on this list.  You don't need
	* to list it but it won't hurt to do so.
	*/
	public String[] fields();

	/**
	* return a list of user-friendly display names.  For example "First Name", instead of "firstname";
	* this must be the same length as fields()
	*/
	public String[] displayNames();

	/**
	* This is the index for the DataObject.  This determines the order that it is returned
	* in.  This is used both to create the index, and in the order by phrase when doing a select.
	*
	* You can specify multiple fields in the index, like "lastname,firstname"
	*/
	public String index();

	/**
	* Get the row id of the object.
	*/
	public long getID();

}