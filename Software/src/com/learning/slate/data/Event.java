package com.learning.slate.data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.learning.slate.SlateComponent.TuioAction;
import com.learning.slate.data.Library.Piece;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("event")
public class Event implements Delayed
{
	public enum EventType
	{
		COMMAND,
		TUTORIAL,
		WIDGET;
	}
	
	public enum EventLevel
	{
		INFO,
		WARN,
		ERROR;
	}
	
	public class Parameters {
		private Float x;
		private Float y;
		private Float angle;
		
		public Parameters(float x, float y, float angle) {
			this.x = x;
			this.y = y;
			this.angle = angle;
		}
		
		public Float getX() {
			return x;
		}
		public void setX(Float x) {
			this.x = x;
		}
		public Float getY() {
			return y;
		}
		public void setY(Float y) {
			this.y = y;
		}
		public Float getAngle() {
			return angle;
		}
		public void setAngle(Float angle) {
			this.angle = angle;
		}
	}
	
	/* old event
	<event logger="com.learning.slate.assess.Event$EventType.WIDGET" timestamp="1302174102500" level="INFO" type="5" id="177">
		<action>Update</action>
		<name>InclinedPlane2</name>
		<parameters>
			<x>0.5415441</x>
			<y>0.41890934</y>
			<angle>1.743657</angle>
		</parameters>
	</event>
	*/
	
	// FIXME - not sure if we want to conform to old style - may just want to use new names
	// NOTE: No longer need type (i.e. fiducial ID).  Only the name is needed to look
	// up the corresponding piece on the target system.
	@XStreamAsAttribute @XStreamAlias("timestamp")
	private long timeStamp;
	@XStreamAsAttribute @XStreamAlias("logger")
	private EventType eventType;
	@XStreamAsAttribute @XStreamAlias("level")
	private EventLevel eventLevel;
	@XStreamAsAttribute @XStreamAlias("id")
	private long sessionId;
	@XStreamAlias("action")
	private TuioAction tuioAction;
	private String name;
	private String value;
	private Parameters parameters;
	
	public long getTimeStamp()        { return timeStamp;  }
	public EventType getEventType()   { return eventType;  }
	public EventLevel getEventLevel() { return eventLevel; }
	public String getName()           { return name;       }
	public long getSessionId()        { return sessionId;  }
	public TuioAction getTuioAction() { return tuioAction; }
	public String getValue()          { return value;      }
	public float getAngle()           { return parameters.getAngle(); }
	public float getX()               { return parameters.getX();     }
	public float getY()               { return parameters.getY();     }
	
	// optional, for Delay Queue logic
	@XStreamOmitField
	private long replayStartTimeStamp;
	@XStreamOmitField
	private long sessionStartTimeStamp;
	
	public Event(long ts, EventType et, EventLevel el, long sessionId, TuioAction tuioAction, String value, Piece piece)
	{
		this.timeStamp = ts;
		this.eventType = et;
		this.eventLevel = el;
		this.name = piece.getName();
		this.sessionId = sessionId;
		this.tuioAction = tuioAction;
		this.value = value;
		this.parameters = new Parameters(
				piece.getX(),
				piece.getY(),
				piece.getAngle());
	}
	
	public void setReplayStartTimeStamp(long replayStartTimeStamp) {
		this.replayStartTimeStamp = replayStartTimeStamp;
	}
	public void setSessionStartTimeStamp(long sessionStartTimeStamp) {
		this.sessionStartTimeStamp = sessionStartTimeStamp;
	}
	
	public long getDelay(TimeUnit timeUnit) {
		long now = System.currentTimeMillis();
		long delay = timeUnit.convert((timeStamp - sessionStartTimeStamp) - (now - replayStartTimeStamp), TimeUnit.MILLISECONDS);
		//System.out.println("[" + timeStamp + " - " + sessionStartTimeStamp + "] - [" + now + " - " + replayStartTimeStamp + "]" + " = " + (timeStamp - sessionStartTimeStamp) + " - " + (now - replayStartTimeStamp) + " = " + delay);
		return delay;
	}
	
	public int compareTo(Delayed delayed) {
		Event event = (Event) delayed;
		long eventTs = event.getTimeStamp();
		long thisTs = getTimeStamp();
		
		if (thisTs < eventTs) return -1;
		else if (thisTs > eventTs) return 1;
		else return 0;
	}	
	
	public String getKey() {
		return name + "." + sessionId;
	}
}