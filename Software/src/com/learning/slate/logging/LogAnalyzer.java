/**
 * 
 */
package com.learning.slate.logging;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
import com.learning.slate.data.SlateSettings;
import com.learning.slate.data.ToolkitSettings;

/**
 * TODO Stax implementation.
 * Graphical output (jfreechart?)
 * @author coram
 *
 */
public class LogAnalyzer
{
	private enum Metric
	{
		ADD,
		REMOVE,
		UPDATE,
		ROTATE,
		TRANSLATE//,
		//COMMAND;
	}
	
	// 1 minute?
	private static final long TIME_UNIT = 1000l * 60l;
	
	// FIXME (should get all this data from the log file?)
	private SlateSettings slateSettings;
	private ToolkitSettings toolkitSettings;
	
	public void go()
	{
		slateSettings = DataManager.getSlateSettings();
		
		// get the log file name from the file system
		File logFolder = new File(DataManager.getLogDirectoryName(toolkitSettings.getName()));
		File[] listOfLogFiles = logFolder.listFiles();
		
		for (File file : listOfLogFiles)
		{
			long start = System.currentTimeMillis();
			analyzeLogFile(file);
			long duration = System.currentTimeMillis() - start;
			System.out.println("Duration: " + (duration / 1000) + " seconds");
		}
	}
	
	private void analyzeLogFile(File logFile)
	{
		try
		{
			int lastStatus = -1;
			Map<String,Map<Long,Float>> metricMap = new HashMap<String,Map<Long,Float>>();
			Map<String,Event> pieceMap = new HashMap<String,Event>();
			
			System.out.println(logFile.getName());
			
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			builderFactory.setValidating(false);
			builderFactory.setNamespaceAware(true);
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document doc = builder.parse(logFile);
			XPathFactory xpathFactory = XPathFactory.newInstance();
			
			// get global log variables (timestamp)
			XPath xpath = xpathFactory.newXPath();
			XPathExpression xpathExpr = xpath.compile("./session");
			NodeList sessions = (NodeList) xpathExpr.evaluate(doc, XPathConstants.NODESET);
			if (sessions.getLength() == 0)
			{
				throw new Exception("No session!");
			}
			Node sessionNode = sessions.item(0);
			Session session = Session.fromXml(sessionNode, xpath, xpathExpr);
			long logTimeStamp = session.getTimeStamp();
			
	
			// FIXME TODO Load toolkit from the log file
			// For now, use the existing toolkit
			toolkitSettings = DataManager.getToolkitSettings(toolkitSettings.getName());
			
			// initialize map for this log file
			initializeMetricMap(metricMap, logTimeStamp);
			
			xpathExpr = xpath.compile("./event");
			NodeList events = (NodeList) xpathExpr.evaluate(sessionNode, XPathConstants.NODESET);
			if (events.getLength() == 0)
			{
				throw new Exception("No events!");
			}
			
			int eventLength = events.getLength();
			System.out.println(eventLength + " events");
			
			for (int i = 0; i < eventLength; i++)
			{
				Node node = events.item(i);
				
				// FIXME TODO - get event from xml
				Event event = null; //DataManager.fromXml(xml);
				
				if (event == null) continue;
				
				int currentFileLoadStatus =  (int) (((float) i / (float) eventLength) * 100f);
				
				if (currentFileLoadStatus > lastStatus)
				{
					System.err.println(currentFileLoadStatus + "%");
					lastStatus = currentFileLoadStatus;
				}
			
				updateMetricMap(metricMap, event, pieceMap, logTimeStamp);
				updatePieceMap(event, pieceMap);
			}
			
			printResults(logFile, metricMap);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void initializeMetricMap(Map<String,Map<Long,Float>> metricMap, long initialTimeStamp)
	{
		//private Map<String,Map<String,Map<Long,Float>>> metricMap = new HashMap<String,Map<String,Map<Long,Float>>>();
		for (Metric metric : Metric.values())
		{
			Map<Long,Float> map = new HashMap<Long,Float>();
			map.put(initialTimeStamp, 0f);
			metricMap.put(metric.name(), map);
		}
	}
	
	private void updateMetricMap(Map<String,Map<Long,Float>> metricMap, Event piece, Map<String,Event> pieceMap, long initialTimeStamp)
	{
		//private Map<String,Map<String,Map<Long,Float>>> metricMap = new HashMap<String,Map<String,Map<Long,Float>>>();
		
		// Don't process command events
		String name = piece.getName();
		if (toolkitSettings.getActions().containsKey(name)) {
			return;
		}
		
		TuioAction tuioAction = piece.getTuioAction();
		long timeStamp = piece.getTimeStamp();
		
		long timeStep = (timeStamp - initialTimeStamp) / TIME_UNIT;
		long timeWindow = (timeStep * TIME_UNIT) + initialTimeStamp;
		
		// If the last piece is null, we never detected the Add, so treat this like an Add
		Event lastPiece = pieceMap.get(piece.getKey());
		if (lastPiece == null)
		{
			System.err.println("Treating an Update like an Add: " + piece.getKey() + "[" + timeStamp + "]");
			tuioAction = TuioAction.Add;
		}
		
		if (TuioAction.Add.equals(tuioAction))
		{
			updateMetric(metricMap, Metric.ADD, timeWindow, 1f);
		}
		else if (TuioAction.Update.equals(tuioAction))
		{
			double absAngle = Math.abs((lastPiece.getAngle() - piece.getAngle()) * (360d / (2*Math.PI)));
			
			// FIXME - convert to inches rather than pixels?
			double dist = Math.sqrt(
					Math.pow((lastPiece.getX()*slateSettings.getWindowWidth() - piece.getX()*slateSettings.getWindowWidth()), 2) + 
					Math.pow((lastPiece.getY()*slateSettings.getWindowHeight() - piece.getY()*slateSettings.getWindowHeight()), 2));
			
			updateMetric(metricMap, Metric.UPDATE,    timeWindow, 1f);
			updateMetric(metricMap, Metric.ROTATE,    timeWindow, (float) absAngle);
			updateMetric(metricMap, Metric.TRANSLATE, timeWindow, (float) dist);
		}
		else if (TuioAction.Remove.equals(tuioAction))
		{
			updateMetric(metricMap, Metric.REMOVE, timeWindow, 1f);
		}
	}
	
	private void updateMetric(Map<String,Map<Long,Float>> metricMap, Metric metric, long key, float delta)
	{
		Map<Long,Float> map = metricMap.get(metric.name());
		Float value = map.get(key);
		if (value == null)
		{
			value = 0f;
		}
		value += delta;
		map.put(key, value);
	}
	
	private void updatePieceMap(Event piece, Map<String,Event> pieceMap)
	{
		TuioAction tuioAction = piece.getTuioAction();
		String key = piece.getKey();
		
		if (TuioAction.Add.equals(tuioAction))
		{
			//System.out.println("add " + key);
			pieceMap.put(key, piece);
		}
		else if (TuioAction.Update.equals(tuioAction))
		{
			//System.out.println("update " + key);
			pieceMap.put(key, piece);
		}
		else if (TuioAction.Remove.equals(tuioAction))
		{
			//System.out.println("remove " + key);
			pieceMap.remove(key);
		}
	}
	
	private void printResults(File logFile, Map<String,Map<Long,Float>> metricMap)
	{
		for (Entry<String,Map<Long,Float>> entry : metricMap.entrySet())
		{
			// FIXME convert to readable time
			System.out.println("\t" + entry.getKey());
			
			for (Entry<Long,Float> entry2 : entry.getValue().entrySet())
			{
				System.out.println("\t\t" + entry2.getKey() + "," + entry2.getValue());
			}
		}
	}
	
	public static void main(String[] args)
	{
		LogAnalyzer la = new LogAnalyzer();
		la.go();
	}
}