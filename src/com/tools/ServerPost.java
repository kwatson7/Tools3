package com.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.tools.CustomAsyncTask.FinishedCallback;
import com.tools.ServerPost.ServerReturn;

import android.app.Activity;
import android.util.Log;

public class ServerPost {

	// class members
	private String url; 											// the url to post to
	private MultipartEntity multipartEntity; 						// the form post holder

	// constants
	private final Charset ENCODING_TYPE = Charset.forName("UTF-8"); // the type of the data
	private final int RANDOM_FILENAME_LENGTH = 64; 					// if we create a random filename, the length of the filename
	private final int GOOD_RETURN_CODE = 200; 						// The return code for a successful sync with server

	//enums for file types
	public enum FileType {
		// different file types
		JPEG("image/jpeg");

		// the type
		private String type;

		/**
		 *  construct with a string
		 * @param type
		 */
		private FileType(String type) {
			this.type = type;
		}

		/**
		 *  return the type of file
		 * @return
		 */
		public String getType() {
			return type;
		}
	}

	/**
	 * Create a server post object
	 * @param url the url to post to
	 */
	public ServerPost(String url){
		// store inputs
		this.url = url;

		// initialize the data
		initializeOptions();
	}

	/**
	 * Various initialization steps
	 */
	private void initializeOptions(){

		// initialize entity
		multipartEntity = new MultipartEntity(
				HttpMultipartMode.BROWSER_COMPATIBLE);
	}

	/**
	 * Add a string value to the post
	 * @param key the key to associate with the data
	 * @param value the value to post
	 */
	public void addData(String key, String value){
		try{
			multipartEntity.addPart(key, new StringBody(value, ENCODING_TYPE));
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Add a file to the post
	 * @param key the key to identify the file
	 * @param data the byte[] data of the file
	 * @param fileType the type of file
	 * @param fileName the filename to store with the file
	 */
	public void addFile(String key, byte[] data, FileType fileType, String fileName){
		multipartEntity.addPart(key, new ByteArrayBody(data, fileType.getType(), fileName));
	}

	/**
	 * Add a file to the post with a random file name attached
	 * @param key the key to identify the file
	 * @param data the byte[] data of the file
	 * @param fileType the type of file
	 * @return the filename assigned to the data
	 */
	public String addFile(String key, byte[] data, FileType fileType){
		String fileName = com.tools.Tools.randomString(RANDOM_FILENAME_LENGTH);
		multipartEntity.addPart(key, new ByteArrayBody(data, fileType.getType(), fileName));
		return fileName;
	}

	/**
	 * Add the file to the post
	 * @param key the key associated with this file
	 * @param fileType the type of file
	 * @param fileName the filename full path. Just the name of the file will be associated with this post
	 * @return true if we were able to read the file, and false otherwise.
	 */
	public boolean addFile(String key, FileType fileType, String fileName){
		// read the file
		byte[] data = com.tools.Tools.readFile(fileName);
		if (data == null)
			return false;

		// get the name of the file
		File file = new File(fileName);
		String name = file.getName();

		// save file in post
		addFile(key, data, fileType, name);

		return true;
	}

	/**
	 * Post the data to the url
	 * @return the server response
	 */
	public ServerReturn post(){

		// initialize http client and post to correct page
		HttpClient client = new DefaultHttpClient();

		// initialize post
		HttpPost httpPost = new HttpPost(url);

		// set to not open tcp connection
		httpPost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);

		// set the values to the post
		httpPost.setEntity(multipartEntity);

		// default statusCode
		int statusCode= -1;

		// the return values
		String serverReturnValue = "";
		String serverReturnValueLastLine = "";

		// send post
		try {
			// actual send
			HttpResponse response = client.execute(httpPost);

			// check what kind of return
			StatusLine statusLine = response.getStatusLine();
			statusCode = statusLine.getStatusCode();

			// good return
			if (statusCode == GOOD_RETURN_CODE) {
				// read return
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				StringBuilder builder = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					builder.append(line + "\n");
					serverReturnValueLastLine = line;
				}
				content.close();
				serverReturnValue = builder.toString();

				// bad return code
			} else {
				ServerReturn value = new ServerReturn(statusCode);
				return value;
			}

			// different failures	
		} catch (IOException e) {
			ServerReturn value = new ServerReturn(e);
			return value;
		}

		// successful return
		ServerReturn value = new ServerReturn(serverReturnValue, serverReturnValueLastLine);
		return value;
	}

	/**
	 * Post the data to the server and run the callback when completed. onPostFinishedUiThread is called first
	 * @param act The activity to post the callback to. Can be null
	 * @param callback the callback to call when post is finished
	 */
	public <ACTIVITY_TYPE extends CustomActivity> void postInBackground(
			ACTIVITY_TYPE  act,
			final PostCallback<ACTIVITY_TYPE> callback){

		// setup background thread and execute
		PostAsync<ACTIVITY_TYPE> task = new PostAsync<ACTIVITY_TYPE>(act, callback);
		task.setFinishedCallback(new CustomAsyncTask.FinishedCallback<ACTIVITY_TYPE, ServerReturn>() {

			@Override
			public void onFinish(ACTIVITY_TYPE activity, ServerReturn result) {
				callback.onPostFinishedUiThread(activity, result);					
			}
		});
		task.execute();
	}

	/**
	 * class used to post to server in the background
	 */
	public class PostAsync <ACTIVITY_TYPE extends CustomActivity>
	extends CustomAsyncTask<ACTIVITY_TYPE, Void, ServerReturn>{

		private PostCallback<ACTIVITY_TYPE> callback;
		public PostAsync(
				ACTIVITY_TYPE act,
				final PostCallback<ACTIVITY_TYPE> callback) {
			super(
					act,
					-1,
					false,
					false,
					null);
			this.callback = callback;
		}

		@Override
		protected void onPreExecute() {			
		}

		@Override
		protected ServerReturn doInBackground(Void... params) {
			ServerReturn result= post();
			callback.onPostFinished(callingActivity, result);
			return result;
		}

		@Override
		protected void onProgressUpdate(Void... progress) {			
		}

		@Override
		protected void onPostExectueOverride(ServerReturn result) {

		}

		@Override
		protected void setupDialog() {
		}
	}

	public static class ServerReturn{
		// member variables
		private String serverReturnValue = "";
		private String serverReturnValueLastLine = "";
		private ReturnType returnType;
		private String detailMessage = "";

		// enums for server return types
		public enum ReturnType {
			// different return types
			/** 
			 * Return with a return code of 200 and parsed return data
			 */
			COMPLETED, 

			/**
			 * Returned with a code other than 200
			 */
			BAD_CODE,

			/**
			 * Error with the post
			 */
			CLIENT_PROTOCOL_ERROR,

			/**
			 * Error reading return
			 */
			IO_EXCEPTION;
		}

		/**
		 * Create a completed server return item
		 * @param serverReturnValue the entire return from the server
		 * @param serverReturnValueLastLine just the last line return from the server
		 */
		public ServerReturn(
				String serverReturnValue,
				String serverReturnValueLastLine){
			// store values
			this.serverReturnValue = serverReturnValue;
			this.serverReturnValueLastLine = serverReturnValueLastLine;
			this.returnType = ReturnType.COMPLETED;
		}

		/**
		 * Create a return value that is just the bad code.
		 * @param code
		 */
		public ServerReturn(int code){
			this.returnType = ReturnType.BAD_CODE;
			this.detailMessage = String.valueOf(code);
		}

		/**
		 * Create a server return that had a bad client protocol
		 * @param e the exception that was thrown
		 */
		public ServerReturn(ClientProtocolException e){
			this.returnType = ReturnType.CLIENT_PROTOCOL_ERROR;
			this.detailMessage = e.getMessage();
		}

		/**
		 * Create a server return that had a bad io
		 * @param e the exception that was thrown
		 */
		public ServerReturn(IOException e){
			this.returnType = ReturnType.IO_EXCEPTION;
			this.detailMessage = e.getMessage();
		}

		/**
		 * @return the return type from the server
		 */
		public ReturnType getReturnType(){
			return returnType;
		}

		/**
		 * @return the detail message of the return
		 */
		public String getDetailMessage(){
			return detailMessage;
		}

		/**
		 * @return the server return value
		 */
		public String getServerReturn(){
			return serverReturnValue;
		}

		/**
		 * @return Only the last line of the server return value
		 */
		public String getServerReturnLastLine(){
			return serverReturnValueLastLine;
		}
		
		/**
		 * Return the server output as a JSON object. Will be null if we could not convert to a JSON object
		 * @return
		 */
		public JSONObject getJSONObject(){
			JSONObject out = null;
			try {
				out = new JSONObject(getServerReturnLastLine());
			} catch (JSONException e) {
				out = null;
				Log.e("ServerPost", Log.getStackTraceString(e));
			}
			return out;
		}
		
		/**
		 * Return the server output as a JSON Array. Will be null if we could not convert to a JSON Array
		 * @return
		 */
		public JSONArray getJSONArray(){
			JSONArray out = null;
			try {
				out = new JSONArray(getServerReturnLastLine());
			} catch (JSONException e) {
				out = null;
				Log.e("ServerPost", Log.getStackTraceString(e));
			}
			return out;
		}
	}

	public interface PostCallback <ACTIVITY_TYPE extends CustomActivity>{
		/**
		 * This is called when we are done posting to the server on the calling thread
		 * @param the activity that is currently active after the task completed
		 * @param result The server result
		 */
		public void onPostFinished(ACTIVITY_TYPE act, ServerReturn result);

		/**
		 * This is called on the ui thread when the post is finished
		 * @param the activity that is currently active after the task completed
		 * @param result The server result
		 */
		public void onPostFinishedUiThread(ACTIVITY_TYPE act, ServerReturn result);
	}
}
