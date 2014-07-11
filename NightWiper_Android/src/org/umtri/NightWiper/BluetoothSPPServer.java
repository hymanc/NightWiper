package org.umtri.NightWiper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.app.Activity;

/**
 * 
 * @author Cody Hyman
 * @brief A Bluetooth serial-port-protocol (SPP) server class for Android
 */
public class BluetoothSPPServer{
	
	private final String NAME = "BluetoothServer";
	private final String TAG = "BluetoothSPPServer";
	//private final UUID MY_UUID = UUID.randomUUID();
	private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothSocket mBluetoothSocket = null;
	
	private ConnectThread connectThread;
	private AcceptThread acceptThread;
	
	private Context mContext;
	private Handler mHandler;
	
	private boolean isConnected;
	
	public static final int REQUEST_ENABLE_BT = 1;
	
	/**
	 * @brief Constructor for Bluetooth serial-port-protocol (SPP) class
	 * @param context Server context
	 * @param handler Parent activity message handler
	 */
	public BluetoothSPPServer(Context context, Handler handler)
	{
		isConnected = false;
		
		mContext = context;
		mHandler = handler;
		
		Log.i(TAG,"Starting Bluetooth SPP Server");
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mBluetoothAdapter.enable();
		
		if(!mBluetoothAdapter.isEnabled())
		{
			//Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			//mContext.getApplicationContext().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			Log.i(TAG,"Bluetooth is not enabled");
		}
		
		/*
		Log.i(TAG,"Setting device as discoverable");
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
		mContext.startActivity(discoverableIntent);
		*/
		
		if(mBluetoothAdapter != null)
		{
			try
			{
				acceptThread = new AcceptThread();
				acceptThread.start();
			}
			catch(Exception e)
			{
				Log.e(TAG, "Exception accepting connection: " + e.toString());
			}
		}
	}
	
	/**
	 * @brief Manages connection startup for a Bluetooth communication socket
	 * @param socket Bluetooth socket to connected device
	 */
	public void manageConnectedSocket(BluetoothSocket socket)
	{
		Log.i(TAG,"Managing connected socket");
		mBluetoothSocket = socket;
		connectThread = new ConnectThread(mBluetoothSocket);
		connectThread.start();
		isConnected = true;
	}
	
	/**
	 * @brief Closes Bluetooth communication socket
	 */
	public void closeSocket()
	{
		//mBluetoothSocket.close();
		connectThread.cancel();
	}
	
	/**
	 * @brief Writes a string as a line to the Bluetooth SPP interface
	 * @param line Line string to write over Bluetooth
	 */
	public void writeLine(String line)
	{
		if(isConnected)
			this.connectThread.write((line + "\r\n").getBytes());
	}
	
	/**
	 * @brief Writes a string to the Bluetooth SPP interface
	 * @param str String to write over bluetooth
	 */
	public void writeString(String str)
	{
		if(isConnected)
			this.connectThread.write(str.getBytes());
	}
	
	/**
	 * @brief Obtains a string containing bytes currently in the given byte-array buffer
	 * @param btBuffer Buffer storage element
	 * @param nBytes Number of bytes in the buffer
	 * @return String representation of buffer array
	 */
	public String getBufferString(byte[] btBuffer, int nBytes)
	{
		return new String(btBuffer).substring(0,nBytes);
	}
	
	/**
	 * @brief Check if socket is connected
	 * @return Connected status of BT socket
	 */
	public boolean getConnected()
	{
		return isConnected;
	}
	
	
	/**
	 * @author Cody Hyman
	 * @brief Utility class to accept external client Bluetooth connections
	 */
	private class AcceptThread extends Thread
	{
		private final BluetoothServerSocket mServerSocket;
		
		/**
		 * @brief AcceptThread constructor
		 */
		public AcceptThread()
		{
			BluetoothServerSocket tmp = null;
			try
			{
				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME,MY_UUID);
			}
			catch(IOException e)
			{
				Log.e(TAG,"Exception in accepting Bluetooth connection: " + e.toString());
			}
			mServerSocket = tmp;
		}
		
		/**
		 * @brief AcceptThread Thread run() function
		 */
		public void run()
		{
			BluetoothSocket socket = null;
			while(true)
			{
				try
				{
					Log.i(TAG, "Waiting for Bluetooth connection");
					socket = mServerSocket.accept();
					String connectedName = socket.getRemoteDevice().getName();
					Log.i(TAG, "Connected to " + connectedName);
					Toaster.makeToast("Connected to " + connectedName);
					break;
				}
				catch(IOException e)
				{
					Log.e(TAG,"Exception in Bluetooth thread: " + e.toString());
					break;
				}
			}
			if(socket != null)
			{
				Log.i(TAG,"Socket connection accepted, managing");
				try {
					BluetoothSPPServer.this.manageConnectedSocket(socket);
					mServerSocket.close();
				} catch (IOException e) {
					Log.e(TAG, "Exception in passing opened socket: " + e.toString());
				}
			}
			else
			{
				Log.i(TAG,"Socket is null");
			}
		}
		
		/**
		 * @brief Closes server acceptance socket
		 */
		public void cancel()
		{
			try
			{
				Log.i(TAG, "Closing server socket");
				mServerSocket.close();
			}
			catch(IOException e)
			{
				Log.e(TAG, "Exception caught closing bluetooth socket: " + e.toString());
			}
		}
	};

	/**
	 * @author Cody Hyman
	 * @brief Utility Bluetooth connection thread
	 */
	private class ConnectThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		
		/**
		 * @brief Bluetooth connection thread constructor
		 * @param Bluetooth socket Socket to connect with
		 */
		public ConnectThread(BluetoothSocket socket)
		{
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			try
			{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			}
			catch(IOException e)
			{
				Log.e(TAG, "Exception in assigning BT I/O streams: " + e.toString());
			}
			
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		
		/**
		 * @brief Bluetooth connection thread run() implementation
		 */
		public void run()
		{
			byte[] buffer = new byte[1024];
			int bytes;
			
			if(mmOutStream != null)
			{
				String startMessage = "NightWiper BT Interface";
				write(startMessage.getBytes());
			}
			
			// Connection loop
			while(true)
			{
				Log.i(TAG, "ConnectThread");
				Log.i(TAG,"Writing periodic message");
				//write("NightWiper ALIVE\r\n".getBytes());
				
				//try { Thread.sleep(200);} catch (InterruptedException e1) {}
				
				try
				{
					bytes = mmInStream.read(buffer);
					String bufferTxt = BluetoothSPPServer.this.getBufferString(buffer, bytes);
					Log.i(TAG,"Received: " + new String(buffer).substring(0,bytes));
					Message msg = mHandler.obtainMessage(NightWiperActivity.MESSAGE_BT_READ);
					Bundle bundle = new Bundle();
					bundle.putString(NightWiperActivity.BT_MESSAGE, bufferTxt);
					msg.setData(bundle);
					mHandler.sendMessage(msg);
					//mHandler.obtainMessage(NightWiperActivity.MESSAGE_BT_READ, bytes, -1, buffer).sendToTarget();
					
					/*try {
						Thread.sleep(200);
					} 
					catch (InterruptedException e) {}*/
				}
				catch(IOException e)
				{
					Log.e(TAG,"Exception in Bluetooth connect thread: " + e.toString());
				}
			}
		}
		
		/**
		 * @brief Writes byte array to SPP client
		 * @param bytes Bytes to write to SPP client
		 */
		public void write(byte[] bytes)
		{
			try
			{
				Log.i(TAG,"Writing Bluetooth Message: " + new String(bytes));
				mmOutStream.write(bytes);
			}
			catch(IOException e)
			{
				Log.e(TAG,"Exception writing Bluetooth message: " + e.toString());
			}
		}
		
		/**
		 * @brief Closes Bluetooth SPP connection
		 */
		public void cancel()
		{
			try
			{
				Log.i(TAG, "Closing Bluetooth socket");
				isConnected = false;
				mmSocket.close();
			}
			catch(IOException e)
			{
				Log.e(TAG, "Exception in closing Bluetooth socket: " + e.toString());
			}
		}
	};
}
