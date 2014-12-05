package odesk.johnlife.glimpse.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

import odesk.johnlife.glimpse.Constants;
import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.FileHandler;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

public class MailConnector implements Constants {
	
	public interface OnItemDownloadListener {
		public void onItemDownload();
	}
	
	private static final String SSL_FACTORY = "odesk.johnlife.glimpse.util.AlwaysTrustSSLContextFactory";
	private static final String LOG_TAG = MailConnector.class.getSimpleName();
	
	private String user;
	private String pass;
	private String server;
	private Context context;
	private OnItemDownloadListener onItemDownLoadListener;
	byte[] buf = new byte[4096];
	
	public MailConnector(String user, String pass, Context context, OnItemDownloadListener onItemDownLoadListener) {
		this.user = user;
		this.pass = pass;
		this.context = context;
		this.server = context.getString(R.string.email_server);
		this.onItemDownLoadListener = onItemDownLoadListener;
	}

	public void connect() {
		Log.d(LOG_TAG, String.format("Connecting to %s, user=%s, pass=%s", server, user, pass));
	    Properties properties = System.getProperties();
	    properties.setProperty("mail.store.protocol", "imaps");
	    properties.setProperty("mail.imaps.socketFactory.class", SSL_FACTORY);
		try {
	        Session session = Session.getDefaultInstance(properties, null);
	        Store store = session.getStore("imaps");
		    store.connect(server, user, pass);
			Log.d(LOG_TAG, "Connected to store");
			Folder folder = null;
			folder = store.getDefaultFolder().getFolder("INBOX");
			folder.open(Folder.READ_WRITE);
			Log.d(LOG_TAG, "Opened inbox");
			FileHandler fileHandler = GlimpseApp.getFileHandler();
			Message[] messages = fileHandler.isEmpty() ?
				folder.getMessages() : 
				folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
			Log.d(LOG_TAG, "Got "+messages.length+(fileHandler.isEmpty() ? " total" : " new")+" messages.");
			int position = GlimpseApp.getFileHandler().size();
			PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(PREF_POSITION_NEW_PHOTOS, position).commit();
			List<File> attachments = new ArrayList<File>();
			for(Message msg : messages) {
				try {
					attachments.addAll(getAttachments((Multipart) msg.getContent()));
					msg.setFlag(Flags.Flag.DELETED, true);
				} catch (Exception e) {
					Log.e(LOG_TAG, "Error: ", e);
				//	PushLink.sendAsyncException(e);
				}
			}
			if (!attachments.isEmpty()) {
				fileHandler.add(attachments);
				onItemDownLoadListener.onItemDownload();
			}
			folder.setFlags(messages, new Flags(Flags.Flag.SEEN), true);
			folder.close(true);
			store.close();
		} catch (MessagingException e) {
			Log.e(LOG_TAG, "Error: ", e);
//			PushLink.sendAsyncException(e);
		}
	}
	
	private List<File> getAttachments(Multipart multipart) throws MessagingException {
		List<File> attachments = new ArrayList<File>();
		for (int i = 0; i < multipart.getCount(); i++) {
			try {
				BodyPart bodyPart = multipart.getBodyPart(i);
				if (
					!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
					(bodyPart.getFileName() == null ||
					bodyPart.getFileName().isEmpty())
				) {
					if (bodyPart.isMimeType("multipart/*")) {
						attachments.addAll(getAttachments((Multipart) bodyPart.getContent()));
					}
					continue; // dealing with attachments only
				} 
				InputStream is = bodyPart.getInputStream();
				File f = new File(GlimpseApp.getTempDir(), bodyPart.getFileName());
				FileOutputStream fos = new FileOutputStream(f);
				int bytesRead;
				while ((bytesRead=is.read(buf)) != -1) {
					fos.write(buf, 0, bytesRead);
				}
				fos.close();
				attachments.add(f);
			} catch (IOException e) {
//				PushLink.sendAsyncException(e);
			}
		}
		Log.d(LOG_TAG, "Found "+attachments.size()+" attachments.");
		return attachments;
	}
	
//	/**
//     * Return the primary text content of the message.
//     */
//    private String getText(Part p) throws MessagingException, IOException {
//        if (p.isMimeType("text/*")) {
//            String s = (String)p.getContent();
////            textIsHtml = p.isMimeType("text/html");
//            return s;
//        }
//
//        if (p.isMimeType("multipart/alternative")) {
//            // prefer html text over plain text
//            Multipart mp = (Multipart)p.getContent();
//            String text = null;
//            for (int i = 0; i < mp.getCount(); i++) {
//                Part bp = mp.getBodyPart(i);
//                if (bp.isMimeType("text/plain")) {
//                    if (text == null)
//                        text = getText(bp);
//                    continue;
//                } else if (bp.isMimeType("text/html")) {
//                    String s = getText(bp);
//                    if (s != null)
//                        return s;
//                } else {
//                    return getText(bp);
//                }
//            }
//            return text;
//        } else if (p.isMimeType("multipart/*")) {
//            Multipart mp = (Multipart)p.getContent();
//            for (int i = 0; i < mp.getCount(); i++) {
//                String s = getText(mp.getBodyPart(i));
//                if (s != null)
//                    return s;
//            }
//        }
//
//        return null;
//    }

}
