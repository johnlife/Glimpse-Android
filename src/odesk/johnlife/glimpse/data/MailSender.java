package odesk.johnlife.glimpse.data;

import android.content.Context;
import android.util.Log;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import odesk.johnlife.glimpse.Constants;
import odesk.johnlife.glimpse.R;

public class MailSender extends Authenticator implements Constants {	

	private String email;
	private String password;

	public MailSender(String email, String password) {
		this.email = email;
		this.password = password;
	}

	public void postMail(Context context, String recipient, String picturePath) {
		try {
			Properties props = new Properties();
			props.put("mail.smtp.host", EMAIL_SERVER);
			props.put("mail.smtp.auth", "true");
			Session session = Session.getInstance(props, this);
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(email));
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
			msg.setSubject(context.getString(R.string.email_subject));
			Multipart multipart = new MimeMultipart();
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(context.getString(R.string.email_text));
			multipart.addBodyPart(messageBodyPart);
			BodyPart attachBodyPart = new MimeBodyPart();
			DataSource source = new FileDataSource(picturePath);
			attachBodyPart.setDataHandler(new DataHandler(source));
			attachBodyPart.setFileName(picturePath);
			multipart.addBodyPart(attachBodyPart);
			msg.setContent(multipart);
			Transport.send(msg);
		} catch (Exception e) {
			Log.e("Sending like", e.getMessage(), e);
		}
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(email, password);
	}
}
