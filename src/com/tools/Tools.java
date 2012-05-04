/**
 * 
 */
package com.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

/**
 * @author Kyle Watson
 *
 */
public class Tools {
//TODO: class not commented
	// static variables for use in methods
	/** SMS field to be inserted as a received message */
	public static final int MESSAGE_TYPE_INBOX = 1; 
	/** SMS field to be inserted as a sent message */
	public static final int MESSAGE_TYPE_SENT = 2; 
	/** field to register receiver for sent sms */
	public static final String SENT = "SMS_SENT";
	/** field to register receiver for delivered sms */
	public static final String DELIVERED = "SMS_DELIVERED";
	/** field where sms receivers can store optional string info */
	public static final String OPTION = "OPTION";
	/** field where sms receivers store the number of texts sent */
	public static final String NUM_MESSAGES = "NUM_MESSAGES";

	/** Take input of original Size object that must fit within fitSize 
	 * and output new size that preserves aspect ratio with no cropping.*
	 * @param originalSize WidthHeight object of original size object
	 * @param fitSize WidthHeight object that the new object must fit into
	 * @return a WidthHeight object that is the new size
	 */
	public static WidthHeight fitNoCrop(WidthHeight originalSize, WidthHeight fitSize){

		WidthHeight result = null;
		// width hits edge first
		if (originalSize.getAspectRatio() >= fitSize.getAspectRatio()){

			result = new WidthHeight(fitSize.width, Math.round(fitSize.width/originalSize.getAspectRatio()));

			// height hits edge first	
		}else{
			result = new WidthHeight(Math.round(fitSize.height*originalSize.getAspectRatio()), fitSize.height);
		}

		return result;
	}

	/** Take input of original Size object that must fit within fitSize 
	 * and output new size that preserves aspect ratio butt can make 
	 * image larger that fitSize that then must be cropped.
	 * @param originalSize WidthHeight object of original size object
	 * @param fitSize WidthHeight object that the new object must fit into
	 * @return a WidthHeight object that is the new size
	 */
	public static WidthHeight fitCrop(WidthHeight originalSize, WidthHeight fitSize){

		WidthHeight result = null;
		// height hits edge first
		if (originalSize.getAspectRatio() < fitSize.getAspectRatio()){

			result = new WidthHeight(fitSize.width, Math.round(fitSize.width/originalSize.getAspectRatio()));

			// height hits edge first	
		}else{
			result = new WidthHeight(Math.round(fitSize.height*originalSize.getAspectRatio()), fitSize.height);
		}

		return result;
	}

	/** attempts to save byte data from camera to the next default location on the SDcard. 
	 * Does not throw any exceptions, but returns success and any exceptions that were
	 * thrown as strings. Will return filename saved if successful.<p>
	 * make sure to put this in your manifest:<br>
	 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
	 * @param ctx Context where various android data is pulled from, usually getApplicationContext
	 * @param data Byte array of data to be stored
	 * @param caption Caption to be stored for picture file
	 * @param displayToast Boolean to display toast message
	 * @param fileNameInput write data to this filename. If null, then writes to next available file on external storage
	 * @param exifOrientation the orientation to store in exif header. See ExifInterface for details. Input null to not save anything.
	 * @param showImageInScanner boolean to show the image in the media scanner. Usually true
	 * */
	public static SuccessReason saveByteDataToFile(
			Context ctx, 
			byte[] data, 
			String caption, 
			Boolean displayToast, 
			String fileNameInput,
			Integer exifOrientation,
			boolean showImageInScanner){

		//TODO: save correct gps data
		//TODO: this whole method needs to be cleaned up

		// initialize result and fileName
		SuccessReason result = new SuccessReason(true);
		String fileName = "";

		// try catch wrapper over entire class
		try{
			// create values to store in file, just type and date
			ContentValues values = new android.content.ContentValues(2);
			values.put(MediaColumns.DATE_ADDED, System.currentTimeMillis()); 
			values.put(MediaColumns.MIME_TYPE, "image/jpeg");

			// store into database
			Uri uriTarget = null;
			if (fileNameInput == null){
				uriTarget = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

				// grab filename
				fileName = getFileNameUri(ctx, uriTarget);
			}else
				fileName = fileNameInput;

			// write file
			FileOutputStream imageFileOS = null;
			try {
				if (fileNameInput==null)
					imageFileOS = (FileOutputStream) ctx.getContentResolver().openOutputStream(uriTarget);
				else{
					try{
						imageFileOS = ctx.openFileOutput(fileNameInput, Context.MODE_WORLD_WRITEABLE);
					}catch (Exception e) {
						// likely had a file separator, so just write directly to that
						imageFileOS = new FileOutputStream(fileNameInput);
					}
				}
				imageFileOS.write(data);
				imageFileOS.flush();
				imageFileOS.close();

				// display toast
				if (displayToast)
					Toast.makeText(ctx,
							"Image saved: " + fileName,
							Toast.LENGTH_LONG).show();

				// error catch	
			} catch (FileNotFoundException e) {
				result = new SuccessReason(false, result.getReason()+" "+e.getLocalizedMessage());
			} catch (IOException e) {
				result = new SuccessReason(false, result.getReason()+" "+e.getLocalizedMessage());
			}

			// add orientation data and/or gps
			try{
				if (exifOrientation != null){
					ExifInterface EI = new ExifInterface(fileName);
					EI.setAttribute(ExifInterface.TAG_ORIENTATION, ""+exifOrientation);
					//EI.setAttribute(ExifInterface.TAG_GPS_LATITUDE, Tools.convertAngletoString(15.42));
					//EI.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "12/1,2/1,300/100");
					//EI.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
					//EI.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
					EI.saveAttributes();
				}

			}catch(Exception e){
				result = new SuccessReason(false, result.getReason()+" "+e.toString());
			}	

			if (showImageInScanner)
				MediaScannerConnection.scanFile(ctx, new String[] {fileName}, new String[] {"image/jpeg"}, null);
		}catch(Exception e){
			result = new SuccessReason(false, result.getReason()+" "+e.toString());
		}

		// no exceptions then success
		if (result.getReason().length()==0)
			result = new SuccessReason(true, fileName);

		return result;
	}

	public static String getFileNameUri(Context ctx, Uri uri){

		String[] projection = { MediaStore.Images.ImageColumns.DATA,
				MediaStore.Images.ImageColumns.DISPLAY_NAME}; 

		String result = null;
		Cursor cur = ctx.getContentResolver().query(uri, projection, null, null, null); 
		if (cur!=null && cur.moveToFirst()) { 
			result = cur.getString(0); 
		} 
		if (cur != null)
			cur.close();

		return result;
	} 

	public static String convertAngletoString(double angle){

		int deg;
		int min;
		int sec;

		deg = (int) angle;
		min = (int) ((angle - deg)*60);
		sec = (int) ((angle - (deg + min/60.0))*3600);
		return deg+"/1,"+min+"/1,"+sec*100+"/100";
	}

	/** Grab phones full number, see getMyStrippedPhoneNumber for removing +1*/
	public static String getMyPhoneNumber(Activity act){  
		TelephonyManager mTelephonyMgr;  
		mTelephonyMgr = (TelephonyManager)  
		act.getSystemService(Context.TELEPHONY_SERVICE);   
		return mTelephonyMgr.getLine1Number(); 
	}  

	/** Grab phone number with leading +1 removed... if there */
	public static String getMyStrippedPhoneNumber(Activity act){  
		String s = getMyPhoneNumber(act); 

		if (s != null){
			// strip off 1 and +
			if (s.length()==0)
				return s;
			if (s.charAt(0) == '+')
				s = s.substring(1);
			if (s.length()==0)
				return s;
			if (s.charAt(0) == '1')
				s = s.substring(1);
		}

		return s; 
	}

	/** Eliminate all but numerics from a string*/
	public static String keepOnlyNumerics(String input){
		return input.replaceAll("[^0-9]", "");
	}

	/** Format phone number to 123-456-7890.
	 * If it cannot be formatted this way, then simply
	 * the incoming string is returned */
	public static String formatPhoneNumber(String input){

		String out = input;

		if (out == null || out.length() == 0)
			return out;
		// strip off 1 and +
		if (out.charAt(0) == '+')
			out = out.substring(1);
		if (out.length() == 0)
			return out;
		if (out.charAt(0) == '1')
			out = out.substring(1);

		out = keepOnlyNumerics(out);
		if (out.length() != 10)
			out = input;
		else
			out = out.substring(0, 3)+"-"+out.substring(3, 6)+"-"+out.substring(6);

		return out;
	}

	/** get the string indicated by the tag in the given xml string
	 * Return null if not found*/
	public static String getXmlValueAtTag(String xml, String tag){

		// null inputs
		if (xml==null || tag==null)
			return null;

		// look for tag
		int begOfFirstTag = xml.indexOf("<"+tag);
		if (begOfFirstTag==-1)
			return null;

		// find end of tag
		int endOfFirstTag = xml.indexOf(">", begOfFirstTag+tag.length()+1);
		if (endOfFirstTag==-1)
			return null;

		// look for beginning of end tag
		int begOfSecondTag = xml.indexOf("</"+tag, endOfFirstTag+1);
		if (begOfSecondTag==-1)
			return null;

		// find end of end tag
		int endOfSecondTag = xml.indexOf(">", begOfSecondTag+tag.length()+2);
		if (endOfSecondTag==-1)
			return null;

		// all tags were found, so simply look between tags
		return xml.substring(endOfFirstTag+1, begOfSecondTag);

	}

	/**
	 * Hide the keyboard.
	 * @param ctx The context to perform this action
	 * @param view Any view in the context that can request the keyboard to be hidden
	 */
	public static void hideKeyboard(Context ctx, View view){
		InputMethodManager imm = (InputMethodManager)ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	/**
	 * Show the keyboard.
	 * @param ctx The context to perform this action
	 * @param view Any view in the context that can request the keyboard to be shown
	 */
	public static void showKeyboard(Context ctx, View view){
		InputMethodManager imm = (InputMethodManager)ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(view, 0);
	}

	/** Resize a byte array keeping aspect ratio. Will either
	 * crop the data, fill extra data with black bars, or resize the image 
	 * to as close to newWidthHeight, but not guaranteed.
	 * @author Kyle Watson
	 * @param input Byte array input data
	 * @param cropFlag "crop", "blackBars", "resizeLarge", "resizeSmall" options for what 
	 * to do with image that doesn't fit new size. 
	 * @param ctx Context context, usually getApplicationContext,
	 * if null, then we cannot find orientation angle from inside byte array
	 * @param newWidthHeight New desired width and height
	 * @param orientationAngle Float for the orientation of the byte array. If the data
	 * is already stored in the byte array, then pass null and the value will be extracted.
	 * @throws IllegalArgumentException if cropFlag is not the right input type
	 */
	public static byte[] resizeByteArray(byte[] input, 
			WidthHeight newWidthHeight, 
			String cropFlag, 
			Context ctx, 
			Float orientationAngle) 
	throws IllegalArgumentException{

		// grab orientation angle from exif data
		if (orientationAngle == null && ctx != null)
			orientationAngle = getExifOrientationAngle(input, ctx);
		if (orientationAngle == null)
			orientationAngle = 0f;

		// create bitmap from data
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inDither = true;
		Bitmap bitmapOrg = BitmapFactory.decodeByteArray(input, 0, input.length, opt);

		// grab width and height from bitmap
		int width = bitmapOrg.getWidth();
		int height = bitmapOrg.getHeight();

		// check rotation to see if we need to switch width and height
		if ((int) Math.round(orientationAngle) % (int) 180 == 90){
			int tmp = width;
			width = height;
			height = tmp;
		}

		// check if no resizing required
		if (width == newWidthHeight.width && height == newWidthHeight.height && 
				orientationAngle == 0)
			return input;

		// find new width and height for temporary bitmap object
		WidthHeight tmpWidthHeight = null;
		if (cropFlag.compareToIgnoreCase("crop")==0 ||
				cropFlag.compareToIgnoreCase("resizeLarge")==0)
			tmpWidthHeight = com.tools.Tools.fitCrop
			(new WidthHeight(width, height), newWidthHeight);
		else if (cropFlag.compareToIgnoreCase("blackBars")==0 || 
				cropFlag.compareToIgnoreCase("resizeSmall")==0)
			tmpWidthHeight = com.tools.Tools.fitNoCrop
			(new WidthHeight(width, height), newWidthHeight);
		else
			throw new IllegalArgumentException
			("resizeByteArray cropFlag must have \"crop\", \"blackBars\", \"resizeLarge\", or \"resizeSmall\" as an input");

		// grab height and width
		int newWidth = tmpWidthHeight.width;
		int newHeight = tmpWidthHeight.height;

		// calculate the scale
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();
		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);
		// set rotation angle based on exif data
		if (orientationAngle != 0)
			matrix.postRotate(orientationAngle);

		// check rotation to see if we need to switch back to original width and height
		if ((int) Math.round(orientationAngle) % (int) 180 == 90){
			int tmp = width;
			width = height;
			height = tmp;
		}

		// recreate the new Bitmap
		Bitmap tmpResizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0,
				width, height, matrix, true);

		Bitmap resizedBitmap = null;

		// if resizeSmall or resizeLarge, then tmpResizedBitmap is the same as final.
		// Also if the desired output is the same as the tmpSize then, also just copy over
		if (cropFlag.compareToIgnoreCase("resizeSmall")==0 ||
				cropFlag.compareToIgnoreCase("resizeLarge")==0 ||
				(tmpResizedBitmap.getWidth() == newWidthHeight.width && 
						tmpResizedBitmap.getHeight() == newWidthHeight.height))
			resizedBitmap = tmpResizedBitmap;

		// crop option, we will grab a subset of the tmpBitmap
		else if (cropFlag.compareToIgnoreCase("crop")==0){
			resizedBitmap = Bitmap.createBitmap
			(newWidthHeight.width, newWidthHeight.height, Bitmap.Config.RGB_565);
			int[] pixels = new int[resizedBitmap.getWidth()*resizedBitmap.getHeight()];
			int x = (int) Math.round((tmpResizedBitmap.getWidth() - resizedBitmap.getWidth())/2.0);
			int y = (int) Math.round((tmpResizedBitmap.getHeight() - resizedBitmap.getHeight())/2.0);
			tmpResizedBitmap.getPixels(pixels, 0, resizedBitmap.getWidth(), x, y, 
					resizedBitmap.getWidth(), resizedBitmap.getHeight());
			resizedBitmap.setPixels(pixels, 0, resizedBitmap.getWidth(), 0, 0, 
					resizedBitmap.getWidth(), resizedBitmap.getHeight());
		}

		// the blackBars option, we create a new bitmap that is larger and fill with tmpBitmap
		else {
			resizedBitmap = Bitmap.createBitmap
			(newWidthHeight.width, newWidthHeight.height, Bitmap.Config.RGB_565);
			int[] pixels = new int[resizedBitmap.getWidth()*resizedBitmap.getHeight()];
			int x = (int) -Math.round((tmpResizedBitmap.getWidth() - resizedBitmap.getWidth())/2.0);
			int y = (int) -Math.round((tmpResizedBitmap.getHeight() - resizedBitmap.getHeight())/2.0);
			tmpResizedBitmap.getPixels(pixels, 0, tmpResizedBitmap.getWidth(), 0, 0, 
					tmpResizedBitmap.getWidth(), tmpResizedBitmap.getHeight());
			resizedBitmap.setPixels(pixels, 0, tmpResizedBitmap.getWidth(), x, y, 
					tmpResizedBitmap.getWidth(), tmpResizedBitmap.getHeight());
		}

		// turn back into byte array
		ByteArrayOutputStream out = new ByteArrayOutputStream(resizedBitmap.getWidth()*resizedBitmap.getHeight());
		resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);   
		byte[] result = out.toByteArray();

		return result;		
	}

	/** Rotate a byte array keeping aspect ratio. 
	 * @param input Byte array input data
	 * @param ctx Context context, usually getApplicationContext
	 * @param orientationAngle Float for the orientation of the byte array. If the data
	 * is already stored in the byte array, then pass null and the value will be extracted.
	 * @throws IllegalArgumentException if cropFlag is not the right input type
	 */
	public static byte[] rotateByteArray(
			byte[] input, 
			Context ctx, 
			Float orientationAngle){

		// grab orientation angle from exif data
		if (orientationAngle == null)
			orientationAngle = getExifOrientationAngle(input, ctx);

		// create bitmap from data
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inDither = true;
		Bitmap bitmapOrg = BitmapFactory.decodeByteArray(input, 0, input.length, opt);

		// grab width and height from bitmap
		int width = bitmapOrg.getWidth();
		int height = bitmapOrg.getHeight();

		// check rotation to see if we need to switch width and height
		if ((int) Math.round(orientationAngle) % (int) 180 == 90){
			int tmp = width;
			width = height;
			height = tmp;
		}

		// check if no rotating required
		if (orientationAngle == 0)
			return input;

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();

		// set rotation angle based on exif data
		if (orientationAngle != 0)
			matrix.postRotate(orientationAngle);

		// check rotation to see if we need to switch back to original width and height
		if ((int) Math.round(orientationAngle) % (int) 180 == 90){
			int tmp = width;
			width = height;
			height = tmp;
		}

		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0,
				width, height, matrix, true);

		// turn back into byte array
		ByteArrayOutputStream out = new ByteArrayOutputStream(resizedBitmap.getWidth()*resizedBitmap.getHeight());
		resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);   
		byte[] result = out.toByteArray();

		return result;		
	}


	public static ExifInterface readExifDataFromByteArray(byte[] data, Context ctx){

		// write data to file
		SuccessReason tmp = saveByteDataToFile(ctx, data, "", false, null, null, true);//, tmpFile);
		if (!tmp.getSuccess())
			return null;
		String tmpFile = tmp.getReason();

		// read exif data from file
		ExifInterface exif;
		try {
			exif = new ExifInterface(tmpFile);
		} catch (IOException e) {
			exif = null;
		}

		// delete file
		new File(tmpFile).delete();

		return exif;
	}

	public static float getExifOrientationAngle(ExifInterface exif){

		// null exif, return 0 angle
		if (exif==null)
			return 0;

		// read orienation
		int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 
				ExifInterface.ORIENTATION_NORMAL);

		// these are the angle corresponding to these orientations
		float angle = 0;
		if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
			angle = 90;
		else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
			angle = 180;
		else if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
			angle = 270;

		return angle;
	}

	public static float getExifOrientationAngle(byte[] data, Context ctx){

		// read exif from byte array and then read the angle data
		return getExifOrientationAngle(readExifDataFromByteArray(data, ctx));
	}

	public static float getExifOrientationAngle(String file){

		// read exif data
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(file);
		} catch (IOException e) {
		}

		// grab the angle from exif data
		return getExifOrientationAngle(exif);
	}

	/**
	 * Send an sms message. If message is empty or null, or if the phoneNumber is empty or null,
	 *  then nothing happens and simply returns. Can send multi-part messages > 160 characters.
	 *  To pick up action on the receive and delivered put this code in you main context. Also
	 *  make sure that the receivers are only turned on once, and turned off when not needed 
	 *  anymore else, they will be called over and over. Any user info is stored in the broadcast intents
	 *  under com.tools.Tools.OPTION, and the total number of texts sent (for multi-part-texts) is saved
	 *  under com.tools.Tools.NUM_MESSAGES.
	 *  * <pre>
	 * {@code
	 * ctx.registerReceiver(new BroadcastReceiver(){ ... }, new IntentFilter(com.tools.Tools.SENT));
	 * ctx.registerReceiver(new BroadcastReceiver(){ ... }, new IntentFilter(com.tools.Tools.DELIVERED));
	 * </pre>
	 * @param ctx The context to use
	 * @param phoneNumber The phone number to send it to
	 * @param message The message
	 * @param option A string to be sent with the intent for optional info. It is stored in the intent as com.tools.Tools.OPTION
	 * @return the total number of texts attempted to be sent, look in receiver for final notification
	 */
	public static int sendSMS(final Context ctx, String phoneNumber, String message, String option){

		// break out if empty message
		if (message == null 
				|| message.length()==0
				|| phoneNumber == null
				|| phoneNumber.length() == 0)
			return 0;

		// grab the sms manager
		SmsManager sms = SmsManager.getDefault();

		// see if we need to break up the message
		int nMessages = 1;
		ArrayList<String> parts = null;
		if (message.length() > 160){
			parts = sms.divideMessage(message);
			nMessages = parts.size();
		}

		// create intent for sending
		Intent intentSent = new Intent(SENT);
		intentSent.putExtra(OPTION, option);
		intentSent.putExtra(NUM_MESSAGES, nMessages);
		PendingIntent sentPI = PendingIntent.getBroadcast(ctx, 0,
				intentSent, PendingIntent.FLAG_UPDATE_CURRENT);

		// create intent for delivering
		Intent intentDelivered = new Intent(SENT);
		intentDelivered.putExtra(OPTION, option);
		intentDelivered.putExtra(NUM_MESSAGES, nMessages);
		PendingIntent deliveredPI = PendingIntent.getBroadcast(ctx, 0,
				intentDelivered, PendingIntent.FLAG_UPDATE_CURRENT);   

		// send the messages
		if (nMessages > 1){
			for (int i = 0; i < parts.size(); i++)
				sms.sendTextMessage(phoneNumber, null, parts.get(i), sentPI, deliveredPI);
		}else{
			sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
		}

		// save text into database
		insertSMSDatabse(ctx, phoneNumber, message, MESSAGE_TYPE_SENT);

		return nMessages;

		// example broadcast receivers
		/*
		//---when the SMS has been sent---
		ctx.registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode())
				{
				case Activity.RESULT_OK:
					Toast.makeText(ctx, "SMS sent", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(ctx, "Generic failure", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(ctx, "No service", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(ctx, "Null PDU", 
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(ctx, "Radio off", 
							Toast.LENGTH_SHORT).show();
					break;
				}
			}
		}, new IntentFilter(SENT));

		//---when the SMS has been delivered---
		ctx.registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode())
				{
				case Activity.RESULT_OK:
					Toast.makeText(ctx, "SMS delivered", 
							Toast.LENGTH_SHORT).show();
					break;
				case Activity.RESULT_CANCELED:
					Toast.makeText(ctx, "SMS not delivered", 
							Toast.LENGTH_SHORT).show();
					break;                        
				}
			}
		}, new IntentFilter(DELIVERED));    
		 */ 
	}

	/**
	 * Insert a message
	 * @param ctx The context to use to perform this action
	 * @param phoneNumber The phone number attached to the sms
	 * @param message The message of the sms
	 * @param messageType the message is either sent (com.tools.Toos.MESSAGE_TYPE_SENT) 
	 * or received (com.tools.Tools.MESSAGE_TYPE_INBOX)
	 * @return The url of the sms database in which the message was inserted
	 */
	public static Uri insertSMSDatabse(Context ctx,
			String phoneNumber, 
			String message, 
			int messageType){

		// the tags for various setting to go into the database
		//final String PERSON = "person"; 
		final String ADDRESS = "address"; 
		final String DATE = "date"; 
		final String READ = "read"; 
		final String STATUS = "status"; 
		final String TYPE = "type"; 
		final String BODY = "body";  

		// default values
		int status = -1;
		int read = 1;

		// grab current data
		Date date = new Date();

		ContentValues values = new ContentValues(); 
		values.put(ADDRESS, phoneNumber); 
		values.put(DATE, String.valueOf(date.getTime())); 
		values.put(READ, read); 
		values.put(STATUS, status); 
		values.put(TYPE, messageType); 
		values.put(BODY, message); 
		Uri inserted = null;
		try{
			inserted = ctx.getContentResolver().insert
			(Uri.parse("content://sms//sent"), values);
		}catch(Exception e){
			e.printStackTrace();
		}

		return inserted;
	}

	private class SmsSentClass extends Activity {

		@Override
		protected void onCreate(Bundle savedInstanceState){
			int kyle = 6;
		}
	}

	/** Show a dialog that will only be shown once at startup. 
	 * This inputs in the calling ctx are required:
	 * SharedPreferences mPrefs;
	 * final String welcomeScreenShownPref = "welcomeScreenShown";
	 * 
	 * @param ctx The context where this dialog will be displayed
	 * @param prefs The preferences object that must present in the calling context/activity
	 * @param welcomeScreenShownPref The string where the preference is stored
	 * @param title The title of the box
	 * @param text The text in the body
	 * @param toShow Boolean whether to show or not. Input null to read from preferences
	 */
	public static void showOneTimeDialog(Context ctx,
			SharedPreferences prefs,
			String welcomeScreenShownPref,
			String title,
			String text, 
			Boolean toShow){
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

		// second argument is the default to use if the preference can't be found
		if (toShow == null)
			toShow = !prefs.getBoolean(welcomeScreenShownPref, false);

		if (toShow) {
			// here you can launch another activity if you like
			// the code below will display a popup

			new AlertDialog.Builder(ctx).
			setIcon(android.R.drawable.ic_dialog_info).
			setTitle(title).setMessage(Html.fromHtml(text)).
			setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).show();
			if (prefs != null){
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean(welcomeScreenShownPref, true);
				editor.commit(); // Very important to save the preference
			}
		}
	}

	/**
	 * Create a new group in the android databse
	 * @param ctx The context to use
	 * @param accountType the account type, for example "com.google", null for phone
	 * @param accountName The account name, for example "user@gmail.com", null for phone
	 * @param groupName The group name, fore example "friends"
	 * @return The group ID is returned
	 */
	public static String makeNewGroup(Context ctx,
			String accountType,
			String accountName,
			String groupName){

		// grab the content resolver, put group name, accountType, and accountName into database and create
		ContentResolver cr = ctx.getContentResolver();
		ContentValues groupValues = new ContentValues();
		groupValues.put(ContactsContract.Groups.TITLE, groupName);
		groupValues.put(ContactsContract.Groups.ACCOUNT_NAME, accountName);
		groupValues.put(ContactsContract.Groups.ACCOUNT_TYPE, accountType);
		Uri uri = cr.insert(ContactsContract.Groups.CONTENT_URI, groupValues);

		// the group ID
		return uri.getLastPathSegment();
	}

	/**
	 * Take in an amount of minutes and convert it to a human readable output.
	 * For example, if minutes=384, then the output will be:
	 * "6 hours and 24 minutes".
	 * <p></p>
	 * The allowable outputs are years (365 days), weeks, days, hours, minutes, seconds
	 * <p></p>
	 * @param minutes
	 * @return
	 */
	public static String convertMinutesToFormattedString(double minutes){

		// the output string
		String output = "";

		// find how many years
		int years = (int) Math.floor(minutes/(365*60*24));
		if (years > 0){
			output = years + " years, ";
			minutes = minutes - (years*365*60*24);
		}

		// how many weeks
		int weeks = (int) Math.floor(minutes/(60*24*7));
		if (weeks > 0){
			output += weeks + " weeks, ";
			minutes = minutes - (weeks*60*24*7);
		}

		// how many days
		int days = (int) Math.floor(minutes/(60*24));
		if (days > 0){
			output += days + " days, ";
			minutes = minutes - (days*60*24);
		}

		// how many hours
		int hours = (int) Math.floor(minutes/(60));
		if (hours > 0){
			output += hours + " hours, ";
			minutes = minutes - (hours*60);
		}

		// how many minutes
		int minutesDisp = (int) Math.floor(minutes);
		if (minutesDisp > 0){
			output += minutesDisp + " minutes, ";
			minutes = minutes - (minutesDisp);
		}

		// how many seconds
		float seconds =  (float) (minutes*60.0);
		if (seconds > 0){
			output += seconds + " seconds, ";
		}

		// return the string minus the last comma and space
		return output.substring(0, output.length()-2);		
	}

	/**
	 * Request a sync for all your accounts. Make sure to include:
	 * <uses-permission android:name="android.permission.READ_SYNC_SETTINGS"/> 
	 * in your manifest
	 * @param ctx
	 */
	public static void requestSync(Context ctx)
	{
		try{
			AccountManager am = AccountManager.get(ctx);
			Account[] accounts = am.getAccounts();

			for (Account account : accounts)
			{
				int isSyncable = ContentResolver.getIsSyncable(account, ContactsContract.AUTHORITY);

				if (isSyncable > 0)
				{
					Bundle extras = new Bundle();
					extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
					ContentResolver.requestSync(account, ContactsContract.AUTHORITY, extras);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Take string as input (a phone number) and pull off all non numeric characters and any leading 1 or +
	 * @param input
	 * @return The only numeric phone number
	 */
	public static String fixPhoneString(String input){

		if (input == null)
			return input;

		// only numerics
		String out = com.tools.Tools.keepOnlyNumerics(input);

		// strip off 1 and +
		if (out.length()==0)
			return out;
		if (out.charAt(0) == '+')
			out = out.substring(1);
		if (out.length()==0)
			return out;
		if (out.charAt(0) == '1')
			out = out.substring(1);

		return out;
	}

	/**
	 * Determine if folder is empty. If it has empty folders in it, that is still considered empty.
	 * @param folderPath The path to determine if it is empty.
	 * @return true if folder is empty or only contains empty folders, false otherwise
	 */
	public static boolean isFolderEmpty(String folderPath){
		// create file object
		File folder = new File(folderPath);

		// if it's a file, return false, otherwise iterate through the folder
		if (folder.isDirectory()){
			// get the list of files and check if they themselves are empty
			File[] files = folder.listFiles();
			for (int i = 0; i < files.length; i++){
				if (!isFolderEmpty(files[i].getAbsolutePath()))
					return false;
			}
		}else
			return false;

		// if we got here, then it must be empty
		return true;	
	}

	/**
	 * Delete a folder and all of its sub folders given they are empty. <br>
	 * If they are not empty, then nothing happens
	 * @param folder The folder to delete
	 */
	public static void deleteEmptyFolder(File folder) {
		if (!isFolderEmpty(folder.getAbsolutePath()))
			return;
		File[] files = folder.listFiles();
		if(files!=null) { //some JVMs return null for empty dirs
			for(File f: files) {
				if(f.isDirectory()) {
					deleteEmptyFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}
	
	/**
	 * Parse an arraylist into a list separated by the input delim.
	 * @param array the array to put into a string
	 * @param delim the delimiter to user when separatting strings (ie ",")
	 * @return A string, ie "bob,jane,bill"
	 * @see setArrayFromString
	 */
	public static <TYPE>String parseArrayIntoString(ArrayList<TYPE> array, String delim){
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < array.size()-1; i++){
			builder.append(array.get(i).toString());
			builder.append(delim);
		}
		if (array.size() >= 1){
			builder.append(array.get(array.size()-1).toString());
			builder.append(delim);
		}
		return builder.toString();
	}
	/**
	 *  Parse a string into an arraylist separating by delimiter
	 *  @param listString the string to parse 
	 * 	@param delim the delimiter that separates strings
	 * 	@return An arrayList of the individual strings
	 *  @see parseArrayIntoString
	 */
	public static ArrayList<String> setArrayFromString(String listString, String delim){
		if (listString == null)
			return new ArrayList<String>(0);
		
		// break up string by commas
		String[] tokens = listString.split(delim);
		ArrayList<String> array = new ArrayList<String>(tokens.length);
		
		// fill array
		for (int i = 0; i < tokens.length; i++){
			array.add(tokens[i]);
		}
		
		return array;
	}
	
	/**
	 * Create a byte array thumbnail from the input image byte array. <br>
	 * Will take into account the exifOrientation, but can only handle rotations, not transposing or inversions. <br>
	 * This can only rescale by integer amounts. For example if original image is 128x128, and you input
	 * maxThumbnailDimension as 100, we can only rescale by a factor of 2, so the image will be 64x64. <p>
	 * *** Also If you input too large of a maxThumbnailDimension, you may crash due to memory overflow ***
	 * However this is memory intelligent, meaning it doesn't load the whole bitmap into memory and then resize,
	 * but only sub-samples the image. This is why we can only scale by integer amounts. 
	 * @param imageData The image data to resize
	 * @param exifOrientation the exifOrientation tag. If unknown tag, no rotation is assumed. @See ExifOrientation
	 * @param maxThumbnailDimension The maximum thumbnail dimension in either height or width
	 * @param forceBase2 If we force to downsample by base2, it is faster, but then we can only
	 * resize by a factor of 2,4,8,16...
	 * @return The resized and rotated thumbnail. So the new orientation tag is ExifInterface.ORIENTATION_NORMAL
	 */
	public static byte[] makeThumbnail(
			final byte[] imageData,
			int exifOrientation,
			final int maxThumbnailDimension,
			boolean forceBase2){
			
		if (imageData == null || imageData.length == 0)
			return null;
		
		// determine the size of the image first, so we know at what sample rate to use.
		BitmapFactory.Options options=new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
		double scale = ((double)Math.max(options.outHeight, options.outWidth))/maxThumbnailDimension;
		
		// convert to integer scaling ratio to base 2 or not depending on input
		int intScale = 1;
		if (forceBase2)
			intScale = (int)Math.pow(2, Math.ceil(com.tools.MathTools.log2(scale)));
		else
			intScale = (int) Math.ceil(scale);
		
		// now actually do the resizeing
		BitmapFactory.Options options2 = new BitmapFactory.Options();
		options2.inSampleSize = intScale;
		Bitmap thumbnailBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options2);
		
		// determine the rotation angle
		int angle = 0;
		switch (exifOrientation){
		case ExifInterface.ORIENTATION_NORMAL:
			// do nothing
			break;
		case ExifInterface.ORIENTATION_ROTATE_90:
			angle = 90;
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			angle = 180;
			break;
		case ExifInterface.ORIENTATION_ROTATE_270:
			angle = 270;
			break;
		}
		
		// now do the rotation
		if (angle != 0) {
	        Matrix matrix = new Matrix();
	        matrix.postRotate(angle);

	        thumbnailBitmap = Bitmap.createBitmap(thumbnailBitmap, 0, 0, thumbnailBitmap.getWidth(),
	        		thumbnailBitmap.getHeight(), matrix, true);
	    }
		
		// convert back to byte array.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();  
		thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		byte[] byteArray = baos.toByteArray();
		
		// return result
		return byteArray;
	}
	
	/**
	 * Generate a random string using 0-9, and a-v
	 * @param nCharacters The number of characters to create
	 * @return The random string
	 */
	public static String randomString(int nCharacters){
		SecureRandom random = new SecureRandom();
		return new BigInteger(nCharacters*5, random).toString(32);
	}
	
	/**
	 * Post a notification to the notification bar
	 * @param act The calling activity
	 * @param icon The id to the icon to use
	 * @param tickerText The text that shows in the notification bar
	 * @param contentTitle The bolded text that shows once the user pulls down the menu
	 * @param contentText The non-bolded text to show when the user pulls down the menu
	 * @param notificationId An id used to keep track of what notification this is. If
	 * you post a new notification with the same ID, and an old one is still present,
	 * the old one will be overwritten.
	 * @param intentToLaunch the intent to launch when the user click the notification. Use null for nothing.
	 */
	public static void postNotification(
			Context ctx,
			int icon,
			String tickerText,
			String contentTitle,
			String contentText,
			int notificationId,
			Intent intentToLaunch){

		// load the manager
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(ns);

		//Instantiate the Notification:
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);

		//Define the notification's message and PendingIntent:
		Context context = ctx.getApplicationContext();
		if (intentToLaunch == null)
			intentToLaunch = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intentToLaunch, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		//Pass the Notification to the NotificationManager:
		mNotificationManager.notify(notificationId, notification);
	}
	
	/**
	 * Rotate the exif data in a picture by 90 degrees.
	 * @param filePath The path of the file
	 * @param direction any negative number for ccw, any positive for cw, and 0 does nothing
	 * @throws IOException 
	 */
	public static void rotateExif(String fileName, int direction){
		
		// 0 rotation
		if (direction == 0)
			return;
		
		ExifInterface EI;
		try {
			EI = new ExifInterface(fileName);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		// get the angle
		float angle = getExifOrientationAngle(fileName);
		
		// rotate the angle
		if (direction < 0)
			angle = angle - 90;
		else
			angle = angle + 90;
		
		// modulate by 360
		while (angle < 0)
			angle += 360;
		angle = angle % 360;
		
		// determine the rotation angle
		int exifOrientation = ExifInterface.ORIENTATION_UNDEFINED;
		switch ((int)angle){
		case 0:
			exifOrientation = ExifInterface.ORIENTATION_NORMAL;
			break;
		case 90:
			exifOrientation = ExifInterface.ORIENTATION_ROTATE_90;
			break;
		case 180:
			exifOrientation = ExifInterface.ORIENTATION_ROTATE_180;
			break;
		case 270:
			exifOrientation = ExifInterface.ORIENTATION_ROTATE_270;
			break;
		}
		
		EI.setAttribute(ExifInterface.TAG_ORIENTATION, ""+exifOrientation);
		try {
			EI.saveAttributes();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * Share a picture with a sharing intent
	 * @param ctx The context required to start the intent
	 * @param subject The subject of the message to send
	 * @param body The body in the message
	 * @param fileName The filename to send. 
	 * @param prompt The prompt to the user when selecting an app to share the picture with
	 * @return true if we could launch the intent
	 */
	public static boolean sharePicture(Context ctx, String subject, String body, String fileName, String prompt){
		// if any values are null, then return false
		if (ctx == null || subject == null || body == null || prompt == null || fileName == null || fileName.length() == 0)
			return false;
		
		// grab the file
		File file = new File(fileName);
		
		// if no exist, then return false
		if (!file.exists())
			return false;
		
		// create the intent
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		
		// set the type
		sharingIntent.setType("image/jpeg");
		
		// create and load the message and subject
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
		
		// put picture in intent
		sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
		
		// launch the intent
		ctx.startActivity(Intent.createChooser(sharingIntent, prompt));
		
		// return true
		return true;
	}
	
	/**
     * Try to create the thumbnail from the full picture reading any exif data.
     * @param fullFile the path to the full file
     * @param maxPixelSize the maximum sixe in pixels for any dimension of the thumbnail. 
     * @param forceBase2 forcing the downsizing to be powers of 2 (ie 2,4,8). Faster, but obviously less specific size is allowable.
     * @return the bitmap, or null if any errors occured
     */
    public static Bitmap getThumbnail(
    		String fullFile,
    		int maxPixelSize,
    		boolean forceBase2){
    	
    	// open the full file
    	if (fullFile == null || fullFile.length() == 0)
    		return null;
    	RandomAccessFile f = null;
    	try{
    		f = new RandomAccessFile(fullFile, "r");
    	}catch (FileNotFoundException  e){
    		return null;
    	}
    	
    	// read the file data
    	byte[] b = null;
    	ExifInterface exif = null;
    	try{
    		b = new byte[(int)f.length()];
    		f.read(b);
    		f.close();
    		
    		// read the orientaion
   		 	exif = new ExifInterface(fullFile);
    	}catch(IOException e){
    		e.printStackTrace();
    		return null;
    	}

    	// grab the rotation
		int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 
				ExifInterface.ORIENTATION_UNDEFINED);
		
		// create the byte array
		byte[] thumbnail = com.tools.Tools.makeThumbnail(
				b,
				rotation,
				maxPixelSize,
				forceBase2);
		
		if (thumbnail == null)
			return null;
		
		// convert to bitmap
		return BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
	}
    
    /**
     * Save the view and all its children to a bitmap <br>
     * @param view the view to save
     * @param viewsToHide a list of views to hide before creating the bitmap, null is acceptabl
     * @param viewToTakeFocus allow for the given view to take focus. Null is accetpable
     * This is helpful, because if you selected a button to trigger this method, the button will be highlighted
     * A layout view is a usually a good choice
     * @param nullColor the color to ignore and to only extract the center region, for example to chop the black edges
     * enter Color.rgb(0, 0, 0); Enter null to ignore
     * @return the bitmap created, null if error occured
     */
    public static Bitmap saveViewToBitmap(View view, ArrayList<View> viewsToHide, View viewToTakeFocus, Integer nullColor){
		
    	if (view == null){
    		Log.e("com.tools", "null view was input");
    		return null;
    	}
    	
		// find which view has focus and turn it off
		int childHasFocus = -1;
		if (view instanceof ViewGroup){
			ViewGroup vg = (ViewGroup) view;
			for (int i = 0; i < vg.getChildCount(); i++){
				View v = vg.getChildAt(i);
				if (v.hasFocus()){
					childHasFocus = i;
					break;
				}
			}
		}
		
		// hide requested views
		HashMap<View, Integer> previousStateViews = new HashMap<View, Integer>();
		if (viewsToHide != null){
			for (View item : viewsToHide){
				if (item != null){
					previousStateViews.put(item, item.getVisibility());
					item.setVisibility(View.INVISIBLE);
					item.invalidate();
				}
			}
		}

		// the the focus to the given view
		boolean isFocusable = false;
		if (viewToTakeFocus != null){
			isFocusable = viewToTakeFocus.isFocusableInTouchMode();
			viewToTakeFocus.setFocusableInTouchMode(true);
			viewToTakeFocus.requestFocus();
		}
		
		// create the bitmap
		Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
		if (bitmap != null){
			Canvas c = new Canvas(bitmap);
			view.draw(c);
		}
		
		// extract center region of bitmap
		if (nullColor != null)
			bitmap = bitmapExtractCenter(bitmap, nullColor);

		// show the views that we previously hid
		if (viewsToHide != null){
			for (View item : viewsToHide){
				if (item != null){
					item.setVisibility(previousStateViews.get(item));
					item.invalidate();
				}
			}
		}
		
		// reset focus
		if (viewToTakeFocus != null){
			viewToTakeFocus.setFocusableInTouchMode(isFocusable);
		}
		if (view instanceof ViewGroup && childHasFocus != -1){
			ViewGroup vg = (ViewGroup) view;
			View v = vg.getChildAt(childHasFocus);
			v.requestFocus();
		}
		
		// the bitmap
		return bitmap;
	}
    
    /**
     * Remove the edges of bitmap by extracting the center region that do not match the given nullColor
     * @param bitmap the source bitmap
     * @param nullColor the color that is considered void and we will chop. For example, for black simply enter <br>
     * Color.argb(0, 0, 0, 0); 
     * @return
     */
    public static Bitmap bitmapExtractCenter(Bitmap bitmap, int nullColor){
    	if (bitmap == null)
    		return null;
    	
    	// measure size
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		
		// grab pixel data
		int[] pixels = new int[width*height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
		
		// which row will we start and end and cols as well
		int rowStart = 0;
		int rowEnd = height-1;
		int colStart = 0;
		int colEnd = width-1;
		
		// find the top that we can crop
		boolean outerBreak = false;
		for (int i = 0; i <height; i++){
			int ii = i*width;
			for (int j = 0; j < width; j++){				
				if (pixels[ii + j] != nullColor){
					outerBreak = true;
					break;				
				}
			}
			if (outerBreak)
				break;
			rowStart++;
		}
		
		// find the bottom that we can crop
		outerBreak = false;
		for (int i = height-1; i >= 0; i--){
			int ii = i*width;
			for (int j = 0; j < width; j++){
				if (pixels[ii + j] != nullColor){
					outerBreak = true;
					break;				
				}
			}
			if (outerBreak)
				break;
			rowEnd--;
		}
		
		// find the left we can crop
		outerBreak = false;
		for (int j = 0; j < width; j++){
			for (int i = 0; i <height; i++){	
				if (pixels[i*width + j] != nullColor){
					outerBreak = true;
					break;				
				}
			}
			if (outerBreak)
				break;
			colStart++;
		}
		
		// find the right that we can crop
		outerBreak = false;
		for (int j = width-1; j >= 0; j--){
			for (int i = 0; i <height; i++){	
				if (pixels[i*width + j] != nullColor){
					outerBreak = true;
					break;				
				}
			}
			if (outerBreak)
				break;
			colEnd--;
		}
		
		// sub index of matrix
		int newWidth = colEnd-colStart+1;
		int newHeight = rowEnd-rowStart+1;
		if (newWidth <= 0 || newHeight <= 0)
			return null;
		return Bitmap.createBitmap(bitmap, colStart, rowStart, newWidth, newHeight);	
    }
}