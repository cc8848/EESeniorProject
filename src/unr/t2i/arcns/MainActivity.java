package unr.t2i.arcns;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

/*
 * Get the BluetoothAdapter
 * Enable BlueTooth
 * If connecting to a BlueTooth serial board then use SPP UUID 00001101-0000-1000-8000-00805F9B34FB
 *  
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MainActivity extends Activity {

    protected enum ArcnsError {
        NO_BT_FOUND, UNKNOWN
    };

    // /////// Class Types ///////////////////
    static protected ArrayAdapter<String> mPairAdapter;
    // static private String mConnectedDeviceName = null;
    // static private StringBuffer mOutStringBuffer;

    // bluetooth adapter/connection info
    static protected BluetoothAdapter mBtAdapter = null;
    static protected BluetoothDevice mBtDevice = null;
    static protected BluetoothSocket mBtSocket = null;

    // input stream
    static protected InputStream mBtInputStream = null;

    protected Thread mBtListenThread = null;
    protected volatile boolean mBtStopListen = false;
    protected int mBtReadPosition;
    protected byte[] mBtReadBuffer;

    static protected volatile ArrayList<String> mBtMessageBuffer;
    // ///////////////////////////////////////

    // /////// Class Constants ///////////////
    protected final int REQUEST_ENABLE_BT = 1;
    protected final String BT_ADAPTER = "unr.t2i.arcns.MainActivity.BT_ADAPTER";
    protected final int REQUEST_CONNECT_DEVICE = 2;

    // ///////////////////////////////////////

    /** program entry */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        mBtMessageBuffer = new ArrayList<String>();

        ListView messageList = (ListView) findViewById(R.id.message_list);
        final StableArrayAdapter adapter = new StableArrayAdapter(this,
                android.R.layout.simple_list_item_1, mBtMessageBuffer);
        messageList.setAdapter(adapter);

        initBluetooth();
    }
    
    // sends an SMS text message to another device
    private void sendSMS(String phoneNumber, String message)
    {        
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
 
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
            new Intent(SENT), 0);
 
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
            new Intent(DELIVERED), 0);
 
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off", 
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));
 
        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered", 
                                Toast.LENGTH_SHORT).show();
                        break;                        
                }
            }
        }, new IntentFilter(DELIVERED));        
 
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);        
    }

    @Override
    protected synchronized void onStart() {
        super.onStart();

        // re-enable BT if user has shut it off
        if (!mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    /** called after onCreate */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.connect_pairs_menu:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
            /*
             * case R.id.discoverable: // Ensure this device is discoverable by
             * others ensureDiscoverable(); return true;
             */
        }
        return false;
    }

    protected void connectBt() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        mBtSocket = mBtDevice.createRfcommSocketToServiceRecord(uuid);
        
        try {
            mBtSocket.connect();
        } catch (IOException e) {
            Toast.makeText(this, "Unable to connect", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (mBtSocket.isConnected()) {
            // mBtOutputStream = mBtSocket.getOutputStream();
            mBtInputStream = mBtSocket.getInputStream();
            btListen();    
        } else {
            Toast.makeText(this, "Unable to connect", Toast.LENGTH_LONG).show();
            return;
        }
        
        Toast.makeText(this, "Connection Successful!", Toast.LENGTH_LONG).show();
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

    protected void processLowImpact()
    {
        EditText txtPhoneNo = (EditText) findViewById(R.id.phone_number_text);
        
        String phoneNo = txtPhoneNo.getText().toString();                 
        if (phoneNo.length()>0 )                
            sendSMS(phoneNo, "Low impact detected");                
        else
            Toast.makeText(getBaseContext(), 
                "Low Impact Detected but no phone number provided.", 
                Toast.LENGTH_SHORT).show();
    }

    protected void processMediumImpact()
    {
        EditText txtPhoneNo = (EditText) findViewById(R.id.phone_number_text);
        
        String phoneNo = txtPhoneNo.getText().toString();                 
        if (phoneNo.length()>0 )                
            sendSMS(phoneNo, "Medium impact detected");                
        else
            Toast.makeText(getBaseContext(), 
                "Medium Impact Detected but no phone number provided.", 
                Toast.LENGTH_SHORT).show();        
    }

    protected void processHighImpact()
    {
        EditText txtPhoneNo = (EditText) findViewById(R.id.phone_number_text);
        
        String phoneNo = txtPhoneNo.getText().toString();                 
        if (phoneNo.length()>0 )                
            sendSMS(phoneNo, "High impact detected");                
        else
            Toast.makeText(getBaseContext(), 
                "High Impact Detected but no phone number provided.", 
                Toast.LENGTH_SHORT).show();        
    }
    
    protected void processData() {
        // add info to list
        if (mBtMessageBuffer.size() > 0) {
            ListView messageList = (ListView) findViewById(R.id.message_list);
            final StableArrayAdapter adapter = new StableArrayAdapter(this,
                    android.R.layout.simple_list_item_1, mBtMessageBuffer);
            messageList.setAdapter(adapter);
            messageList.setSelection(messageList.getCount() - 1);
        }
        
        // process most recent message
        String msg = mBtMessageBuffer.get(mBtMessageBuffer.size()-1);
        
        if ( msg == "l" )
        {
            processLowImpact();
        } else if ( msg == "m" ) {
            processMediumImpact();
        } else if ( msg == "h" ) {
            processHighImpact();
        }
        
    }

    protected void btListen() {
        final Handler handler = new Handler();
        final byte delimiter = 10; // This is the ASCII code for a newline
                                   // character
        final Object main = this;
        // myReceive.setText("test");
        mBtStopListen = false;
        mBtReadPosition = 0;
        mBtReadBuffer = new byte[1024];
        mBtListenThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()
                        && !mBtStopListen) {
                    try {
                        int bytesAvailable = mBtInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mBtInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    // myReceive.setText("got it");
                                    byte[] encodedBytes = new byte[mBtReadPosition];
                                    System.arraycopy(mBtReadBuffer, 0,
                                            encodedBytes, 0,
                                            encodedBytes.length);
                                    final String data = new String(
                                            encodedBytes, "US-ASCII");
                                    mBtReadPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            // add message to buffer
                                            synchronized (main) {
                                                mBtMessageBuffer.add(data);
                                                processData();
                                            }
                                        }
                                    });
                                } else {
                                    mBtReadBuffer[mBtReadPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        mBtStopListen = true;
                    }
                }
            }
        });

        // start the thread
        mBtListenThread.start();
    }

    /** called when activity for result finishes */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras().getString(
                        DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                mBtDevice = mBtAdapter.getRemoteDevice(address);

                try {
                    connectBt();
                } catch (IOException e) {
                    // TODO: handle exception
                }
            }
            break;
        case REQUEST_ENABLE_BT:
            // If BT isn't enabled ask again, if dialog is canceled then exit
            if (resultCode == Activity.RESULT_OK) {
                enableInterface();
            } else {
                String message = getString(R.string.bt_enable_failed);
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                final MainActivity myself = this;
                dlgAlert.setTitle(getString(R.string.app_name));
                dlgAlert.setMessage(message);
                dlgAlert.setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                Intent enableBtIntent = new Intent(
                                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(enableBtIntent,
                                        REQUEST_ENABLE_BT);
                            }
                        });
                dlgAlert.setNegativeButton(getString(R.string.exit),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                                myself.finish();
                            }
                        });
                dlgAlert.show();
            }
            break;
        }
    }

    /** called when the connect button is clicked */
    public void btnTerminalClick(View view) {
        /*
        EditText inputText = (EditText) findViewById(R.id.message_text);
        String data = inputText.getText().toString();
        // add message to buffer
        synchronized (this) {
            mBtMessageBuffer.add(data);
            processData();
        }
        */
        EditText txtPhoneNo = (EditText) findViewById(R.id.phone_number_text);
        EditText txtMessage = (EditText) findViewById(R.id.message_text);
        
        String phoneNo = txtPhoneNo.getText().toString();
        String message = txtMessage.getText().toString();                 
        if (phoneNo.length()>0 && message.length()>0)                
            sendSMS(phoneNo, message);                
        else
            Toast.makeText(getBaseContext(), 
                "Please enter both phone number and message.", 
                Toast.LENGTH_SHORT).show();
    }

    /** Hide the keyboard */
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (activity.getCurrentFocus() == null) {
            inputMethodManager.toggleSoftInput(
                    InputMethodManager.HIDE_NOT_ALWAYS, 0);
        } else {
            inputMethodManager.hideSoftInputFromInputMethod(activity
                    .getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /** set up the listener for input text */
    /*
     * protected void initInputText() { EditText inputText = (EditText)
     * findViewById(R.id.message_text);
     * inputText.setImeActionLabel(getString(R.string.send),
     * KeyEvent.KEYCODE_ENTER); inputText.setOnEditorActionListener(new
     * EditText.OnEditorActionListener() {
     * 
     * @Override public boolean onEditorAction(TextView v, int actionId,
     * KeyEvent event) { boolean handled = false; if (actionId ==
     * EditorInfo.IME_NULL && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) { //
     * intercept the key up event but don't write another method // this
     * prevents the keyboard from closing if (v.getText().toString().length() >
     * 0) { final TextView messageTerm = (TextView)
     * findViewById(R.id.text_view); messageTerm.append("\nSent    : " +
     * v.getText().toString()); v.setText("");
     * 
     * // scroll to zero messageTerm.post(new Runnable() { public void run() {
     * if (messageTerm.getLayout() != null ) { final int scrollAmount =
     * messageTerm.getLayout().getLineTop(messageTerm.getLineCount()) -
     * messageTerm.getHeight(); if ( scrollAmount > 0) messageTerm.scrollTo(0,
     * scrollAmount); } } }); } handled = true; } // if not handled, return
     * false and allow other listener to handle return handled; } });
     * 
     * final MainActivity myself = this; inputText.setOnFocusChangeListener(new
     * View.OnFocusChangeListener() {
     * 
     * @Override public void onFocusChange(View v, boolean hasFocus) { if
     * (!hasFocus) { hideSoftKeyboard(myself); } } }); }
     */

    /** initialize BlueTooth */
    protected void initBluetooth() {
        // Initialize the BlueTooth
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            fatalError(ArcnsError.NO_BT_FOUND);
            return;
        }

        // Enable BlueTooth if it is off
        if (!mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            enableInterface();
        }
    }

    /** called once BlueTooth is enabled */
    public void enableInterface() {
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        mPairAdapter = new ArrayAdapter<String>(this,
                R.layout.activity_pair_list);

        // If there are devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add name to list of names
                mPairAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    /** Display message then exit app */
    protected void fatalError(ArcnsError type) {
        String message = "Error: ";
        switch (type) {
        case NO_BT_FOUND:
            message += getString(R.string.bt_not_found);
            break;
        case UNKNOWN:
            message += getString(R.string.unknown_error);
            break;
        }

        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        final MainActivity myself = this;

        dlgAlert.setTitle(getString(R.string.app_name))
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                                myself.finish();
                            }
                        }).setCancelable(false).create().show();
    }
}
