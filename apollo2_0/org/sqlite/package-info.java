package org.sqlite;

/**
* This code comes from https://github.com/gwenn/sqlite-jna, which I have modified only
* to put everything in one source code file.  I have excluded extraneous classes.
*
* I am only including the bare minimum JNA wrapper, which is very good.  I don't like JDBC
* or even the equivalent - org.SQLite.Conn.
*
* This is stripped to the bare metal so I can build my own layer on top of it.
* Instead of dealing with objects, you will deal directly with Pointers:
*
* The main one is org.sqlite.SQLite.SQLite3.  This is the pointer to the connection object.
* It is documented at https://www.sqlite.org/c3ref/sqlite3.html.
* The main functions are:
*		sqlite3_open() or sqlite3_open_v2() to create the pointer
*		sqlite3_exec(), which is used with all SQL code except selects
*		sqlite3_close_v2() which closes it
*
* The other main pointer is org.sqlite.SQLite.SQLite3Stmt.
* The main functions are:
*		sqlite3_prepare_v2() to create it, using the SQLite3 pointer and the Select SQL code
*		sqlite3_step() to execute it, one line at a time
*		sqlite3_column_int(), sqlite3_column_text(), sqlite3_column_name() etc to get data from the row
*		sqlite3_finalize()
*
* There is more to it, but this will do 99% of what you need.
*
*/