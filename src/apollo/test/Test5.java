package apollo.test;
import apollo.iface.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import apollo.util.DateYMD;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Test5 {
	//===================================
	//DataObject
	public static class Event implements DataObject, ViewObject {
		public long rowid;
		public String _key;
		public String person_key;  //foreign key, not enforced
		public String name; 		//name of event
		public DateYMD date;
		public String time;

		public String[] fields() {
			return new String[]{"rowid","_key","person_key","name","date","time"};
		}

		public String getTableName() {
			return "Event";
		}

		public String index() {return "date,time";}
		public String getKey() {return _key;}

		public String getSQL() {
			return "SELECT rowid, _key, name, date FROM Event WHERE date > '2015-01-01' ORDER BY date,time LIMIT 50";
		}


		/**
		* This is the name of the view.  Don't use the same name as a table.
		*/
		public String getViewName() {
			return "Event_View";
		}

		//for testing
		public String toString() {
			return rowid+": "+name+" at "+date+" "+time;
		}

	}

	public static String getTime() {
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
		return timeFormat.format(new Date());
	}


	//=====================================
	//usually: localhost
    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];
        //create the new event
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            DataStore ds = (DataStore) registry.lookup("DataStore");

            //first create the table
			Transaction tx=ds.createTransaction();
			System.out.println("transaction "+tx.getID()+" created");
			tx.begin();
			System.out.println("creating table event");
			tx.createTable(new Event());
			tx.commit();

            //populate the object with some fields
            Event ev=new Event();
            ev.name="party";
            ev.date=new DateYMD();  //defaults to today
            ev.time=getTime();

            //populate the object with some fields
          	Event ev2=new Event();
            ev2.name="fireworks";
            ev2.date=new DateYMD(2017,1,1);
            ev2.time="00:01";

            //now do an insert
			Transaction tx2=ds.createTransaction();
			tx2.begin();
			Key k1=tx2.insert(ev);
			Key k2=tx2.insert(ev2);
			tx2.commit();

            //create an event
			Transaction tx3=ds.createTransaction();
			tx3.begin();
			tx3.createView((ViewObject)ev2);
			tx3.commit();

			System.out.println("-----------------");
			//view list of events
			Cursor c1=ds.view((ViewObject)ev2);
			c1.open();
			while (c1.hasNext()) {
				Event ev3=(Event)c1.next();
				System.out.println(ev3);
			}
			c1.close();

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            System.out.println("FAIL");
        }
    }

}