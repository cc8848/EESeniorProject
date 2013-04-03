package unr.t2i.arcns;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	private enum BTErrorType {NO_BT_FOUND};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_main);
		
		// initialize BlueTooth
		initBluetooth();
	}
		
	protected void initBluetooth() {
		// Initialize the BlueTooth
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		if ( btAdapter == null ) {	
			displayBTError(BTErrorType.NO_BT_FOUND);
			return;
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
	

	protected void displayBTError(BTErrorType type) {
		String message = "Error: ";
		switch(type)
		{
			case NO_BT_FOUND:
				message += getString(R.string.no_bt_error);
				break;
			default:
				message += getString(R.string.unknown_bt_error);
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
