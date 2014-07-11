package org.umtri.NightWiper;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;




import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.umtri.NightWiper.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A windshield wiper video detection Android app
 * @author Cody Hyman
 *
 */
public class NightWiperActivity extends Activity implements CvCameraViewListener2 
{    
	private static final String TAG = "NightWiperActivity";

	// Message codes
	public static final int MESSAGE_TOAST	 = 1;
	public static final int MESSAGE_BT_READ  = 2;
	
	// Bundle descriptors
	public static final String BT_MESSAGE 	= "BluetoothMsg";
	public static final String TOAST		= "Toast";
	
	public static final int DEFAULT_VALUE = 10001;
	
    private CameraBridgeViewBase 	mOpenCvCameraView;
    private boolean              	mIsJavaCamera = true;
    private MenuItem             	mItemSwitchCamera = null;
    private NightWiperDetector   	mDetector = null;
    
	private BluetoothSPPServer 	 	mBluetoothServer = null;
	
	private CommunicationThread mCommThread;
	
	private static boolean wiperStatus;
	private static double wiperSpeed;
	private static boolean wipersDetected;
	
	private ArrayList<Mat> lineHistory;
	
	private Mat inputImg;
	private Mat procImg;

	private Mat outImg;
	
	private Mat lastPreproc;
	
	private TextView wiperStatusText=null;
	
	private int filterCounter = 0;
	
	private static final int FRAME_TIME = 67;
	private long tickTime;
	
    private BaseLoaderCallback 	 	mLoaderCallback = new BaseLoaderCallback(this) 
    { 
    	/**
    	 * 
    	 */
    	@Override
        public void onManagerConnected(int status) 
    	{
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.setMaxFrameSize(360, 240);
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /**
     * 
     */
    private final Handler mHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		switch(msg.what)
    		{
    		case MESSAGE_TOAST:
    			Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),Toast.LENGTH_SHORT).show();
    			break;
    		case MESSAGE_BT_READ:
    			Log.i("Bluetooth", "Bluetooth message received, parsing");
    			String msgData = msg.getData().getString(BT_MESSAGE);
    			Log.i("Bluetooth", "Message contents: " + msgData);
    		break;
    		}
    	}
    };
    
    /**
     * Main activity constructor
     */
    public NightWiperActivity() 
    {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_night_wiper);  
        
        Toaster.setToasterHandler(mHandler);
        mBluetoothServer = new BluetoothSPPServer(this, mHandler);
        
        if(mBluetoothServer == null)
        {
        	Log.i(TAG,"Bluetooth server is null!!!");
        }
        lineHistory = new ArrayList();
        
        wipersDetected = false;
        wiperSpeed = 10001;
        wiperStatus = false;
        
        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_surface_view);
        else
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.native_camera_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        
        wiperStatusText = (TextView) findViewById(R.id.wiperStatus);
        
        mCommThread = new CommunicationThread(mBluetoothServer);
        tickTime = SystemClock.currentThreadTimeMillis();
    }

    /**
     * @brief Pause handler
     */
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /**
     * @brief Resume handler
     */
    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    /**
     * 
     */
    public void onDestroy() 
    {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /**
     * 
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemSwitchCamera = menu.add("Toggle Native/Java camera");
        return true;
    }

    /**
     * 
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        String toastMesage = new String();
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemSwitchCamera) {
            mOpenCvCameraView.setVisibility(SurfaceView.GONE);
            mIsJavaCamera = !mIsJavaCamera;

            if (mIsJavaCamera) {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_surface_view);
                toastMesage = "Java Camera";
            } else {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.native_camera_surface_view);
                toastMesage = "Native Camera";
            }

            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.enableView();
            Toast toast = Toast.makeText(this, toastMesage, Toast.LENGTH_LONG);
            toast.show();
        }

        return true;
    }

    /**
     * 
     */
    public void onCameraViewStarted(int width, int height)
    {
    	Log.i(TAG, "Starting camera view");
        lastPreproc = new Mat();
    }

    /**
     * @brief Camera view stopped alert
     */
    public void onCameraViewStopped() 
    {
    	Log.i(TAG, "Stopping camera view");
    }

    /**
     * @brief 
     * @return Processed image to display
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) 
    {
    	//Mat inrgba = inputFrame.rgba();
    	inputImg = new Mat();
    	procImg = new Mat();
    	outImg = new Mat();
    	
    	Mat procImg2 = new Mat();
    	
    	inputImg = inputFrame.rgba();
    	procImg = inputFrame.gray();
    	
    	Log.i(TAG, "Frame Extracted: Size:" + inputImg.size().toString()+":"+inputImg.channels());
    	//Mat frame = new Mat();
    	
    	//Mat outFrame = new Mat();
    	try
        {
        	//Imgproc.cvtColor(inrgba,inrgba, Imgproc.COLOR_BGRA2GRAY);
        	
        	//NightWiperDetector.equalizeColor(frame, frame);
    		Imgproc.pyrDown(procImg, procImg);
        	
    		Imgproc.equalizeHist(procImg, procImg);
    		Imgproc.blur(procImg, procImg, new Size(9,9));

    		Imgproc.adaptiveThreshold(procImg, procImg, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,15,-3.5);
    		Imgproc.medianBlur(procImg, procImg, 7);
    		procImg2 = procImg.clone();
    		if(lastPreproc.size().equals(procImg.size()))
    			Core.subtract(procImg, lastPreproc, procImg);

    		//Imgproc.erode(procImg2, procImg2, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
    		//inputFrame.Imgproc.dilate(procImg2, procImg2, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
    		
    		//Imgproc.blur(procImg2, procImg2, new Size(5,5));
    		//Imgproc.Canny(procImg2, procImg2, 13, 100);
    		
    		//outImg = procImg.clone();
    		//NightWiperDetector.edgeThin(frame, frame, 5, 100);
    		
    		lastPreproc = procImg2.clone();
    		procImg2.release();
    		/*
    		 * Line detection
    		 */
    		Mat lines = new Mat();	// Line array
  
    		//Imgproc.HoughLines(frame, lines,  0.5,  Math.PI/180 , 10, 0 ,0);
    		Imgproc.HoughLinesP(procImg, lines, 1, Math.PI/180, 10, 60, 8);
    		Log.i(TAG,"PreProcessed");
        	//mDetector.process(frame);
    		//outFrame = frame;
    		Log.i("Detector","Found " + lines.cols() + " lines");
    		    		
    		Imgproc.pyrUp(procImg, procImg);
    		Imgproc.cvtColor(procImg, procImg, Imgproc.COLOR_GRAY2RGB);


    		Log.i(TAG,"Adding Lines to History");
    		lineHistory.add(lines);
    		Log.i(TAG,"Lines added");
    		if(lineHistory.size() > 10)
    		{
    			Log.i(TAG,"Popping line");
    			lineHistory.remove(0); // Pop off first reading
    			Log.i(TAG,"Line popped");
    		}
    		Log.i(TAG,"Processing lines");
    		wiperStatus = NightWiperDetector.determineWiperStatus(lineHistory);
    		if(wiperStatus==true)
    		{
    			Log.i(TAG,"Wipers on!");
    			wipersDetected = true;
    			wiperStatusText.setText("Wipers: On");
    			filterCounter = 500;
    		}
    		else
    		{
    			Log.i(TAG,"No wiper update, waiting");
    			filterCounter --;
    		}
    		if(filterCounter < 1)
    		{
    			Log.i(TAG,"Wiper timeout");
    			wiperStatusText.setText("Wipers: Off");
    			wipersDetected = false;
    		}
    		Log.i(TAG,"Processed");
    		
    		Scalar lineColor;
    		if(wiperStatus)
    			lineColor = new Scalar(0,255,0);
    		else
    			lineColor = new Scalar(0,0,255);
    		outImg = NightWiperDetector.drawLinesP(procImg, lines, lineColor);
    		
    		//mBluetoothServer.writeLine("Frame processed");
            //sendWiperMessage();
            Log.i(TAG,"Wiper Message Sent");
    		//return mDetector.getOutput();
        }
        catch(Exception e)
        {
        	Log.i(TAG, "PROBLEM!"+e.toString());
        	outImg = procImg;
        }
    	
    	long remTime = tickTime + FRAME_TIME - SystemClock.currentThreadTimeMillis();
    	if(remTime > 0)
    	{
        	try
        	{
        		Log.i(TAG,"Sleeping for remaining frame time: " + remTime);
        		Thread.sleep(remTime);
        	}
        	catch(Exception e)
        	{}
    	}
    	tickTime = SystemClock.currentThreadTimeMillis();
    	return outImg;
    }
    
    /**
     * @brief Returns the wiper on/off status flag
     * @return Wiper status flag
     */
	public static boolean getWiperStatus()
	{
		return wiperStatus;
	}
	
	/**
	 * @brief Returns the wiper speed interval value
	 * @return Wiper time interval (estimated) in seconds
	 */
	public static double getWiperSpeed()
	{
		return wiperSpeed;
	}
	
	/**
	 * 
	 * @return
	 */
	public static boolean getWipersDetected()
	{
		return wipersDetected;
	}
	
	/**
	 * @deprecated
	 */
	public void sendWiperMessage()
	{
		try
		{
			String wiperMessage;
			if(wipersDetected)
			{
				int wiperStatusNumber = getWiperStatus() ? 1 : 0;
				wiperMessage = "+WIP>" + wiperStatusNumber + "+WSP>" + getWiperSpeed();
			}
			else
			{
				wiperMessage = "+WIP>10001+WSP>10001";
			}
			Log.i(TAG,"Sending wiper message: " + wiperMessage);
			mBluetoothServer.writeLine(wiperMessage);
			Log.i(TAG,"Wiper message init complete");
		}
		catch(Exception e)
		{
			Log.e(TAG,"Exception in sendWiperMessage: " + e.toString());
		}
	}
}
