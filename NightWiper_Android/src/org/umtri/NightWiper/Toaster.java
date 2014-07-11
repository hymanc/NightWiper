package org.umtri.NightWiper;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 
 * @author Cody Hyman
 *
 */
public class Toaster {
	
	private static Handler mHandler; 
	
	/**
	 * 
	 * @param handler
	 */
	public static void setToasterHandler(Handler handler)
	{
		mHandler = handler;
		Log.i("Toaster","Assigning handler");
	}
	
	/**
	 * 
	 * @param data
	 */
	public static void makeToast(String data)
	{
		Message msg = mHandler.obtainMessage(NightWiperActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(NightWiperActivity.TOAST, data);
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
}
