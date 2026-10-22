package in.dibc.kavitul.bluetoothmodule;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AppDeviceConnectedThread extends Thread {
    public AppBluetoothHelper btHelper;
    private final String TAG = "DIBC_Bt_ConnectedThread";
    public final BluetoothSocket bluetoothSocket;
    private final InputStream inStream;
    private final OutputStream outStream;
    private final String rx_buffer = "";
    private byte[] last_packet;
    private final int repeat_limit = 15;
    private int count = 0;


    public AppDeviceConnectedThread(BluetoothSocket btsoc) {
        super("ConnectedThread");
        bluetoothSocket = btsoc;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        try {
            tmpIn = bluetoothSocket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        inStream = tmpIn;
        outStream = tmpOut;
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format(" 0x%02x", b));
        return sb.toString();
    }

    public static String charArrayToHex(char[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (char b : a)
            sb.append(String.format(" 0x%02x", (byte) b));
        return sb.toString();
    }

    public void run() {

        int numBytes; // bytes returned from read()
        Log.d(TAG, "run: ");
        // Keep listening to the InputStream until an exception occurs.
        while (!this.isInterrupted()) {
            // Make sure things haven't gone wrong
            if ((inStream == null) || (outStream == null)) {
                Log.e(TAG, "Lost bluetooth connection!");
                break;
            }
            // Read data and add it to the buffer
            if(bluetoothSocket.isConnected()) {
                String s = read();

                //noinspection StringConcatenationInLoop
//                if (s.length() > 0)
//                    rx_buffer += s;
            }

        }

//        Handler hnd = .getThreadHandler();
//        hnd.sendMessage(hnd.obtainMessage(BluetoothThread.SEND_CODE, 0, 0, null));

        // If thread is interrupted, close connections
        disconnect();
        Log.d(TAG, "run: disconnected");


    }

    /**
     * Return data read from the socket, or a blank string.
     */
    private String read() {
        String s = "";

        try {
            // Check if there are bytes available
            if (inStream.available() > 0) {
                // Read bytes into a buffer
//                byte[] inBuffer = new byte[128]; // 1 byte buffer. byte[1024] = buffer of 1024 bytes
//                int bytesRead = inStream.read(inBuffzzzer);
                byte[] oneByteRead = new byte[1];
                byte[] statusBytesRead = new byte[btHelper.LOSB];
                byte[] dataBytesRead = new byte[btHelper.LODB];


                oneByteRead = readBytes(1);
                int msgLen = 0;

                if ((oneByteRead[0] & 0xff) == btHelper.SORa || (oneByteRead[0] & 0xff) == btHelper.SORb) {
                    /* & 0xff to convert to unsigned while comparing */
                    if ((oneByteRead[0] & 0xff) == btHelper.SORa) {
                        statusBytesRead = readBytes(btHelper.LOSB);
                    }
//                    dataBytesRead = readBytes(btHelper.LODB);
                    char[] dataCharRead = readChar(btHelper.LODB);
//                    rx_buffer += charArrayToHex(dataCharRead);
//                    rx_buffer += charArrayToHex(dataCharRead);
                    oneByteRead = readBytes(1);
                    if ((oneByteRead[0] & 0xff) == btHelper.EOR) {
                        Message msg = btHelper.mHandlersbluetooth.obtainMessage();
                        Bundle bundle = new Bundle();
                        bundle.putByte("packetType", oneByteRead[0]);
                        bundle.putByteArray("statusBytes", statusBytesRead);
                        bundle.putCharArray("dataChar", dataCharRead);
//                        Log.v("Packet Type :: ", ""+oneByteRead[0]);
                        msg.arg1 = count++;
//                        Log.d(TAG, "read: count"+count);
//                        Log.d(TAG, "read: "+byteArrayToHex(ch1));
                        msg.what = 1; // ECG_DATA
                        msg.setData(bundle);
                        btHelper.mHandlersbluetooth.sendMessage(msg);
                        sendAck(true);

//                        if(count>10000){
//                            outStream.write(0x53);
//                            Log.d(TAG, "read 10k bytes: "+rx_buffer);
//                        }
                    } else {
                        sendAck(false);
                        Log.d(TAG, "read: packet mismatch " + charArrayToHex(dataCharRead));
                        //skip till next end of packet..
                        for (int i = 0; i < btHelper.LODB; i++) {
                            oneByteRead = readBytes(1);
//                            Log.d(TAG, "read: skipping " + byteArrayToHex(oneByteRead));
                            if ((oneByteRead[0] & 0xff) == btHelper.EOR) {
                                break;
                            }
                        }
                    }
                } else if (oneByteRead[0] == 0x20) {
                    Message msg = btHelper.mHandlersbluetooth.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putInt("deviceResponce", -4);
                    bundle.putInt("deviceVersion", 2);
                    msg.what = 2; // VER_DATA
                    msg.setData(bundle);
                    btHelper.mHandlersbluetooth.sendMessage(msg);
                }

//
            }

        } catch (Exception e) {
            Log.e(TAG, "Read failed!", e);
        }

        return s;
    }

    public void sendAck(boolean status) {
        if (btHelper.LODB == 16 || count%500 == 0) {
            byte[] oneByteWrite = new byte[1];
            oneByteWrite[0] = (byte) (status ? btHelper.CMD_ACK : btHelper.CMD_ERR);
            write(oneByteWrite);
        }
    }

    private char[] readChar(int len){
        char[] charArr = new char[len];
        try {
            for (int i = 0; i < len; i++) {
                while (inStream.available() < 1) {
                    /* wait for byte to be available for reading */
                }
                charArr[i] = (char) inStream.read();
//                Log.d(TAG, "readChar: "+charArrayToHex(new char[] {charArr[i]}));
            }
            //Log.d(TAG, "readBytes: len:"+bytesRead+" data:"+byteArrayToHex(recBytes));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return charArr;
    }

    public void disconnect() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }

    private byte[] readBytes(int len) {
        byte[] recBytes = new byte[len]; // 1 byte buffer. byte[1024] = buffer of 1024 bytes
        int bytesRead;
        try {
            while (inStream.available() < len) {
                /* wait for expected no of bytes to be available for reading */
            }
            bytesRead = inStream.read(recBytes);
            //Log.d(TAG, "readBytes: len:"+bytesRead+" data:"+byteArrayToHex(recBytes));
//            Log.d(TAG, "readBytes: "+byteArrayToHex(recBytes));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return recBytes;
    }

    /**
     * Write data to the socket.
     */
    public void write(byte[] b) {
        try {
            outStream.write(b[0]);
            //Log.d(TAG, "write: " + byteArrayToHex(b));
            last_packet = b;
            /* SP 88888888 if(b[0] == 0x53){
                flushInstream();
            }*/
        } catch (Exception e) {
            Log.e(TAG, "Write failed!", e);
            Message msg = btHelper.mHandlersbluetooth.obtainMessage();
            msg.what = 9;
            btHelper.mHandlersbluetooth.sendMessage(msg);
        }
    }

    public void flushInstream(){
        try {
            inStream.skip(inStream.available());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
