# apollo
A wrapper around Sqlite to make it an object database

The name Apollo was inspired by Oracle and Delphi, and also by the lunar program.  Apollo is a database server that can be connected
to by multiple clients.  All of the SQL will be on the server side.  

To use it, create a DataObject for each Table, and a ViewObject for each custom query.  To add data, get a Transaction object by using
DataStore.createTransaction(), and then call the insert method with the DataObject.

See the test code for examples of how to use it.

Update 12/28/2016
The code is complete and is usable.  We can call it version 1.0.
