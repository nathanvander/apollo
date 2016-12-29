package apollo.server;
import apollo.iface.*;
import java.rmi.*;
import java.lang.reflect.Field;
import apollo.util.DateYMD;


/**
* This creates its own Connection in the open() method, which is closed upon closing the cursor
*/

public class CursorObject implements Cursor {
	String filename;
	Connection conn;
	Statement stmt;
	DataObject d;
	ViewObject v;
	String sql;

	//use for a select all
	public CursorObject(String fn,DataObject d,int limit,int offset) throws DataStoreException {
		filename=fn;
		this.d=d;

		if (limit==0) {limit=100;}
		String sql="SELECT * from "+d.getTableName();
		String order=d.index();
		if (order!=null) {
			sql+=" ORDER BY "+order;
		}
		sql+=" LIMIT "+limit+" OFFSET "+offset;
		this.sql=sql;
	}

	//use for a ViewObject
	public CursorObject(String fn,ViewObject v) {
		filename=fn;
		this.v=v;
		this.sql=v.getSQL();
	}

	public String getSql() throws RemoteException {
		return sql;
	}

	//this seems like it could be part of the constructor,
	//but we want to make opening the connection separate from constructing it to make
	//sure it is in its own thread, separate from that of the parent
	public void open() throws RemoteException, DataStoreException {
		conn=new Connection(filename);
		stmt=new Statement(conn,sql);
	}

	public boolean hasNext() throws RemoteException, DataStoreException {
		return stmt.step();
	}

	//return the DataObject or ViewObject
	//this code was taken from DataStoreEngine.get, and probably can be combined with it in a refactor
	public DataObject next() throws RemoteException, DataStoreException {
		//first, create the object that will be used to return the data
		Object o=null;
		Class klaz=null;
		try {
			if (d!=null) {
				klaz=d.getClass();
				o=klaz.newInstance();
			} else if (v!=null) {
				klaz=v.getClass();
				//cast the view object to data for a common appearance
				o=klaz.newInstance();
			}
		} catch (Exception x) {
			throw new DataStoreException(x.getClass().getName()+": "+x.getMessage()+" when instantiating data",0);
		}

		//get number of columns
		int cols=stmt.getColumnCount();
		for (int j=0;j<cols;j++) {
			//get the column name
			//note that in the view object, this should be declared with AS in the list of columns in the select
			String colName=stmt.getColumnName(j);
			try {
				Field f=klaz.getDeclaredField(colName);
				f.setAccessible(true);  //turn off security checks
				String ft=f.getType().getName();

				//this needs more types
				if (ft.equals("java.lang.String")) {
					f.set(o,stmt.getString(j));
				} else if (ft.equals("apollo.util.DateYMD")) {
					String v=stmt.getString(j);
					DateYMD date=DateYMD.fromString(v);
					f.set(o,date);
				} else if (ft.equals("java.math.BigDecimal")) {
					String v=stmt.getString(j);
					f.set(o,new java.math.BigDecimal(v));
				} else if (ft.equals("int")) {
					f.setInt(o,stmt.getInt(j));
				} else if (ft.equals("long")) {
					f.setLong(o,stmt.getLong(j));
				} else if (ft.equals("double")) {
					f.setDouble(o,stmt.getDouble(j));
				} else if (ft.equals("boolean")) {
					//expect it to be true
					String v=stmt.getString(j);
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
		return (DataObject)o;
	}


	public void close() throws RemoteException {
		stmt.close();
		conn.close();
	}
}