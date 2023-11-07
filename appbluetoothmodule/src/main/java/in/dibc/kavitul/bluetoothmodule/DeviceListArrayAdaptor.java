package in.dibc.kavitul.bluetoothmodule;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class DeviceListArrayAdaptor extends ArrayAdapter<BluetoothDevice> {
    String TAG = "DIBC_Bluetooth_DeviceListArrayAdaptor";
    private Context mcontext;
    private List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();

    public DeviceListArrayAdaptor(@NonNull Context context, int bluetooth_device_list_item, ArrayList<BluetoothDevice> bluetoothDevices) {
        super(context, bluetooth_device_list_item, bluetoothDevices);
        mcontext = context;
        devices = bluetoothDevices;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if(rowView == null) {
            Log.d(TAG, "getView: rowview = nulll");
               rowView = LayoutInflater.from(mcontext).inflate(R.layout.bluetooth_device_list_item, parent, false);
        }

        BluetoothDevice dev = devices.get(position);

        TextView textViewName = rowView.findViewById(R.id.txt_deviceName);
        textViewName.setText(devices.get(position).getName());
        TextView textViewAddress = rowView.findViewById(R.id.txt_deviceAddress);
        textViewAddress.setText(devices.get(position).getAddress());
        ImageView imageView = rowView.findViewById(R.id.ic_blutooth_connected);
        // change the icon for bonded devices
        if (devices.get(position).getBondState() == 0) {
            imageView.setImageResource(R.drawable.ic_bluetooth_connected_black_24dp);
        } else {
            imageView.setImageResource(R.drawable.ic_bluetooth_black_24dp);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "dev: "+devices.get(position).getName()+"=="+devices.get(position).getAddress()+"=="+devices.get(position).getBluetoothClass()+"=="+devices.get(position).getBondState()+"=="+devices.get(position).getType()+"=="+devices.get(position).getClass()+"=="+devices.get(position).getUuids());
        }
        return rowView;
    }

}
