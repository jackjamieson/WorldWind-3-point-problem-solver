package worldwind3pp;

/**
 * 
 * @author jjamieso
 * Performs calculations for the haversine formula
 *
 */
public class Calculations {

	// //////////////////////////////////////////////////////////////////////////////////////
	// Calculation functions
	// Calculate the distance based on two points lat and long
	// p1 and p2 are arrays with [0] containing lat and [1] containing long
	
	
	public double calcDistance(double[] p1, double[] p2) {

		// haversine formula
		double R = 6371 * 1000; // km to m
		double x1 = toRadians(p1[0]);
		double x2 = toRadians(p2[0]);
		double dx = toRadians((p2[0] - p1[0]));
		double dy = toRadians((p2[1] - p1[1]));

		double a = Math.sin(dx / 2) * Math.sin(dx / 2) + Math.cos(x1)
				* Math.cos(x2) * Math.sin(dy / 2) * Math.sin(dy / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		double d = R * c;

		return d;// distance between 2 points

	}


	//Calculate the aziumuth between hm and hl
	public double calcBearing(double[] p1, double[] p2)
	{

		double x1 = toRadians(p1[0]);
		double y1 = toRadians(p1[1]);
		double x2 = toRadians(p2[0]);
		double y2 = toRadians(p2[1]);

		double y = Math.sin(y2-y1) * Math.cos(x2);
		double x = Math.cos(x1)*Math.sin(x2) -
			Math.sin(x1)*Math.cos(x2)*Math.cos(y2-y1);
		double brng = toDegrees(Math.atan2(y, x));

		return brng; //the azimuth aka bearing


	}

	//p1 is the high point, p2 is the lower point, dist is always from the lowest
	public double calcPlunge(double[] p1,double[] p2,double dist)
	{
		double plunge = Math.atan((p2[2] - p1[2])/dist);
		plunge = toDegrees(plunge);

		return plunge * -1;

	}
	
	//Takes in 2 points + plunge and calculate the strike and dip from them
	public StrikeDipQuad calcStrikeDip(double hmAz, double hlAz, double hmPl, double hlPl)
	{
		//Set up the variables for calculation
		double l1Alpha, l1Beta, l1Gamma, l2Alpha, l2Beta, l2Gamma, theta;
		double dAlpha, dBeta, dGamma, hAlpha, hBeta, hGamma;
		double poleAzimuth, polePlunge;
		double strike, dip, dipaz;
		String quad;

		//x,y,z linear 1
		l1Alpha = (Math.sin(toRadians(hmAz)) * Math.sin(toRadians(90-hmPl)));
		l1Beta = (Math.cos(toRadians(hmAz)) * Math.sin(toRadians(90-hmPl)));
		l1Gamma = Math.cos(toRadians(90-hmPl));

		//x,y,z linear 2
		l2Alpha = (Math.sin(toRadians(hlAz)) * Math.sin(toRadians(90-hlPl)));
		l2Beta = (Math.cos(toRadians(hlAz)) * Math.sin(toRadians(90-hlPl)));
		l2Gamma = Math.cos(toRadians(90-hlPl));

		//theta
		theta = Math.acos(l1Alpha * l2Alpha + l1Beta * l2Beta + l1Gamma * l2Gamma);


		//cross product 3d
		if(theta == 0)
		{
			dAlpha = 0;
			dBeta = 0;
			dGamma = 0;
		}
		else{
			dAlpha = ((l1Beta * l2Gamma - l1Gamma * l2Beta) / Math.sin(theta));
			dBeta = ((-1 * (l1Alpha * l2Gamma - l1Gamma * l2Alpha)) / Math.sin(theta));
			dGamma = ((l1Alpha * l2Beta - l1Beta * l2Alpha) / Math.sin(theta));
		}

		//lower hemi
		hAlpha = dAlpha * sign(dGamma);
		hBeta = dBeta * sign(dGamma);
		hGamma = dGamma * sign(dGamma);



		//calc pole azimuth
		if(hAlpha == 0 && hBeta == 0)
		{
			poleAzimuth = 0;
		}
		if(hAlpha < 0 && hBeta >= 0)
		{
			poleAzimuth = (450 - (toDegrees(Math.atan2(hBeta, hAlpha))));
		}
		else
		{
			poleAzimuth = (90 - (toDegrees(Math.atan2(hBeta, hAlpha))));

		}



		//calc pole plunge
		polePlunge = 90 - toDegrees(Math.acos(hGamma));



		//calc strike
		if(poleAzimuth >= 0 && poleAzimuth <= 90)
		{
			strike = 270 + poleAzimuth;	
		}
		else if(poleAzimuth > 90 && poleAzimuth <= 180)
		{
			strike = poleAzimuth - 90;
		}
		else if(poleAzimuth > 180 && poleAzimuth <= 270)
		{
			strike = poleAzimuth + 90;
		}
		else if(poleAzimuth > 270 && poleAzimuth <= 360)
		{
			strike = poleAzimuth - 270;	
		}
		else strike = 80085;



		//calc dip
		dip = 90 - polePlunge;



		//calc quad
		if(poleAzimuth >= 0 && poleAzimuth <= 90)
		{
			quad = "W";	
		}
		else if(poleAzimuth > 90 && poleAzimuth <=180)
		{
			quad = "W";
		}
		else if(poleAzimuth > 180 && poleAzimuth <= 270)
		{
			quad = "E";
		}
		else if(poleAzimuth > 270 && poleAzimuth <= 360)
		{
			quad = "E";
		}
		else quad = "ERROR";

		//calc dip azimuth
		//TODO: this is different than GE, it seemed off so I changed the apparent dip azimuth.  the real result is still shown.
		// in the output window and when exported to KML.
		if(poleAzimuth < 180)
		{
			dipaz = poleAzimuth + 180;
		}
		else dipaz = poleAzimuth - 180;

		StrikeDipQuad strikeDipQuad = new StrikeDipQuad(strike, dip, dipaz, quad);
		

		return strikeDipQuad;

	}
	 

	private double toRadians(double degrees) {

		return (degrees * Math.PI) / 180;

	}
	
	private double toDegrees(double radians) {
		return (radians * 180) / Math.PI;
	}
	
	private double sign(double x){
		return x > 0 ? 1 : x < 0 ? -1 : 0;
	}

}
