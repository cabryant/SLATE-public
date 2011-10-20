/**
 * 
 */
package com.learning.slate.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import com.learning.slate.Logger;
import com.learning.slate.data.DataManager;
import com.learning.slate.data.Event;
import com.learning.slate.data.Session;
import com.learning.slate.data.ToolkitSettings;

/**
 * TODO should use a STAX implementation?
 * 
 * @author coram
 */
public class EventLogger
{
	private BufferedWriter writer;
	private Session session;
	
	public void beginLog(long timestamp, ToolkitSettings toolkitSettings) throws Exception
	{
		session = new Session(timestamp);
		String logDirName = DataManager.getLogDirectoryName(toolkitSettings.getName());
		
		File logDir = new File(logDirName);
		
		if (logDir.exists())
		{
			if (!logDir.isDirectory())
			{
				Logger.error(logDirName + " exists, but is not a directory!");
			}
		}
		else
		{
			boolean successful = logDir.mkdir();
			if (!successful)
			{
				Logger.error("Failed to create " + logDirName);
			}
		}
		
		String fileName = DataManager.getNewLogFileName(toolkitSettings.getName());
		
		Logger.info("creating log file: " + fileName);
		writer = new BufferedWriter(new FileWriter(fileName));
		writer.write(session.getXmlStart(false, true));
		writer.write(DataManager.toPrettyPrintedXml(toolkitSettings));
	}
	
	public void endLog() throws Exception
	{
		Logger.info("closing log file");
		writer.write(session.getXmlEnd(false));
		writer.close();
	}
	
	public void logEvent(Event event) throws Exception
	{
		if (event != null)
		{
			writer.write("\n" + DataManager.toPrettyPrintedXml(event));
		}
	}
}