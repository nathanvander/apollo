package apollo.iface;

/**
* These are mostly SQLite error codes
*
* Compare to java.sql.SQLException
*/
public class DataStoreException extends Exception implements java.io.Serializable {
	int errcode;

	//this has errorcode 2nd because Exception has message in the first parameter
	public DataStoreException(String msg,int errcode) {
			super(msg);
			this.errcode=errcode;
	}

	public int getErrCode() {
		return errcode;
	}

	public String toString() {
		return getMessage()+" (error code: "+errcode+")";
}

}