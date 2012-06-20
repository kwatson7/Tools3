package com.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.SensorManager;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

public class CameraHelperOld {

	// orientation variables
	private OrientationEventListener mOrientationEventListener;
	private int mOrientation =  -1;
	private static final int ORIENTATION_PORTRAIT_NORMAL =  1;
	private static final int ORIENTATION_PORTRAIT_INVERTED =  2;
	private static final int ORIENTATION_LANDSCAPE_NORMAL =  3;
	private static final int ORIENTATION_LANDSCAPE_INVERTED =  4;
	
	// orientation enum
	private enum Orientation{
		ORIENTATION_PORTRAIT_NORMAL(90),ORIENTATION_LANDSCAPE_NORMAL(0),
		ORIENTATION_PORTRAIT_INVERTED(270), ORIENTATION_LANDSCAPE_INVERTED(180);
		
		private int orientationAngle = 0;
		
		private Orientation(int angle){
			orientationAngle = angle;
		}
		
		private int getAngle(){
			return orientationAngle;
		}
	}
	private Orientation orientation = Orientation.ORIENTATION_LANDSCAPE_NORMAL;
	
	
	private int jpegQuality = 90;

	private Camera mCamera = null;
	private Boolean mChangeParameters;
	private OnRotationCallback callback;
	
	// other variables
	boolean isPreviewRunning = false;			// keep track if the preview is currently running.

	/** Class to keep camera surface from rotating and also to keep track of 
	 * orientation of camera so the picture is stored correctly.
	 * <p>
	 * You must call:<p>
	 *      (1)this class's onResume in the calling activity's onResume<br>
	 *      (2)this class's onPause in the calling activity's onPause<br>
	 *      (3)this class's onCreate in the calling activity's onCreate<br>
	 *      (4)this class's updateCam when the calling activity's camera is updated<br>
	 * @param cam, pass null if not activated yet
	 * @param jpegQuality 1-100, the jpeg quality, a good value is usually ~90
	 * @param changeParameters, Boolean to change the parameters of the rotation in
	 * the camera settings. This should be true if you want the camera drivers to rotate the 
	 * image for you, or use getRotation() manually later to do it yourself
	 * @param callback to happen when surface is rotated. Null if none desired
	 */
	public CameraHelperOld(Activity activity, Camera cam, int jpegQuality, Boolean changeParameters, OnRotationCallback callback){
		// store image quality
		if (jpegQuality < 1)
			jpegQuality = 1;
		else if (jpegQuality > 100)
			jpegQuality = 100;
		this.jpegQuality = jpegQuality;
		
		// store camera
		updateCam(cam);
		
		// store other values
		this.mChangeParameters = changeParameters;
		this.callback = callback;
	}

	/** call this in the calling activity's onResume. MUST be done */
	public void onResume(Context ctx) {

		// prepare listener for orientation changes
		if (mOrientationEventListener == null) {            
			mOrientationEventListener = new OrientationEventListener(ctx, SensorManager.SENSOR_DELAY_NORMAL) {

				@Override
				public void onOrientationChanged(int orientation) {

					// determine our orientation based on sensor response
					int lastOrientation = mOrientation;

					if (orientation >= 315 || orientation < 45) {
						if (mOrientation != ORIENTATION_PORTRAIT_NORMAL) {                          
							mOrientation = ORIENTATION_PORTRAIT_NORMAL;
						}
					}
					else if (orientation < 315 && orientation >= 225) {
						if (mOrientation != ORIENTATION_LANDSCAPE_NORMAL) {
							mOrientation = ORIENTATION_LANDSCAPE_NORMAL;
						}                       
					}
					else if (orientation < 225 && orientation >= 135) {
						if (mOrientation != ORIENTATION_PORTRAIT_INVERTED) {
							mOrientation = ORIENTATION_PORTRAIT_INVERTED;
						}                       
					}
					else { // orientation <135 && orientation > 45
						if (mOrientation != ORIENTATION_LANDSCAPE_INVERTED) {
							mOrientation = ORIENTATION_LANDSCAPE_INVERTED;
						}                       
					}   

					if (lastOrientation != mOrientation) {
						changeRotation(mOrientation, lastOrientation);
					}
				}
			};
		}
		
		// enable the listener
		if (mOrientationEventListener.canDetectOrientation()) {
			mOrientationEventListener.enable();
		}
	}

	/** call this in the calling activity's onPause. MUST be done */
	public void onPause() {
		mOrientationEventListener.disable();
		mOrientationEventListener = null;
		updateCam(null);
	}

	/**
	 * Performs required action to accommodate new orientation
	 * @param orientation
	 * @param lastOrientation
	 */
	private void changeRotation(int orientation, int lastOrientation) {
		
		// main switching of camera
		if (!(mChangeParameters == null || !mChangeParameters || mCamera == null)){
			Camera.Parameters parameters = mCamera.getParameters();
			switch (orientation) {
			case ORIENTATION_PORTRAIT_NORMAL:
				parameters.setRotation(90);
				break;
			case ORIENTATION_LANDSCAPE_NORMAL:
				parameters.setRotation(0);
				break;
			case ORIENTATION_PORTRAIT_INVERTED:
				parameters.setRotation(270);
				break;
			case ORIENTATION_LANDSCAPE_INVERTED:
				parameters.setRotation(180);
				break;
			}

			mCamera.setParameters(parameters);	
		}
		
		// post callback
		if (callback != null)
			callback.onRotation(getRotation(orientation), getRotation(lastOrientation));
	}

	/** call this in the calling activity's onCreate. MUST be done */
	public void onCreate(Activity act){

		// force portrait layout
		act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		//act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE | ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
	}

	/** call this function to get the camera rotation, usually at the time of taking the picture
	 * Return value will be 0, 90, 180, or 270 */
	public int getRotation(){
		int rotation = 0;
		switch (mOrientation) {
		case ORIENTATION_PORTRAIT_NORMAL:
			rotation = 90;
			break;
		case ORIENTATION_LANDSCAPE_NORMAL:
			rotation = 0;
			break;
		case ORIENTATION_PORTRAIT_INVERTED:
			rotation = 270;
			break;
		case ORIENTATION_LANDSCAPE_INVERTED:
			rotation = 180;
			break;
		}
		
		return rotation;
	}
	
	/** call this function to get the camera rotation, usually at the time of taking the picture
	 * Return value will be 0, 90, 180, or 270 */
	private int getRotation(int orientation){
		int rotation = 0;
		switch (orientation) {
		case ORIENTATION_PORTRAIT_NORMAL:
			rotation = 90;
			break;
		case ORIENTATION_LANDSCAPE_NORMAL:
			rotation = 0;
			break;
		case ORIENTATION_PORTRAIT_INVERTED:
			rotation = 270;
			break;
		case ORIENTATION_LANDSCAPE_INVERTED:
			rotation = 180;
			break;
		}
		
		return rotation;
	}

	/** call this in the calling activity, when camera is updated. <br>
	 * MUST be done <br>
	 * Also stops any currently running previews on old camera and assumes new camera does not have a preview running
	 * */
	public void updateCam(Camera newCam){
		// stop any running previews
		stopPreview();
		
		// delete callback on old camera
		//if (mCamera != null)
		//	mCamera.setPreviewCallback(null);
		
		// set new camera
		mCamera = newCam;	

		// set the quality
		if (mCamera != null){
			Parameters params = mCamera.getParameters();
			Log.i("TAG", params.getJpegQuality()+"");
			params.setJpegQuality(jpegQuality);
			mCamera.setParameters(params);
		}
	}
	
	/** Determine the optimal width and height, based on max size and optimal choice */
	public static WidthHeight getBestWidthHeight(List <Size> sizes, WidthHeight maxWH, WidthHeight optWH){

		// check if none
		if (sizes.isEmpty())
			return null;
		
		// loop through possible ones and find the ones that are below the max
		ArrayList <Size> belowMax = new ArrayList<Size>();
		for (Iterator<Size> it = sizes.iterator (); it.hasNext();) {
		    Size s = it.next ();
		    if (maxWH == null)
		    	belowMax.add(s);
		    else if (s.width <= maxWH.width && s.height <= maxWH.height)
		    	belowMax.add(s);
		}
		
		// check if none
		if (belowMax.isEmpty())
			return null;
		
		// function to check optimal is diff(width)^2 + diff(height)^2, and aspect ratio is 10x more important
		WidthHeight result = new WidthHeight(0, 0);
		double fitness = 1e12;
		double tmpFitness;
		for (Iterator<Size> it = belowMax.iterator (); it.hasNext();) {
		    Size s = it.next ();
		    tmpFitness = (double) Math.sqrt(Math.pow(s.width - optWH.width, 2) + 
		    			 Math.pow(s.height - optWH.height, 2))/(optWH.height*.5+optWH.width*.5)+
		    			 Math.abs((double)optWH.width/optWH.height - (double)s.width/s.height)*10;
		    if (tmpFitness < fitness){
		    	fitness = tmpFitness;
		    	result.width = s.width;
		    	result.height = s.height;
		    }
		}
		
		// check if nothing matched
		if (result.width == 0 && result.height == 0)
			result = null;
		
		// return result
		return result;
		
	}
	
	/**
	 * Get the preview size the fits best into the given width of height
	 * @param width Width of are where preview can go in pixels
	 * @param height Height of where preview can go in pixels
	 * @param parameters camera parameters that store the possible preview sizes
	 * @return the best preview size
	 */
	public Camera.Size getBestPreviewSize(
			int width,
			int height,
			Camera.Parameters parameters) {
		Camera.Size result=null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result=size;
				}
				else {
					int resultArea=result.width * result.height;
					int newArea=size.width * size.height;

					if (newArea > resultArea) {
						result=size;
					}
				}
			}
		}

		return(result);
	}
	
	/**
	 * Return the allowable flash modes of the camera
	 * Make sure to turn on in manifest:<br>
	 * <uses-permission android:name="android.permission.CAMERA" /> <br>
  	 * <uses-feature android:name="android.hardware.camera" /> <br>
  	 * <uses-feature android:name="android.hardware.camera.autofocus" /> <br>
	 * @return a list of supported flash modes for this camera
	 */
	public List<String> getSupportedFlashModes(){
		return mCamera.getParameters().getSupportedFlashModes();
	}
	
	
	/**
	 * Set the surfaceView size to be within the set limits and scaled correctly. Also set the camera and preivew size.
	 * @param act An activity required to set some various parameters
	 * @param flashMode The flash mode desired. See getSupportedFlashModes for options. Camera.Parameters.FLASH_MODE_AUTO is a good option.
	 * @param optimalWidthHeight The desired WidthHeight to make the final picture. Null if the maximum of camera is desired.
	 * @param maxWidthHeight The max WidthHeight to make the camera size. Null if maximum is desired
	 * @param windowSize The max WidthHeight to fit the preview in. Null if the full screen is desired.
	 * @param switchOrientation if orientation of layout is opposite of orientation of camera. Usually true if layout is portrait.
	 * @param surfaceView The surfaceView to manipulate.
	 * @throws Exception if we cannot find sizes for preview or camera that are acceptable.
	 */
	public void setSurfaceSizePreviewSizeCameraSize(
			Activity act,
			String flashMode,
			WidthHeight optimalWidthHeight,
			WidthHeight maxWidthHeight,
			WidthHeight windowSize,
			boolean switchOrientation,
			SurfaceView surfaceView)
	throws Exception {

		// stop the preview
		stopPreview();
		
		// grab default parameters
		android.hardware.Camera.Parameters params = mCamera.getParameters();

		// set orientation
		int orientation = ((WindowManager) act.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
		int rotation = 0;
		if (orientation == 0)
			rotation = 90;
		else if (orientation == 3)
			rotation = 180;
		else if (orientation == 1)
			rotation = 0;
		mCamera.setDisplayOrientation(rotation);

		// set the flash mode
		params.setFlashMode(flashMode);	
		
		// get possible preview sizes and image sizes
		List <Size> sizes = params.getSupportedPictureSizes();
		List<Size> previewSizes = params.getSupportedPreviewSizes();
		
		// determine the max camera size
		WidthHeight max = new WidthHeight(0, 0);
		long pix = 0;
		for (Size item : sizes){
			long tmp = item.height*item.width;
			if (tmp > pix){
				pix = tmp;
				max.height=item.height;
				max.width=item.width;
			}
		}
		
		// optimal is the max if null was input
		if (optimalWidthHeight == null)
			optimalWidthHeight = new WidthHeight(max.width, max.height);
		
		// max is the max if null was input
		if (maxWidthHeight == null)
			maxWidthHeight = new WidthHeight(max.width, max.height);
		
		// max preview is window if null input
		if (windowSize == null){
			windowSize = new WidthHeight(
					act.getWindowManager().getDefaultDisplay().getWidth(), 
					act.getWindowManager().getDefaultDisplay().getHeight());
			if (switchOrientation)
				windowSize = windowSize.switchDimensions();
		}
		
		// get the image size that is closest to our optimal and set it
		WidthHeight bestWidthHeight = null;
		bestWidthHeight = getBestWidthHeight(sizes, maxWidthHeight, optimalWidthHeight);
		if (bestWidthHeight == null){
			throw new Exception("Could not find a camera size below maxWidthHeight and close to optimalWidthHeight");
		}else{
			params.setPictureSize(bestWidthHeight.width, bestWidthHeight.height);
		}
		
		// get the preview size that is closest to the image size
		WidthHeight bestWidthHeightPreivew = null;
		bestWidthHeightPreivew = 
			getBestWidthHeight(previewSizes, maxWidthHeight, bestWidthHeight);
		if (bestWidthHeightPreivew == null)
			throw new Exception("Could not find a camera preview size.");
		
		// determine how best to fit camera preview into surface
		params.setPreviewSize(bestWidthHeightPreivew.width, bestWidthHeightPreivew.height);
		WidthHeight fitWindowWidthHeight = ImageProcessing.fitNoCrop(bestWidthHeightPreivew, windowSize);
		if (switchOrientation)
			fitWindowWidthHeight = fitWindowWidthHeight.switchDimensions();

		// change height, but only if need be.
		if (surfaceView.getWidth() != fitWindowWidthHeight.width ||
				surfaceView.getHeight() != fitWindowWidthHeight.height){
			LayoutParams surfaceParams = surfaceView.getLayoutParams();
			surfaceParams.height = fitWindowWidthHeight.height;
			surfaceParams.width = fitWindowWidthHeight.width;
			surfaceView.setLayoutParams(surfaceParams);
		}
		
		// actually set the  parameters to camera
		mCamera.setParameters(params);		
	}
	
	/**
	 * Set the surfaceView size to be within the set limits and scaled correctly.
	 * @param act An activity required to set some various parameters
	 * @param optimalWidthHeight The desired WidthHeight to make the final picture. Null if the maximum of camera is desired.
	 * @param maxWidthHeight The max WidthHeight to make the camera size. Null if maximum is desired
	 * @param windowSize The max WidthHeight to fit the preview in. Null if the full screen is desired.
	 * @param switchOrientation if orientation of layout is opposite of orientation of camera. Usually true if layout is portrait.
	 * @param surfaceView The surfaceView to manipulate.
	 * @throws Exception if we cannot find sizes for preview or camera that are acceptable.
	 */
	public void setSurfacePreviewHolderSize(
			Activity act,
			WidthHeight optimalWidthHeight,
			WidthHeight maxWidthHeight,
			WidthHeight windowSize,
			boolean switchOrientation,
			SurfaceView surfaceView)
	throws Exception {

		// stop the preview
		stopPreview();
		
		// grab default parameters
		android.hardware.Camera.Parameters params = mCamera.getParameters();
		
		// get possible preview sizes and image sizes
		List <Size> sizes = params.getSupportedPictureSizes();
		List<Size> previewSizes = params.getSupportedPreviewSizes();
		
		// determine the max camera size
		WidthHeight max = new WidthHeight(0, 0);
		long pix = 0;
		for (Size item : sizes){
			long tmp = item.height*item.width;
			if (tmp > pix){
				pix = tmp;
				max.height=item.height;
				max.width=item.width;
			}
		}
		
		// optimal is the max if null was input
		if (optimalWidthHeight == null)
			optimalWidthHeight = new WidthHeight(max.width, max.height);
		
		// max is the max if null was input
		if (maxWidthHeight == null)
			maxWidthHeight = new WidthHeight(max.width, max.height);
		
		// max preview is window if null input
		if (windowSize == null){
			windowSize = new WidthHeight(
					act.getWindowManager().getDefaultDisplay().getWidth(), 
					act.getWindowManager().getDefaultDisplay().getHeight());
			if (switchOrientation)
				windowSize = windowSize.switchDimensions();
		}
		
		// get the image size that is closest to our optima
		WidthHeight bestWidthHeight = null;
		bestWidthHeight = getBestWidthHeight(sizes, maxWidthHeight, optimalWidthHeight);
		if (bestWidthHeight == null)
			throw new Exception("Could not find a camera size below maxWidthHeight and close to optimalWidthHeight");
		
		// get the preview size that is closest to the image size
		WidthHeight bestWidthHeightPreivew = null;
		bestWidthHeightPreivew = 
			getBestWidthHeight(previewSizes, maxWidthHeight, bestWidthHeight);
		if (bestWidthHeightPreivew == null)
			throw new Exception("Could not find a camera preview size.");
		
		// determine how best to fit camera preview into surface
		WidthHeight fitWindowWidthHeight = ImageProcessing.fitNoCrop(bestWidthHeightPreivew, windowSize);
		if (switchOrientation)
			fitWindowWidthHeight = fitWindowWidthHeight.switchDimensions();

		// change height, but only if need be.
		if (surfaceView.getWidth() != fitWindowWidthHeight.width ||
				surfaceView.getHeight() != fitWindowWidthHeight.height){
			LayoutParams surfaceParams = surfaceView.getLayoutParams();
			surfaceParams.height = fitWindowWidthHeight.height;
			surfaceParams.width = fitWindowWidthHeight.width;
			surfaceView.setLayoutParams(surfaceParams);
		}	
	}
	
	/**
	 * Start the camera preview. If it is currently running, then it will stop it and restart.
	 * If mCamera == null, then nothing will happen.
	 */
	public synchronized void startPreview(){
		if (mCamera == null){
			isPreviewRunning = false;
			return;
		}
		if (isPreviewRunning)
			mCamera.stopPreview();
		new Thread(new Runnable() {
			public void run() {
				mCamera.startPreview();
				isPreviewRunning = true;
			}
		}).start();
	}
	
	/**
	 * Stop the camera preview. If null camera or already stopped, nothing happens.
	 */
	public void stopPreview(){
		if (mCamera == null){
			isPreviewRunning = false;
			return;
		}
		if (isPreviewRunning)
			mCamera.stopPreview();
		isPreviewRunning = false;
		
	}
	
	/**
	 * Manually set if preview is currently running. <br>
	 * Normally use stopPreview or startPreview, but you can use this call if some other method has started or stopped the preivew and we
	 * want to keep track of this.
	 * @param running
	 */
	public void setIsPreviewRunning(boolean running){
		isPreviewRunning = running;
	}
	
	/**
	 * Is the preview currently running
	 * @return
	 */
	public boolean isPreviewRunning(){
		return isPreviewRunning;
	}
	
	public static abstract class OnRotationCallback{	
		public abstract void onRotation(int orientation, int lastOrientation);
	}
}
