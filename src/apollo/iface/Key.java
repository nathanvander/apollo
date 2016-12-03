package apollo.iface;

/** The IDs are unique within the database, but we need the tableName to know where to
* get it.
*/
public class Key implements java.io.Serializable {
	public String tableName;
	public String id;
}