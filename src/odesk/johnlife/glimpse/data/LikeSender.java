package odesk.johnlife.glimpse.data;

import android.content.Context;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;

import odesk.johnlife.glimpse.R;

public class LikeSender {

	public static final String EMAIL = "email";
	public static final String FILE = "file";

	public void sendLike(Context context, String recipient, String picturePath) throws Exception {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(context.getString(R.string.like_url));
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		entityBuilder.addTextBody(EMAIL, recipient);
		entityBuilder.addBinaryBody(FILE, new File(picturePath));
		httpPost.setEntity(entityBuilder.build());
		HttpResponse response = httpclient.execute(httpPost);
		HttpEntity httpEntity = response.getEntity();
		Log.d("Sending like", "result=" + EntityUtils.toString(httpEntity));
	}

}