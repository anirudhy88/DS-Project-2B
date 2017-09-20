package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/*
 * GroupMessengerProvider is a key-value table. We do not implement full support for SQL as
 * a usual ContentProvider does. We re-purpose ContentProvider's interface to use it as a
 * key-value table.
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * Two main methods to implement : insert() and query().
 */
public class GroupMessengerProvider extends ContentProvider {
    static final String TAG = GroupMessengerProvider.class.getSimpleName();
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Need not implement this for this project.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // Need not to implement this for this project.
        return null;
    }

    /* ---------------------------------------------------------------------------------
    * @Override
    * @name insert() 
    * @desc This method writes ContentValues which will have two columns (a key  
            and a value column) and one row that contains the actual (key, value) 
            pair into the file system. Storage option used: simple file-system storage   
    * @param uri, values
    * @return the URI
    ---------------------------------------------------------------------------------- */
    public Uri insert(Uri uri, ContentValues values) {
        // Need to get contents out of the variable "values" of the type "ContentValues"
        String key = (String)values.get("key");
        String value = (String) values.get("value");
        Log.v(TAG, "key is " + key);
        Log.v(TAG, "value is "+ value);
        
        // Creating a file to store the value
        String fileName = key;
        
        // To write data into the file, need to create a file output stream.
        FileOutputStream outputStream;
        try {
            // Getting file output stream using openFileOutput
            outputStream = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
            
            // Write data into the file using the stream
            outputStream.write(value.getBytes());
            
            // Once you are done writing, just close it
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Failure of writing data into a file");
        }

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // This is implemented if we need perform any one-time initialization task. 
        // Not required for this project
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Need not to implement this for this project.
        return 0;
    }

    /* ---------------------------------------------------------------------------------
    * @Override
    * @name query() 
    * @desc This method returns a Cursor object containing the result to the requested query 
    * @param uri, projection, selection, selectionArgs, sortOrder
    * @return the Cursor
    ---------------------------------------------------------------------------------- */
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Open the file and search for the row
        FileInputStream inputStream ;
        String value = "";
        StringBuffer sb = new StringBuffer("");
        byte[] buffer = new byte[1024];
        int n = 0;
        
        // Performing read operation on a file
        // File name is sent as an argument 'selection'
        try {
            inputStream = getContext().openFileInput(selection);
            while((n = inputStream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, n));
            }
        } catch(FileNotFoundException e) {
            Log.e(TAG,"file not found");
        } catch (IOException e) {
            Log.e(TAG,"io exception");
        }
        value = sb.toString();
        
        // Need to build an object of type MatrixCursor
        // http://developer.android.com/reference/android/database/MatrixCursor.html
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
        
        // addRow(Object[] columnValues)
        // Adds a new row to the end with the given column values.
        String[] columnValues = {selection, value};
        cursor.addRow(columnValues);
        Log.v("query", selection);
        return cursor;
    }
}
