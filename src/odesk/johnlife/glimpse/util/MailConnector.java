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

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.FileHandler;
import android.content.Context;
import android.util.Log;

public class MailConnector {
//	private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
	private static final String SSL_FACTORY = "odesk.johnlife.glimpse.util.AlwaysTrustSSLContextFactory";
	
	private String user;
	private String pass;
	private String server;
	byte[] buf = new byte[4096];
	
	public MailConnector(String user, String pass, Context context) {
		this.user = user;
		this.pass = pass;
		this.server = context.getString(R.string.email_server);
	}

	public void connect() {
		Log.d(getClass().getSimpleName(), String.format("Connecting to %s, user=%s, pass=%s", server, user, pass));
	    Properties properties = System.getProperties();
	    properties.setProperty("mail.store.protocol", "imaps");
	    properties.setProperty("mail.imaps.socketFactory.class", SSL_FACTORY);
		try {
	        Session session = Session.getDefaultInstance(properties, null);
	        Store store = session.getStore("imaps");
		    store.connect(server, user, pass);
			Log.d(getClass().getSimpleName(), "Connected to store");
			Folder folder = null;
			folder = store.getDefaultFolder().getFolder("INBOX");
			folder.open(Folder.READ_WRITE);
			Log.d(getClass().getSimpleName(), "Opened inbox");
			FileHandler fileHandler = GlimpseApp.getFileHandler();
			Message[] messages = fileHandler.isEmpty() ?
				folder.getMessages() : 
				folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
			Log.d(getClass().getSimpleName(), "Got "+messages.length+(fileHandler.isEmpty() ? " total" : " new")+" messages.");
			for(Message msg : messages) {
				try {
					List<File> attachments = getAttachments((Multipart) msg.getContent());
					Log.d(getClass().getSimpleName(), "Found "+attachments.size()+" attachments.");
					for (File file : attachments) {
						fileHandler.add(file);
					}
					msg.setFlag(Flags.Flag.DELETED, true);
				} catch (Exception e) {
//					PushLink.sendAsyncException(e);
				}
			}
			folder.setFlags(messages, new Flags(Flags.Flag.SEEN), true);
			folder.close(true);
			store.close();
		} catch (MessagingException e) {
			Log.e(getClass().getSimpleName(), "Error: ", e);
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
