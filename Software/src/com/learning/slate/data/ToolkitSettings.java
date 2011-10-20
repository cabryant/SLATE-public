/**
 * 
 */
package com.learning.slate.data;

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import com.learning.slate.Logger;
import com.learning.slate.SlateComponent;
import com.learning.slate.toolkits.IToolkit;
import com.learning.slate.toolkits.Resources;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author cbryant
 *
 */
@XStreamAlias("toolkit")
public class ToolkitSettings
{
	private static final String DEFAULT_VERSION = "1.0";
	private static final String DEFAULT_TOOLKIT_CLASS = "com.learning.slate.toolkits.base.BaseToolkit";
		
	public ToolkitSettings(String name) {
		this.name = name;
		this.version = DEFAULT_VERSION;
		this.toolkitClass = DEFAULT_TOOLKIT_CLASS;
	}
	
	public void init() {
		if (this.actions == null)   this.actions = new TreeMap<String,Action>();
		if (this.tangibles == null) this.tangibles = new TreeMap<String,Tangible>();
		if (this.tutorials == null) this.tutorials = new TreeMap<String,Tutorial>();
	}

	@XStreamAlias("action")
	public static class Action
	{
		@XStreamAsAttribute
		private String name;
		@XStreamAsAttribute
		private String img;
		@XStreamAsAttribute
		private String instructionsFileName;
		@XStreamAsAttribute
		private String tutorial;
	
		// Constructor for creating default Actions
		public Action(String name, String img, String ifn, String tutorial) {
			this.name = name;
			this.img = img;
			this.instructionsFileName = ifn;
			this.tutorial = tutorial;
		}

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setImg(String img) {
			this.img = img;
		}
		public void setInstructionsFileName(String instructionsFileName) {
			this.instructionsFileName = instructionsFileName;
		}
		public String getTutorial() {
			return tutorial;
		}
		public void setTutorial(String tutorial) {
			this.tutorial = tutorial;
		}
		
		public String getImagePath(String toolkitName) {
			return DataManager.getActionFilePath(toolkitName, img);
		}
		public String getInstructionsFilePath(String toolkitName) {
			return DataManager.getActionFilePath(toolkitName, instructionsFileName);
		}
	}
	
	@XStreamAlias("tangible")
	public static class Tangible
	{
		@XStreamAsAttribute
		private String name;
		@XStreamAsAttribute
		private String img;
		@XStreamAsAttribute
		private Boolean rotates;
		@XStreamAsAttribute
		private String tutorial;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setImg(String img) {
			this.img = img;
		}
		public Boolean isRotates() {
			return rotates;
		}
		public void setRotates(Boolean rotates) {
			this.rotates = rotates;
		}
		public String getTutorial() {
			return tutorial;
		}
		public void setTutorial(String tutorial) {
			this.tutorial = tutorial;
		}
		
		public String getImagePath(String toolkitName) {
			return DataManager.getTangiblesFilePath(toolkitName, img);
		}
	}
	
	@XStreamAlias("tutorial")
	public static class Tutorial
	{
		@XStreamAsAttribute
		private String name;
		@XStreamAsAttribute
		private String img;
		@XStreamAsAttribute
		private String folder;
		@XStreamAsAttribute
		private Integer slideCount;
	
		// Constructor for creating default Tutorials
		public Tutorial(String name, String img, String folder, Integer slideCount) {
			this.name = name;
			this.img = img;
			this.folder = folder;
			this.slideCount = slideCount;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setImg(String img) {
			this.img = img;
		}
		public void setFolder(String folder) {
			this.folder = folder;
		}
		public Integer getSlideCount() {
			return slideCount;
		}
		public void setSlideCount(Integer slideCount) {
			this.slideCount = slideCount;
		}
		
		// Tutorial Support
		@XStreamOmitField
		private static final long TUTORIAL_SLIDE_DURATION = 1800l;
		@XStreamOmitField
		private int currentTutorial = 1;	// tutorials are 1-based
		@XStreamOmitField
		private boolean running = false;
		@XStreamOmitField
		private Timer tutorialTimer = null;
		
		private String getCurrentTutorialFileName(String toolkitName) {
			return DataManager.getTutorialsFilePath(toolkitName, folder, img).replace(".", "_" + currentTutorial + ".");
		}
		private void resetIndices() {
			// tutorials are 1-based
			currentTutorial = 1;
		}
		private void next() {
			if (currentTutorial + 1 > slideCount) {
				resetIndices();
			} else {
				currentTutorial++;
			}
		}
		public boolean isRunning() {
			return running;
		}
		public void start() {
			if (running) {
				return;
			}
			running = true;
			setTimeout(TUTORIAL_SLIDE_DURATION);
		}
		private void setTimeout(long period) {
			tutorialTimer = new Timer();
			tutorialTimer.scheduleAtFixedRate(new TutorialTimerTask(), new Date(), period);
		}
		class TutorialTimerTask extends TimerTask {
			public void run() {
				next();
			}
		}
		public void show(Graphics2D g2, String tutorialName) throws IOException {
			Resources.drawHeaderImage(g2, getCurrentTutorialFileName(tutorialName));
		}
		public void stop() {
			running = false;
			if (tutorialTimer != null) {
				tutorialTimer.cancel();
				tutorialTimer = null;
			}
			resetIndices();
		}
	}
	
	public static class ActionsConverter implements Converter {

		@SuppressWarnings({ "rawtypes" })
		public boolean canConvert(Class type) {
			return TreeMap.class.isAssignableFrom(type);
		}

		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			@SuppressWarnings("unchecked")
			TreeMap<String,Action> actions = (TreeMap<String,Action>) source;
			for (Entry<String,Action> entry : actions.entrySet()) {
				writer.startNode(Action.class.getSimpleName().toLowerCase());
				context.convertAnother(entry.getValue());
				writer.endNode();
			}
		}

		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			TreeMap<String,Action> actions = new TreeMap<String,Action>();
			while (reader.hasMoreChildren()) {
				reader.moveDown();
				Action action = (Action) context.convertAnother(actions, Action.class);
				actions.put(action.getName(), action);
				reader.moveUp();
			}
			return actions;
		}
	}
	
	public static class TangiblesConverter implements Converter {

		@SuppressWarnings({ "rawtypes" })
		public boolean canConvert(Class type) {
			return TreeMap.class.isAssignableFrom(type);
		}

		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			@SuppressWarnings("unchecked")
			TreeMap<String,Tangible> tangibles = (TreeMap<String,Tangible>) source;
			for (Entry<String,Tangible> entry : tangibles.entrySet()) {
				writer.startNode(Tangible.class.getSimpleName().toLowerCase());
				context.convertAnother(entry.getValue());
				writer.endNode();
			}
		}

		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			TreeMap<String,Tangible> tangibles = new TreeMap<String,Tangible>();
			while (reader.hasMoreChildren()) {
				reader.moveDown();
				Tangible tangible = (Tangible) context.convertAnother(tangibles, Tangible.class);
				tangibles.put(tangible.getName(), tangible);
				reader.moveUp();
			}
			return tangibles;
		}
	}
	
	public static class TutorialsConverter implements Converter {

		@SuppressWarnings({ "rawtypes" })
		public boolean canConvert(Class type) {
			return TreeMap.class.isAssignableFrom(type);
		}

		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			@SuppressWarnings("unchecked")
			TreeMap<String,Tutorial> tutorial = (TreeMap<String,Tutorial>) source;
			for (Entry<String,Tutorial> entry : tutorial.entrySet()) {
				writer.startNode(Tutorial.class.getSimpleName().toLowerCase());
				context.convertAnother(entry.getValue());
				writer.endNode();
			}
		}

		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			TreeMap<String,Tutorial> tutorials = new TreeMap<String,Tutorial>();
			while (reader.hasMoreChildren()) {
				reader.moveDown();
				Tutorial tutorial = (Tutorial) context.convertAnother(tutorials, Tutorial.class);
				tutorials.put(tutorial.getName(), tutorial);
				reader.moveUp();
			}
			return tutorials;
		}
	}
	
	private String name;
	private String version;
	private String toolkitClass;
	
	@XStreamOmitField
	private IToolkit toolkit;
	public IToolkit getToolkit(SlateComponent slateComponent)
		throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (toolkit == null) {
			try {
				toolkit = (IToolkit) Class.forName(toolkitClass).newInstance();
			} catch (Throwable t) {
				//t.printStackTrace();
				Logger.info("Defaulting to " + DEFAULT_TOOLKIT_CLASS);
				toolkit = (IToolkit) Class.forName(DEFAULT_TOOLKIT_CLASS).newInstance();
			}
			toolkit.init(slateComponent);
		}
		return toolkit;
	}
	
	@XStreamConverter(ActionsConverter.class)
	private TreeMap<String,Action> actions;
	
	@XStreamConverter(TangiblesConverter.class)
	private TreeMap<String,Tangible> tangibles;
		
	@XStreamConverter(TutorialsConverter.class)
	private TreeMap<String,Tutorial> tutorials;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getToolkitClass() {
		return toolkitClass;
	}
	public void setToolkitClass(String toolkitClass) {
		this.toolkitClass = toolkitClass;
	}
	public TreeMap<String, Action> getActions() {
		return actions;
	}
	public void setActions(TreeMap<String, Action> actions) {
		this.actions = actions;
	}
	public TreeMap<String, Tangible> getTangibles() {
		return tangibles;
	}
	public void setTangibles(TreeMap<String, Tangible> tangibles) {
		this.tangibles = tangibles;
	}
	public TreeMap<String, Tutorial> getTutorials() {
		return tutorials;
	}
	public void setTutorials(TreeMap<String, Tutorial> tutorials) {
		this.tutorials = tutorials;
	}
}
