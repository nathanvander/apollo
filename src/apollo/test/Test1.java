//test1

package apollo.test;
import apollo.iface.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Test1 {

	//usually: localhost
    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            DataStore ds = (DataStore) registry.lookup("DataStore");
            System.out.println("SQLite library version: " + ds.getLibVersionNumber());
            System.out.println("db filename: " + ds.getDatabaseFileName());
            String[] tables = ds.listTables();
            System.out.println("number of tables: "+tables.length);
            System.out.println("SUCCESS");

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            System.out.println("FAIL");
        }
    }
}