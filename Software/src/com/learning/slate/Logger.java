/**
 * 
 */
package com.learning.slate;

/**
 * TODO replace with log4j
 * 
 * @author coram
 *
 */
public class Logger
{
	private static boolean verbose = true;
	
	// prevent direct instantiation
	private Logger() {}
	
	public static void toggleVerbose()
	{
		verbose = !verbose;
	}
	
	public static void info(String message)
	{
		if (verbose)
		{
			System.out.println("mX: " + message);
		}
	}
	
	public static void error(String message)
	{
		if (verbose)
		{
			System.err.println("mX: " + message);
		}
	}
}
