package org.umtri.NightWiper;

import java.util.ArrayList;

import org.opencv.core.*;
import org.opencv.imgproc.*;

import android.util.Log;

/**
 * 
 * @author Cody Hyman
 *
 */
public class NightWiperDetector 
{	
	private final static String TAG = "NightWiperDetector";
	private Mat processedImage;
	private Mat lines;
	
	private static final double ROC_THRESHOLD = 8;
	/**
	 * 
	 * @param src Source image
	 * @param dst Destination image
	 * @param nIter Number of thinning iterations
	 * @param lowerThreshold Lower thinning threshold
	 * @deprecated
	 */
	public static void edgeThin(Mat src, Mat dst, int nIter, int lowerThreshold)
	{
		dst = src;
		for(int i = 0; i<nIter; i++)
		{
			Imgproc.blur(dst, dst, new Size(5,5));
			Core.inRange(dst, new Scalar(lowerThreshold), new Scalar(255), dst);
		}
	}
	
	/**
	 * 
	 * @param src Source image
	 * @param dst Destination image
	 */
	public static void equalizeColor(Mat src, Mat dst)
	{
		if(src.channels() >= 3)
		{
			Mat lumChrom = new Mat();
			Imgproc.cvtColor(src, lumChrom, Imgproc.COLOR_RGB2YCrCb);
			ArrayList<Mat> channels = new ArrayList();
			Core.split(lumChrom, channels);
			Imgproc.equalizeHist(channels.get(0), channels.get(0));
			Core.merge(channels, lumChrom);
			Imgproc.cvtColor(lumChrom, dst, Imgproc.COLOR_YCrCb2RGB);
		}
		else
		{
			dst = src; 
		}
	}
	
	/**
	 * @brief Wiper Line Pre-Processing (Deprecated)
	 * @param src
	 * @param dst
	 * @deprecated
	 */
	public void process(Mat src)
	{
    	Mat inputImg = src;
    	Mat procImg = new Mat();
    	Mat outImg = new Mat();
    	
    	
    	Log.i(TAG, "Frame Extracted: Size:" + inputImg.size().toString()+":"+inputImg.channels());

    	try
        {
    		Imgproc.cvtColor(inputImg,procImg, Imgproc.COLOR_BGRA2GRAY);
        	
    		Imgproc.equalizeHist(procImg, procImg);
    		Imgproc.blur(procImg, procImg, new Size(9,9));
    	
    		Imgproc.adaptiveThreshold(procImg, procImg, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,15,-4);
    		Imgproc.erode(procImg, procImg, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
    		Imgproc.dilate(procImg, procImg, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
    		Imgproc.medianBlur(procImg, procImg, 7);

    		Mat lines = new Mat();
  
    		Imgproc.HoughLinesP(procImg, lines, 1, Math.PI/180, 10, 70, 8);
    		Log.i(TAG,"PreProcessed");
        }
    	catch(Exception e)
    	{
    		
    	}
	}

	/**
	 * @brief
	 * @return
	 */
	public Mat getOutput()
	{
		return processedImage;
	}
	
	/**
	 * @brief Draws Hough lines on a given image
	 * @param src
	 * @return
	 */
	public static Mat drawLines(Mat src, Mat drawLines)
	{
		Mat output = src;
		Log.i("LINEDRAW","Line matrix size:"+drawLines.size().toString());
		for(int i = 0; i <drawLines.rows(); i++)
		{
			double vec[] = drawLines.get(i, 0);
			double rho = vec[0];
			double theta = vec[1];
			Log.i("LINEDRAW","Rho="+rho+" Theta="+theta);
			double a = Math.cos(theta);
			double b = Math.sin(theta);
			double x0 = a*rho; 
			double y0 = b*rho;
			Point pt1 = new Point();
			Point pt2 = new Point();
			pt1.x = Math.round(x0 + 1000*(-b));
			pt1.y = Math.round(y0 + 1000*(a));
			pt2.x = Math.round(x0 - 1000*(-b));
			pt2.y = Math.round(y0 - 1000*(a));
			Core.line(output, pt1, pt2, new Scalar(0,0,255),3);
		}
		return output;
	}
	
	/**
	 * @brief Draws lines on an image
	 * @param src Source image
	 * @param drawLines HoughP lines to draw
	 * @return Image with lines drawn on it
	 */
	public static Mat drawLinesP(Mat src, Mat drawLines)
	{
		Mat output = src;
		for(int i = 0; i < drawLines.cols(); i++)
		{
			double vec[] = drawLines.get(0,i);
			Point pt1 = new Point(2*vec[0],2*vec[1]);
			Point pt2 = new Point(2*vec[2],2*vec[3]);
			Core.line(output, pt1, pt2, new Scalar(0,0,255),3);
		}
		return output;
	}
	
	/**
	 * @brief Draws lines on an image
	 * @param src Source image
	 * @param drawLines HoughP lines to draw
	 * @param color Line color
	 * @return Image with lines drawn on it
	 */
	public static Mat drawLinesP(Mat src, Mat drawLines, Scalar color)
	{
		Mat output = src;
		for(int i = 0; i < drawLines.cols(); i++)
		{
			double vec[] = drawLines.get(0,i);
			Point pt1 = new Point(2*vec[0],2*vec[1]);
			Point pt2 = new Point(2*vec[2],2*vec[3]);
			Core.line(output, pt1, pt2, color,3);
		}
		return output;
	}
	/**
	 * @brief Determines the angles of all lines in the image
	 * @param lines Probabilistic Hough transform output lines
	 * @return Angles of each line segment (in degrees)
	 */
	public static ArrayList<Double> getLineAngles(Mat lines)
	{
		ArrayList<Double> angles = new ArrayList();
		double x1,y1,x2,y2;
		double currentAngle;
		double vec[];
		for(int i = 0; i<lines.cols();i++)
		{
			vec = lines.get(0,i);
			x1 = vec[0];
			y1 = vec[1];
			x2 = vec[2];
			y2 = vec[3];
			currentAngle = Math.atan2(y2-y1,x2-x1)*180/Math.PI;
			Log.i("Detector", "Found angle at " + currentAngle + " degrees");
			angles.add(currentAngle);
		}
		return angles;
	}
	
	/**
	 * Computes the mean of an ArrayList
	 * @param inputValues List of values to compute mean of
	 * @return Mean value of input values
	 */
	public static double meanArray(ArrayList<Double> inputValues)
	{
		double mean = 0;
		if(inputValues.size() > 0)
		{
			for(double value : inputValues)
			{
				mean += value;
			}
			mean = mean / inputValues.size();
			return mean;
		}
		return 0;
	}
	
	/**
	 * 
	 * @param inputValues
	 * @return
	 */
	public static double medianArray(ArrayList<Double> inputValues)
	{
		double median = 0;
		return median;
	}
	
	/**
	 * Computes the variance of an ArrayList
	 * @param inputValues List of values to compute variance of
	 * @return Variance of input values
	 */
	public static double varArray(ArrayList<Double> inputValues)
	{
		double var = 0;
		double mean = meanArray(inputValues);
		if(inputValues.size() > 0)
		{
			for(double value : inputValues)
			{
				var += Math.pow(value - mean, 2);
			}
			return var / inputValues.size();
		}
		return 0;
	}
	
	/**
	 * 
	 * @param inputValues
	 * @return
	 */
	public static double avgRocArray(ArrayList<Double> inputValues)
	{
		ArrayList<Double> rocList = new ArrayList();
		
		if(inputValues.size() > 0)
		{
			for(int i=1; i<inputValues.size(); i++)
			{
				double currentVal = inputValues.get(i);
				double lastVal = inputValues.get(i-1);
				if(Math.abs(currentVal) > 2 && Math.abs(lastVal) > 2)
				{
					rocList.add(Math.abs(currentVal-lastVal));
				}
			}
			double roc = meanArray(rocList);
			Log.i(TAG,"ROC: " + roc);
			return roc;
		}
		return 0;
	}
	
	
	/**
	 * @brief Determines the dominant wiper angle
	 * @param angles
	 * @return
	 */
	public static double getDominantAngle(ArrayList<Double> angles)
	{
		double domAngle = 0;
		int angleCount = 0;
		double mean = meanArray(angles);
		double var = varArray(angles);
		
		Log.i("Angles","Mean: " + mean);
		Log.i("Angles","Variance: " + var);
		ArrayList<Double> candidateAngles = new ArrayList();

		for(double angle : angles)
		{
			//if((90 - Math.abs(angle) > 5) && (Math.abs(angle) > 5))
			if(Math.abs(mean-angle) < 15)
			{
				candidateAngles.add(angle);
			}
		}
		
		domAngle = meanArray(candidateAngles);
		Log.i("Angles", "Dominant Angle: " + domAngle);
		
		return domAngle;
	}
	
	/**
	 * @brief Determine whether the wipers are on or off (primary algorithm)
	 * @param lineHistory A multi-frame history of detected line segments
	 * @return The binary on/off state of the wipers
	 */
	public static boolean determineWiperStatus(ArrayList<Mat> lineHistory)
	{
		Log.i("Detector", "Determining wiper angle");
		boolean wiperState;
		if(lineHistory.size() < 5)	// Check for sufficient number of prior frames
		{
			Log.i("Detector","Insufficient number of frames buffered: " + lineHistory.size());
			return false;
		}
		
		ArrayList<ArrayList<Double>> angleHist = new ArrayList<ArrayList<Double>>();	// Line angle history
		ArrayList<Double> domAngleHist = new ArrayList<Double>();						// Dominant angle history
		ArrayList<Double> tempAngles = new ArrayList<Double>();							// temp angle history
		double domAngle;																// Dominant angle
		
		// Compute dominant angles per frame
		for(int i=0; i<lineHistory.size(); i++)
		{
			tempAngles = getLineAngles(lineHistory.get(i));
			domAngle = getDominantAngle(tempAngles);
			Log.i(TAG,"Dominant angle: " + domAngle);
			angleHist.add(tempAngles);
			domAngleHist.add(domAngle);
			// Get line angles and compare to previous to check for motion of wipers
		}
		double avgRoc = avgRocArray(domAngleHist);	// Compute average rate-of-change for feature angles
		
		// Check for rate-of-change being over threshold
		if(avgRoc > ROC_THRESHOLD)
		{
			wiperState = true;
			Log.i(TAG,"Wiper ROC over threshold");
		}
		else
		{
			wiperState = false;
			Log.i(TAG,"Wiper ROC under threshold");
		}
		return wiperState;
	}
}
