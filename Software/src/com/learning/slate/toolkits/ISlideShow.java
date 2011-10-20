/**
 * 
 */
package com.learning.slate.toolkits;

/**
 * @author cbryant
 *
 */
public interface ISlideShow
{
	void start();
	void end();
	boolean isRunning();
	boolean playOnce();
	boolean hasPlayed();
	boolean next();
	String getCurrentImageFileName();
}
