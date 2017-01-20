package apollo.util;
import apollo.iface.DataObject;
import apollo.iface.DataStoreException;
import java.math.BigDecimal;
import java.awt.TextArea;
import java.awt.Choice;
import java.lang.reflect.Field;

public class DynamicSql {

	public static String generateInsertSql(DataObject d) throws DataStoreException {
		//generate the insert statement
		StringBuffer sql=new StringBuffer();
		sql.append("INSERT INTO "+d.getTableName()+" \n");
		sql.append("("+fieldNames(d)+")"+" \n");
		sql.append("VALUES ("+"\n");

		String[] fields=d.fields();
		Field f=null;

		boolean first=true;
		for (int i=0;i<fields.length;i++) {
			if (fields[i].equalsIgnoreCase("rowid") || fields[i].equalsIgnoreCase("oid") ) {
				continue;
			}
			String fn=fields[i];
			if (first) {
				first=false;
			} else {
				sql.append(",");
			}
			sql.append(getFieldValue(d,fn)+"\n");
		}
		sql.append(")");
		return sql.toString();
	}



	/**
	* Return a String, which is the list of field names, separated by a comma.
	* We get this from DataObject, but check the names, because we DON'T insert a rowid.
	*/
	private static String fieldNames(DataObject d) {
		StringBuilder sb=new StringBuilder();
		String[] fields=d.fields();

		boolean first=true;
		for (int i=0;i<fields.length;i++) {
			if (fields[i].equalsIgnoreCase("rowid") || fields[i].equalsIgnoreCase("oid") ) {
				continue;
			}
			if (first) {
				//only prepend comma if not first
				first=false;
			} else {
				sb.append(",");
			}
			sb.append(fields[i]);
		}
		return sb.toString();
	}

	//the oid is available from DataObject.getOID()
	public static String generateUpdateSql(DataObject d) throws DataStoreException {
		if (d.getID()<1) {
			throw new IllegalArgumentException("dataobject oid = "+d.getID());
		}

		//generate the update sql
		StringBuilder sql=new StringBuilder();
		String[] fields=d.fields();
		boolean first=true;
		sql.append("UPDATE "+d.getTableName()+" SET \n");
		for (int i=0;i<fields.length;i++) {
			if (fields[i].equalsIgnoreCase("rowid") || fields[i].equalsIgnoreCase("oid") ) {
				continue;
			}

			if (first) {
				first=false;
			} else {
				sql.append(",");
			}
			String value=getFieldValue(d,fields[i]);
			sql.append(fields[i]+"="+value+"\n");
		}

		//add the where clause
		sql.append("WHERE rowid="+d.getID()+"\n");
		return sql.toString();
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
					val=val.replaceAll("'","`");
					return "'"+val+"'";
				}
			} else if (ft.equals("java.util.Date")) {
				java.util.Date val=(java.util.Date)f.get(d);
				if (val==null) {
					return null;
				} else {
					return "'"+val.toString()+"'";
				}
			} else if (ft.equals("java.sql.Timestamp")) {
				java.sql.Timestamp val=(java.sql.Timestamp)f.get(d);
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
			} else if (ft.equals("apollo.util.DateYM")) {
				DateYM val=(DateYM)f.get(d);
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
					//it will be stored as a double in the database
					return val.toPlainString();
				}
			} else if (ft.equals("java.awt.TextArea")) {
				TextArea ta=(TextArea)f.get(d);
				if (ta==null) {
					return null;
				} else {
					String text=ta.getText();
					//escape single quote
					text=text.replaceAll("'","`");
					return "'"+text+"'";
				}
			} else if (ft.equals("java.awt.Choice")) {
				Choice ch=(Choice)f.get(d);
				if (ch==null) {
					return null;
				} else {
					String selected=ch.getSelectedItem();
					return "'"+selected+"'";
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

}