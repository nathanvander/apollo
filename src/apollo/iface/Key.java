package apollo.iface;

/** The IDs are unique within the database, but we need the tableName to know where to
* get it.
*/
public class Key implements java.io.Serializable {
	public String tableName;
	public String id;
	//constructor for ease of use
	public Key(String tn,String k) {
		tableName=tn;
		id=k;
	}
}