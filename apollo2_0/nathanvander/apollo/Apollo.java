package nathanvander.apollo;
import java.io.Serializable;

/**
* This interface is the design of Apollo 2.0.  It has static inner interfaces for the ease
* of design.  These could easily be moved to separate files if they get too big. I add a line
* of equal signs to separate the classes, so you can see how many files this would need.
*
* I am inspired by reading the source code to SQLite, which has one humungous file with
* 150,000 lines of code.
*
* Apollo is my data access layer around SQLite.  I was calling it a network operating
* system, but that is too complicated.
*
* Features:
*	1. uses Relational Algebra to abstract the data.
*	2. Security built-in.  This is very weak but better than nothing.  Users must be
*		identified.
*	3. Explicit Transactions required.
*	4. Auditing of changes to data
*
* The complete design will use the following tables:
*	_system, to record security keys and other parameters
*	_user, for usernames and passwords
*	_master, extension of sqlite_master, to record java classes associated with relations
*	_audit, for use by audit table
*	_transaction
*
* Proposed ideas which are not part of this are:
*	sessions.  I think this leads to unnecessary complication.  The Transaction object should act
*		as a temporary session
*	service.  It is anticipated that various APIs will use this, and they should have a common
*		service interface which is extended.  I am leaving that out to keep this simple.
*	name lookup.  This may be used by RMI or by a custom RMI implementation, but that is outside
* 		the scope of this.
*	kernel. This may have a kernel, with command-line methods to add users, etc. That is outside
*		the scope of this. This layer will require users, but not have ways to add them.
*		They can be added with a command-line tool, or some other way.
*
* The implementation will use a very low-level api of SQLite.  I want to get away from the
* Connection/Statement/ResultSet model and use the model presented here, which is:
*
*	Relation - used to view data
*	Transaction - used to change data
*
* Transactions as used here are the same as AccountingTransactions (except for the type of
* data updated), and are meant to be just wrapped by an appropriate class.  This may be overkill
* because every transaction is logged in the _transaction table, but I think this is a good design.
* The _transaction table can always be trimmed if it gets too big.
*
* This is part of a 4-layer architecture:
*	1. User Interface
*	2. Service APIs
*	3. Data Access Layer (this interface).
*	4. Database (Sqlite)
*
* A Word about nulls.  They are highly disfavored.  Some parts of relational algebra theory
* as based on nulls, but not this.  If you are joining relations with a common column that includes
* nulls, the nulls do not match.  It will still be possible to select data with nulls by using an
* operator like NOT_NULL or IS_NULL.
*
* Some thoughts about the reason for this.  I am NOT trying to create my own relational database.
* Instead, I am trying to make it easier to use.  There is a huge mismatch between the relational
* layer and the object layer.  This leads to ugly solutions like JDBC.  One solution is to create
* a custom scripting language which handles sql natively, like PL/SQL.  The other solution, which
* I am doing is to move the relational part into the object layer.
*
* This is not a complete replacement for SQL.  There will be things you can do with SQL that you can't
* do with this.
*/

public interface Apollo {
	/**
	* Credentials are used for security.  Some methods require that we know who is requesting
	* the change.
	*/
	public static interface Credentials extends Serializable {
		public String getUserName();
		/**
		* The password is encrypted.
		*/
		public String getPassword();
	}

	//=========================================================
	/**
	* RelationException is used for all exception here or in the lower-level code.
	* This was previously called DataStoreException.
	* This is the equivalent of java.sql.SQLException
	*/
	public static class RelationException extends Exception {
		//UNAUTHORIZED means permission was denied to access
		public final static int UNAUTHORIZED=127;
		//don't use 0, which means SQLITE_OK
		public final static int OTHER=255;

		int errcode;

		//this has errorcode 2nd because Exception has message
		public RelationException(String msg,int errcode) {
				super(msg);
				this.errcode=errcode;
		}

		public int getErrCode() {
			return errcode;
		}

		public String toString() {
			return getMessage()+" (error code: "+errcode+")";
		}
	}

	//=========================================================
	/**
	* This is the high-level interface.  In previous versions, I called this
	* 	DataStore, because it was like an object database, like JavaSpaces.
	*
	* I am giving this a different name to emphasize the redesign.
	*/
	public static interface RelationalDatabase {
		//get the version of this code, currently 2.0
		public float getVersion();

		/**
		* Get the version of the underlying SQLite library.
		*/
		public int getLibVersionNumber();

		public String getDatabaseFileName();

		/**
		/* get a list of all relations in the database
		/* these may be either Tables or Views
		*/
		public String[] listRelations();

		/**
		* Get a relation, which may be either a Table or View.
		* Does not require a username because no changes are made to the data.
		* It seems like this might be holding all the data in a given table,
		* but it is not.  This is just a pointer to the information, so it will be lightweight.
		*/
		public Relation getRelation(String relationName);

		/**
		* The RelationManager is used to Create or Drop relations, and to create indexes.
		* This should also be able to store relations that were created dynamically
		*/
		public RelationManager getRelationManager();

		/**
		* A Transaction is needed to make any changes to the database.
		* The transaction type is optional.  If it is omitted (null) then it will be
		* set to "USER"
		*/
		public Transaction createTransaction(Credentials user, TransactionType tt) throws RelationException;
	}
	//=========================================================
	/**
	* A Tuple is a row of data in a table.
	* I much prefer that these be implemented by a class with public fields that can be discovered
	* with Reflection, but since Relations can be created dynamically, Tuples need to
	* as well.  Plus there could be sub-tuples.
	*
	* In the JDBC world, you could compare this to ResultSet or ResultSetMetaData
	*/
	public static interface Tuple {

	}

	//=============================================================
	/**
	* A Relation is like a matrix or spreadsheet.  It could be a table or view or just exist
	* temporarily in a query.
	*
	* This defines 3 "operations" on a Relation: project(), select() and join().
	* There are other operations possible in Relational Algebra, like
	*	Cartesian cross-products, unions, and complements.
	*
	* The operations return new Relations, which I call "derived" relations.  They will only
	* exist for this connection.  You can save derived using SchemaManager, which will create
	* new views from them.
	*
	* This is not trying to be the ultimate definition of a Relation, only to create a usable
	* interface.
	*
	* We assume that every writeable relation has a column called ID, rowid, or something similar that is
	* auto-incremented.
	*
	* Note that indexes must be explicitly defined for every relation.  This is done via the
	* RelationManager.
	*
	*/
	public static interface Relation {
		//some relations are read-only
		public boolean isWriteable();

		public String getName();

		/**
		* Get the model for the relation.  This will return a blank tuple, which will show
		* the ordered list of columns and the type for each.  This is basically metadata.
		*/
		public Tuple getModel();

		/**
		* Get the number of rows (Tuples) in the relation.
		*/
		public int getRows();

		//----------------------------
		//operations on a Relation
		/**
		* projection narrows the number of columns in a relation.
		* This is the part of an sql statement that list the columns, e.g.
		*	SELECT firstname,lastname FROM contact.
		* The contact table has a lot columns that these, but we only need these
		* two.
		*/
		public Relation project(Tuple t) throws RelationException;

		/**
		* Select narrows the number of rows returned.
		*/
		public Relation select(Condition c) throws RelationException;

		/**
		* Join this relation with another one based on the condition, and give it a name.
		* The implementation of this is complicated, because it must also create a dynamic
		* Tuple
		*/
		public Relation join(Relation r, Condition c, String name) throws RelationException;
		//-------------------------------

		/**
		* A cursor is the equivalent of a ResultSet.  There may be different types
		* of Cursors depending on if this is coming directly from the database
		* or from a derived relation.
		*/
		public Cursor getCursor() throws RelationException;

		//-------------------------------
		//change data in the relation
		//I had this as part of Transaction, but I guess it belongs here.
		/**
		* Insert a Tuple into the relation.  This may fail for various reasons
		* including IO problems, invalid transaction state, or constraint violations.
		* Or if the relation is read-only
		*/
		public Key insert(Transaction tx,Tuple t) throws RelationException;

		/**
		* Get the tuple that you just inserted by the Key
		*/
		public Tuple get(Key k) throws RelationException;

		/**
		* Update the relation with changed data in the Tuple.
		* The old data is stored in the _audit table.
		*/
		public void update(Transaction tx,Tuple old, Tuple nu) throws RelationException;

		/**
		* Delete the object.  Must provide old state of object before doing so, so it
		* can be audited.
		*/
		public void delete(Transaction tx,Tuple old) throws RelationException;

	}
	//============================================================================
	public static interface RelationManager {
		//to be defined.  Includes creating and dropping tables and views and indexes
	}
	//============================================================================
	//a key is just the tablename/id combination
	public static class Key {
		public String tableName;
		public long rowid;
		public Key(String tn, long k) {tableName=tn;rowid=k;}
	}
	//==========================================================================
	//based on java.util.Iterator,
	public static interface Cursor {
		public void open() throws RelationException;

		//the hasNext() method actually loads the object, so don't skip it
		public boolean hasNext() throws RelationException;

		//this retrieves the next object.  use in confunction with hasNext();
		public Tuple next() throws RelationException;

		public void close() throws RelationException;
	}

	//============================================================================
	//I think there is a slight difference between
	//	a SelectCondition, which will compare to a value
	//and a JoinCondition, which will compare columns in two different relations
	//this is the where clause
	public static class Condition {
		//to be determined

		//a condition is made up of one or more expressions
		//with boolean connector AND
	}
	//=======================================================
	//commented out because we haven't referenced it yet
	//public static class Expression {
	//
	//	an expression will have a subject, verb, and object
	// where verb is something like: EQ,NE,GTE,LT and maybe IN, NOT_IN
	//}
	//=============================================================================
	/**
	* Transaction is basically metadata about a transaction.  You can do a transaction
	* without using this, but it won't have the transaction log recorded.
	* This should be used by all user transactions.
	*
	* There will be a related class called TransactionInfo to retrieve
	* AccountingTransaction information, but that will be part of the accounting system.
	*/
	public static interface Transaction {
		//you can't create a Transaction directly
		//it must be obtained from RelationalDatabase, which in turn gets the transaction id from kernel
		//the transaction also holds its authorized user, but we don't need to display that
		public long getID() throws RelationException;

		public void begin() throws RelationException;

		//--------------------------------------------
		//used only for accounting transactions
		//date is a string in YYYY-MM-DD format
		public void setDate(String ymd) throws RelationException;
		public void setSource(String source) throws RelationException;
		public void setString(String description) throws RelationException;

		//---------------------------------------
		public void commit() throws RelationException;
		public void rollback() throws RelationException;
	}

	//==============================================================================
	/**
	* This defines a Kernel class.  I'm not sure it should be here but lets give it a try.
	* The kernel will create the system tables, and handle security.  This doesn't define
	* everything the kernel does, only things we need from it.
	*
	* Note that Connections are NOT obtained from the kernel, they are obtained directly through
	* the code.
	*
	* Also note that low-level transactions are not obtained from the kernel and may be started
	* directly in this layer.  However, code that is in a higher-level than this must use the
	* higher-level Transaction interface as defined in the Apollo layer.
	*
	* Furthermore note that tables are inserted without using the kernel,
	* except part of it is done here, to define the master class.
	*
	* What are we expecting from the Kernel, specifically?
	*	1. user validation (_system and user _tables)
	*	2. assigning transaction ids (_transaction)
	*	3. some functions when adding relations
	*	4. auditing (_audit)
	*
	* We might move more functions in later as well.
	*
	* It may be more appropriate to call this something like SystemHelper, but I am not going to change
	* it now.
	*/
	public static interface Kernel {

		/**
		* This is an optional way of getting a connection.  login with the given Credentials,
		* and the database file name.
		*
		* If valid, this will return a
		*  		org.sqlite.SQLite.SQLite3 pointer (which extends com.sun.jna.PointerType)
		*
		* You are not required to use this method to get the pointer, and you can get it directly
		* from the SQLite layer if you want. Make sure to close it when you are done.
		*
		* This will throw an exception if not authorized.
		*
		* Why am I passing around pointers?  Because this is very low-level code.
		*/
		public com.sun.jna.PointerType login(Credentials c) throws RelationException;

		/**
		* close the connection when done.  Will throw an exception only if the pointer passed in
		* is null or if it is not an org.sqlite.SQLite.SQLite3 pointer.
		*/
		public void close(com.sun.jna.PointerType connection) throws RelationException;

		/**
		* Get the database file name
		*/
		public String getDatabaseFileName();

		/**
		* Get a list of users from the system.
		* This doesn't require validation, because it is read only data.
		* I'm not saying that this should be exposed to the outside world, but someone on the "inside" can
		* just read the database directly.
		*/
		public String[] listUsers();

		/**
		* validate the user credentials. Returns true if valid, and false if invalid.
		* It is up to the code here to decide what to do if the credentials are not validated.
		* Throws an exception only if the database code fails for some other reason.  This is so you
		* can test credentials without an exception being thrown.
		*/
		public boolean validate(Credentials c) throws RelationException;

		/**
		* First of all, this does throw an exception if the credentials are invalid.  If you are not
		* sure, use the previous code to test it.
		*
		* Second, to make this more confusing, this doesn't actually start a transactions, it collects
		* metadata about a transaction.  This is how you obtain a transaction id.  The kernel
		* will do an insert into the _transaction.  It is marked with a timestamp.
		*
		* If you use createTransaction() here, you must also call commitTransaction() with more information
		* when it is commited.
		*
		*/
		public long createTransaction(Credentials c, TransactionType tt) throws RelationException;

		/**
		* This doesn't actually commit the transaction instead it marks it as commited.
		* So maybe it should say "markTransactionAsCommitted"
		*/
		public void commitTransaction(long id) throws RelationException;

		public void rollbackTransaction(long id) throws RelationException;

		/**
		* This has more information about an accounting transaction.
		* Date must be in YYYY-MM-DD format
		* Source is the source journal, like "General"
		* Description is the description of the transaction
		*/
		public void commitAccountingTransaction(long id,String date,String source,String description) throws RelationException;

		/**
		* The _master table is an extension of SQLite master.  It has the name of the
		* class that implements Tuple.  This is used when retrieving data.
		*/
		public void insertClassNameIntoMaster(String tableName, String className) throws RelationException;

		//used when dropping a table
		public void deleteClassNameFromMaster(String tableName) throws RelationException;

		//audit
		//this must be called whenever changes are made to existing data
		public void audit(long oid,String _table,String undo_sql,String new_sql) throws RelationException;
	}

	//===========================================================

	/**
	* This has 3 types of Transactions
	*/
	public static enum TransactionType  {
		SYS,		//used for system transactions
		ACCT,		//Accounting transactions
		USER		//other user-defined transactions
	}

}