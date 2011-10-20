package com.learning.slate.toolkits;

/**
 * @author cbryant
 *
 */
public interface IStateEnum {
	
	public String getFileName();
	public ISlideShow getSlideShow();
	boolean startSlideShow();	
	boolean stopSlideShow();
}