package odesk.johnlife.glimpse.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.db.DatabaseHelper;
import android.content.Context;

import com.sun.mail.pop3.POP3SSLStore;

public class MailConnector {
	private static final int POP_PORT = 995;
	private static final String POP3 = "pop3";
	private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
	private static final Properties pop3Props = new Properties();

	static {
		pop3Props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
		pop3Props.setProperty("mail.pop3.socketFactory.fallback", "false");
		pop3Props.setProperty("mail.pop3.port", String.valueOf(POP_PORT));
		pop3Props.setProperty("mail.pop3.socketFactory.port", String.valueOf(POP_PORT));
	}
	
	private String user;
	private String pass;
	private String server;
	
	public MailConnector(String user, String pass, Context context) {
		this.user = user;
		this.pass = pass;
		this.server = context.getString(R.string.email_server);
	}

	public void connect(DatabaseHelper databaseHelper) {
		URLName url = new URLName(POP3, server, POP_PORT, "", user, pass);
		Session session = Session.getInstance(pop3Props, null);
		Store store = new POP3SSLStore(session, url);
		try {
			store.connect();
			Folder folder = null;
			folder = store.getDefaultFolder().getFolder("INBOX");
			folder.open(Folder.READ_ONLY);
			Message[] messages = folder.getMessages();
			for(Message msg : messages) {
				try {
					List<File> attachments = getAttachments((Multipart) msg.getContent());
					for (File file : attachments) {
						databaseHelper.toDb(file.getCanonicalPath());
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					//TODO
					e.printStackTrace();
				}
			}
		} catch (MessagingException e) {
			e.printStackTrace();
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
				File f = new File(GlimpseApp.getPicturesDir(), bodyPart.getFileName());
				FileOutputStream fos = new FileOutputStream(f);
				byte[] buf = new byte[4096];
				int bytesRead;
				while ((bytesRead=is.read(buf)) != -1) {
					fos.write(buf, 0, bytesRead);
				}
				fos.close();
				attachments.add(f);
			} catch (IOException e) {
				//TODO handle
				e.printStackTrace();
			}
		}
		return attachments;
	}
	
	/**
     * Return the primary text content of the message.
     */
    private String getText(Part p) throws MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
//            textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getText(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }

}
