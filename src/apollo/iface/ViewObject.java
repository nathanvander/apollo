package apollo.iface;

/**
* A ViewObject is a read-only structure of data.  This is called ViewObject to make it similar to
* DataObject and because view is a verb.
*
* The only field types allowed are the SQLite types:
*	Text: String
*	integer: int or long
*	real: float or double
* Also booleans are allowed and represented as true and false.
* Blobs are not used.
*
* This extends DataObject, which doesn't make a lot of sense because it doesn't use any of the
* DataObject methods except fields().  However, this simplifies the interface of CursorObject.next()
*/
public interface ViewObject extends DataObject {
	/**
	* return the sql that underlies the View.  It starts with the word "SELECT";
	* The fields in the view must be named with the keyword AS, that matches the field names in the
	* View object.
	*/
	public String getSQL();

	/**
	* This is the name of the view.  Don't use the same name as a table.
	*/
	public String getViewName();

	/**
	* get a list of all the fields in the object in order.  Reflection is used to get the
	* information, but we need to have an order.
	* Note that if a field in the object is not listed here, it won't be stored.
	* The first two columns (rowid, key) are understood to be on this list.  You don't need
	* to list them but it won't hurt to do so.
	*/
	//public String[] fields();
}