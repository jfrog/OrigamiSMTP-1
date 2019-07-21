package com.pessetto.origamismtp.filehandlers;


import com.pessetto.origamismtp.commandhandlers.DataHandler;
import com.pessetto.origamismtp.commandhandlers.MAILHandler;
import com.pessetto.origamismtp.commandhandlers.RCPTHandler;
import com.pessetto.origamismtp.filehandlers.inbox.Inbox;
import com.pessetto.origamismtp.filehandlers.inbox.Message;

/** Represents a handler for the email message
 * @author Travis Pessetto
 * @author pessetto.com
 */
public class EmailHandler
{

	/** Creates new EmailHandler instance
	 * @param mail The current MAILHandler
	 * @param rcpt The current RCPTHandler
	 * @param data The current DataHandler
	 */

	Inbox inbox = Inbox.getInstance();

	public EmailHandler() {}

	public void addNewMessage(MAILHandler mail, RCPTHandler rcpt, DataHandler data)
	{
		if(mail != null && rcpt != null && data != null)
		{
			Message message = new Message();
			message.setFrom(mail.getSender());
			message.setTo(rcpt.getRecipient());
			message.setSubject(Integer.toString(getUnixTime()));
			message.setMessage(data.getData());
			inbox.addMessage(message);
		}
	}

	public Inbox getInbox() {
		return inbox;
	}

	private int getUnixTime()
	{
		return (int)(System.currentTimeMillis() / 1000L);
	}
}
