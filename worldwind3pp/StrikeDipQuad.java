package worldwind3pp;

/**
 * 
 * @author jjamieso
 * Object to hold the result of the calculation
 */
public class StrikeDipQuad {
	
	private double strike, dip, dipAzimuth;
	private String quad;
	
	public StrikeDipQuad(double strike, double dip, double dipAzimuth, String quad){
		
		this.strike = strike;
		this.dip = dip;
		this.dipAzimuth = dipAzimuth;
		this.quad = quad;
		
	}

	public double getStrike() {
		return strike;
	}

	public void setStrike(double strike) {
		this.strike = strike;
	}

	public double getDip() {
		return dip;
	}

	public void setDip(double dip) {
		this.dip = dip;
	}

	public double getDipAzimuth() {
		return dipAzimuth;
	}

	public void setDipAzimuth(double dipAzimuth) {
		this.dipAzimuth = dipAzimuth;
	}

	public String getQuad() {
		return quad;
	}

	public void setQuad(String quad) {
		this.quad = quad;
	}
	
	public String toString(){
		
		return "Strike: " + strike + "\nDip: " + dip + "\nQuad: " + quad + "\nDip-Azimuth: " + dipAzimuth;
		
	}

}
