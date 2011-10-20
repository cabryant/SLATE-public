/**
 * 
 */
package com.learning.slate.toolkits.mechanix;

import com.learning.slate.toolkits.ISlideShow;
import com.learning.slate.toolkits.IStateEnum;

/**
 * @author coram
 *
 */
public enum MechanixStateEnum implements IStateEnum
{
	None(           null,              null),
	Start1(         "start.png",       new SplashSlideShow()),
	Start2(         "save.png",        null),
	TakeAChallenge( "challenge.png",   null),
	Run1(           "challenge.png",   null),
	Run2(           "freeplay.png",    null),
	Save2(          "save.png",        null),
	Tutorial(       null,              null),
	Help(           "challenge.png",   null),
	NoSolution(     "no_solution.png", null),
	Network(        "network.png",     null),
	// FIXME TODO IMPROVE
	Network_Waiting("network_waiting.png", null),
	Network_Take_A_Challenge("network_take_a_challenge.png", null), // Request
	Network_Make_A_Challenge("network_make_a_challenge.png", null), // accept
	Network_Watch("network_watch.png", null),
	Network_Watch_2("network_watch_2.png", null),
	Network_Solve("network_solve.png",     null),
	Network_Help("network_help.png",       null),
	Network_Try("network_try.png",         null),
	Network_Saved("network_saved.png",     null),
	// FIXME TODO IMPROVE
	Personalize_1(  "personalize.png",     null),
	Personalize_2(  "personalize.png",     null),
	// FIXME TODO
	LogSelect(      "log_select.png",      null),
	LogView(        "log_view.png",        null);
	
	private String fileName;
	private ISlideShow slideShow;
	
	MechanixStateEnum(String fn, ISlideShow ss) {
		fileName = fn;
		slideShow = ss;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileName() {
		return fileName;
	}
	public void setSlideShow(ISlideShow slideShow) {
		this.slideShow = slideShow;
	}
	public ISlideShow getSlideShow() {
		return slideShow;
	}
	
	public boolean startSlideShow()
	{
		if (slideShow == null)
		{
			return false;
		}
		
		if (slideShow.playOnce() && slideShow.hasPlayed())
		{
			return false;
		}
		
		slideShow.start();
		return true;
	}
	
	public boolean stopSlideShow()
	{
		if (slideShow != null)
		{
			slideShow.end();
			return true;
		}
		
		return false;
	}
	
	public String getCurrentImage()
	{
		if ((slideShow != null) && (slideShow.isRunning()))
		{
			return slideShow.getCurrentImageFileName();
		}
		else
		{
			return fileName;
		}
	}
}
