package apollo.server;
import apollo.iface.DataObject;
import apollo.iface.DataStoreException;
import apollo.util.DynamicSql;

/**
* The audit table records all changes to existing data.  It doesn't record
* a table being created, or data being inserted, but it will record a table being
* dropped, or data being updated or deleted.  To make this generic, it will
* record the undo_sql, and the new_sql.
*
* The undo_sql is the sql command that will undo this operation.
*	for an update, it will be another update
*	for a delete, it will be an insert
*
* This won't be enough information to reverse a transaction, but it will show you
* what has changed.
*/
public class Audit {
	//this class isn't meant to be instantiated.
	//these fields are for planning purposes only
	public long rowid;				//the rowid of this table only
	public java.sql.Timestamp timestamp;	//the timestamp of the audit entry.  May be different from the transaction date/time
	public long oid;				//the object id.  may be zero if it doesn't apply
	public String table;
	public String undo_sql;		//the sql command that will undo this operation, can be null
	public String new_sql;

	public static String getTableName() {return "_audit";}

	//replace all single-quotes with back quotes
	public static String escapeSingleQuote(String s) {
		if (s==null) {return null;}
		else {return s.replaceAll("'","`");}
	}

	//return the sql that will create this audit table
	public static String createTableSql() {
		StringBuffer sql=new StringBuffer("CREATE TABLE IF NOT EXISTS _audit (");
		sql.append("rowid INTEGER PRIMARY KEY,");
		sql.append("timestamp TEXT,");
		sql.append("oid INTEGER,");
		sql.append("table TEXT,");
		sql.append("undo_sql TEXT,");
		sql.append("new_sql TEXT)");
		return sql.toString();
	}

	//record a drop table event
	//there is no good way to undo it, but we definitely want to record the command
	//we expect the DROP command to look like:
	//DROP TABLE IF EXISTS table
	//this can also be used for DROP INDEX, but I really don't care about that
	public static String auditDrop(String table, String dropSql) {
		if (table==null || dropSql==null || !dropSql.startsWith("DROP")) {
			throw new IllegalArgumentException(dropSql);
		}
		StringBuffer sql=new StringBuffer("INSERT INTO _audit (");
		sql.append("timestamp,oid,table,new_sql) ");
		sql.append("VALUES ( datetime(),0,'"+table+"','"+dropSql+"')");
		return sql.toString();
	}

	/**
	* Generate an audit command for an update statement.
	* The oid must be greater than 0.
	* The DataObject old is the data before it was changed.
	* The updateSql is the statement that will change it.  We could generate this
	* but we want to see exactly what the command is.
	*/
	public static String auditUpdate(long oid,String table,DataObject old,String updateSql) throws DataStoreException {
		if (table==null || updateSql==null || !updateSql.startsWith("UPDATE")) {
			throw new IllegalArgumentException(updateSql);
		}
		String oldUpdateSql=DynamicSql.generateUpdateSql(old);
		String oldUpdateSql2=escapeSingleQuote(oldUpdateSql);
		String newUpdateSql2=escapeSingleQuote(updateSql);

		StringBuffer sql=new StringBuffer("INSERT INTO _audit (");
		sql.append("timestamp,oid,table,undo_sql,new_sql) ");
		sql.append("VALUES ( datetime(),"+oid+",'"+table+"','"+oldUpdateSql2+"','"+newUpdateSql2+"')");
		return sql.toString();
	}

	/**
	* Used for delete commands.  Don't do delete all, do it one row at a time.
	* This stores the sql command that would do an insert of the old state
	* as well as the command used to delete.
	*/
	public static String auditDelete(long oid,String table,DataObject old,String deleteSql) throws DataStoreException {
		if (table==null || deleteSql==null || !deleteSql.startsWith("DELETE")) {
			throw new IllegalArgumentException(deleteSql);
		}

		String oldInsertSql=DynamicSql.generateInsertSql(old);
		String oldInsertSql2=escapeSingleQuote(oldInsertSql);
		String deleteSql2=escapeSingleQuote(deleteSql);

		StringBuffer sql=new StringBuffer("INSERT INTO _audit (");
		sql.append("timestamp,oid,table,undo_sql,new_sql) ");
		sql.append("VALUES ( datetime(),"+oid+",'"+table+"','"+oldInsertSql2+"','"+deleteSql2+"')");
		return sql.toString();
	}

}