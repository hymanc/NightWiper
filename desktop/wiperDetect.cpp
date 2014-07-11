#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <iostream>
#include <opencv2/opencv.hpp>
#include <math.h>

using namespace cv;
using namespace std;

double wiperDerivative(vector<double>& wiperAngles)
{
  // Compute differences between frames, look for patterns, return sensible movement data
  return 0;
}

// TODO: Replace with LLS or RANSAC fit
double wiperAngle(vector<Vec2f>& lines, double angleThreshold)
{
  int i, count;
  double theta_i, sum;

  count = 0;
  sum = 0;
  for(i=0;i<lines.size();i++)
  {
    theta_i = CV_PI/2 - lines[i][1]; // Line angle
    //cout << theta_i << endl;
    if(abs(theta_i) > angleThreshold) // Not horizontal
    {
      sum += abs(theta_i);
      count++;
    }
    //sum += ; // Add line angle
  }
  if(count > 0)
  {
    double meanAngle = sum/count;
    //meanAngle = CV_PI/2 - meanAngle; // Convert to a sensible angle
    //cout << "Mean Angle:" << meanAngle << endl;
    return meanAngle;// Compute mean line angle
  }
  else
    return 0;
}

/*
 * edgeThin()
 * blur-based edge thinning algorithm for binary image
 */
void edgeThin(const Mat& src, Mat& result, int niter, int lowerThreshold)
{
  int i;
  result = src;
  for(i=0;i<niter;i++)
  {
    // Blur and suppress
    blur(result,result,Size(5,5));
    inRange(result,Scalar(lowerThreshold),Scalar(255),result);
  }
}

/*
 * equalizeColor()
 * Intensity equalization for color/non-color images
 */
void equalizeColor(const Mat& src, Mat& result)
{
  if(src.channels() >= 3)
  {
      Mat lumchrom;
      cvtColor(src,lumchrom,CV_BGR2YCrCb);
      vector<Mat> ch;
      split(lumchrom,ch);
      equalizeHist(ch[0],ch[0]); // Equalize intensity
      merge(ch,lumchrom); // Remerge channels
      cvtColor(lumchrom,result,CV_YCrCb2BGR);
  }
  else
    result = src;
}

/*
 * houghLines()
 * Helper to find and display Hough transform lines
 */
vector<Vec2f> houghLines(const Mat& src, Mat& result)
{
  namedWindow("TempView",CV_WINDOW_NORMAL);
  Mat tempImg;
  medianBlur(src,tempImg,35);
  cvtColor(tempImg,tempImg,CV_RGB2HSV);
  inRange(tempImg, Scalar(0,0,5),Scalar(255,255,90),tempImg);
  medianBlur(tempImg,tempImg,25);
  edgeThin(tempImg,tempImg,3,200);
  Canny(tempImg,tempImg,5,100,5);
  imshow("TempView",tempImg);
  result = src;
  vector<Vec2f> lines;
  
  HoughLines(tempImg,lines, 1, CV_PI/180, 50, 0, 0);
  cout << "Found " << lines.size() << " lines\n";
  for( size_t i = 0; i < lines.size(); i++ )
  {
    float rho = lines[i][0], theta = lines[i][1];
    Point pt1, pt2;
    double a = cos(theta), b = sin(theta);
    double x0 = a*rho, y0 = b*rho;
    pt1.x = cvRound(x0 + 1000*(-b));
    pt1.y = cvRound(y0 + 1000*(a));
    pt2.x = cvRound(x0 - 1000*(-b));
    pt2.y = cvRound(y0 - 1000*(a));
    line(result, pt1, pt2, Scalar(0,0,255), 3, CV_AA);
  }
  
  /*
  vector<Vec4i> linesP;
  HoughLinesP(tempImg, linesP, 1, CV_PI/360, 50, 250, 150 );
  cout << "Found " << linesP.size() << " lines\n";
  for( size_t i = 0; i < linesP.size(); i++ )
  {
    Vec4i l = linesP[i];
    line( result, Point(l[0], l[1]), Point(l[2], l[3]), Scalar(0,0,255), 3, CV_AA);
  }*/
  
  return lines;
}

int main(int argc, char ** argv)
{
  Mat srcImg;
  Mat threshImg;
  Mat procImg;
  
  if(argc > 1)
  {
    if(strcmp(argv[1],"-c") == 0) // Video mode
    {
      cout << "Attempting to open camera " << argv[2] << endl;
      VideoCapture cap(atoi(argv[2]));
      if(!cap.isOpened())
      {
	printf("Camera capture failed to initialize, exiting\n");
      }
      else
      {
	  cap.set(CV_CAP_PROP_FRAME_WIDTH,640);
	  cap.set(CV_CAP_PROP_FRAME_HEIGHT,480);
	  
	  double xrescheck = cap.get(CV_CAP_PROP_FRAME_WIDTH);
	  double yrescheck = cap.get(CV_CAP_PROP_FRAME_HEIGHT);
	  printf("Camera resolution set to %dx%d\n",(int)xrescheck,(int)yrescheck);
      }
          if(!cap.isOpened())
	  {
	    printf("Camera capture failed to initialize, exiting\n");
	    return -1;
	  }
	  
	  cout << "Creating video windows\n";
	  namedWindow("Camera",CV_WINDOW_NORMAL);
	  namedWindow("Process",CV_WINDOW_AUTOSIZE);
	  Mat srcImg;
	  Mat procImg;
	  double domAngle;
	  vector<Vec2f> lines;
	  int finished = -1;
	  while(finished == -1)
	  {
	    cap >> srcImg;
	    imshow("Camera",srcImg);
	    lines = houghLines(srcImg,procImg);
	    domAngle = wiperAngle(lines,0.25);
	    domAngle = (180/CV_PI)*domAngle; // Convert to degrees
	    cout << "Dominant Wiper Angle: " << domAngle << endl;
	    imshow("Process",procImg);
	    finished = cvWaitKey(100);
	  }
	  destroyAllWindows();
	  
    }
    else
    { // Still mode
      srcImg = imread(argv[1]); // Read in images if they exist
      resize(srcImg,srcImg,Size(0,0),0.3,0.3,INTER_LINEAR);
      namedWindow("Wiper Raw",CV_WINDOW_NORMAL);
      namedWindow("Wiper Threshold",CV_WINDOW_NORMAL);
      namedWindow("Wiper Detected",CV_WINDOW_NORMAL);
      
      imshow("Wiper Raw",srcImg);
      
      equalizeColor(srcImg,threshImg);
      
      //imshow("Wiper Raw",threshImg);
      //blur(threshImg,threshImg,Size(9,9));
      
      imshow("Wiper Raw",threshImg);
      //cvtColor(srcImg,threshImg,CV_BGR2GRAY);
      //Canny(threshImg,threshImg,100,190,3);
      
      //cvtColor(srcImg,threshImg,CV_RGB2HSV);
      //inRange(threshImg, Scalar(0,0,5),Scalar(255,255,90),threshImg);
      //blur(threshImg,threshImg,Size(31,31));
      //medianBlur(threshImg,threshImg,55); // Ghetto non-max
      //edgeThin(threshImg,threshImg,20);
      //Canny(threshImg,procImg,20,500,5);
      //imshow("Wiper Threshold",threshImg);
      //for(int j=0;j<100;j++)
      vector<Vec2f> lines = houghLines(srcImg,procImg);
      imshow("Wiper Detected",procImg);
      double angle = wiperAngle(lines,0.25);
      angle = (180/CV_PI)*angle; // Convert to degrees
      cout << "Detected mean wiper angle: " << angle <<" degrees" << endl;
      // Segment
      // Show imag
      cvWaitKey(0);
    }
  }
  else
  {
    cerr << "Error: Not enough arguments\n";
  }
  destroyAllWindows();
  return 0;
}