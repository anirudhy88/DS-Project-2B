package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InterfaceAddress;
import java.util.ArrayList;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
/*

References for the ISIS algorithm for total ordering included :
1. Lecture slides
2. Birman, K. and Joseph, T.  Reliable Communication in the Presence of Failures. In ACM Transactions on Computer Systems 5, 1 (February 1987), 47-76
3. Coulouris, G., Dollimore, J. and Kindberg, T.  Distributed Systems: Concepts and Design. 4thedition (2005), Addison-Wesley.
4. https://courses.engr.illinois.edu/cs425/fa2009/L5tmp.ppt
5. https://studylib.net/doc/7830646/isis-algorithm-for-total-ordering-of-messages
Though the mentioned references have been used for algorithm understanding, the implementation is completely self done.
 */
class Message implements Comparable<Message>{
    String message = "";
    Integer messageId = 0;
    Integer fromProcessId = 0;
    Integer finalCount = Integer.MAX_VALUE;
    Integer myProcess = 0;
    Boolean delivered = false;
    Message(String a, Integer b, Integer c, Integer d, Integer e, Boolean f) {
        message = a;
        messageId = b;
        fromProcessId = c;
        finalCount = d;
        myProcess = e;
        delivered = f;
    }
    public int compareTo(Message m1) {
        if(finalCount == m1.finalCount) {
            if(delivered == true && m1.delivered == true) {
                return fromProcessId - m1.fromProcessId;
            } else if(delivered == false && m1.delivered == true) {
                return -1;
            } else if(delivered == true && m1.delivered == false) {
                return 1;
            } else {
                return 1;
            }

        } else {
            return finalCount - m1.finalCount;
        }
    }
}
class Seq {
    Integer seqNum = 0;
    Integer processId = 0;
}
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static int sequenceNum = 0;
    private static int serverSideCount = 0; // s(i)
    private static int clientSideCount = 0; // counter(i)
    private static ArrayList<Seq> testList;
    private static ArrayList<String> serverList;
    static final int SERVER_PORT = 10000;
    private static List<Message> queue = new LinkedList<Message>();
    private static final HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
    private static final HashMap<String, Boolean> map2 = new HashMap<String, Boolean>();
    private static int myPor = 0;
    private static int brokenProcessId = 0;
    private static Boolean broken = false;
    private static long waitTime = 0;
    private static long endTime = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        // Getting the port number of myself(AVD)
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        myPor = Integer.parseInt(myPort);

        // As we did in the PA1, lets create a server asynTask and client asyncTask
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch(IOException e) {
            Log.e(TAG, "Error in creating a socket");
            return;
        }
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString();
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                remoteTextView.append("\n");

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket server = null;
            String msg = null;
            while(true) {
                try {
                    Log.i(TAG, "Server Task : Waiting for a connection");
                    waitTime = System.currentTimeMillis();
                    Log.i(TAG, "Wait time is :" + waitTime );
                    server = serverSocket.accept();
                    InputStream is = server.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    StringBuilder sb2 = new StringBuilder();
                    String line = "";
                    String line2 = "";
                    String actualString = "";
                    //Log.i(TAG, "Server Task : Just before read line");
                    line = br.readLine();

                    //Log.i(TAG, "Server Task : Just after read line");
                    sb.append(line);
                    String[] stringArray = sb.toString().split(" ");
                    Log.i(TAG, "Server Task : string array received is :"+sb.toString());
                    if(stringArray[0].contains("test")) {
                        Log.i(TAG, "Server Task : Contains test");
                        serverSideCount++;
                        String msgToSend = stringArray[2] + " " + serverSideCount + "\n";
                        Log.i(TAG, "Server Task : Sending message :"+msgToSend);
                        //Log.i(TAG, "Server Task : msg to send : " + msgToSend);
                        DataOutputStream outToServer = new DataOutputStream(server.getOutputStream());
                        outToServer.writeBytes(msgToSend);


                        Message message = new Message(stringArray[1], Integer.parseInt(stringArray[2]),
                                Integer.parseInt(stringArray[3]), (serverSideCount), myPor, false);
                        //   message.delivered = false;
                        //Log.i(TAG, "Server Task : Just before adding message to queue ");
                        queue.add(message);
                        Log.i(TAG, "Server Task : Just after adding message:"+message.message+"to queue size :"+queue.size());
                        Collections.sort(queue);
                    } else if(stringArray[0].equals("max")){
                        Log.i(TAG, "Server Task : Contains max, queue size is :" + queue.size());
                        serverSideCount = Math.max(serverSideCount, Integer.parseInt(stringArray[3]));
                        //Log.i(TAG, "Server Task")

                        for(Message mg : queue) {
                            Log.i(TAG, "Server Task : Message id is :"+mg.messageId + " and fromProcessId is :"+ mg.fromProcessId);
                            Log.i(TAG, "Server Task : Integer.parseInt(stringArray[2]) : " + Integer.parseInt(stringArray[2]));
                            if(Integer.parseInt(stringArray[1]) == (mg.messageId) && Integer.parseInt(stringArray[2]) == mg.fromProcessId) {
                                Log.i(TAG,"Server Task : Found match so changing it to deliverable in queue ");
                                mg.finalCount = Integer.parseInt(stringArray[3]);
                                mg.myProcess = Integer.parseInt(stringArray[4]);
                                mg.delivered = true;
                                Log.i(TAG, "CServer Task : Message id is :"+mg.messageId + " and fromProcessId is :"+ mg.fromProcessId
                                        + " message is :" + mg.message + " max is : " + mg.finalCount);
                            }
                        }

                        Collections.sort(queue);
                        Log.i(TAG, "Server Task : sending ACK MSG");
                        DataOutputStream outToServer2 = new DataOutputStream(server.getOutputStream());
                        outToServer2.writeBytes("ACK MSG");
                        List<Message> newList = new LinkedList<Message>();
                        Boolean youHaveToContinue = false;
                        Log.i(TAG, "Anything broken :" + broken + " which one :"+ brokenProcessId);
                        for(Message mg : queue) {
                            Log.i(TAG, "Checking msg :"+mg.message + " delivered :" + mg.delivered);
                            if(mg.delivered) {
                                Log.i(TAG, "Server Task : publishing progress for :" + mg.message);
                                publishProgress(mg.message);
                                //queue.remove(mg);
                                newList.add(mg);
                            } else {
                                if(broken && brokenProcessId == 0)
                                    if(mg.fromProcessId == 11108) {
                                        newList.add(mg);
                                        youHaveToContinue = true;
                                    }
                                if(broken && brokenProcessId == 1)
                                    if(mg.fromProcessId == 11112) {
                                        newList.add(mg);
                                        youHaveToContinue = true;
                                    }
                                if(broken && brokenProcessId == 2)
                                    if(mg.fromProcessId == 11116) {
                                        newList.add(mg);
                                        youHaveToContinue = true;
                                    }
                                if(broken && brokenProcessId == 3)
                                    if(mg.fromProcessId == 11120) {
                                        newList.add(mg);
                                        youHaveToContinue = true;
                                    }
                                if(broken && brokenProcessId == 4)
                                    if(mg.fromProcessId == 11124) {
                                        newList.add(mg);
                                        youHaveToContinue = true;
                                    }
                                if(!youHaveToContinue)
                                    break;
                                youHaveToContinue = false;
                            }
                        }
                        queue.removeAll(newList);
                    } else if(stringArray[0].equals("broken")){
                        Log.i(TAG, " Something is broken :" + stringArray[1]);
                        broken = true;
                        brokenProcessId = Integer.parseInt(stringArray[1]);
                        List<Message> newList2 = new LinkedList<Message>();
                        Integer temp = 0;
                        Boolean youHaveToContinue2 = false;
                        switch(brokenProcessId) {
                            case 0:
                                temp = Integer.parseInt(REMOTE_PORT0);
                                break;
                            case 1:
                                temp = Integer.parseInt(REMOTE_PORT1);
                                break;
                            case 2:
                                temp = Integer.parseInt(REMOTE_PORT2);
                                break;
                            case 3:
                                temp = Integer.parseInt(REMOTE_PORT3);
                                break;
                            case 4:
                                temp = Integer.parseInt(REMOTE_PORT4);
                                break;

                        }
                        for(Message mg : queue) {
                            Log.i(TAG, "Checking msg :"+mg.message + " delivered :" + mg.delivered);
                            if (mg.fromProcessId == temp) {
                                Log.i(TAG, " Adding "+ mg.message + " to the remove list");
                                newList2.add(mg);
                            }
                        }
                        queue.removeAll(newList2);


                        for(Message mg : queue) {
                            Log.i(TAG, "Checking msg :"+mg.message + " delivered :" + mg.delivered);
                            if(mg.delivered) {
                                Log.i(TAG, "Server Task : publishing progress for :" + mg.message);
                                publishProgress(mg.message);
                                //queue.remove(mg);
                                newList2.add(mg);
                            } else {
                                if(broken && brokenProcessId == 0)
                                    if(mg.fromProcessId == 11108) {
                                        newList2.add(mg);
                                        youHaveToContinue2 = true;
                                    }
                                if(broken && brokenProcessId == 1)
                                    if(mg.fromProcessId == 11112) {
                                        newList2.add(mg);
                                        youHaveToContinue2 = true;
                                    }
                                if(broken && brokenProcessId == 2)
                                    if(mg.fromProcessId == 11116) {
                                        newList2.add(mg);
                                        youHaveToContinue2 = true;
                                    }
                                if(broken && brokenProcessId == 3)
                                    if(mg.fromProcessId == 11120) {
                                        newList2.add(mg);
                                        youHaveToContinue2 = true;
                                    }
                                if(broken && brokenProcessId == 4)
                                    if(mg.fromProcessId == 11124) {
                                        newList2.add(mg);
                                        youHaveToContinue2 = true;
                                    }
                                if(!youHaveToContinue2)
                                    break;
                                youHaveToContinue2 = false;
                            }
                        }
                        queue.removeAll(newList2);

                    } else {
                        DataOutputStream outToServer2 = new DataOutputStream(server.getOutputStream());
                        outToServer2.writeBytes("ACKA MSG");
                    }
                    endTime = System.currentTimeMillis();
                    Log.i(TAG,"end time is :"+ endTime);
                    server.close();
                    //    Log.i(TAG, "Server Task : just before break");
                } catch (IOException e) {
                    Log.e(TAG, "publish progress failed");
                }
            }
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            //String[] str = strReceived.split(" ");
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");
            // insert into database
            // Need to create Uri object and contentValue to insert

            //Building URI object
            Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
            // Building ContentValues Object
            ContentValues contVal = new ContentValues();
            contVal.put(KEY_FIELD, Integer.toString(sequenceNum));
            contVal.put(VALUE_FIELD,strReceived);
            sequenceNum++;
            // Inserting
            getContentResolver().insert(uri, contVal);
            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                // So need to send messages for all of the AVD's including myself
                // The below code has been taken from my PA1
                //Log.i(TAG, "client task before socket creation");
                Log.i(TAG, "Difference is :"+ String.valueOf(waitTime - endTime));
                String[] remotePorts = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
                testList = new ArrayList<Seq>();
                int incrementNum = 0;
                for (int i = 0; i < 5; i++) {
                    if(broken && brokenProcessId == i) {
                        Log.i(TAG, "Client Task : Continuing for processID :"+ brokenProcessId);
                        continue;
                    }
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePorts[i]));
                    Log.i(TAG, "Client Task : socket selected for i :" + i);
                    //String msgToSend = "test " + msgs[0];
                    clientSideCount++;
                    Log.i(TAG, "Client Task : client Side Counter incremented :"+ clientSideCount);
                    incrementNum++;
                    String msgToSend = "test" + " " + msgs[0] + " " + String.valueOf(clientSideCount) + " " + msgs[1] + "\n";
                    map.put(i, clientSideCount);
                    Log.i(TAG, "Client Task : msgToSend for i :" + i + " is :" + msgToSend);
                    DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
                    outToServer.writeBytes(msgToSend);
                    outToServer.flush();

                    InputStream is = socket.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line = "";
                    line = br.readLine();
                    sb.append(line);

                    String[] stringArray = sb.toString().split(" ");
                    //Log.i(TAG, "Client Task : Msg with seqNum is " + stringArray[0] + " " + stringArray[1]);
                    Log.i(TAG, "Client Task : The message from server is :"+ sb.toString());
                    Seq seq = new Seq();
                    Integer fromProcess = 0;
                    switch(i) {
                        case 0:
                            fromProcess = Integer.parseInt(REMOTE_PORT0);
                            break;
                        case 1:
                            fromProcess = Integer.parseInt(REMOTE_PORT1);
                            break;
                        case 2:
                            fromProcess = Integer.parseInt(REMOTE_PORT2);
                            break;
                        case 3:
                            fromProcess = Integer.parseInt(REMOTE_PORT3);
                            break;
                        case 4:
                            fromProcess = Integer.parseInt(REMOTE_PORT4);
                            break;
                    }

                    seq.processId = fromProcess;
                    // Log.i(TAG, " sb is :")
                    if(line != null) {
                        seq.seqNum = Integer.parseInt(stringArray[1]);
                        testList.add(seq);
                        Log.i(TAG,"Client Task : Adding testList : from process Id :"+ seq.processId + " and seqNum as :"+seq.seqNum);
                    } else {
                        brokenProcessId = i;
                        broken = true;
                        //socket.close();
                        for (int z = 0; z < 5; z++) {
                           // socket.close();
                            if(z == brokenProcessId) continue;
                            Socket socketx = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePorts[z]));

                            String msgToSendx = "broken" + " " + String.valueOf(brokenProcessId)+ "\n";
                            Log.i(TAG, "Client Task : msgToSend for i :" + z + " is :" + msgToSendx);
                            DataOutputStream outToServerx = new DataOutputStream(socketx.getOutputStream());
                            outToServerx.writeBytes(msgToSendx);
                            outToServerx.flush();

                            InputStream isx = socketx.getInputStream();
                            BufferedReader brx = new BufferedReader(new InputStreamReader(isx));
                            StringBuilder sbx = new StringBuilder();
                            String linex = "";
                            linex = brx.readLine();
                            sbx.append(line);
                            socketx.close();
                        }
                    }
                    socket.close();
                }
                int maxSeqNum = Integer.MIN_VALUE;
                int minProcessId = Integer.MAX_VALUE;
                for(Seq seq : testList) {
                    Log.i(TAG, "testList verification : seqNum is :"+seq.seqNum + " and fromprocessId is : "+ seq.processId);
                    if(seq.seqNum > maxSeqNum)
                        maxSeqNum = seq.seqNum;
                }

                for(Seq seq : testList) {
                    if(seq.seqNum == maxSeqNum)
                        if(seq.processId < minProcessId)
                            minProcessId = seq.processId;
                }
                Log.i(TAG, "Client Task : size equalled : " + testList.size());
                Log.i(TAG, "Chosen max value is :" + maxSeqNum +" and chose min Process Id is :" + minProcessId);
                int temp = 0;
                // temp = clientSideCount-4;
                temp = clientSideCount-incrementNum+1;
                for (int j = 0; j < 5; j++) {
                    if(broken && j == brokenProcessId) continue;
                    Socket socketS = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePorts[j]));

                    String msgToSend2 = "max " + String.valueOf(map.get(j)) + " " + myPor + " " +String.valueOf(maxSeqNum)
                            + " " + String.valueOf(minProcessId) + "\n";
                    Log.i(TAG, "Client Task : max msg to send " + msgToSend2);
                    DataOutputStream outToServer2 = new DataOutputStream(socketS.getOutputStream());
                    Log.i(TAG,"Client Task : after data output stream");
                    outToServer2.writeBytes(msgToSend2);
                    Log.i(TAG,"Client Task : after write bytes");
                    outToServer2.flush();
                    temp++;

                    InputStream is2 = socketS.getInputStream();
                    Log.i(TAG,"Client Task : after input stream");
                    BufferedReader br2 = new BufferedReader(new InputStreamReader(is2));
                    StringBuilder sb2 = new StringBuilder();
                    String line2 = "";
                    line2 = br2.readLine();
                    sb2.append(line2);
                    if(line2 == null) {
                        //socketS.close();
                        Log.i(TAG,"It's broken and found here :"+ j);
                        brokenProcessId = j;
                        broken = true;
                        for (int z = 0; z < 5; z++) {
                            Log.i(TAG, "Not receiving response for max:"+z);
                            if(z == brokenProcessId) continue;
                            Socket socketx = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePorts[z]));


                            String msgToSendx = "broken" + " " + String.valueOf(brokenProcessId)+ "\n";
                            Log.i(TAG, "Client Task :msgToSend for i :" + z + " is :" + msgToSendx);
                            DataOutputStream outToServerx = new DataOutputStream(socketx.getOutputStream());
                            outToServerx.writeBytes(msgToSendx);
                            outToServerx.flush();

                            InputStream isx = socketx.getInputStream();
                            BufferedReader brx = new BufferedReader(new InputStreamReader(isx));
                            StringBuilder sbx = new StringBuilder();
                            String linex = "";
                            linex = brx.readLine();
                            sbx.append(linex);
                            socketx.close();
                        }
                    }

                    String[] stringArray2 = sb2.toString().split(" ");
                    Log.i(TAG, "Client Task : Msg with seqNum is " + sb2.toString());
                    //testList.add(Integer.parseInt(stringArray2[1]));
                    socketS.close();
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }
    }
}
