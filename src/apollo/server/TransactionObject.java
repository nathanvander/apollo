package apollo.server;
import apollo.iface.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;
import java.util.Date;
import apollo.util.DateYMD;
import java.math.BigDecimal;

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
			//add our dataobject key.  This starts with an underscore so it doesn't conflict with sql keyword KEY
			sql.append(" _key TEXT UNIQUE");
			for (int i=0; i< columns.length; i++) {
				String col=columns[i];
				Field f=k.getField(col);
				String typeName=f.getType().getName();
				String declaredType=sqliteType(typeName);

				if (col.equalsIgnoreCase("rowid") || col.equalsIgnoreCase("_key") ) {
					continue;
				} else {
					if (i<columns.length) { sql.append(", "); }
					sql.append(col+" "+declaredType);
				}
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
			String sql3="CREATE UNIQUE INDEX IF NOT EXISTS idx_"+d.getTableName()+"_key ON "+d.getTableName()+"("+"_key"+")";
			//System.out.println(sql3);
			conn.exec(sql3);

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
	*/
	public static String sqliteType(String typeName) {
		int dot=typeName.lastIndexOf('.');
		if (dot>-1) {
			//these are objects and therefore can be null
			return typeName.substring(dot+1);
		} else {
			return typeName+" NOT NULL";
		}
	}

	/**
	* This will drop the table only if it is empty.  If there are records in it, it will throw an exception
	*/
	public void dropTable(DataObject d) throws RemoteException,DataStoreException {
		System.out.println("DEBUG: in TransactionObject.dropTable");
		String sql="select count(rowid) from "+d.getTableName();
		int rows=0;
		Statement stmt=new Statement(conn,sql);
		stmt.step();
		rows=stmt.getInt(0);
		stmt.close();

		if (rows>0) {
			throw new DataStoreException("trying to drop table "+d.getTableName()+" but it is not empty",0);
		}

		String sql2="DROP TABLE IF EXISTS "+d.getTableName();
		conn.exec(sql2);

		String sql3="DROP INDEX IF EXISTS idx_"+d.getTableName();
		conn.exec(sql3);

		//also index by key
		String sql4="DROP INDEX IF EXISTS idx_"+d.getTableName()+"_key";
		conn.exec(sql4);

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
		//get a sequence number
		String k=Sequence.getInstance().nextKey(conn);

		//generate the insert statement
		StringBuffer sql=new StringBuffer();
		sql.append("INSERT INTO "+d.getTableName()+" \n");
		sql.append("("+fieldNames(d)+")"+" \n");
		sql.append("VALUES ("+"\n");
		sql.append("'"+k+"'\n");		//key

		String[] fields=d.fields();
		Field f=null;

		for (int i=0;i<fields.length;i++) {
			if (fields[i].equalsIgnoreCase("rowid") || fields[i].equalsIgnoreCase("_key") ) {
				continue;
			}
			String fn=fields[i];
			sql.append(","+getFieldValue(d,fn)+"\n");
		}
		sql.append(")");

		//now execute it
		conn.exec(sql.toString());

		//return the key
		return new Key(d.getTableName(),k);
	}

	/**
	* Get the value of the field, escaping it if needed.
	*/
	private static String getFieldValue(DataObject d,String fieldName) throws DataStoreException {
		try {
			Field f=d.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);  //turn off security checks
			String ft=f.getType().getName();

			//we only do the most common types
			//other types that may be needed:
			//	Boolean (capital B),
			//	Integer (capital i)
			if (ft.equals("java.lang.String")) {
				String val=(String)f.get(d);
				if (val==null) {
					return val;
				} else {
					return "'"+val+"'";
				}
			} else if (ft.equals("java.util.Date")) {
				Date val=(Date)f.get(d);
				if (val==null) {
					return null;
				} else {
					return "'"+val.toString()+"'";
				}
			} else if (ft.equals("apollo.util.DateYMD")) {
				DateYMD val=(DateYMD)f.get(d);
				if (val==null) {
					return null;
				} else {
					return "'"+val.toString()+"'";
				}
			} else if (ft.equals("java.math.BigDecimal")) {
				//use for currency fields
				BigDecimal val=(BigDecimal)f.get(d);
				if (val==null) {
					return null;
				} else {
					//no quotes needed
					return val.toPlainString();
				}
			} else if (ft.equals("int")) {
				int iv=f.getInt(d);
				return String.valueOf(iv);
			} else if (ft.equals("long")) {
				long lv=f.getLong(d);
				return String.valueOf(lv);
			} else if (ft.equals("float")) {
				float fv=f.getFloat(d);
				return String.valueOf(fv);
			} else if (ft.equals("double")) {
				double dv=f.getDouble(d);
				return String.valueOf(dv);
			} else if (ft.equals("boolean")) {
				//just use true and false.  Space is cheap
				boolean bv=f.getBoolean(d);
				return "'"+bv+"'";
			} else {
				throw new DataStoreException("unknown type "+ft,0);
			}
		} catch (Exception x) {
			throw new DataStoreException(x.getClass().getName()+": "+x.getMessage()+", when getting the value of the field "+fieldName,0);
		}
	}

	/**
	* Return a String, which is the list of field names, separated by a comma.
	* We get this from DataObject, but check the names, because we don't insert a rowid, and we do insert
	* a _key.
	*/
	private static String fieldNames(DataObject d) {
		//hardcode the field _key in here
		StringBuilder sb=new StringBuilder("_key");
		String[] fields=d.fields();

		for (int i=0;i<fields.length;i++) {
			if (fields[i].equalsIgnoreCase("rowid") || fields[i].equalsIgnoreCase("_key") ) {
				continue;
			}
			sb.append(","+fields[i]);
		}
		return sb.toString();
	}

	public void update(DataObject d) throws RemoteException,DataStoreException {
		if (d==null || d.getKey()==null) {
			throw new DataStoreException("cannot update because key is null",0);
		}

		//generate the update sql
		StringBuilder sql=new StringBuilder();
		String[] fields=d.fields();
		sql.append("UPDATE "+d.getTableName()+" SET \n");
		for (int i=0;i<fields.length;i++) {
			if (fields[i].equalsIgnoreCase("rowid") || fields[i].equalsIgnoreCase("_key") ) {
				continue;
			}

			if (i!=0) {sql.append(",");}
			String value=getFieldValue(d,fields[i]);
			sql.append(fields[i]+"="+value+"\n");
		}

		//add the where clause
		sql.append("WHERE _key='"+d.getKey()+"'"+"\n");
		//now execute it
		//rows is the number of rows changed - should be 1
		int rows=conn.exec(sql.toString());
	}

	public void delete(Key k)  throws RemoteException,DataStoreException {
		String sql="DELETE FROM "+k.tableName+" WHERE _key='"+id+"'";
		conn.exec(sql);
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