/**
 * 
 */
package com.learning.slate.toolkits.mechanix;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import com.learning.slate.Logger;
import com.learning.slate.SlateComponent;
import com.learning.slate.SlateComponent.PointF;
import com.learning.slate.SlateComponent.TuioAction;
import com.learning.slate.SlateObject;
import com.learning.slate.data.DataManager;
import com.learning.slate.data.DataSource;
import com.learning.slate.data.Event;
import com.learning.slate.data.Event.EventLevel;
import com.learning.slate.data.Event.EventType;
import com.learning.slate.data.Library.Challenge;
import com.learning.slate.data.Library.LibraryEntry;
import com.learning.slate.data.Library.Piece;
import com.learning.slate.data.Library.Solution;
import com.learning.slate.data.SlateSettings.HelpParadigm;
import com.learning.slate.data.SlateSettings.RotationParadigm;
import com.learning.slate.data.ToolkitSettings;
import com.learning.slate.data.ToolkitSettings.Action;
import com.learning.slate.data.ToolkitSettings.Tutorial;
import com.learning.slate.logging.EventLogger;
import com.learning.slate.logging.LogViewer;
import com.learning.slate.logging.NetworkLogger;
import com.learning.slate.toolkits.ISlateObject;
import com.learning.slate.toolkits.IToolkit;
import com.learning.slate.toolkits.Resources;
import com.learning.slate.toolkits.Resources.TextType;
import com.learning.slate.toolkits.base.BaseToolkit;

/**
 * @author cbryant
 *
 */
public class MechanixToolkit extends BaseToolkit
{
	// special piece names . . . not sure where best to place these.
	private static final String TANGIBLE_MONSTER = "Monster";
	private static final String TANGIBLE_HOME = "Home";
	
	// SLATE SESSION
	private SlateComponent slateComponent;
	
	// Data Loggers
	private NetworkLogger networkLogger;
	private EventLogger eventLogger;
	
	// Tuio Members
	private SlateObject currentCommandObject;
	private SlateObject currentCommandHelperObject; // HelpParadigm.Classic
	
	// State Machine
	private MechanixStateEnum state;
	private MechanixStateEnum previousState;
	private Tutorial currentTutorial;
	private Challenge currentChallenge;
	private int currentChallengeCounter;
	// Mechanix State Machine
	private ISlateObject challengeMonster;
	private int currentHelp1Counter;
	private int currentHelp2Counter;
	private List<Piece> currentSortedHelpPieces;
	private int numSolutions;
	private int numSolutionSteps;
	// HelpParadigm.Modern
	private float lastLateralCheckPointY;
	private float progressBarStart;
	private static final int PROGRESS_BAR_GAP = 4;			// TODO find a home for this (and scale value to screen size?)
	private static final float PROGRESS_BAR_LENGTH = .28f;	// TODO find a home for this
	
	// Log Members
	private int numLogFiles;
	private int currentLogFileCounter;
	private File currentLogFile;
	private LogViewer currentLogViewer;
	
	// Personalization Members
	private String userName;
	
	public MechanixToolkit() {}
	
	public void init(SlateComponent slateComponent)
	{
		this.slateComponent = slateComponent;
		reset();
		// FIXME - remove this after user testing - we don't always want to log
		//toggleEventLogger();
	}
	
	public void reset()
	{
		// clear timers
		clearRepaintTimer();
		clearState1Timeout();
		clearSave1Timeout();
		
		stopNetworkLogger();
		stopEventLogger();
		
		currentCommandObject = null;
		// TODO? objectMap = new HashMap<Integer,Map<Long,SlateObject>>();
		
		state = MechanixStateEnum.Start1;
		state.startSlideShow();
		previousState = MechanixStateEnum.None;
		currentTutorial = null;
		
		currentChallenge = null;
		currentChallengeCounter = 0;
		challengeMonster = null;
		
		currentHelp1Counter = 0;
		currentHelp2Counter = 0;
		currentSortedHelpPieces = null;
		numSolutions = 0;
		numSolutionSteps = 0;
		lastLateralCheckPointY = 0f;
		progressBarStart = 0f;
		currentCommandHelperObject = null;
		
		numLogFiles = 0;
		currentLogFileCounter = 0;
		currentLogFile = null;
		currentLogViewer = null;
		
		userName = "";
	}
	
	private void stopNetworkLogger()
	{
		if (networkLogger != null)
		{
			networkLogger.stop();
			networkLogger = null;
			clearRepaintTimer();
		}
	}
	
	private void stopEventLogger()
	{
		try
		{
			if (eventLogger != null)
			{
				eventLogger.endLog();
				eventLogger = null;
				clearRepaintTimer();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void toggleEventLogger()
	{
		try
		{
			if (eventLogger == null)
			{
				eventLogger = new EventLogger();
				eventLogger.beginLog(System.currentTimeMillis(), slateComponent.getToolkitSettings());
				startRepaintTimer(0);
			}
			else
			{
				eventLogger.endLog();
				eventLogger = null;
				clearRepaintTimer();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void logEvent(SlateObject slateObject, Action action, TuioAction tuioAction) {
		// Event Logger
		if (eventLogger != null) {
			try {
				Event event = createEvent(slateObject, action, tuioAction);
				eventLogger.logEvent(event);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Network Logger
		if (networkLogger != null) {
			try {
				Event event = createEvent(slateObject, action, tuioAction);
				networkLogger.logEvent(event);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private Event createEvent(SlateObject slateObject, Action action, TuioAction tuioAction)
	{
		long timeStamp = System.currentTimeMillis();
		EventType eventType = (action == null) ?
				(slateComponent.isInTutorialZone(slateObject) ? EventType.TUTORIAL : EventType.WIDGET) : 
				EventType.COMMAND;
		EventLevel eventLevel = EventLevel.INFO;
		long sessionId = slateObject.getSessionID();
		String v = null; // ??
		
		Piece piece = slateComponent.getPiece(slateObject);
		
		return new Event(
				timeStamp,
				eventType,
				eventLevel,
				sessionId,
				tuioAction,
				v,
				piece);
	}
	
	@Override
	public void processEvent(SlateObject slateObject, Action action,
			TuioAction tuioAction, float deltaAngle, float deltaX,
			float deltaY, boolean isInTutorialZone) {
		
		logEvent(slateObject, action, tuioAction);
		
		if ((MechanixStateEnum.Run1.equals(state) || MechanixStateEnum.Tutorial.equals(state) || MechanixStateEnum.Run2.equals(state)) && isInTutorialZone)
		{
			action = slateComponent.getAction(ACTION_TUTORIAL);
		}
		else if (MechanixStateEnum.Network.equals(state) && isInTutorialZone)
		{
			action = slateComponent.getAction(ACTION_NETWORK_EXIT);
		}
		else if (action == null)
		{
			return;
		}
		
		// HelpParadigm.Classic
		if (slateComponent.getAction(ACTION_HELP_2).equals(action))
		{
			currentCommandHelperObject = TuioAction.Remove.equals(tuioAction) ? null : slateObject;
		}
		else
		{
			currentCommandObject = TuioAction.Remove.equals(tuioAction) ? null : slateObject;
		}
		
		if (slateComponent.getAction(ACTION_RESET).equals(action))
		{
			reset();
			return;
		}
		
		if (slateComponent.getAction(ACTION_NETWORK).equals(action))
		{
			if (TuioAction.Add.equals(tuioAction))
			{
				// Let Add be a toggle, for now
				if (networkLogger == null)
				{
					state = MechanixStateEnum.Network;
					startRepaintTimer(0);
					System.err.println("START NETWORKING");
					networkLogger = new NetworkLogger(slateComponent.getSlateSettings().getNetworkRole(), slateComponent);
					networkLogger.start();
					
				}
				/*else
				{
					System.err.println("STOP NETWORKING");
					clearRepaintTimer();
					reset();
				}*/
			}
		}
		if (slateComponent.getAction(ACTION_NETWORK_EXIT).equals(action))
		{
			System.err.println("STOP NETWORKING");
			clearRepaintTimer();
			reset();	
		}
		
		if (MechanixStateEnum.Network.equals(state))
		{
			if (TuioAction.Add.equals(tuioAction))
			{
				if (slateComponent.getAction(ACTION_CHALLENGE).equals(action))
				{
					networkLogger.challenge();
				}
				else if (slateComponent.getAction(ACTION_SAVE).equals(action))
				{
					networkLogger.save();
				}
				else if (slateComponent.getAction(ACTION_HELP_1).equals(action))
				{
					networkLogger.help();
				}
			}
		}
		
		/*
		if (State.Personalize_1.equals(state))
		{
			if (TuioAction.Remove.equals(tuioAction) && Command.Save.equals(command))
			{
				state = State.Start2;
				setTimeoutToSave1(1000);
				return;
			}
		}
		else if (State.Personalize_2.equals(state))
		{
			if (TuioAction.Remove.equals(tuioAction) && Command.Save.equals(command))
			{
				save();
				state = State.Save2;
				setTimeoutToReturnToState1(3000);
				return;
			}
		}*/
		
		// log viewer
		if (slateComponent.getAction(ACTION_LOG_VIEW).equals(action))
		{
			if (TuioAction.Add.equals(tuioAction))
			{
				state = MechanixStateEnum.LogSelect;
			}
			else if (MechanixStateEnum.LogSelect.equals(state))
			{
				if (TuioAction.Update.equals(tuioAction))
				{
					getNextLogFile(deltaAngle);
					return;
				}
				else if (TuioAction.Remove.equals(tuioAction))
				{
					state = MechanixStateEnum.LogView;
					if (currentLogViewer != null)
					{
						// stop log views in progress
						currentLogViewer.dispose();
					}
					currentLogViewer = new LogViewer(currentLogFile);
					return;
				}
			}
		}
		
		
		// The start state
		if ((MechanixStateEnum.Start1.equals(state) || MechanixStateEnum.Start2.equals(state) || MechanixStateEnum.LogView.equals(state)) 
				&& TuioAction.Add.equals(tuioAction))
		{
			// noop: Tutorial, Save, Help, Helper
			if (slateComponent.getAction(ACTION_CHALLENGE).equals(action))
			{
				clearState1Timeout();
				// probably leave this out in case we shift quickly in and out of takeAChallenge . . .
				// currentConfigurationCounter = 0;
				updateCurrentChallenge();
				
				MechanixStateEnum prevState = state;
				state = MechanixStateEnum.TakeAChallenge;
				prevState.stopSlideShow();
			}
			// secret option
			else if (slateComponent.getAction(ACTION_FREE_PLAY).equals(action))
			{
				clearState1Timeout();
				
				challengeMonster = ChallengeMonster.randomlyGetRandomlyPlacedChallengeMonster(
						slateComponent.getSymbolID(MechanixToolkit.TANGIBLE_MONSTER));
				
				MechanixStateEnum prevState = state;
				state = MechanixStateEnum.Run2;
				prevState.stopSlideShow();
			}
		}
		
		// We are taking a challenge.  We now  either select
		// the challenge, or move to the run state
		else if (MechanixStateEnum.TakeAChallenge.equals(state))
		{
			// noop: Tutorial, Save, Help, Helper
			if (slateComponent.getAction(ACTION_CHALLENGE).equals(action))
			{
				if (TuioAction.Update.equals(tuioAction))
				{
					getNextChallengeConfiguration(deltaAngle);
					return;
				}
				else if (TuioAction.Remove.equals(tuioAction))
				{
					// Set up help (global to this state)
					currentHelp1Counter = 0;
					currentHelp2Counter = 0;
					
					state = MechanixStateEnum.Run1;
					return;
				}
			}
		}
		
		// We're in the Run_1 state.  Tutorials and Help are available here
		else if (MechanixStateEnum.Run1.equals(state))
		{
			if (TuioAction.Add.equals(tuioAction))
			{
				if (slateComponent.getAction(ACTION_CHALLENGE).equals(action))
				{
					// If we don't like the challenge we're on, start over
					clearState1Timeout();
					state = MechanixStateEnum.TakeAChallenge;
				}
				if (slateComponent.getAction(ACTION_TUTORIAL).equals(action))
				{
					currentTutorial = slateComponent.getTutorial(slateObject);
					previousState = state;
					state = MechanixStateEnum.Tutorial;
					currentTutorial.start();
					return;
				}
				else if (slateComponent.getAction(ACTION_HELP_1).equals(action))
				{
					state = MechanixStateEnum.Help;
					
					// HelpParadigm.Modern
					//lastLateralCheckPointX = tobj.getX();
					lastLateralCheckPointY = slateObject.getY();
					// TODO gap of 4?
					progressBarStart = slateObject.getY() + 
						((numSolutions == 0) ? 0 : ((PROGRESS_BAR_LENGTH / (float) numSolutions) * currentHelp1Counter));
					//sSystem.err.println(tobj.getY() + ":" + progressBarLength + ":" + numSolutions + ":" + currentHelp1Counter + ":" + progressBarStart);
					
					return;
				}
				else if (slateComponent.getAction(ACTION_SAVE).equals(action))
				{
					//state = State.Personalize_1;
					state = MechanixStateEnum.Start2;
					setTimeoutToSave1(1000);
					return;
				}
				else if (slateComponent.getAction(ACTION_DELETE_ENTRY).equals(action))
				{
					deleteCurrentChallenge();
					return;
				}
				// secret option
				else if (slateComponent.getAction(ACTION_FREE_PLAY).equals(action))
				{
					clearState1Timeout();
					
					challengeMonster = ChallengeMonster.randomlyGetRandomlyPlacedChallengeMonster(
							slateComponent.getSymbolID(MechanixToolkit.TANGIBLE_MONSTER));
					
					state = MechanixStateEnum.Run2;
				}
			}
		}
		
		// We're in the Tutorial State
		else if (MechanixStateEnum.Tutorial.equals(state))
		{
			// No Updates - we're doing animated tutorials now
			/*if (TuioAction.Update.equals(tuioAction))
			{
				getNextTutorial(deltaAngle);
				return;
			}*/
			if (TuioAction.Remove.equals(tuioAction))
			{
				if (slateComponent.getAction(ACTION_TUTORIAL).equals(action))
				{
					currentTutorial.stop();
					//state = State.Run1;
					state = previousState;
					return;
				}
			}
		}
		
		// We're in the Help State
		else if (MechanixStateEnum.Help.equals(state))
		{
			if (TuioAction.Update.equals(tuioAction))
			{
				if (slateComponent.getAction(ACTION_HELP_1).equals(action))
				{
					if (HelpParadigm.Classic.equals(slateComponent.getSlateSettings().getHelpParadigm()))
					{
						updateHelpConfigurationCounter(deltaAngle);
					}
					else if (HelpParadigm.Modern.equals(slateComponent.getSlateSettings().getHelpParadigm()))
					{
						updateHelpConfigurationCounter(slateObject.getX(), slateObject.getY());
						updateHelpPieceCounter(deltaAngle);
					}
					else if (HelpParadigm.Simple.equals(slateComponent.getSlateSettings().getHelpParadigm()))
					{
						updateHelpConfigurationCounter(deltaAngle);
					}

					return;
				}
				// HelpParadigm.Classic
				if (slateComponent.getAction(ACTION_HELP_2).equals(action))
				{
					updateHelpPieceCounter(deltaAngle);
					return;
				}
			}
			else if (TuioAction.Remove.equals(tuioAction))
			{
				if (slateComponent.getAction(ACTION_HELP_1).equals(action))
				{
					state = MechanixStateEnum.Run1;
					return;
				}
			}
		}
		
		// We're in the Run_2 state.  Tutorials are available here
		else if (MechanixStateEnum.Run2.equals(state))
		{
			if (TuioAction.Add.equals(tuioAction))
			{
				if (slateComponent.getAction(ACTION_TUTORIAL).equals(action))
				{
					currentTutorial = slateComponent.getTutorial(slateObject);
					previousState = state;
					state = MechanixStateEnum.Tutorial;
					currentTutorial.start();
					return;
				}
				else if (slateComponent.getAction(ACTION_SAVE).equals(action))
				{
					//state = State.Personalize_2;
					saveChallenge();
					state = MechanixStateEnum.Save2;
					setTimeoutToReturnToState1(3000);
					return;
				}
				// secret option
				else if (slateComponent.getAction(ACTION_CHALLENGE).equals(action))
				{
					clearState1Timeout();
					state = MechanixStateEnum.TakeAChallenge;
				}
			}
		}
	}
	
	// TODO - CHANGE THIS ENTIRE PARADIGM.  THERE SHOULD BE A START
	// ANGLE -> THEN IT SHOULD REQUIRE TURNING A CERTAIN AMOUNT TO GET
	// TO THE NEXT ONE.  TO DO THIS, WE NEED TO KEEP A LAST ANGLE, AND
	// RESET IT WHEN 0 COMES THROUGH.
	
	// [0+,5] - the smaller the number, the more sensitive
	// 0+ is too sensitive, and 5 feels very sluggish.  > 5 is not practical
	//private static final float SENSITIVITY_FACTOR = 4.25f;
	//private static final float SENSITIVITY_FACTOR = 8.25f;
	
	// This is counter-intuitive because of the
	// mirrored projection.  So, rotateLeft works
	// like rotate-right would, logically (and vice versa)
	private boolean rotateRight(float deltaAngle)
	{
		return deltaAngle < ((-1 * slateComponent.getSlateSettings().getSensitivityFactor() * Math.PI) / 180f);
	}
	
	private boolean rotateLeft(float deltaAngle)
	{
		return deltaAngle > ((slateComponent.getSlateSettings().getSensitivityFactor() * Math.PI) / 180f);
	}
	
	private void getNextChallengeConfiguration(float deltaAngle)
	{
		if (rotateLeft(deltaAngle))
		{
			currentChallengeCounter--;
		}
		else if (rotateRight(deltaAngle))
		{
			currentChallengeCounter++;
		}
		updateCurrentChallenge();
	}
	
	// HelpParadigm.Classic and HelpParadigm.Simple
	private void updateHelpConfigurationCounter(float deltaAngle)
	{
		if (rotateLeft(deltaAngle))
		{
			currentHelp1Counter--;
		}
		else if (rotateRight(deltaAngle))
		{
			currentHelp1Counter++;
		}
		updateCurrentHelp();
	}
	// HelpParadigm.Modern
	private void updateHelpConfigurationCounter(float x, float y)
	{
		if (slideLeft(y))
		{
			lastLateralCheckPointY = y;
			currentHelp1Counter--;
		}
		else if (slideRight(y))
		{
			lastLateralCheckPointY = y;
			currentHelp1Counter++;
		}
		updateCurrentHelp();
	}
	//HelpParadigm.Modern and HelpParadigm.Classic
	private void updateHelpPieceCounter(float deltaAngle)
	{
		//error("Delta Angle: " + deltaAngle);
		if (rotateLeft(deltaAngle))
		{
			currentHelp2Counter--;
		}
		else if (rotateRight(deltaAngle))
		{
			currentHelp2Counter++;
		}
		updateCurrentHelp();
	}
	
	private boolean slideLeft(float y)
	{
		//System.out.println("[l] llcpy:" + lastLateralCheckPointY + " y:" + y + " delta:" + (lastLateralCheckPointY - y) + " slots:" + (.85f / (float) (2* numSolutions)) + " status: " + currentHelp1Counter + " of " + numSolutions);
		return (lastLateralCheckPointY - y) < ((-1f * PROGRESS_BAR_LENGTH) / (float) (numSolutions+2));
	}
	private boolean slideRight(float y)
	{
		//System.out.println("[r] llcpy:" + ((lastLateralCheckPointY - y) > (progressBarLength / (float) (numSolutions+1))) + " " + lastLateralCheckPointY + " y:" + y + " delta:" + (lastLateralCheckPointY - y) + " slots:" + (progressBarLength / (float) (numSolutions)) + " status: " + currentHelp1Counter + " of " + numSolutions);
		return (lastLateralCheckPointY - y) > (PROGRESS_BAR_LENGTH / (float) (numSolutions+1));
	}
	
	/*
	private void getNextTutorial(float deltaAngle)
	{
		if (currentTutorial == null)
		{
			return;
		}
		
		if (rotateLeft(deltaAngle))
		{
			currentTutorial.prev();
		}
		else if (rotateRight(deltaAngle))
		{
			currentTutorial.next();
		}
	}*/
	
	private void getNextLogFile(float deltaAngle)
	{
		if (rotateLeft(deltaAngle))
		{
			currentLogFileCounter--;
		}
		else if (rotateRight(deltaAngle))
		{
			currentLogFileCounter++;
		}
		
		updateCurrentLogFile();
	}
	
	Timer repaintTimer = null;
	private void startRepaintTimer(long delay)
	{
		repaintTimer = new Timer();
		repaintTimer.scheduleAtFixedRate(new RefreshTimerTask(), delay, 60);
	}
	class RefreshTimerTask extends TimerTask
	{
		public void run()
		{
			slateComponent.repaint();
		}
	}
	private void clearRepaintTimer()
	{
		if (repaintTimer != null)
		{
			repaintTimer.cancel();
			repaintTimer = null;
		}
	}
	
	Timer state1Timer = null;
	private void setTimeoutToReturnToState1(long delay)
	{
		state1Timer = new Timer();
		state1Timer.schedule(new State1TimerTask(), delay);
	}
	class State1TimerTask extends TimerTask
	{
		public void run()
		{
			state = MechanixStateEnum.Start1;
			state.startSlideShow();
		}
	}
	private void clearState1Timeout()
	{
		if (state1Timer != null)
		{
			state1Timer.cancel();
			state1Timer = null;
		}
	}
	
	Timer save1Timer = null;
	private void setTimeoutToSave1(long delay)
	{
		//System.err.println("scheduling!");
		save1Timer = new Timer();
		save1Timer.schedule(new Save1TimerTask(), delay);
	}
	class Save1TimerTask extends TimerTask
	{
		public void run()
		{
			//System.err.println("running!");
			saveSolution();
			setTimeoutToReturnToState1(3000);
		}
	}
	private void clearSave1Timeout()
	{
		if (save1Timer != null)
		{
			save1Timer.cancel();
			save1Timer = null;
		}
	}
	
	
	private void updateCurrentLogFile()
	{
		// get the log file name from the file system
		File logFolder = new File(slateComponent.getLogDirectoryName());
		File[] listOfLogFiles = logFolder.listFiles();
		
		numLogFiles = listOfLogFiles.length;

		if (numLogFiles == 0)
		{
			return;
		}
		
		if ((RotationParadigm.Bounded.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentLogFileCounter < 0)) ||
			(RotationParadigm.Continuous.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentLogFileCounter >= numLogFiles)))
		{
			currentLogFileCounter = 0;
		}
		
		if ((RotationParadigm.Bounded.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentLogFileCounter >= numLogFiles)) ||
			(RotationParadigm.Continuous.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentLogFileCounter < 0)))
		{
			currentLogFileCounter = numLogFiles - 1;
		}
		
		// Now, get the log file
		currentLogFile = listOfLogFiles[currentLogFileCounter];
	}
	
	/*
	 * Typically currentChallengeCounter is incremented/decremented before calling this function.
	 */
	private void updateCurrentChallenge()
	{	
		TreeMap<Integer,Challenge> challenges = slateComponent.getLibrary().getChallenges();
		int numChallenges = challenges.size();
		
		if (numChallenges == 0)
		{
			return;
		}
		
		if ((RotationParadigm.Bounded.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentChallengeCounter < 0)) ||
			(RotationParadigm.Continuous.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentChallengeCounter >= numChallenges)))
		{
			currentChallengeCounter = 0;
		}
		
		if ((RotationParadigm.Bounded.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentChallengeCounter >= numChallenges)) ||
			(RotationParadigm.Continuous.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentChallengeCounter < 0)))
		{
			currentChallengeCounter = numChallenges - 1;
		}
		
		int index = 0;
		for (Entry<Integer,Challenge> entry : challenges.entrySet())
		{
			if (index == currentChallengeCounter)
			{
				currentChallenge = entry.getValue();
				break;
			}
			index++;
		}
	}
	
	private void updateCurrentHelp()
	{
		List<Solution> solutions = slateComponent.getLibrary().getSolutions(currentChallenge);
		numSolutions = solutions.size();
		
		// no solutions
		if (numSolutions == 0)
		{
			return;
		}
		
		if ((RotationParadigm.Bounded.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentHelp1Counter < 0)) ||
			(RotationParadigm.Continuous.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentHelp1Counter >= numSolutions)))
		{
			currentHelp1Counter = 0;
			
			// HelpParadigm.Modern
			progressBarStart = currentCommandObject.getY() + ((PROGRESS_BAR_LENGTH / (float) numSolutions) * currentHelp1Counter);
			lastLateralCheckPointY = progressBarStart;
		}
		
		if ((RotationParadigm.Bounded.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentHelp1Counter >= numSolutions)) ||
			(RotationParadigm.Continuous.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentHelp1Counter < 0)))
		{
			currentHelp1Counter = numSolutions - 1;
			
			if (HelpParadigm.Modern.equals(slateComponent.getSlateSettings().getHelpParadigm()))
			{
				progressBarStart = currentCommandObject.getY() + ((PROGRESS_BAR_LENGTH / (float) numSolutions) * currentHelp1Counter);
				lastLateralCheckPointY = currentCommandObject.getY();
			}
		}
		
		Solution solution = solutions.get(currentHelp1Counter);
		List<Piece> currentHelpPieces = solution.getPieces();
		
		// Sort the solution pieces
		List<Piece> sortedHelpPieces = new ArrayList<Piece>(currentHelpPieces);
		Collections.sort(sortedHelpPieces, new Comparator<Piece>()
		{
			public int compare(Piece p1, Piece p2)
			{
				return Float.compare(p2.getX(), p1.getX());
			}
		});
		
		// Filter the current challenge pieces out of the solution			
		// CHANGES sortedHelpPieces (removes challenge pieces)
		slateComponent.removeChallengePieces(sortedHelpPieces, currentChallenge);
		
		numSolutionSteps = sortedHelpPieces.size();
		
		// no solution steps
		if (numSolutionSteps == 0)
		{
			Logger.info("nullifying currentSortedHelpPieces");
			currentSortedHelpPieces = null;
			return;
		}
		
		// Don't set this until now, to avoid flickering (i.e. intermediate
		// states of currentSortedHelpPieces being displayed on paint)
		currentSortedHelpPieces = sortedHelpPieces;
		
		if ((RotationParadigm.Bounded.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentHelp2Counter < 0)) ||
			(RotationParadigm.Continuous.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentHelp2Counter >= numSolutionSteps)))
		{
			currentHelp2Counter = 0;
		}
		
		if ((RotationParadigm.Bounded.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentHelp2Counter >= numSolutionSteps)) ||
			(RotationParadigm.Continuous.equals(slateComponent.getSlateSettings().getRotationParadigm()) && (currentHelp2Counter < 0)))
		{
			currentHelp2Counter = numSolutionSteps - 1;
		}
		
		// HelpParadigm.Simple
		// (Always show all solution steps)
		if (HelpParadigm.Simple.equals(slateComponent.getSlateSettings().getHelpParadigm()))
		{
			currentHelp2Counter = numSolutionSteps - 1;
		}
	}
	
	private void deleteCurrentChallenge()
	{
		slateComponent.getLibrary().removeChallenge(currentChallenge);
		updateCurrentChallenge();
	}
	
	private void saveSolution()
	{
		// "take a challenge"
		String idref = currentChallenge.getId().toString();
		Solution solution = new Solution();
		solution.setIdref(idref);
		saveLibraryEntry(solution, null, false);
		
		DataManager.saveLibrary(slateComponent.getLibrary(), slateComponent.getToolkitSettings().getName());
	}
	
	private void saveChallenge()
	{
		// "make a challenge"
			
		// 1. Create the corresponding challenge by filtering out the solution pieces
		Entry<Integer,Challenge> lastEntry = slateComponent.getLibrary().getChallenges().lastEntry();
		int nextId = (lastEntry == null || lastEntry.getValue() == null) ? 0 : lastEntry.getValue().getId() + 1;
			
		// Get and sort the current solution pieces
		List<SlateObject> sortedSolutionPieces = new ArrayList<SlateObject>();
		Iterator<Entry<Integer,Map<Long,SlateObject>>> iterator = slateComponent.getObjectMap().entrySet().iterator();
		while (iterator.hasNext())
		{
			Entry<Integer,Map<Long,SlateObject>> entry = iterator.next();
			Integer symbolId = entry.getKey();
			if (!slateComponent.isTangible(symbolId)) continue;
			
			Map<Long,SlateObject> map = entry.getValue();
			Iterator<Entry<Long,SlateObject>> mapIterator = map.entrySet().iterator();
			while (mapIterator.hasNext())
			{
				Entry<Long,SlateObject> mapEntry = mapIterator.next();
				SlateObject mo = mapEntry.getValue();
				sortedSolutionPieces.add(mo);
			}
		}
		// Sort the solution pieces from top to bottom
		Collections.sort(sortedSolutionPieces, new Comparator<SlateObject>() {
			public int compare(SlateObject mo1, SlateObject mo2) {
				return Float.compare(mo2.getX(), mo1.getX());
			}
		});
		
		// remove Challenge Pieces
		removeChallengePieces(sortedSolutionPieces);
		//System.out.println("number after challenge removed: " + sortedSolutionPieces.size());
		
		// Save the challenge.  Pass in a list of solution pieces
		Challenge challenge = new Challenge();
		challenge.setId(nextId);
		saveLibraryEntry(challenge, sortedSolutionPieces, true);
		
		// 2. Save a solution if there are any additional pieces
		if (sortedSolutionPieces.size() != 0)
		{
			Solution solution = new Solution();
			solution.setIdref(Integer.toString(nextId));
			saveLibraryEntry(solution, null, true);
		}
		
		DataManager.saveLibrary(slateComponent.getLibrary(), slateComponent.getToolkitSettings().getName());
	}
	
	private void removeChallengePieces(List<SlateObject> sortedObjects)
	{
		if (sortedObjects.size() == 0) {
			return;
		}
		// remove the first element
		sortedObjects.remove(0);
		
		// remove any monsters or home pieces
		boolean filtered = false;
		while (!filtered)
		{
			filtered = true;

			int size = sortedObjects.size();
			
			if (size == 0)
			{
				break;
			}
			
			for (int i = 0; i < size; i++)
			{
				SlateObject slateObject = sortedObjects.get(i);
				String resourceName = slateComponent.getResourceName(slateObject);
				
				if (TANGIBLE_HOME.equals(resourceName) || TANGIBLE_MONSTER.equals(resourceName))
				{
					sortedObjects.remove(i);
					filtered = false;
					break;
				}
			}
		}
	}
	
	// filter list = pieces not to include
	private void saveLibraryEntry(LibraryEntry libraryEntry, List<SlateObject> filterList, boolean saveChallengeMonster)
	{
		List<Piece> pieces = new ArrayList<Piece>();
	
		Iterator<Entry<Integer,Map<Long,SlateObject>>> iterator = slateComponent.getObjectMap().entrySet().iterator();
		while (iterator.hasNext())
		{
			Entry<Integer,Map<Long,SlateObject>> entry = iterator.next();
			Integer symbolId = entry.getKey();
			if (!slateComponent.isTangible(symbolId)) continue;
			
			Map<Long,SlateObject> map = entry.getValue();
			Iterator<Entry<Long,SlateObject>> mapIterator = map.entrySet().iterator();
			while (mapIterator.hasNext())
			{
				Entry<Long,SlateObject> mapEntry = mapIterator.next();
				SlateObject slateObject = mapEntry.getValue();
			
				if ((filterList != null) && 
					slateComponent.isInFilterList(slateObject, filterList)) {
					continue;
				}
				
				Piece piece = slateComponent.getPiece(slateObject);
				pieces.add(piece);
			}
		}
		
		// save challenge monster
		Logger.info(saveChallengeMonster + "," + (challengeMonster != null));
		if (saveChallengeMonster && (challengeMonster != null))
		{
			Piece piece = slateComponent.getPiece(challengeMonster);
			pieces.add(piece);
		}
		
		if (pieces.size() > 0) {
			libraryEntry.setPieces(pieces);
			slateComponent.getLibrary().addLibraryEntry(libraryEntry);
		}
	}
	
	
	public void updateGraphics(Graphics g)
	{
		if (slateComponent == null) {
			System.err.println("slateComponent is null");
			return;
		}
		
		try
		{
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			
			g2.setColor(Color.white);
			g2.fillRect(0,0,slateComponent.getWidth(),slateComponent.getHeight());
			
			// Order these in terms of what should be on top
			
			// 1. Draw challenge and help pieces first
			if (MechanixStateEnum.TakeAChallenge.equals(state) ||
				MechanixStateEnum.Run1.equals(state) ||
				(MechanixStateEnum.Tutorial.equals(state) && MechanixStateEnum.Run1.equals(previousState)))
			{
				drawPieces(g2);
			}
			if (MechanixStateEnum.Help.equals(state))
			{
				drawPieces(g2);
				// drawing this below, so it's on top
				// drawSolutionProgress(g2);
				drawHelp(g2);
			}
			if (MechanixStateEnum.Run2.equals(state))
			{
				drawVirtualObjects(g2);
			}
			
			// 2. Draw Tutorials/Messages (clips pieces moving off the work area)
			if (MechanixStateEnum.Tutorial.equals(state))
			{
				if (currentTutorial != null)
				{
					currentTutorial.show(g2, slateComponent.getToolkitSettings().getName());
				}
			}
			else if (MechanixStateEnum.Help.equals(state) && (numSolutions == 0))
			{
				Resources.drawHeaderImage(g2, slateComponent.getCurrentStateImage(MechanixStateEnum.NoSolution));
			}
			// FIXME TODO MAKE BETTER
			else if (MechanixStateEnum.Network.equals(state))
			{
				MechanixStateEnum subState = MechanixStateEnum.valueOf(networkLogger.getNetworkSubState().name());
				if (MechanixStateEnum.None.equals(subState))
				{
					Resources.drawHeaderImage(g2, slateComponent.getCurrentStateImage(state));
				}
				else
				{
					Resources.drawHeaderImage(g2, slateComponent.getCurrentStateImage(subState));
				}
			}
			// FIXME TODO MAKE BETTER
			else
			{	
				Resources.drawHeaderImage(g2, slateComponent.getCurrentStateImage(state));
			}
			
			// 4. Draw inline instructions
			if (MechanixStateEnum.TakeAChallenge.equals(state))
			{
				drawChallengeInstructions(g2);
			}
			if (MechanixStateEnum.Help.equals(state))
			{
				drawHelpInstructions(g2);
			}
			
			/*
			if (State.Personalize_1.equals(state) || State.Personalize_2.equals(state))
			{
				drawPersonalization(g2);
			}*/
			
			// 3. Draw Challenge and Help progress to be on top
			if (MechanixStateEnum.TakeAChallenge.equals(state))
			{
				drawChallengeProgress(g2);
			}
			if (MechanixStateEnum.Help.equals(state))
			{
				drawSolutionProgress(g2);
			}
			if (MechanixStateEnum.LogSelect.equals(state))
			{
				drawLogSelectProgress(g2);
			}
			
			// 5. Draw Log File Name
			if (MechanixStateEnum.LogSelect.equals(state))
			{
				drawLogFileName(g2);
			}
			
			// 6. Draw Log File Contents
			if (MechanixStateEnum.LogView.equals(state))
			{
				if (currentLogViewer != null)
				{
					if (currentLogViewer.isReady())	
					{
						drawLogPieces(g2);
					}
					else
					{
						drawLoadingLogFileMessage(g2);
					}
				}
			}
			
			// 7. Draw Network Pieces
			drawNetworkPieces(g2);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}
	
	private void drawPieces(Graphics2D g2)
	{
		if (currentChallenge == null)
		{
			Logger.error("current challenge is null");
			return;
		}
		
		for (Piece piece : currentChallenge.getPieces())
		{
			drawPiece(
					g2,
					piece.getName(),
					piece.getX(),
					piece.getY(),
					piece.getAngle(),
					DataSource.Live);
		}
	}
	
	private void drawVirtualObjects(Graphics2D g2)
	{
		// FIXME TODO this was challengeMonster
		//List<ISlateObject> virtualObjects = slateComponent.getVirtualObjects();
		List<ISlateObject> virtualObjects = new ArrayList<ISlateObject>();
		virtualObjects.add(challengeMonster);
		
		for (ISlateObject virtualObject : virtualObjects)
		{
			PointF point = slateComponent.getProjectionPoint(virtualObject);
			drawPiece(
					g2,
					slateComponent.getResourceName(virtualObject),
					point.getX(),
					point.getY(),
					virtualObject.getAngle(),
					DataSource.Live);
		}
	}
	
	private void drawNetworkPieces(Graphics2D g2)
	{
		if (networkLogger == null) {
			return;
		}
		
		// FIXME TODO - do anything with remote toolkit?
		ToolkitSettings remoteToolkitSettings = networkLogger.getRemoteToolkitSettings();
		if (remoteToolkitSettings == null) {
			return;
		}
		
		Collection<Event> events = networkLogger.getCurrentEvents();
		for (Event event : events)
		{
			if (slateComponent.isTangible(event.getName()))
			{
				drawPiece(
						g2,
						event.getName(),
						event.getX(),
						event.getY(),
						event.getAngle(),
						DataSource.Network);
			}
		}
	}
	
	private void drawLogPieces(Graphics2D g2)
	{
		Collection<Event> pieces = currentLogViewer.getCurrentPieces();
		for (Event piece : pieces)
		{
			drawPiece(
					g2,
					piece.getName(),
					piece.getX(),
					piece.getY(),
					piece.getAngle(),
					DataSource.Log);
		}
	}
	
	private void drawPiece(Graphics2D g2, String id, float x, float y, float angle, DataSource dataSource)
	{
		String fileName = null;
		Point translationAnchorPoint;
		
		try
		{
			//log("drawing piece[" + sm + "] (" + x + "," + y + ") @ " + angle);
			translationAnchorPoint = slateComponent.getAnchorPoint(id);
			fileName = slateComponent.getImageFileName(id);

			if (DataSource.Network.equals(dataSource))
			{
				// TODO could be a bit more elegant
				if (new File(fileName.replace(".", "_green.")).exists())
				{
					fileName = fileName.replace(".", "_green.");
				}
			}
			
			AffineTransform saveXform = g2.getTransform();
			
			BufferedImage img = ImageIO.read(new File(fileName));
			
			AffineTransform transform = new AffineTransform();
			// FIXME IF THIS WORKS, MOVE IT TO BASETOOLKIT!
			// Translate such that the anchor point is on the point, not the top left of the picture
			transform.translate(x - translationAnchorPoint.getX(), y - translationAnchorPoint.getY());
			// rotate the piece around the anchor point!
			transform.rotate(angle, translationAnchorPoint.getX(), translationAnchorPoint.getY());
			g2.transform(transform);
			g2.drawImage(img, null, 0, 0);
			g2.setTransform(saveXform);
		}
		catch (Exception e)
		{
			System.err.println("\n\n\nfile: " + fileName + "\n\n\n");
			Logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/*
	private void drawTutorialRegion(Graphics2D g2)
	{
		try
		{
			AffineTransform saveXform = g2.getTransform();
			
			BufferedImage img = ImageIO.read(new File(Resources.TUTORIAL_ZONE_FILE_NAME));
			
			AffineTransform transform = new AffineTransform();
			transform.translate(200, 15);
			g2.transform(transform);
			g2.drawImage(img, null, 0, 0);
			g2.setTransform(saveXform);
		}
		catch (IOException e)
		{
			error(e.getMessage());
			e.printStackTrace();
		}
	}
	*/
	
	private void drawLogFileName(Graphics2D g2)
	{
		if (currentLogFile != null)
		{
			drawCenteredText(g2, currentLogFile.getName(), TextType.LogSelect);
		}
	}
	
	private void drawLoadingLogFileMessage(Graphics2D g2)
	{
		if (currentLogViewer != null)
		{
			String percentage = String.format("%2f", currentLogViewer.getCurrentFileLoadStatus());
			drawCenteredText(g2, "Loading " + currentLogFile.getName() + " [ " + percentage + "% ]", TextType.LogSelect);
		}
	}
	
	private void drawCenteredText(Graphics2D g2, String text, TextType textGroup)
	{
		AffineTransform saveXform = g2.getTransform();
		Font saveFont = g2.getFont();
		RenderingHints saveHints = g2.getRenderingHints();
		Color saveColor = g2.getColor();
		
		g2.setColor(Color.BLACK);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		g2.setFont(textGroup.getFont());
		
		AffineTransform transform = new AffineTransform();
		transform.translate(400, 600);
		transform.rotate(-Math.PI/2f, 0, 0);
		
		g2.transform(transform);
		g2.drawString(text, 0, 0);
		
		g2.setColor(saveColor);
		g2.setRenderingHints(saveHints);
		g2.setFont(saveFont);
		g2.setTransform(saveXform);
	}
	
	/*
	private void drawPiecexxxx(Graphics2D g2, SimpleMachine sm, float x, float y, float angle)
	{
		try
		{
			AffineTransform saveXform = g2.getTransform();
			
			SimpleMachine sm = SimpleMachine.InclinedPlane;
			BufferedImage img = ImageIO.read(new File(sm.getFileName()));
			int locationX = 400;	// from TUIO (scaled to fit coordinate system)
			int locationY = 300;	// from TUIO (scaled to fit coordinate system)
									// y factor is correct
									// x factor is inverse.  So if x factor = (1 - x_factor)!
			int anchorPointX = sm.getAnchorPointX();	// must calculate for each image.  Store in enum
			int anchorPointY = sm.getAnchorPointY();	// must calculate for each image.  Store in enum
			float angle = 1.10f;			// from TUIO (in radians) - might need an offset if
									// the physical pieces weren't calculated properly
			
			AffineTransform transform = new AffineTransform();
			// Translate such that the anchor point is on the point, not the top left of the picture
			transform.translate(locationX - anchorPointX, locationY - anchorPointY);
			// rotate the piece around the anchor point!
			transform.rotate(angle, anchorPointX, anchorPointY);
			g2.transform(transform);
			g2.drawImage(img, null, 0, 0);
			g2.setTransform(saveXform);
			
			
			
			sm = SimpleMachine.WheelAndAxle;
			img = ImageIO.read(new File(sm.getFileName()));
			locationX = 900;	// from TUIO (scaled to fit coordinate system)
			locationY = 500;	// from TUIO (scaled to fit coordinate system)
									// y factor is correct
									// x factor is inverse.  So if x factor = (1 - x_factor)!
			anchorPointX = sm.getAnchorPointX();	// must calculate for each image.  Store in enum
			anchorPointY = sm.getAnchorPointY();	// must calculate for each image.  Store in enum
			angle = 1.10f;			// from TUIO (in radians) - might need an offset if
									// the physical pieces weren't calculated properly
			
			transform = new AffineTransform();
			// Translate such that the anchor point is on the point, not the top left of the picture
			transform.translate(locationX - anchorPointX, locationY - anchorPointY);
			// rotate the piece around the anchor point!
			transform.rotate(angle, anchorPointX, anchorPointY);
			g2.transform(transform);
			g2.drawImage(img, null, 0, 0);
			g2.setTransform(saveXform);
			
			/*
			AffineTransform transform = new AffineTransform();
			transform.translate(locationX - anchorPointX, locationY - anchorPointY);
			g2.transform(transform);
			g2.drawImage(img, null, 0, 0);
			g2.setTransform(saveXform);
			
			transform.rotate(Math.toRadians(angle), anchorPointX, anchorPointY);
			g2.transform(transform);
			g2.drawImage(img, null, 0, 0);
			g2.setTransform(saveXform);*/
			
			
			/*
			AffineTransform transform = new AffineTransform();
			transform.translate(300,300);
			g2.transform(transform);
			g2.drawImage(img, null, 0, 0);
			g2.setTransform(saveXform);
			
			transform.rotate(Math.toRadians(45),0,0);
		    g2.transform(transform);
		    g2.drawImage(img, null, 0, 0);
		    g2.setTransform(saveXform);
		    
		    transform.translate(10, -10);
		    g2.transform(transform);
		    g2.drawImage(img, null, 0, 0);
		    g2.setTransform(saveXform);
		    
		    AffineTransform transform2 = new AffineTransform();
		    transform2.rotate(Math.toRadians(45),0,0);
		    g2.transform(transform2);
			g2.drawImage(img, null, 0, 0);
			g2.setTransform(saveXform);
			
			transform2.translate(600,300);
			g2.transform(transform2);
		    g2.drawImage(img, null, 0, 0);
		    g2.setTransform(saveXform);
		    
		    
			AffineTransform saveXform = g2.getTransform();
			AffineTransform transform = new AffineTransform();
			transform.rotate(Math.toRadians(-90));
			//transform.translate(-width/1.5f, 20);
			transform.translate(-img.getWidth()/.875, 20);
			g2.transform(transform);
			g2.drawImage(img, null, 0, 0);
			g2.setTransform(saveXform);
			
		}
		catch (IOException e)
		{
			error(e.getMessage());
			e.printStackTrace();
		}
	}
	*/
	
	
	private void drawHelp(Graphics2D g2)
	{
		try 
		{
			if (currentSortedHelpPieces != null)
			{
				AffineTransform saveXform = g2.getTransform();
				
				for (int i = 0; i <= currentHelp2Counter && i < currentSortedHelpPieces.size(); i++)
				{
					Piece piece = currentSortedHelpPieces.get(i);
					drawPiece(
							g2,
							piece.getName(),
							piece.getX(),
							piece.getY(),
							piece.getAngle(),
							DataSource.Live);
				}
					
				g2.setTransform(saveXform);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void drawLogSelectProgress(Graphics2D g2)
	{
		if (currentLogFile != null)
		{
			drawCircularProgress(g2, currentCommandObject, numLogFiles, currentLogFileCounter);
		}
	}
	
	private void drawChallengeProgress(Graphics2D g2)
	{
		//System.err.println("++++ " + currentConfigurationCounter + " / " + numConfigurations);
		drawCircularProgress(g2, currentCommandObject, slateComponent.getLibrary().getChallenges().size(), currentChallengeCounter);
	}
	
	private void drawSolutionProgress(Graphics2D g2)
	{
		if (numSolutions == 0)
		{
			return;
		}
		
		//System.err.println("---- " + currentHelp1Counter + " / " + numSolutions);
		
		if (HelpParadigm.Classic.equals(slateComponent.getSlateSettings().getHelpParadigm()))
		{
			// Draw circular progress of number of solutions 
			drawCircularProgress(g2, currentCommandObject, numSolutions, currentHelp1Counter);
		}
		else if (HelpParadigm.Modern.equals(slateComponent.getSlateSettings().getHelpParadigm()))
		{
			// Draw lateral progress through the number of solutions
			drawLateralProgress(g2, currentCommandObject, numSolutions, currentHelp1Counter);
		}
		else if (HelpParadigm.Simple.equals(slateComponent.getSlateSettings().getHelpParadigm()))
		{
			// Draw circular progress through number of solutions 
			drawCircularProgress(g2, currentCommandObject, numSolutions, currentHelp1Counter);
		}
		
		if (numSolutionSteps == 0)
		{
			return;
		}
		
		if (HelpParadigm.Classic.equals(slateComponent.getSlateSettings().getHelpParadigm()))
		{
			// Draw circular progress of steps in the solution
			drawCircularProgress(g2, currentCommandHelperObject, numSolutionSteps, currentHelp2Counter);
		}
		else if (HelpParadigm.Modern.equals(slateComponent.getSlateSettings().getHelpParadigm()))
		{
			// Draw circular progress of steps in the solution
			drawCircularProgress(g2, currentCommandObject, numSolutionSteps, currentHelp2Counter);
		}
		// NOTE: no progress indicator for # of pieces for HelpParadigm.Simple
	}
	
	private void drawCircularProgress(Graphics2D g2, SlateObject slateObject, int num, int counter )
	{
		if (slateObject == null)
		{
			return;
		}
		
		AffineTransform saveXform = g2.getTransform();
		Color saveColor = g2.getColor();
		
		Point anchorPoint = slateComponent.getAnchorPoint(slateObject);
		PointF projectionPoint = slateComponent.getProjectionPoint(slateObject);
		
		// Translate such that the anchor point is on the point, not the top left of the picture
		
		//System.out.println("(" + to.getX() + "," + to.getY() + ") -> (" + xPos + "," + yPos + ")");
		
		for (int i = counter; i >= 0; i--)
		{
			int start = ((int) ((((float)num - (i + 1)) / ((float) num)) * 360) - 180);
			int angle = ((int) ((((float)i + 1)        / ((float) num)) * 360));
			
			if (start == 0) start = 360;
			if (angle == 0) angle = 360;
			
			g2.setColor((i % 2 == 0) ? Color.blue : Color.yellow);
			g2.fillArc(
					(int) (projectionPoint.getX() - anchorPoint.getX()*8d/3d),
					(int) (projectionPoint.getY() - anchorPoint.getY()*8d/3d),
					(int) (anchorPoint.getX()*16d/3d),
					(int) (anchorPoint.getX()*16d/3d),
					start,
					angle);
			
			//System.err.println("(" + start + "," + angle + "," + ((i % 2 == 0) ? "blue" : "pink") + ")");
		}
		
		g2.setColor(Color.white);
		g2.fillOval(
				(int) (projectionPoint.getX() - anchorPoint.getX()*32d/15d),
				(int) (projectionPoint.getY() - anchorPoint.getY()*32d/15d),
				(int) (anchorPoint.getX()*64d/15d),
				(int) (anchorPoint.getX()*64d/15d));
				
		//System.err.println("(" + start + "," + end + ")");
		
        g2.setColor(saveColor);
		g2.setTransform(saveXform);	
	}
	
	// FIXME CONSTANTS
	private void drawLateralProgress(Graphics2D g2, SlateObject slateObject, int num, int counter)
	{
		if (slateObject == null) return;
		
		float totalDistance = PROGRESS_BAR_LENGTH * slateComponent.getHeight();
		float stepSize = totalDistance / (float) num;
		//float stepSize = totalDistance - (progressBarGap * (num - 1)) / ((float) num);
		int thickness = 15;
		
		PointF startPoint = slateComponent.getProjectionPoint(slateObject, -125, progressBarStart);
		float x = startPoint.getX();
		float y = startPoint.getY();
		
		AffineTransform saveXform = g2.getTransform();
		Color saveColor = g2.getColor();
		
		int i = 0;
		for (; i <= counter; i++, y -= stepSize)
		{
			g2.setColor((i % 2 == 0) ? Color.blue : Color.yellow);
			g2.fillRect((int) x, (int) y - (PROGRESS_BAR_GAP * i), thickness, (int) stepSize);
			//System.err.println("drew rect: (" + (int)x + "," + (int)y + ") 5x" + (int)stepSize + "[" + totalDistance + ":" + num + ":" + counter + "]");
		}
		for (; i < num; i++, y -=  stepSize)
		{
			//int grayScale = 0x66 + i*((0xCC - 0x66)/num);
			int grayScale = 0xBB;
			g2.setColor(new Color(grayScale, grayScale, grayScale));
			g2.fillRect((int) x, (int) y - (PROGRESS_BAR_GAP * i), thickness, (int) stepSize);
		}
		
	    g2.setColor(saveColor);
		g2.setTransform(saveXform);	
		
	}
	
	private void drawChallengeInstructions(Graphics2D g2)
	{
		String fileName = slateComponent.getInstructionsImageFileName(ACTION_CHALLENGE);
		Point anchorPoint = slateComponent.getInstructionsAnchorPoint(ACTION_CHALLENGE);
		drawCommandInstructions(g2, fileName, anchorPoint);
	}
	
	private void drawHelpInstructions(Graphics2D g2)
	{
		// Get the appropriate file name and anchor points
		String fileName = slateComponent.getInstructionsImageFileName(ACTION_HELP_1).replace(".", "_" + slateComponent.getSlateSettings().getHelpParadigm().name() + ".");
		Point anchorPoint = slateComponent.getInstructionsAnchorPoint(ACTION_HELP_1);
		
		if (HelpParadigm.Modern.equals(slateComponent.getSlateSettings().getHelpParadigm()))
		{
			String fileName1 = fileName.replace(".", "_0.");
			drawCommandInstructions(g2, fileName1, anchorPoint);
			
			String fileName2 = fileName.replace(".", "_1.");
			drawLateralHelpInstructions(g2, fileName2);
		}
		else if (HelpParadigm.Simple.equals(slateComponent.getSlateSettings().getHelpParadigm()))
		{
			anchorPoint = new Point(115, 110);
			drawCommandInstructions(g2, fileName, anchorPoint);
		}
		// Note: Don't draw the image for classic help paradigm
	}
	
	/*
	private int currentSelectedLetterIndex = 0;
	private boolean showSpace = false;
	private long lastTextToggle = 0;
	private void drawPersonalization(Graphics2D g2)
	{
		long now = System.currentTimeMillis();
		if (now - lastTextToggle > 1000)
		{
			lastTextToggle = now;
			showSpace = !showSpace;
		}
		
		String name = "CAB"; //userName;
		if (showSpace && (name.length() > 0))
		{
			name = name.substring(0, currentSelectedLetterIndex) + "_" + name.substring(currentSelectedLetterIndex + 1);
		}
		
		//System.out.println(name);
		drawCenteredText(g2, name, TextType.NameEntry);
	}*/
	
	private void drawLateralHelpInstructions(Graphics2D g2, String fileName)
	{
		if (currentCommandObject == null) return;
		
		// FIXME TODO CONSTANTS
		PointF point = slateComponent.getProjectionPoint(currentCommandObject, -125, progressBarStart);
		Point anchorPoint = new Point(28, (int) (PROGRESS_BAR_LENGTH * slateComponent.getHeight()));
		drawInstructions(g2, fileName, point, anchorPoint);
	}
	
	private void drawCommandInstructions(Graphics2D g2, String fileName, Point anchorPoint)
	{
		if (currentCommandObject == null) return;
		
		// Find the center of the currentCommandObject
		PointF point = slateComponent.getProjectionPoint(currentCommandObject);		
		drawInstructions(g2, fileName, point, anchorPoint);
	}
	
	private void drawInstructions(Graphics2D g2, String fileName, PointF point, Point anchorPoint)
	{
		// Draw the image
		try
		{
			AffineTransform saveXform = g2.getTransform();
			BufferedImage img = ImageIO.read(new File(fileName));
			AffineTransform transform = new AffineTransform();
			// Translate such that the anchor point is on the point, not the top left of the picture
			transform.translate(point.getX() - anchorPoint.getX(), point.getY() - anchorPoint.getY());
			// rotate the piece around the anchor point!
			g2.transform(transform);
			g2.drawImage(img, null, 0, 0);
			g2.setTransform(saveXform);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
