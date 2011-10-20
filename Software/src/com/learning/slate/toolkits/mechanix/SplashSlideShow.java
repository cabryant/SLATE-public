/**
 * 
 */
package com.learning.slate.toolkits.mechanix;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import sun.audio.AudioStream;

import com.learning.slate.toolkits.ISlideShow;
import com.learning.slate.toolkits.Resources;

/**
 * @author coram
 *
 */
public class SplashSlideShow implements ISlideShow
{
	static class AudioSegment
	{
		private long duration;
		private List<Integer> imageIndices;
		
		public AudioSegment(long d, List<Integer> ii)
		{
			duration = d;
			imageIndices = ii;
		}
		
		public long getAudioDuration()
		{
			return duration;
		}
		
		public long getImageDuration()
		{
			return duration / imageIndices.size();
		}
		
		public List<Integer> getImageIndices()
		{
			return imageIndices;
		}
	}
	
	private Map<Integer,AudioSegment> audioImageMap = new HashMap<Integer,AudioSegment>();
	{
		// Audio segment 1
		List<Integer> ii1 = new ArrayList<Integer>();
		ii1.add(1);
		ii1.add(2);
		ii1.add(3);
		AudioSegment as1 = new AudioSegment(2875L + 4000L, ii1);
		//AudioSegment as1 = new AudioSegment(2563L, ii1); #2
		audioImageMap.put(1, as1);
	
		// Audio segment 2
		List<Integer> ii2 = new ArrayList<Integer>();
		ii2.add(4);
		AudioSegment as2 = new AudioSegment(2063L + 3000L, ii2);
		audioImageMap.put(2, as2);
		
		// Audio segment 3
		List<Integer> ii3 = new ArrayList<Integer>();
		ii3.add(5);
		AudioSegment as3 = new AudioSegment(2313L + 3000L, ii3);
		audioImageMap.put(3, as3);
		
		// Audio segment 4
		List<Integer> ii4 = new ArrayList<Integer>();
		ii4.add(6);
		AudioSegment as4 = new AudioSegment(2938L + 4000L, ii4); // #1
		//AudioSegment as4 = new AudioSegment(2875L + 4000L, ii4); // #2
		//AudioSegment as4 = new AudioSegment(3188L + 4000L, ii4); // #3
		audioImageMap.put(4, as4);
		
		// Audio segment 5
		List<Integer> ii5 = new ArrayList<Integer>();
		ii5.add(7);
		ii5.add(8);
		ii5.add(9);
		AudioSegment as5 = new AudioSegment(2813L + 4500L, ii5);
		audioImageMap.put(5, as5);
		
		// Audio segment 6
		List<Integer> ii6 = new ArrayList<Integer>();
		ii6.add(10);
		AudioSegment as6 = new AudioSegment(2813L + 3500L, ii6);
		//AudioSegment as6 = new AudioSegment(2938L, ii6); #2
		//AudioSegment as6 = new AudioSegment(3000L, ii6); #3
		audioImageMap.put(6, as6);
		
		// Audio segment 7
		List<Integer> ii7 = new ArrayList<Integer>();
		ii7.add(11);
		ii7.add(12);
		ii7.add(13);
		ii7.add(14);
		AudioSegment as7 = new AudioSegment(2000L + 3500L, ii7);
		//AudioSegment as7 = new AudioSegment(2563L + 3500L, ii7); #2
		//AudioSegment as7 = new AudioSegment(2563L, ii7); #3
		audioImageMap.put(7, as7);
		
		// Audio segment 8
		List<Integer> ii8 = new ArrayList<Integer>();
		ii8.add(15);
		ii8.add(16);
		ii8.add(17);
		AudioSegment as8 = new AudioSegment(2125L + 8000L, ii8);
		audioImageMap.put(8, as8);
		
		// Spacer
		/*
		List<Integer> ii9 = new ArrayList<Integer>();
		ii9.add(18);
		AudioSegment as9 = new AudioSegment(10000L, ii9);
		start1Map.put(9, as9);*/
	}
	
	private String baseImgFileName;
	private String baseAudioFileName;
	private boolean playOnce;
	private boolean hasPlayed;
	private int currentImageIndex;
	private int currentAudioIndex;
	private int lastAudioIndex;
	
	private boolean running = false;
	private Timer slideShowTimer = null;
	private AudioStream currentAudioStream = null;
	
	public SplashSlideShow()
	{
		String fn = "splash";
		String ift = "png";
		String aft = "aif";
		boolean po = true;
		
		baseImgFileName =   fn + File.separator + fn + "." + ift;
		baseAudioFileName = fn + File.separator + fn + "." + aft;
		
		playOnce = po;
		
		resetIndices();
	}
	
	public boolean next()
	{
		AudioSegment as = audioImageMap.get(currentAudioIndex);
		if (as == null)
		{
			if (playOnce)
			{
				end();
				return false;
			}
			
			resetIndices();
			return true;
		}
		
		if (!as.getImageIndices().contains(currentImageIndex + 1))
		{
			currentAudioIndex++;
			// recursive call
			return next();
		}
		
		currentImageIndex++;
		
		return true;
	}
	
	public boolean playOnce()
	{
		return playOnce;
	}
	
	public boolean hasPlayed()
	{
		return hasPlayed;
	}
	
	private void resetIndices()
	{
		// file names are 1-based
		currentImageIndex = 1;
		currentAudioIndex = 1;
		lastAudioIndex = 0;
	}
	
	private String getFormattedIndex(int slide)
	{
		return (slide < 10) ? "0" + slide : Integer.toString(slide);
	}

	public String getCurrentImageFileName()
	{
		return baseImgFileName.replace(".", "_" + getFormattedIndex(currentImageIndex) + ".");
	}
	
	private String getCurrentAudioFileName()
	{
		return baseAudioFileName.replace(".", "_" + getFormattedIndex(currentAudioIndex) + ".");
	}
	
	private long getCurrentImageFileDuration()
	{
		return audioImageMap.get(currentAudioIndex).getImageDuration();
	}
	
	public boolean isRunning()
	{
		return running;
	}
	
	public void start()
	{
		// FIXME - uncomment to activate the slide show
		/*
		if (running)
		{
			return;
		}
		
		running = true;
		hasPlayed = true;
		play();
		*/
	}
	
	public void end()
	{
		running = false;
		
		if (currentAudioStream != null)
		{
			Resources.stopAudioStream(currentAudioStream);
			currentAudioStream = null;
		}
		
		if (slideShowTimer != null)
		{
			slideShowTimer.cancel();
			slideShowTimer = null;
		}
		
		resetIndices();
	}
	
	private void setTimeout(long delay)
	{
		slideShowTimer = new Timer();
		slideShowTimer.schedule(new SlideShowTimerTask(), delay);
	}
	
	class SlideShowTimerTask extends TimerTask
	{
		public void run()
		{
			boolean continuePlaying = next();
			if (continuePlaying)
			{
				play();
			}
		}
	}
	
	private void play()
	{
		if (currentAudioIndex != lastAudioIndex)
		{
			lastAudioIndex = currentAudioIndex;
			Resources.stopAudioStream(currentAudioStream);
			currentAudioStream = Resources.playAudio(getCurrentAudioFileName());
		}
		
		long duration = getCurrentImageFileDuration();
		setTimeout(duration);
	}
}