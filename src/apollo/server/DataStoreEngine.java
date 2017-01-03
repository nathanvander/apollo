package apollo.server;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import apollo.iface.*;
import java.lang.reflect.Field;
import java.security.Permission;
import apollo.util.DateYMD;
import java.math.BigDecimal;

/**
* This is the main class.  Because of SQLITE_BUSY result codes, every sql call needs its own transaction.
*/
public class DataStoreEngine implements DataStore {
	protected String dbFileName;

	//this doesn't throw RemoteException because it can't be accessed remotely
	public DataStoreEngine(String fn) throws DataStoreException {
		dbFileName=fn;

		//initialize the sequence and master class objects
		Connection c=new Connection(fn);
		c.exec("BEGIN IMMEDIATE TRANSACTION");
		//this will do up to 3 sql statements with either a SELECT, CREATE TABLE and INSERT
		//or 2 selects
		Sequence.init(c);
		String sql1=MasterClass.createMasterTableSql();
		c.exec(sql1);
		c.exec("COMMIT TRANSACTION");
		c.close();
	}

	public int getLibVersionNumber() throws RemoteException {
		return Connection.libversion_number();
	}

	/**
	* Returns the name of the DB file on the server.
	*/
	public String getDatabaseFileName() throws RemoteException {
		return dbFileName;
	}

	/**
	* Connect to the Database and create a Transaction.  You can change multiple things at once in the transaction.
	*/
	public Transaction createTransaction() throws RemoteException,DataStoreException {
		TransactionObject tx=new TransactionObject(dbFileName);
		//we are not returning the transaction object, just its stub
		Transaction stub =(Transaction)UnicastRemoteObject.exportObject(tx,0);
		return stub;
	}

	//list all tables in the database and return a String array
	//The top level interface will hold a Connection to do the listTables and get methods.
	//It will hold the database only 5 minutes and then reopen it so the connection doesn't get stale
	public String[] listTables() throws RemoteException,DataStoreException {
		Connection conn;	//local variable
		conn=new Connection(dbFileName);

		//find number of rows
		int numTables=0;
		String sql="select count(name) from sqlite_master where type='table'";
		Statement stmt=new Statement(conn,sql);
		if (stmt.step()) {
			numTables=stmt.getInt(0);  //look in the first column
		} else {
			//shouldn't happen
			throw new DataStoreException("error in: "+sql,0);
		}
		stmt.close();

		//create the string array
		String[] tables = new String[numTables];
		String sql2="select name from sqlite_master where type='table'";
		Statement stmt2=null;
		if (numTables!=0) {
			//then create a Statement
			stmt2=new Statement(conn,sql2);
			//step through it
			int count=0;
			while (stmt2.step()) {
				//get the table name and add it to the array
				tables[count]=stmt.getString(0);
				count++;
				//it is theoretically possible that this could overrun the array, but we just
				//asked for a count of the tables
			}
		}
		if (stmt2!=null) {
			stmt2.close();
		}
		conn.close();
		return tables;
	}

	/**
	* Get the DataObject specified by the given key.  Return null if not found
	*/
	public DataObject get(Key k) throws RemoteException,DataStoreException {
		Connection conn=new Connection(dbFileName);

		//get the classname
		String className=MasterClass.getClassName(conn,k.tableName);
		if (className==null) {
			throw new DataStoreException("className for "+k.tableName+" is null",0);
		}

		String sql="SELECT * FROM "+k.tableName+" WHERE _key='"+k.id+"'";
		Statement st=new Statement(conn,sql);
		Object o=null;
		if (st.step()) {

			Class klaz=null;
			try {
				klaz=Class.forName(className);
				o=klaz.newInstance();
			} catch (Exception x) {
				throw new DataStoreException(x.getClass().getName()+": "+x.getMessage()+" when instantiating "+k.tableName,0);
			}

			//get number of columns
			int cols=st.getColumnCount();
			for (int j=0;j<cols;j++) {
				//get the column name
				String colName=st.getColumnName(j);
			try {
				Field f=klaz.getDeclaredField(colName);
				f.setAccessible(true);  //turn off security checks
				String ft=f.getType().getName();

				//this needs more types
				if (ft.equals("java.lang.String")) {
					f.set(o,st.getString(j));
				} else if (ft.equals("apollo.util.DateYMD")) {
					String v=st.getString(j);
					DateYMD date=DateYMD.fromString(v);
					f.set(o,date);
				} else if (ft.equals("java.math.BigDecimal")) {
					String v=st.getString(j);
					f.set(o,new java.math.BigDecimal(v));
				} else if (ft.equals("int")) {
					f.setInt(o,st.getInt(j));
				} else if (ft.equals("long")) {
					f.setLong(o,st.getLong(j));
				} else if (ft.equals("double")) {
					f.setDouble(o,st.getDouble(j));
				} else if (ft.equals("boolean")) {
					//expect it to be true
					String v=st.getString(j);
					if (v.equalsIgnoreCase("true") || v.equals("1")) {
						f.setBoolean(o,true);
					} else {
						f.setBoolean(o,false);
					}
				} else {
					throw new DataStoreException("unknown type "+ft,0);
				}
			} catch (DataStoreException dx) {
				throw dx;
			} catch (Exception x) {
				throw new DataStoreException(x.getClass().getName()+": "+x.getMessage()+" when setting field "+colName,0);
			}

			}	//end for

		} //else not found;
		st.close();
		conn.close();
		return (DataObject)o;
	}

	//is this necessary?
	public int rows(String tableName) throws RemoteException,DataStoreException {
		Connection conn=new Connection(dbFileName);

			String sql="SELECT count(*) FROM "+tableName;
			Statement stmt = new Statement(conn,sql);
			stmt.step();
			int i=stmt.getInt(0);
			stmt.close();
		conn.close();
		return i;
	}

	public Cursor selectAll(DataObject d) throws RemoteException,
		DataStoreException {
		return selectAll(d,100,0);
	}
	/**
	* This has limited capabilities on purpose.  It returns every DataObject in the database
	* in the default sort order.  Of course, more options will be needed, but
	* this will work for simple applications.
	* This uses its own Connection and Statement.
	*
	* This uses a DataObject so the index() field can be used
	*/
	public Cursor selectAll(DataObject d,int limit,int offset) throws RemoteException,
		DataStoreException {
		CursorObject cx=new CursorObject(dbFileName,d,limit,offset);
		//we are not returning the transaction object, just its stub
		Cursor stub =(Cursor)UnicastRemoteObject.exportObject(cx,0);
		return stub;
	}

	/**
	* Return the data specified by the view.
	*/
	public Cursor view(ViewObject v) throws RemoteException, DataStoreException {
		CursorObject cx=new CursorObject(dbFileName,v);
		//we are not returning the transaction object, just its stub
		Cursor stub =(Cursor)UnicastRemoteObject.exportObject(cx,0);
		return stub;
	}

	/**
	* Get a sequence number from the database.  The standard one is "_key".  Use this if you need
	* a database generated sequence before an insert is made.
	* This format is Base-12.
	*/
	public String nextId(String seqName) throws RemoteException, DataStoreException {
		if (seqName==null) {return null;}
		else if (seqName.equals("_key")) {
			//then use the standard sequence object
			Connection c=new Connection(dbFileName);
			c.exec("BEGIN IMMEDIATE TRANSACTION");
			String k=Sequence.getInstance().nextKey(c);
			c.exec("COMMIT");
			c.close();
			return k;
		} else {
			Connection c=new Connection(dbFileName);
			String sql="SELECT nextid,nextval FROM _sequence WHERE name = '"+seqName+"'";
			Statement stmt = new Statement(c,sql);
			int nid=-1;
			String nval=null;
			if (stmt.step()) {
				nid=stmt.getInt(0);
				nval=stmt.getString(1);
			}
			stmt.close();

			String k=null;
			c.exec("BEGIN IMMEDIATE TRANSACTION");
			if (nid<1) {
				int id=1;
				nid=2;
				k=Integer.toString(id,12);
				String nextVal=Integer.toString(nid,12);
				//insert the next number into the table
				String insertSql="INSERT INTO _sequence (name,nextid,nextval) VALUES ('"+seqName+"',"+nid+",'"+nextVal+"')";
				c.exec(insertSql);
			} else {
				k=nval;
				//increment it
				nid++;
				String nextVal=Integer.toString(nid,12);
				//update it
				String updateSql="UPDATE _sequence SET nextid="+nid+",nextval='"+nextVal+"' WHERE name = '"+seqName+"'";
				c.exec(updateSql);
			}
			c.exec("COMMIT");
			c.close();
			return k;
		}
	}

	//======================================================================
	//start up the Engine and bind it to the registry
    public static void main(String[] args) {
		String filename="vos2.sqlite";  //hard-coded, would be very easy to have it passed in

		//we don't need this object here.  Any apps will either get it
		//from the register or call create() directly
		DataStore ds=create(filename);
	}

	/**
	* Create a new DataSource and export it to the RMI registry.
	* Don't call this more than once per process.  If you need another handle,
	* go to the RMI registry to get it.
	*/
	public static DataStore create(String filename) {
		//remove all security
		System.setSecurityManager(
			new SecurityManager() {
				public void checkPermission(Permission perm) {}
			}
		);

		//start rmi registry
		try {
			LocateRegistry.createRegistry(1099);
			System.out.println("RMI Registry started on port 1099");
		} catch (Exception x) {
			System.out.println("unable to start RMI Registry");
			//is it already started by another process? if so, this is not fatal
		}

        try {
            String name = "DataStore";
            DataStore ds = new DataStoreEngine(filename);
            DataStore stub =
                (DataStore)UnicastRemoteObject.exportObject(ds, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(name, stub);
            System.out.println("DataStoreEngine bound to RMI Registry as '"+name+"'");
            return ds;
        } catch (Exception e) {
            System.err.println("fatal DatabaseStore exception:");
            e.printStackTrace();
            System.exit(1);
		}
		return null;
	}

}