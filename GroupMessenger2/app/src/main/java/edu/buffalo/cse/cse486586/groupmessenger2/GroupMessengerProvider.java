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

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to imple  ment---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    static final String TAG = GroupMessengerProvider.class.getSimpleName();
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        // Need to get contents out of the variable "values" of the type "ContentValues"
        // https://developer.android.com/reference/android/content/ContentValues.html
        // Returns a set of all of the keys and values - valueSet()

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
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */

        // Open the file and search for the row
        FileInputStream inputStream ;
        String value = "";
        StringBuffer sb = new StringBuffer("");
        byte[] buffer = new byte[1024];
        int n = 0;
        // Similar to writing into a file, this is read operation from a file
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
        // Basic thing is we need to build an object of type Cursor and return it
        // As recommended lets create a Matrix Cursor object
        // So this is a query method, some requirements are given as part of the arguments, using
        // which we need to retrieve values from our file and store it in the cursor object and return it.
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
        /* addRow(Object[] columnValues)
        Adds a new row to the end with the given column values.*/
        String[] columnValues = {selection, value};
        cursor.addRow(columnValues);
        Log.v("query", selection);
        return cursor;
    }
}
