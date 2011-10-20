package com.learning.slate.data;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("slate")
public class SlateSettings
{
	public enum HelpParadigm
	{
		Classic,	// 2 Command Markers
		Modern,		// 1 Command Marker w/ 2 functions (rotate through pieces, slide through solutions)
		Simple;		// 1 Command Marker w/ 1 function  (rotate through solutions)
	
		public HelpParadigm next()
		{
			int count = HelpParadigm.values().length;
			for (int index = 0; index <  count; index++)
			{
				HelpParadigm helpParadigm = HelpParadigm.values()[index];
				if (helpParadigm.equals(this)) {
					return HelpParadigm.values()[(index + 1) == count ? 0 : index];
				}
			}
			// This should never happen
			return this;
		}
	}
	
	public enum RotationParadigm
	{
		Bounded,
		Continuous
	}
	
	public enum NetworkRole
	{
		Client,
		Server
	}
	
	public float getOffsetX(float cameraX, float projectorX) {
		//return windowWidth * (1f - 2f*Math.abs(cameraX - 1f));
		return (projectorX * getWindowWidth()) - (Math.abs(cameraX - 1) * getWindowWidth());
	}
	public float getOffsetY(float cameraY, float projectorY) {
		// return (1f - 2f*cameraY)*(windowHeight/2f);
		return (projectorY * getWindowHeight()) - (cameraY * getWindowHeight());
	}
	public float getDistortionOffsetX(float cameraX, float projectorX) {
		return ((projectorX * getWindowWidth()) - (Math.abs(cameraX - 1) * getWindowWidth())) / (-(cameraX - .5f));
	}
	public float getDistortionOffsetY(float cameraY, float projectorY) {
		return ((projectorY * getWindowHeight()) - (cameraY * getWindowHeight()) - calibration.offsetY) / (cameraY - .5f);		
	}
	
	public Float getProjectionXFromCameraX(float cameraX)
	{	
		// FIXME (this should be saving proportions of screen size, not the actual pixel displacement)
		// FIXME (this means that when we show a piece, we have to multiple by current screen sizes.
		// NEW: float distortionOffset = (camX > .5f) ? -20f : -20f;
		//float distortionOffset = (camX > .5f) ? 30f : 40f;
		float distortionOffset = (cameraX > .5f) ? calibration.distortionOffsetXHi : calibration.distortionOffsetXLo;
		return (Math.abs(cameraX - 1) * getWindowWidth()) + calibration.offsetX - ((cameraX - .5f) * distortionOffset);
		//return (Math.abs(camX - 1) * width) + 185;
		//return ((camX - X_PROJECTION_MIN) / (X_PROJECTION_MAX - X_PROJECTION_MIN));
	}
	
	public Float getProjectionYFromCameraY(float cameraY)
	{	
		// FIXME see above
		// NEW: float distortionOffset = (camY > .5f) ? 25f : 30f;
		//float distortionOffset = (camY > .5f) ? 50f : 40f;
		float distortionOffset = (cameraY > .5f) ? calibration.distortionOffsetYHi : calibration.distortionOffsetYLo;
		return (cameraY * getWindowHeight()) + calibration.offsetY + ((cameraY - .5f) * distortionOffset);
		//return (Math.abs(camY - 1) * height);
		//return ((camY - Y_PROJECTION_MIN) / (Y_PROJECTION_MAX - Y_PROJECTION_MIN));
	}
	
	public Float getProjectionAngleFromCameraAngle(float cameraAngle, float baseAngle, boolean rotates)
	{
		// FIXME perform angle calibration (not just angle rotation)?
		// In examining this code, it is important to remember that:
		//	a) the camera is rotated PI/2 radians
		//	b) the camera detects the inverse angle of that viewed from the front of the display
		float angle = (rotates? (baseAngle - cameraAngle) : 0f) - (float) (Math.PI/2f);
		while (angle > 2d*Math.PI) {
			angle -= (2d*Math.PI);
		}
		while (angle < -2d*Math.PI) {
			angle += (2d*Math.PI);
		}
		if (angle > 0) {
			//System.out.println("\t> 0");
			return Double.valueOf(angle - Math.PI).floatValue();
		} else {
			//System.out.println("\t< 0");
			return angle;
		}
	}
	
	// Slate Settings and defaults
	private Calibration calibration;
	private String toolkit;
	private HelpParadigm helpParadigm;
	private RotationParadigm rotationParadigm;
	private NetworkRole networkRole;
	private String serverIp;
	private Integer serverPort;
	private Float sensitivityFactor;
	private Float tangibleBaseInches;
	private Float displayWidthInches;
	private Float displayHeightInches;
	//private Integer windowWidth;
	//private Integer windowHeight;
	//private Boolean fullScreen;
	//private Boolean undecorated;
	//private Boolean externalDisplay;
	
	// FIXME TODO there is no need to have default calibration values.  These should be
	// generated based on the toolkit (+ default) contents after running the calibration process.
	private static final Float DEFAULT_SYSTEM_OFFSET_X = -4f;
	private static final Float DEFAULT_SYSTEM_OFFSET_Y = 0f;
	private static final Float DEFAULT_SYSTEM_DISTORTION_OFFSET_X_HI = -80f;
	private static final Float DEFAULT_SYSTEM_DISTORTION_OFFSET_X_LO = -40f;
	private static final Float DEFAULT_SYSTEM_DISTORTION_OFFSET_Y_HI = 0f;
	private static final Float DEFAULT_SYSTEM_DISTORTION_OFFSET_Y_LO = -10f;
	
	public static final String DEFAULT_TOOLKIT                      = "base";
	private static final HelpParadigm DEFAULT_HELP_PARADIGM         = HelpParadigm.Simple;
	private static final RotationParadigm DEFAULT_ROTATION_PARADIGM = RotationParadigm.Bounded;
	private static final NetworkRole DEFAULT_NETWORK_ROLE           = NetworkRole.Client;
	private static final String DEFAULT_SERVER_IP                   = "171.64.184.216";
	private static final Integer DEFAULT_SERVER_PORT                = 4444;
	private static final Float DEFAULT_SENSITIVITY_FACTOR           = 4.25f;
	
	private static final Float DEFAULT_TANGIBLE_BASE_INCHES         = 2.5f;
	private static final Float DEFAULT_DISPLAY_WIDTH_INCHES         = 17.5f;
	private static final Float DEFAULT_DISPLAY_HEIGHT_INCHES        = 23.5f;
	private static final Integer DEFAULT_WINDOW_WIDTH               = 1024; //800;
	private static final Integer DEFAULT_WINDOW_HEIGHT              = 768;  //600;
	/*private static final Boolean DEFAULT_FULL_SCREEN                = false;
	private static final Boolean DEFAULT_UNDECORATED                = true;
	private static final Boolean DEFAULT_EXTERNAL_DISPLAY           = true;*/
	
	public void setDefaultValues() {
		if (calibration == null)                          calibration = new Calibration();
		if (calibration.getOffsetX() == null)             calibration.setOffsetX(DEFAULT_SYSTEM_OFFSET_X);
		if (calibration.getOffsetY() == null)             calibration.setOffsetY(DEFAULT_SYSTEM_OFFSET_Y);
		if (calibration.getDistortionOffsetXHi() == null) calibration.setDistortionOffsetXHi(DEFAULT_SYSTEM_DISTORTION_OFFSET_X_HI);
		if (calibration.getDistortionOffsetXLo() == null) calibration.setDistortionOffsetXLo(DEFAULT_SYSTEM_DISTORTION_OFFSET_X_LO);
		if (calibration.getDistortionOffsetYHi() == null) calibration.setDistortionOffsetYHi(DEFAULT_SYSTEM_DISTORTION_OFFSET_Y_HI);
		if (calibration.getDistortionOffsetYLo() == null) calibration.setDistortionOffsetYLo(DEFAULT_SYSTEM_DISTORTION_OFFSET_Y_LO);
		
		if (toolkit == null)             setToolkit(DEFAULT_TOOLKIT);
		if (helpParadigm == null)        setHelpParadigm(DEFAULT_HELP_PARADIGM);
		if (rotationParadigm == null)    setRotationParadigm(DEFAULT_ROTATION_PARADIGM);
		if (networkRole == null)         setNetworkRole(DEFAULT_NETWORK_ROLE);
		if (serverIp == null)            setServerIp(DEFAULT_SERVER_IP);
		if (serverPort == null)          setServerPort(DEFAULT_SERVER_PORT);
		if (sensitivityFactor == null)   setSensitivityFactor(DEFAULT_SENSITIVITY_FACTOR);
		if (tangibleBaseInches == null)  setTangibleBaseInches(DEFAULT_TANGIBLE_BASE_INCHES);
		if (displayWidthInches == null)  setDisplayWidthInches(DEFAULT_DISPLAY_WIDTH_INCHES);
		if (displayHeightInches == null) setDisplayHeightInches(DEFAULT_DISPLAY_HEIGHT_INCHES);
		//if (windowWidth == null)         setWindowWidth(DEFAULT_WINDOW_WIDTH);
		//if (windowHeight == null)        setWindowHeight(DEFAULT_WINDOW_HEIGHT);
		//if (fullScreen == null)          setFullScreen(DEFAULT_FULL_SCREEN);
		//if (undecorated == null)         setUndecorated(DEFAULT_UNDECORATED);
		//if (externalDisplay == null)     setExternalDisplay(DEFAULT_EXTERNAL_DISPLAY);
	}
	
	@XStreamAlias("calibration")
	public static class Calibration
	{
		@XStreamAsAttribute
		private Float offsetX;
		@XStreamAsAttribute
		private Float offsetY;
		@XStreamAsAttribute
		private Float distortionOffsetXHi;
		@XStreamAsAttribute
		private Float distortionOffsetXLo;
		@XStreamAsAttribute
		private Float distortionOffsetYHi;
		@XStreamAsAttribute
		private Float distortionOffsetYLo;
		
		public Float getOffsetX() {
			return offsetX;
		}
		public void setOffsetX(Float offsetX) {
			this.offsetX = offsetX;
		}
		public Float getOffsetY() {
			return offsetY;
		}
		public void setOffsetY(Float offsetY) {
			this.offsetY = offsetY;
		}
		public Float getDistortionOffsetXHi() {
			return distortionOffsetXHi;
		}
		public void setDistortionOffsetXHi(Float distortionOffsetXHi) {
			this.distortionOffsetXHi = distortionOffsetXHi;
		}
		public Float getDistortionOffsetXLo() {
			return distortionOffsetXLo;
		}
		public void setDistortionOffsetXLo(Float distortionOffsetXLo) {
			this.distortionOffsetXLo = distortionOffsetXLo;
		}
		public Float getDistortionOffsetYHi() {
			return distortionOffsetYHi;
		}
		public void setDistortionOffsetYHi(Float distortionOffsetYHi) {
			this.distortionOffsetYHi = distortionOffsetYHi;
		}
		public Float getDistortionOffsetYLo() {
			return distortionOffsetYLo;
		}
		public void setDistortionOffsetYLo(Float distortionOffsetYLo) {
			this.distortionOffsetYLo = distortionOffsetYLo;
		}
	}
	
	public Calibration getCalibration() {
		return calibration;
	}
	public void setCalibration(Calibration calibration) {
		this.calibration = calibration;
	}
	public String getToolkit() {
		return toolkit;
	}
	public void setToolkit(String toolkit) {
		this.toolkit = toolkit;
	}
	public HelpParadigm getHelpParadigm() {
		return helpParadigm;
	}
	public void setHelpParadigm(HelpParadigm helpParadigm) {
		this.helpParadigm = helpParadigm;
	}
	public RotationParadigm getRotationParadigm() {
		return rotationParadigm;
	}
	public void setRotationParadigm(RotationParadigm rotationParadigm) {
		this.rotationParadigm = rotationParadigm;
	}
	public NetworkRole getNetworkRole() {
		return networkRole;
	}
	public void setNetworkRole(NetworkRole networkRole) {
		this.networkRole = networkRole;
	}
	public String getServerIp() {
		return serverIp;
	}
	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}
	public Integer getServerPort() {
		return serverPort;
	}
	public void setServerPort(Integer serverPort) {
		this.serverPort = serverPort;
	}
	public Float getSensitivityFactor() {
		return sensitivityFactor;
	}
	public void setSensitivityFactor(Float sensitivityFactor) {
		this.sensitivityFactor = sensitivityFactor;
	}
	public Float getTangibleBaseInches() {
		return tangibleBaseInches;
	}
	public void setTangibleBaseInches(Float tangibleBaseInches) {
		this.tangibleBaseInches = tangibleBaseInches;
	}
	public Float getDisplayWidthInches() {
		return displayWidthInches;
	}
	public void setDisplayWidthInches(Float displayWidthInches) {
		this.displayWidthInches = displayWidthInches;
	}
	public Float getDisplayHeightInches() {
		return displayHeightInches;
	}
	public void setDisplayHeightInches(Float displayHeightInches) {
		this.displayHeightInches = displayHeightInches;
	}
	
	// FIXME TODO
	// Get these from the Graphics Device, and scale
	// everything (including images) appropriately
	public Integer getWindowWidth() {
		return DEFAULT_WINDOW_WIDTH;
	}
	/*public void setWindowWidth(Integer windowWidth) {
		this.windowWidth = windowWidth;
	}*/
	public Integer getWindowHeight() {
		return DEFAULT_WINDOW_HEIGHT;
	}
	/*public void setWindowHeight(Integer windowHeight) {
		this.windowHeight = windowHeight;
	}
	public Boolean getFullScreen() {
		return fullScreen;
	}
	public void setFullScreen(Boolean fullScreen) {
		this.fullScreen = fullScreen;
	}
	public Boolean getUndecorated() {
		return undecorated;
	}
	public void setUndecorated(Boolean undecorated) {
		this.undecorated = undecorated;
	}
	public Boolean getExternalDisplay() {
		return externalDisplay;
	}
	public void setExternalDisplay(Boolean externalDisplay) {
		this.externalDisplay = externalDisplay;
	}*/
	
	public float getTangibleBaseWidthPixels() {
		return (tangibleBaseInches * getWindowWidth()) / displayWidthInches;
	}
}
