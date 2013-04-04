package unr.t2i.arcns;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/*
 * Get the BluetoothAdapter
 * Enable Bluetooth
 * If connecting to a Bluetooth serial board then use SPP UUID 00001101-0000-1000-8000-00805F9B34FB
 *  
 */

public class MainActivity extends Activity {

	protected enum ArcnsError {NO_BT_FOUND, UNKNOWN};
	protected final int REQUEST_ENABLE_BT = 1;
	
	// Bluetooth Adapter, this gets set to the default (since there's only one) 
	protected BluetoothAdapter btAdapter;
	
	protected final String BT_ADAPTER = "unr.t2i.arcns.MainActivity.BT_ADAPTER";
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_main);
		
		// attach listener to send message when Done is pressed 
		EditText inputText = (EditText) findViewById(R.id.message_text);
		inputText.setImeActionLabel(getString(R.string.send), KeyEvent.KEYCODE_ENTER);
		inputText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
					// intercept the key up event but don't write another method
					// this prevents the keyboard from closing
					if (v.getText().toString().length() > 0) {
						TextView messageTerm = (TextView) findViewById(R.id.text_view);
						messageTerm.setText(messageTerm.getText().toString() +
							"\nSent    : " + v.getText().toString());
						v.setText("");
					}
					return true;
				}
				return false;
			}
		});
		
		// initialize BlueTooth
		initBluetooth();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings_menu, menu);
		
		return true;
	}
	
	
	/** called once BlueTooth is enabled */
	public void enableInterface()
	{
		
	}
	
	// called when activity for result finishes
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode)
		{
			case REQUEST_ENABLE_BT:
				// If BT isn't enabled ask again, if dialog is canceled then exit
				if ( resultCode == Activity.RESULT_OK ) {
					enableInterface();
				} else {
					String message = getString(R.string.bt_enable_failed);
					AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
					final MainActivity myself = this;
					dlgAlert.setTitle(getString(R.string.app_name));
					dlgAlert.setMessage(message);
					dlgAlert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
							startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
						}
					});
					dlgAlert.setNegativeButton(getString(R.string.exit), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							myself.finish();
						}
					});
					dlgAlert.show();
				}
				break;
		}
	}
		
	protected void initBluetooth() {
		// Initialize the BlueTooth
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if ( btAdapter == null ) {	
			fatalError(ArcnsError.NO_BT_FOUND);
			return;
		}
		
		// Enable BlueTooth if it is off
		if ( !btAdapter.isEnabled() ) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);			
		} else {
			enableInterface();
		}
	}

	/** Display message then exit app */
	protected void fatalError(ArcnsError type) {
		String message = "Error: ";
		switch(type)
		{
			case NO_BT_FOUND:
				message += getString(R.string.bt_not_found);
				break;
			case UNKNOWN:
				message += getString(R.string.unknown_error);
				break;
		}
		
		AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
		final MainActivity myself = this;
		dlgAlert.setTitle(getString(R.string.app_name));
		dlgAlert.setMessage(message);
		dlgAlert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				myself.finish();
			}
		});
		dlgAlert.setCancelable(false);
		dlgAlert.create().show();
	}
}
