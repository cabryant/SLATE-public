/**
 * 
 */
package com.learning.slate.toolkits;

import java.awt.Graphics;
import java.util.TreeMap;

import com.learning.slate.SlateComponent;
import com.learning.slate.SlateComponent.TuioAction;
import com.learning.slate.SlateObject;
import com.learning.slate.data.CalibrationSettings;
import com.learning.slate.data.ToolkitSettings;
import com.learning.slate.data.ToolkitSettings.Action;

/**
 * @author cbryant
 *
 */
public interface IToolkit
{
	void init(SlateComponent slateComponent);
	void reset();
	void toggleEventLogger();
	
	void updateGraphics(Graphics g);
	
	void processEvent(
			SlateObject slateObject,
			Action action,
			TuioAction tuioAction,
			float deltaAngle,
			float deltaX,
			float deltaY,
			boolean isInTutorialZone);
	
	TreeMap<String,ToolkitSettings.Action> getDefaultToolkitActions();
	TreeMap<String,ToolkitSettings.Tutorial> getDefaultToolkitTutorials();
	TreeMap<String,CalibrationSettings.Action> getDefaultCalibrationActions();
}
