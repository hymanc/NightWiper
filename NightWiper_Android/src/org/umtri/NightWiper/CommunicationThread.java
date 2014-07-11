package org.umtri.NightWiper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.util.Log;

public class CommunicationThread extends Thread
{
	private static final String TAG = "CommunicationThread";
	private BluetoothSPPServer mBluetoothServer;
	
	private boolean running = false;
	
	public CommunicationThread(BluetoothSPPServer server)
	{
		running = true;
		mBluetoothServer = server;
		this.start();
	}
	
	public void run()
	{
		while(running)
		{
			try
			{
				boolean wipersDetected = NightWiperActivity.getWipersDetected();
				boolean wiperStatus = NightWiperActivity.getWiperStatus();
				String wiperStatusString;
				//if(wipersDetected)
				//{
					wiperStatusString = wiperStatus ? "1" : "0";
				//}
				//else 
				//{
				//	wiperStatusString = "10001";
				//}
				
				String wiperMessage = getTimeString() + "+WIP>" + wiperStatusString;
				Log.i(TAG,"Sending message: " + wiperMessage);
				mBluetoothServer.writeLine(wiperMessage);
				Thread.sleep(200);
			}
			catch(InterruptedException e)
			{
				
			}
			catch(Exception e2)
			{
				Log.e(TAG,"General exception in communication thread: " + e2.toString());
			}
		}
	}
	
	public void cancel()
	{
		running = false;
	}
	
	private static String getTimeString()
	{
		String result = "";
		try 
		{
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
			Date cal = Calendar.getInstance().getTime();
			result=formatter.format(cal);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return result;
	}
}
