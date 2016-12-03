//test2

package apollo.test;
import apollo.iface.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
* Test creating a table and drop it.
*/

public class Test2 {
	public static class Person implements DataObject {
		public long rowid;
		public String _key;
		public String firstname;
		public String lastname;
		public int age;

		public String[] fields() {
			return new String[]{"firstname","lastname","age"};
		}

		public String getTableName() {
			return "Person";
		}

		public String index() {return "lastname,firstname";}
		public String getKey() {return _key;}
	}

	//usually: localhost
    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];
        Person p=new Person();
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            DataStore ds = (DataStore) registry.lookup("DataStore");
			Transaction tx=ds.createTransaction();
			System.out.println("transaction "+tx.getID()+" created");
			tx.begin();
			System.out.println("creating table person");
			tx.createTable(p);
			tx.commit();

			//now check it
			String[] tables = ds.listTables();
			System.out.print("tables: ");
			for (int i=0;i<tables.length;i++) {
				System.out.print(tables[i]+" ");
			}
			System.out.println("");
			//--------------------
			//and now drop the table
			Transaction tx2=ds.createTransaction();
			System.out.println("transaction "+tx2.getID()+" created");
			tx2.begin();
			System.out.println("dropping table person");
			tx2.dropTable(p);
			tx2.commit();

            System.out.println("SUCCESS");

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            System.out.println("FAIL");
        }
    }
}