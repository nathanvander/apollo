package apollo.server;
import apollo.iface.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;
import java.util.Date;
import apollo.util.DateYMD;
import apollo.util.DateYM;
import java.math.BigDecimal;
import java.awt.TextArea;
import java.awt.Choice;
import apollo.util.DynamicSql;

/**
* A Transaction is used to make changes to the database.  This is run in its own connection, so you can have
* multiple transactions at once.
*
* The transaction id is available right away.  It is not unique in the database, just to this process.
* To start a transaction, call begin().  End it with either commit() or rollback().
*
* To make sure that the connection runs in its own thread, the connection isn't created until begin() is called.
*/
public class TransactionObject implements Transaction {
	private String filename;
	private Connection conn;
	private static AtomicInteger counter=new AtomicInteger(randomTwoDigit());
	private int id;

	public TransactionObject(String fn) throws DataStoreException {
		filename=fn;
		id=counter.incrementAndGet();	//equivalent of ++counter;
	}

	//return a random 2 digit number from 10..99
	//it won't start with a 0
	public static int randomTwoDigit() {
		return new Random().nextInt(90)+10;
	}

	/**
	* Get the transaction Id.  Mostly for testing purposes, but I could see storing it.  This is available right away,
	* before the transaction begins.
	*/
	public long getID() throws RemoteException {
		return id;
	}

	/**
	* This both opens the connection and begins the transaction. This will create a lock on the entire
	* file until it is released by committing the transaction.
	*/
	public void begin() throws RemoteException, DataStoreException {
		conn=new Connection(filename);
		conn.exec("--begin transaction '"+getID()+"'");
		conn.exec("BEGIN IMMEDIATE TRANSACTION");
	}

	/**
	* This both commits the transaction and closes the connection.
	*/
	public void commit() throws RemoteException, DataStoreException {
		conn.exec("COMMIT TRANSACTION");
		conn.exec("--commit transaction '"+getID()+"'");
		conn.close();
	}

	/**
	* This both rollsback the transaction and closes the connection.
	*/
	public void rollback() throws RemoteException, DataStoreException {
		conn.exec("ROLLBACK TRANSACTION");
		conn.exec("--rollback transaction '"+getID()+"'");
		conn.close();
	}

	/**
	* Create a table based on the fields in the DataObject.  This is done so that we don't have to check if the
	* table exists every time an insert is done.  It doesn't hurt to call this more than once because
	* it creates the table only if it doesn't exist.
	* This also creates an index on the table.
	*/
	public void createTable(DataObject d) throws RemoteException,DataStoreException {
		if (d==null) throw new DataStoreException("DataObject is null",0);
		try {
			Class k=d.getClass();
			String[] columns=d.fields();
			StringBuilder sql=new StringBuilder("CREATE TABLE IF NOT EXISTS "+d.getTableName()+" (");
			//add the primary key
			sql.append(" rowid INTEGER PRIMARY KEY,");

			boolean first=true;
			for (int i=0; i< columns.length; i++) {
				String col=columns[i];
				Field f=k.getField(col);
				String typeName=f.getType().getName();
				String declaredType=sqliteType(typeName);

				if (col.equalsIgnoreCase("rowid") ) {
					continue;
				}

				if (first) {
					first=false;
				} else {
					sql.append(",");
				}
				sql.append(col+" "+declaredType);
			}
			sql.append(")");

			//execute it
			//System.out.println(sql.toString());
			conn.exec(sql.toString());

			//also create a default index
			if (d.index()!=null) {
				String sql2="CREATE INDEX IF NOT EXISTS idx_"+d.getTableName()+" ON "+d.getTableName()+"("+d.index()+")";
				//System.out.println(sql2);
				conn.exec(sql2);
			}

			//also index by key
			//String sql3="CREATE UNIQUE INDEX IF NOT EXISTS idx_"+d.getTableName()+"_key ON "+d.getTableName()+"("+"_key"+")";
			//System.out.println(sql3);
			//conn.exec(sql3);

			//also add the class name
			//first see if it already exists
			String sql4=MasterClass.selectCountBeforeInsertSql(d.getTableName());
			Statement st=new Statement(conn,sql4);
			int c=0;
			if (st.step()) {
				c=st.getInt(0);
			}
			st.close();

			if (c==0) {
				String className=d.getClass().getName();
				String sql5=MasterClass.insertSql(d.getTableName(),className);
				conn.exec(sql5);
			}
			//done
		} catch (DataStoreException dx) {
			throw dx;
		} catch (Exception x) {
				throw new DataStoreException(x.getClass().getName()+": "+x.getMessage()+" when creating table "+d.getTableName(),0);
		}
	}

	/**
	* Sqlite will accept almost anything as the type name.  It just doesn't like ones with periods in them
	* or arrays.  I'm not going to worry about arrays here.
	* This may need to have more specialized cases, like boolean.
	*
	* A trick here is primitive types are not null, whereas objects are unless specifically stated.
	*
	* Note that boolean types will be declared as "boolean" and store the literal "true"/"false".
	* Other types will have the last part after the dot, like "DateYMD", or "BigDecimal"
	*/
	public static String sqliteType(String typeName) {
		if (typeName.equals("java.lang.String")) {
			return "TEXT";
		} else if (typeName.equals("int") || typeName.equals("long")) {
			return "INTEGER";
		} else if (typeName.equals("float") || typeName.equals("double")) {
			return "REAL";
		} else if (typeName.equals("boolean")) {
			//there isn't actually a boolean type in SQLite.  This will map to TEXT
			//and the field will display true or false
			//we could map it to integer, but this is clearer and we don't care about disk space
			return "BOOLEAN";
		} else{
			int dot=typeName.lastIndexOf('.');
			if (dot>-1) {
				//these are objects and therefore can be null
				return typeName.substring(dot+1);
			} else {
				return typeName+" NOT NULL";
			}
		}
	}

	/**
	* This will drop the table only if it is empty.  If there are records in it, it will throw an exception
	*/
	public void dropTable(DataObject d) throws RemoteException,DataStoreException {
		System.out.println("DEBUG: in TransactionObject.dropTable");

		//first, check the number of rows in it
		String sql="select count(rowid) from "+d.getTableName();
		int rows=0;
		Statement stmt=new Statement(conn,sql);
		stmt.step();
		rows=stmt.getInt(0);
		stmt.close();

		if (rows>0) {
			throw new DataStoreException("trying to drop table "+d.getTableName()+" but it is not empty",0);
		}

		//get the drop table command
		String sql2="DROP TABLE IF EXISTS "+d.getTableName();

		//record audit info.  This will do an insert into the audit table
		String sql2audit=Audit.auditDrop(d.getTableName(),sql2);
		conn.exec(sql2audit);

		//now actually drop it
		conn.exec(sql2);

		String sql3="DROP INDEX IF EXISTS idx_"+d.getTableName();
		conn.exec(sql3);

		//also index by key
		//String sql4="DROP INDEX IF EXISTS idx_"+d.getTableName()+"_key";
		//conn.exec(sql4);

		//and drop master class entry
		String sql5=MasterClass.deleteSql(d.getTableName());
		conn.exec(sql5);
	}


	/**
	* Insert a dataobject into the DataStore
	*/
	public Key insert(DataObject d) throws RemoteException,DataStoreException {
		if (d==null) {
			System.out.println("[DEBUG] in TransactionObject.insert, DataObject is null");
			throw new DataStoreException("trying to insert a null data object",0);
		}

		String sql=DynamicSql.generateInsertSql(d);

		//now execute it
		conn.exec(sql.toString());
		long k=conn.lastInsertRowID();

		//return the key
		return new Key(d.getTableName(),k);
	}

	public void update(DataObject old,DataObject nu) throws RemoteException,DataStoreException {
		if (nu==null || nu.getID()<1) {
			throw new DataStoreException("cannot update because id is "+nu.getID(),0);
		}

		//generate the update sql
		String updateSql=DynamicSql.generateUpdateSql(nu);

		//record audit info.  This will do an insert into the audit table
		String auditSql=Audit.auditUpdate(nu.getID(),nu.getTableName(),old, updateSql);
		conn.exec(auditSql);

		//now execute it
		//rows is the number of rows changed - should be 1
		int rows=conn.exec(updateSql);
		if (rows!=1) {
			System.out.println("WARNING: the command "+updateSql+" updated "+rows+" rows");
		}
	}

	//record audit info before deleting
	//this requires the old state of the object before deleting
	public void delete(DataObject old)  throws RemoteException,DataStoreException {
		//double check audit info
		if (old==null || old.getID()<1) {
			throw new DataStoreException("must provide old state of object before delete is allowed",0);
		}

		String deleteSql="DELETE FROM "+old.getTableName()+" WHERE rowid="+old.getID();

		//get the audit info
		String auditSql=Audit.auditDelete(old.getID(),old.getTableName(),old, deleteSql);
		conn.exec(auditSql);

		int rows=conn.exec(deleteSql);
		if (rows!=1) {
			System.out.println("WARNING: the command "+deleteSql+" deleted "+rows+" rows");
		}
	}

	//a view is kind of like a table
	public void createView(ViewObject v) throws RemoteException,DataStoreException {
		StringBuilder sql=new StringBuilder("CREATE VIEW IF NOT EXISTS "+v.getViewName()+" AS ");
		sql.append(v.getSQL());
		conn.exec(sql.toString());
	}

	//this doesn't affect any data
	public void dropView(ViewObject v) throws RemoteException,DataStoreException {
		String sql="DROP VIEW IF EXISTS "+v.getViewName();
		conn.exec(sql);
	}
}