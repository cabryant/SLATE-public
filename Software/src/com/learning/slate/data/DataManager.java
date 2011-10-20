/**
 * 
 */
package com.learning.slate.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;

/**
 * @author cbryant
 *
 */
public class DataManager
{
	private static final String BASE_TOOLKIT_NAME = "base";
	
	private static final String SLATE_XML_FILE_NAME = "slate.xml";
	private static final String TOOLKITS_FOLDER_NAME = "toolkits";
	private static final String TOOLKIT_XML_FILE_NAME = "toolkit.xml";
	private static final String CALIBRATION_SETTINGS_XML_FILE_NAME = "calibration.xml";
	private static final String LIBRARY_XML_FILE_NAME = "library.xml";
	private static final String LOG_FOLDER_NAME = "logs";
	private static final String LOG_FILE_BASE_NAME = "log.xml";
	
	private static final String RESOURCES_FOLDER_NAME = "resources";
	private static final String ACTIONS_FOLDER_NAME = "actions";
	private static final String SLIDE_SHOWS_FOLDER_NAME = "slide_shows";
	private static final String STATES_FOLDER_NAME = "states";
	private static final String TANGIBLES_FOLDER_NAME = "tangibles";
	private static final String TUTORIALS_FOLDER_NAME = "tutorials";
	
	private static final String REACTIVISION_FOLDER_NAME = "reacTIVision-1.4";
	private static final String REACTIVISION_EXE_NAME = "reacTIVision.exe";
	
	private static XStream xStream = new XStream();
	static {
		// pre-process annotations
		// see http://xstream.codehaus.org/annotations-tutorial.html
		xStream.processAnnotations(SlateSettings.class);
		xStream.processAnnotations(SlateSettings.Calibration.class);
		xStream.processAnnotations(CalibrationSettings.class);
		xStream.processAnnotations(CalibrationSettings.Action.class);
		xStream.processAnnotations(CalibrationSettings.Tangible.class);
		xStream.processAnnotations(ToolkitSettings.class);
		xStream.processAnnotations(ToolkitSettings.Action.class);
		xStream.processAnnotations(ToolkitSettings.Tangible.class);
		xStream.processAnnotations(ToolkitSettings.Tutorial.class);
		xStream.processAnnotations(Library.class);
		xStream.processAnnotations(Library.Challenge.class);
		xStream.processAnnotations(Library.Solution.class);
		xStream.processAnnotations(Library.Piece.class);
		xStream.processAnnotations(Event.class);
	}
	
	synchronized public static SlateSettings getSlateSettings() {
		SlateSettings slateSettings = (SlateSettings) loadFromFile(SLATE_XML_FILE_NAME);
		if (slateSettings == null) {
			slateSettings = new SlateSettings();
		}
		slateSettings.setDefaultValues();
		return slateSettings;
	}
	synchronized public static void saveSlateSettings(SlateSettings slateSettings) {
		saveToFile(slateSettings, SLATE_XML_FILE_NAME);
	}
	
	synchronized public static ToolkitSettings getToolkitSettings(String toolkitName) {
		checkToolkitName(toolkitName);
		ToolkitSettings toolkitSettings = (ToolkitSettings) loadFromFile(getToolkitFileName(toolkitName));
		if (toolkitSettings == null) {
			toolkitSettings = new ToolkitSettings(toolkitName);
			saveToolkitSettings(toolkitSettings, toolkitName);
		}
		toolkitSettings.init();
		return toolkitSettings;
	}
	synchronized public static void saveToolkitSettings(ToolkitSettings toolkitSettings, String toolkitName) {
		checkToolkitName(toolkitName);
		saveToFile(toolkitSettings, getToolkitFileName(toolkitName));
	}
	synchronized private static String getToolkitFileName(String toolkitName) {
		return TOOLKITS_FOLDER_NAME + File.separator + toolkitName + File.separator + TOOLKIT_XML_FILE_NAME;
	}
	
	synchronized public static CalibrationSettings getCalibrationSettings(String toolkitName) {
		checkToolkitName(toolkitName);
		CalibrationSettings calibrationSettings = (CalibrationSettings) loadFromFile(getCalibrationSettingsFileName(toolkitName));
		if (calibrationSettings == null) {
			calibrationSettings = new CalibrationSettings();
			saveCalibrationSettings(calibrationSettings, toolkitName);
		}
		calibrationSettings.init();
		return calibrationSettings;
	}
	synchronized public static void saveCalibrationSettings(CalibrationSettings calibrationSettings, String toolkitName) {
		checkToolkitName(toolkitName);
		saveToFile(calibrationSettings, getCalibrationSettingsFileName(toolkitName));
	}
	synchronized private static String getCalibrationSettingsFileName(String toolkitName) {
		return TOOLKITS_FOLDER_NAME + File.separator + toolkitName + File.separator + CALIBRATION_SETTINGS_XML_FILE_NAME;
	}
	
	synchronized public static Library getLibrary(String toolkitName) {
		checkToolkitName(toolkitName);
		Library library = (Library) loadFromFile(getLibraryFileName(toolkitName));
		if (library == null) {
			library = new Library();
			saveLibrary(library, toolkitName);
		}
		library.init();
		return library;
	}
	synchronized public static void saveLibrary(Library library, String toolkitName) {
		checkToolkitName(toolkitName);
		saveToFile(library, getLibraryFileName(toolkitName));
	}
	synchronized private static String getLibraryFileName(String toolkitName) {
		return TOOLKITS_FOLDER_NAME + File.separator + toolkitName + File.separator + LIBRARY_XML_FILE_NAME;
	}
	
	public static String getNewLogFileName(String toolkitName) {
		SimpleDateFormat formatter = new SimpleDateFormat("_yyyyMMdd_hhmmss.");
		String fileSuffix = formatter.format(new Date());
		String fileName = getLogDirectoryName(toolkitName) + File.separatorChar + LOG_FILE_BASE_NAME.replace(".", fileSuffix);
		return fileName;
	}
	public static String getLogDirectoryName(String toolkitName) {
		return TOOLKITS_FOLDER_NAME + File.separator + toolkitName + File.separator + LOG_FOLDER_NAME;
	}
	
	public static List<String> getInstalledToolkits() {
		File toolkitsFolder = new File(TOOLKITS_FOLDER_NAME);
		File[] toolkits = toolkitsFolder.listFiles();

		List<String> installedToolkits = new ArrayList<String>();
		for (File toolkit : toolkits) {
			if (toolkit.isDirectory() && !BASE_TOOLKIT_NAME.equals(toolkit.getName())) {
				installedToolkits.add(toolkit.getName());
			}
		}
		return installedToolkits;		
	}
	
	public static File getReacTIVisionDirectory() {
		return new File(REACTIVISION_FOLDER_NAME);
	}
	
	public static String getReacTIVisionExecutableName() {
		return REACTIVISION_FOLDER_NAME + File.separator + REACTIVISION_EXE_NAME;
	}
	
	public static String getActionFilePath(String toolkitName, String fileName) {
		return getResourceFilePath(toolkitName, ACTIONS_FOLDER_NAME, fileName);
	}
	public static String getSlideShowFilePath(String toolkitName, String filePath) {
		return getResourceFilePath(toolkitName, SLIDE_SHOWS_FOLDER_NAME, filePath);
	}
	public static String getStatesPath(String toolkitName, String fileName) {
		return getResourceFilePath(toolkitName, STATES_FOLDER_NAME, fileName);
	}
	public static String getTangiblesFilePath(String toolkitName, String fileName) {
		return getResourceFilePath(toolkitName, TANGIBLES_FOLDER_NAME, fileName);
	}
	public static String getTutorialsFilePath(String toolkitName, String folderName, String fileName) {
		return getResourceFilePath(toolkitName, TUTORIALS_FOLDER_NAME, 
				((folderName != null) ? folderName + File.separator : "") + fileName);
	}
	private static String getResourceFilePath(String toolkitName, String resourceFolder, String filePath) {
		return TOOLKITS_FOLDER_NAME + File.separator + toolkitName + File.separator + RESOURCES_FOLDER_NAME +
			File.separator + resourceFolder + File.separator + filePath;
	}
	
	private static void checkToolkitName(String toolkitName) throws IllegalArgumentException {
		if ((toolkitName == null) || BASE_TOOLKIT_NAME.equals(toolkitName)) {
			throw new IllegalArgumentException("Illegal toolkit name: " + toolkitName);
		}
	}
	
	synchronized public static String toPrettyPrintedXml(Object data) {
		return xStream.toXML(data);
	}
	synchronized public static String toCompactXml(Object data) {
		StringWriter writer = new StringWriter();
		xStream.marshal(data, new CompactWriter(writer));
		return writer.toString();
	}
	synchronized public static Object fromXml(String xml) {
		return xStream.fromXML(xml);
	}
	
	private static Object loadFromFile(String fileName) {
		File file = new File(fileName);
		
		Object data = null;
		BufferedInputStream bufferedInputStream = null;
	    try
	    {
	        bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
	        data = xStream.fromXML(bufferedInputStream);
	    }
	    catch (Throwable t)
	    {
			t.printStackTrace();
		}
	    finally
	    {
	    	try
	    	{
	    		if (bufferedInputStream != null)
	    		{
	    			bufferedInputStream.close();
	    		}
	    	}
	    	catch (IOException ignored) { }
	    }
	    
	    return data;
	}
	
	private static void saveToFile(Object data, String fileName) {
		PrintWriter printWriter = null;
		try
		{
			printWriter = new PrintWriter(new File(fileName));
			xStream.toXML(data, printWriter);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (printWriter != null)
			{
	    		printWriter.close();
	    	}
		}
	}
}
