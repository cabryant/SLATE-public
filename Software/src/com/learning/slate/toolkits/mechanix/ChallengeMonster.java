/**
 * 
 */
package com.learning.slate.toolkits.mechanix;

import com.learning.slate.toolkits.ISlateObject;

/**
 * @author cbryant
 *
 */
public class ChallengeMonster implements ISlateObject
{
	private static final float X_BORDER = .3f;
	private static final float Y_BORDER = .3f;
	
	private static final double MONSTER_CHANCE = 1d;
	
	private int symbolID;
	private float x;
	private float y;
	private float angle;
	
	// prevent public instantiation
	private ChallengeMonster(int symbolID, float x, float y, float a)
	{
		// Constrain X & Y
		if (x < X_BORDER)       x = X_BORDER;
		if (x > (1 - X_BORDER)) x = 1 - X_BORDER;
		if (y < Y_BORDER)       y = Y_BORDER;
		if (y > (1 - Y_BORDER)) y = 1 - Y_BORDER;
		
		this.symbolID = symbolID;
		this.x = x;
		this.y = y;
		this.angle = a;
	}
	
	public int getSymbolID() { return symbolID; }
	public float getX()      { return x; }
	public float getY()      { return y; }
	public float getAngle()  { return angle; }
	
	public static ChallengeMonster randomlyGetRandomlyPlacedChallengeMonster(int symbolId)
	{
		ChallengeMonster monster = null;
		double chance = Math.random();
		
		if (chance < MONSTER_CHANCE)
		{
			monster = new ChallengeMonster(symbolId,
					(float) Math.random(),
					(float) Math.random(),
					(float) (Math.random() * Math.PI * 2f));
		}
		
		//System.err.println("challenge monster: " + monster + ((monster != null) ? (" " + monster.getX() + "," + monster.getY()) : ""));	
		return monster;
	}
}