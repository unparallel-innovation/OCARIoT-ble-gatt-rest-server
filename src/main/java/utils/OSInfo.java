package utils;

public class OSInfo {
	
	private String _osName;
	
	public static final int LINUX = 0;
	public static final int WINDOWS = 1;
	public static final int MACOS = 2;
	
	/**
	 * 
	 */
	public OSInfo() {
		_osName = System.getProperty("os.name").toLowerCase();
	}
	
	public int getOsType() {
		if(_osName.contains("nix") || _osName.contains("nux") || _osName.contains("aix")) {
			return LINUX;
		} else if(_osName.contains("windows")) {
			return WINDOWS;
		} else if(_osName.contains("mac os") || _osName.contains("macos") || _osName.contains("darwin")) {
			return MACOS;
		} else {
			return -1;
		}
	}
}
