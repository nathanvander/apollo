package apollo.server;
import apollo.iface.DataStoreException;

/**
* We need another column in the sqlite_master table for class_name.  However, that table can't be extended.
* So this creates another system table to handle it.
*
* The sqlite_master table has the following columns:
*	type TEXT,
*	name TEXT,
*	tbl_name TEXT,
*	rootpage INTEGER,
*	sql TEXT
*
* We will create a table called _master, with the following columns
*	type TEXT
*	name TEXT
*	class_name TEXT
*/
public class MasterClass {

	//this will automatically create a rowid column, but that's ok
	public static String createMasterTableSql() {
		String sql="CREATE TABLE IF NOT EXISTS _master (";
		sql+="type TEXT NOT NULL, name TEXT NOT NULL, class_name TEXT NOT NULL)";
		return sql;
	}

	//used when you create a table
	public static String insertSql(String tableName,String className) {
		String sql="INSERT INTO _master (type,name,class_name) VALUES ('table','"+tableName+"','"+className+"')";
		return sql;
	}

	//does it exist already
	public static String selectCountBeforeInsertSql(String tableName) {
		String sql="SELECT COUNT(*) FROM _master WHERE name ='"+tableName+"'";
		return sql;
	}

	//used when you drop a table
	public static String deleteSql(String tableName) {
		String sql="DELETE FROM _master WHERE name='"+tableName+"'";
		return sql;
	}

	//and now get the class
	public static String getClassName(Connection con,String tableName) throws DataStoreException {
		String sql="SELECT class_name FROM _master WHERE name='"+tableName+"'";
		Statement st=new Statement(con,sql);
		String className=null;
		if (st.step()) {
			className=st.getString(0);
		}
		st.close();
		return className;
	}
}