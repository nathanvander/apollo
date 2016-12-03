package apollo.server;
import apollo.iface.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

/**
* A Transaction is used to make changes to the database.  This is run in its own connection, so you can have multiple
* transactions at once.
*
* The transaction id is available right away.  It is not unique in the database, just to this process.
* To start a transaction, call begin().  End it with either commit() or rollback().
*/
public class TransactionObject implements Transaction {
	private Connection conn;
	private static AtomicInteger counter=new AtomicInteger(randomTwoDigit());
	private int id;

	public TransactionObject(String fn) throws DataStoreException {
		conn=new Connection(fn);
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
			String sql2="CREATE INDEX IF NOT EXISTS idx_"+d.getTableName()+" ON "+d.getTableName()+"("+d.index()+")";
			//System.out.println(sql2);
			conn.exec(sql2);

			//also index by key
			String sql3="CREATE UNIQUE INDEX IF NOT EXISTS idx_"+d.getTableName()+"_key ON "+d.getTableName()+"("+"_key"+")";
			//System.out.println(sql3);
			conn.exec(sql3);

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
	*/
	public static String sqliteType(String typeName) {
		int dot=typeName.lastIndexOf('.');
		if (dot>-1) {
			return typeName.substring(dot+1);
		} else {
			return typeName;
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
	}


	public Key insert(DataObject d) throws RemoteException,DataStoreException {
		return null;
	}

	public void update(Key k, DataObject d) throws RemoteException,DataStoreException {}

	public void delete(Key k)  throws RemoteException,DataStoreException {}

	public void createView(ViewObject v) throws RemoteException,DataStoreException {}

	public void dropView(ViewObject v) throws RemoteException,DataStoreException {}

}