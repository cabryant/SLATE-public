/**
 * 
 */
package com.learning.slate.logging;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.DelayQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.learning.slate.SlateComponent.TuioAction;
import com.learning.slate.data.DataManager;
import com.learning.slate.data.Event;
import com.learning.slate.data.Session;
import com.learning.slate.data.ToolkitSettings;


/**
 * @author coram
 *
 */
public class LogViewer
{
	//private int currentEventIndex;
	//private boolean ready = false;
	//private Timer LogViewTimer = null;
	private File logFile;
	
	private Session session;
	private ToolkitSettings toolkitSettings;
	
	// time stamp of the start of the replay
	private long startTimeStamp = -1;
	
	private Thread pollingThread;
	private Thread loadingThread;
	private boolean run;
	
	private Map<String,Event> pieceMap = new HashMap<String,Event>();
	DelayQueue<Event> delayQueue = new DelayQueue<Event>();
	
	float currentFileLoadStatus;
	
	public LogViewer(File lf)
	{
		logFile = lf;
		run = true;
		
		loadLogFileTimer = new Timer();
		loadLogFileTimer.schedule(new LoadLogFileTimerTask(), 1);
		
		goTimer = new Timer();
		goTimer.schedule(new GoTimerTask(), 1000);
	}
	
	public void dispose()
	{
		run = false;
		startTimeStamp = -1;				
	}
	
	public boolean isReady()
	{
		return (startTimeStamp != -1);
	}
	
	public Session getSession()
	{
		return session;
	}
	
	public float getCurrentFileLoadStatus()
	{
		return currentFileLoadStatus;
	}
	
	synchronized public List<Event> getCurrentPieces()
	{
		// Make a copy to avoid concurrent modification exception
		List<Event> pieces = new ArrayList<Event>();
		for (Event piece : pieceMap.values())
		{
			pieces.add(piece);
		}
		return pieces;
	}
	
	Timer loadLogFileTimer = null;
	class LoadLogFileTimerTask extends TimerTask
	{
		public void run()
		{
			loadingThread = new Thread(new Runnable()
			{
				public void run()
				{
					loadLogFile();
			    }
			});
			
			loadingThread.start();
		}
	}
	
	private void loadLogFile()
	{
		try
		{
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			builderFactory.setValidating(false);
			builderFactory.setNamespaceAware(true);
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document doc = builder.parse(logFile);
			XPathFactory xpathFactory = XPathFactory.newInstance();
			
			// get global log variables
			XPath xpath = xpathFactory.newXPath();
			XPathExpression xpathExpr = xpath.compile("./session");
			NodeList sessions = (NodeList) xpathExpr.evaluate(doc, XPathConstants.NODESET);
			if (sessions.getLength() == 0)
			{
				throw new Exception("No session!");
			}
			Node sessionNode = sessions.item(0);
			
			session = Session.fromXml(sessionNode, xpath, xpathExpr);
			
			xpathExpr = xpath.compile("./toolkit");
			NodeList toolkitSettingsList = (NodeList) xpathExpr.evaluate(sessionNode, XPathConstants.NODESET);
			if (toolkitSettingsList.getLength() == 0)
			{
				throw new Exception("No toolkit settings!");
			}
			Node toolkitSettingsNode = toolkitSettingsList.item(0);
			// FIXME TODO - not sure how the toolkit should be used, but it may ultimately
			// be necessary for displaying data from old toolkit versions.  Alternatively
			// A toolkit version could be treated as a unique toolkit, in which case
			// log files would be specific to a toolkit version, making a toolkit node obsolete
			//toolkitSettings = ToolkitSettings.fromXml(toolkitSettingsNode, xpath, xpathExpr);
			
			xpathExpr = xpath.compile("./event");
			NodeList events = (NodeList) xpathExpr.evaluate(sessionNode, XPathConstants.NODESET);
			if (events.getLength() == 0)
			{
				throw new Exception("No events!");
			}
			
			int eventLength = events.getLength();
			for (int i = 0; i < eventLength; i++)
			{
				// verify that we should still be running
				if (!run) break;
				
				Node node = events.item(i);
				
				// FIXME TODO - get event from xml
				StringWriter stringWriter = new StringWriter(); 
	            Transformer transformer = TransformerFactory.newInstance().newTransformer(); 
	            transformer.transform(new DOMSource(node), new StreamResult(stringWriter)); 
	            Event event = (Event) DataManager.fromXml(stringWriter.toString());
				//Event event = Event.fromXml(node, xpath, xpathExpr);
				
				if (event == null) continue;
				
				// Don't initialize the start time stamp until we have something to place in the queue
				// FIXME TODO - should this not be at the start of go? Or, does it matter?
				if (startTimeStamp == -1)
				{
					startTimeStamp = System.currentTimeMillis();
				}
				
				// set timestamps for delay queue
				event.setReplayStartTimeStamp(startTimeStamp);
				event.setSessionStartTimeStamp(session.getTimeStamp());
			
				currentFileLoadStatus = (float) i / (float) eventLength;
				//System.err.println(currentFileLoadStatus);
				
				delayQueue.add(event);
			}
			
			//System.out.println("done!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	Timer goTimer = null;
	class GoTimerTask extends TimerTask
	{
		public void run()
		{
			go();
		}
	}
	
	private void go()
	{
		//System.out.println("go");
		//startTimeStamp = System.currentTimeMillis();
		
		// start polling from delay queue
		// update the pieceMap (make sure this method is synchronized with getCurrentPieces)
		pollingThread = new Thread(new Runnable()
		{
			public void run()
			{
				while (run)
				{
					try
					{
						Event piece = delayQueue.take();
						updatePieceMap(piece);
		            }
		            catch (InterruptedException e)
		            {
		            	e.printStackTrace();
		            }
		         }
		      }
		});
		
		pollingThread.start();
	}
	
	synchronized private void updatePieceMap(Event piece)
	{
		TuioAction tuioAction = piece.getTuioAction();
		
		if (TuioAction.Add.equals(tuioAction))
		{
			//System.out.println("add " + key);
			pieceMap.put(piece.getKey(), piece);
		}
		else if (TuioAction.Update.equals(tuioAction))
		{
			//System.out.println("update " + key);
			pieceMap.put(piece.getKey(), piece);
		}
		else if (TuioAction.Remove.equals(tuioAction))
		{
			//System.out.println("remove " + key);
			pieceMap.remove(piece.getKey());
		}
	}
}