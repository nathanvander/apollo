package apollo.iface;

public class Key implements java.io.Serializable {
	public String tableName;
	public long rowid;

	//constructor for ease of use
	public Key(String tn, long k) {
		tableName=tn;
		rowid=k;
	}
}