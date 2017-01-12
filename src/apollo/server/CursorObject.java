package apollo.server;
import apollo.iface.*;
import java.rmi.*;
import java.lang.reflect.Field;
import apollo.util.DateYMD;
import apollo.util.DateYM;
import java.awt.TextArea;
import java.awt.Choice;


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

		//problem.  If you have defined the ViewObject, you might as well use it.
		//this just uses a regular data object
		//this.sql=v.getSQL();
		this.sql="SELECT * FROM "+v.getViewName();
	}

	//this can also be called on a ViewObject, if the ViewObject has defined the getTableName()
	//to the View name.  There will be 2 where clauses, the where clause of the view and the where
	//clause of the select.  The database is smart enough to figure this out, I think
	public CursorObject(String fn,DataObject d,String whereClause) {
		filename=fn;
		this.d=d;
		String sql="SELECT * FROM "+d.getTableName()+" "+whereClause;
		this.sql=sql;
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
		//System.out.println("DEBUG: CursorObject col count="+cols);
		for (int j=0;j<cols;j++) {
			//get the column name
			//note that in the view object, this should be declared with AS in the list of columns in the select
			String colName=stmt.getColumnName(j);
			//System.out.println("DEBUG: CursorObject colname "+j+"="+colName);
			try {
				Field f=klaz.getDeclaredField(colName);
				f.setAccessible(true);  //turn off security checks
				String ft=f.getType().getName();

				//this needs more types
				if (ft.equals("java.lang.String")) {
					String v=stmt.getString(j);
					if (v!=null) {
						//this causes a problem when the string is null
						//i don't know why
						f.set(o,v);
					}
				} else if (ft.equals("apollo.util.DateYMD")) {
					String v=stmt.getString(j);
					if (v!=null) {
						DateYMD date=DateYMD.fromString(v);
						f.set(o,date);
					}
				} else if (ft.equals("apollo.util.DateYM")) {
					String v=stmt.getString(j);
					if (v!=null) {
						DateYM date=DateYM.fromString(v);
						f.set(o,date);
					}
				} else if (ft.equals("java.math.BigDecimal")) {
					String v=stmt.getString(j);
					if (v!=null) {
						f.set(o,new java.math.BigDecimal(v));
					}
				} else if (ft.equals("java.awt.TextArea")) {
					String text=stmt.getString(j);
					if (text!=null) {
						java.awt.TextArea ta=new java.awt.TextArea(text,3,40,TextArea.SCROLLBARS_VERTICAL_ONLY);
						ta.setName(colName);
						f.set(o,ta);
					}
				} else if (ft.equals("java.awt.Choice")) {
					//the choice list must already exist, we don't have enough
					//info to recreate it
					//so first get the object
					Choice ch=(Choice)f.get(o);
					if (ch==null) {
						System.out.println("Warning: the Choice field for "+colName+" in "+klaz.getName()+" is null. This should have been set");
					} else {
						String text=stmt.getString(j);
						if (text!=null) {
							ch.select(text);
							//now double check it
							String selected=ch.getSelectedItem();
							if (!text.equals(selected)) {
								System.out.println("Warning: the value of the Choice field was supposed to be set to "+text+" but the selected value is "+selected);
							}
						}
					}
				} else if (ft.equals("int")) {
					f.setInt(o,stmt.getInt(j));
				} else if (ft.equals("long")) {
					f.setLong(o,stmt.getLong(j));
				} else if (ft.equals("double")) {
					f.setDouble(o,stmt.getDouble(j));
				} else if (ft.equals("boolean")) {
					//expect it to be true
					String v=stmt.getString(j);
					if (v!=null) {
						if (v.equalsIgnoreCase("true") || v.equals("1")) {
							f.setBoolean(o,true);
						} else {
							f.setBoolean(o,false);
						}
					}
				} else {
					throw new DataStoreException("unknown type "+ft,0);
				}
			} catch (DataStoreException dx) {
				throw dx;
			} catch (Exception x) {
				x.printStackTrace();
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