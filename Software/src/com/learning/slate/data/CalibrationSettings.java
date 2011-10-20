/**
 * 
 */
package com.learning.slate.data;

import java.util.Map.Entry;
import java.util.TreeMap;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author cbryant
 */
@XStreamAlias("calibration")
public class CalibrationSettings
{
	@XStreamAlias("action")
	public static class Action
	{
		@XStreamAsAttribute
		private String name;
		@XStreamAsAttribute
		private Integer id;
		@XStreamAsAttribute
		private Float baseAngle;
		@XStreamAsAttribute
		private Integer anchorPointX;
		@XStreamAsAttribute
		private Integer anchorPointY;
		@XStreamAsAttribute
		private Integer instructionsAnchorPointX;
		@XStreamAsAttribute
		private Integer instructionsAnchorPointY;
		
		// Constructor for creating default Actions
		public Action(String name, Integer id, Float baseAngle, Integer apX, Integer apY, Integer iapX, Integer iapY) {
			this.name = name;
			this.id = id;
			this.baseAngle = baseAngle;
			this.anchorPointX = apX;
			this.anchorPointY = apY;
			this.instructionsAnchorPointX = iapX;
			this.instructionsAnchorPointY = iapY;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}
		public Float getBaseAngle() {
			return baseAngle;
		}
		public void setBaseAngle(Float baseAngle) {
			this.baseAngle = baseAngle;
		}
		public Integer getAnchorPointX() {
			return anchorPointX;
		}
		public void setAnchorPointX(Integer anchorPointX) {
			this.anchorPointX = anchorPointX;
		}
		public Integer getAnchorPointY() {
			return anchorPointY;
		}
		public void setAnchorPointY(Integer anchorPointY) {
			this.anchorPointY = anchorPointY;
		}
		public int getInstructionsAnchorPointX() {
			return instructionsAnchorPointX;
		}
		public void setInstructionsAnchorPointX(Integer instructionsAnchorPointX) {
			this.instructionsAnchorPointX = instructionsAnchorPointX;
		}
		public Integer getInstructionsAnchorPointY() {
			return instructionsAnchorPointY;
		}
		public void setInstructionsAnchorPointY(Integer instructionsAnchorPointY) {
			this.instructionsAnchorPointY = instructionsAnchorPointY;
		}
	}
	
	@XStreamAlias("tangible")
	public static class Tangible
	{
		@XStreamAsAttribute
		private String name;
		@XStreamAsAttribute
		private Integer id;
		@XStreamAsAttribute
		private Float baseAngle;
		@XStreamAsAttribute
		private Integer anchorPointX;
		@XStreamAsAttribute
		private Integer anchorPointY;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}
		public Float getBaseAngle() {
			return baseAngle;
		}
		public void setBaseAngle(Float baseAngle) {
			this.baseAngle = baseAngle;
		}
		public Integer getAnchorPointX() {
			return anchorPointX;
		}
		public void setAnchorPointX(Integer anchorPointX) {
			this.anchorPointX = anchorPointX;
		}
		public Integer getAnchorPointY() {
			return anchorPointY;
		}
		public void setAnchorPointY(Integer anchorPointY) {
			this.anchorPointY = anchorPointY;
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

		@SuppressWarnings("rawtypes")
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
	
	public void init() {
		if (this.actions == null)   this.actions = new TreeMap<String,Action>();
		if (this.tangibles == null) this.tangibles = new TreeMap<String,Tangible>();
	}

	@XStreamConverter(ActionsConverter.class)
	private TreeMap<String,Action> actions;
	
	@XStreamConverter(TangiblesConverter.class)
	private TreeMap<String,Tangible> tangibles;
	
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
	
	public String getTangibleNameByFiducialId(int fiducialId) {
		Tangible tangible = getTangibleByFiducialId(fiducialId);
		return (tangible != null) ? tangible.getName() : null;
	}
	public Tangible getTangibleByFiducialId(int fiducialId) {
		for (Entry<String,Tangible> entry : tangibles.entrySet()) {
			if (entry.getValue().getId() == fiducialId) {
				return entry.getValue();
			}
		}
		return null;
	}
	public boolean isTangible(int fiducialId) {
		return getTangibleNameByFiducialId(fiducialId) != null;
	}
	
	public String getActionNameByFiducialId(int fiducialId) {
		return getActionNameByFiducialId(fiducialId, actions);
	}
	
	public Action getActionByFiducialId(int fiducialId) {
		return getActionByFiducialId(fiducialId, actions);
	}
	
	public boolean isAction(int fiducialId) {
		return getActionNameByFiducialId(fiducialId) != null;
	}
	
	public static String getActionNameByFiducialId(int fiducialId, TreeMap<String,Action> actions) {
		Action action = getActionByFiducialId(fiducialId, actions);
		return (action != null) ? action.getName() : null;
	}
	
	public static Action getActionByFiducialId(int fiducialId,  TreeMap<String,Action> actions) {
		for (Entry<String,Action> entry : actions.entrySet()) {
			if (entry.getValue().getId() == fiducialId) {
				return entry.getValue();
			}
		}
		return null;
	}
}