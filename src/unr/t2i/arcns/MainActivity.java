package unr.t2i.arcns;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	private enum ArcnsError {NO_BT_FOUND, UNKNOWN};

	public final int REQUEST_ENABLE_BT = 1;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_main);

		// initialize BlueTooth
		initBluetooth();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
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
	
	public void sendBluetooth(String message) {
		TextView view = (TextView) findViewById(R.id.text_view);
		
		String new_message = view.getText().toString() + "\nSent    : " + message;
		view.setText(new_message);
	}
	
	public void sendMessage(View view) {
		EditText message_text = (EditText) findViewById(R.id.message_text);
		String message = message_text.getText().toString();
		
		sendBluetooth(message);

		message_text.setText("");
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
