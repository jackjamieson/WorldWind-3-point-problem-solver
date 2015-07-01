/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package worldwind3pp;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ogc.collada.ColladaRoot;
import gov.nasa.worldwind.ogc.collada.impl.ColladaController;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.util.Logging;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


public class Globe extends ApplicationTemplate
{
    private final WorldWindow wwd;
    
    private boolean armed = false;// when the new button is clicked the plotting is 'armed'
    
    private ArrayList<Position> positions = new ArrayList<Position>();
    private boolean active = false;

    private final RenderableLayer layer;
    private final Polyline line;
    //private boolean active = false;
    
    private int numPoints = 1;// the current point number p1,p2,p3
    
    private ArrayList<RenderableLayer> pointsLayers = new ArrayList<RenderableLayer>();
    
    // results of the 3pp solver to return to the gui
    private double strike, dip, dipaz;
    private String quad;
    
    // store the points in arrays
    private double[] point1 = new double[3];
    private double[] point2 = new double[3];
    private double[] point3 = new double[3];
    
    // arraylist to hold the placemarks for exporting
    private ArrayList<PointPlacemark> placemarks = new ArrayList<PointPlacemark>(); 
    
    // arraylist to hold the models for exporting
    private ArrayList<ColladaRoot> models = new ArrayList<ColladaRoot>();

    
    private Position point2Pos;
    
    // TODO: if you want to change the location of the .dae files change it here
    private String url = "https://dl.dropboxusercontent.com/u/89445333/GEsymbols/";

    
    private String shape = url + "5-m-Wcircle.dae";// the default shape
    
    private boolean is3D = false;// 3d and 2d shapes get put on the map differently
    
    private boolean scopeIsThirdPoint = false;

   
    

    public Globe(final WorldWindow wwd, RenderableLayer lineLayer, Polyline polyline) 
    {
        this.wwd = wwd;
        
        if (polyline != null)
        {
            line = polyline;
        }
        else
        {
            this.line = new Polyline();
            this.line.setFollowTerrain(true);
        }
        this.layer = lineLayer != null ? lineLayer : new RenderableLayer();
        //this.layer.addRenderable(this.line);
        this.wwd.getModel().getLayers().add(this.layer);

        this.wwd.getInputHandler().addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent mouseEvent)
            {
                if (armed && mouseEvent.getButton() == MouseEvent.BUTTON1)
                {
                    if (armed && (mouseEvent.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0)
                    {
                        if (!mouseEvent.isControlDown())
                        {
                            active = true;
                            addPosition();
                        }
                    }
                    mouseEvent.consume();
                }
            }
            
            public void mouseReleased(MouseEvent mouseEvent)
            {
                if (armed && mouseEvent.getButton() == MouseEvent.BUTTON1)
                {
                    if (positions.size() == 1)
                        removePosition();
                    active = false;
                    mouseEvent.consume();
                }
            }

            public void mouseClicked(MouseEvent mouseEvent)
            {
                if (armed && mouseEvent.getButton() == MouseEvent.BUTTON1)
                {
                    if (mouseEvent.isControlDown())
                        removePosition();
                    mouseEvent.consume();
                }
            }

            
        });


/*
        this.wwd.addPositionListener(new PositionListener()
        {
            public void moved(PositionEvent event)
            {
                if (!active)
                    return;

                if (positions.size() == 1)
                    addPosition();
                else
                    replacePosition();
            }
        });*/
    }

    /**
     * Returns the layer holding the polyline being created.
     *
     * @return the layer containing the polyline.
     */
    public RenderableLayer getLayer()
    {
        return this.layer;
    }

    /**
     * Returns the layer currently used to display the polyline.
     *
     * @return the layer holding the polyline.
     */
    public Polyline getLine()
    {
        return this.line;
    }

    /**
     * Removes all positions from the polyline.
     */
/*    public void clear()
    {
        while (this.positions.size() > 0)
            this.removePosition();
    }*/

    /**
     * Identifies whether the line builder is armed.
     *
     * @return true if armed, false if not armed.
     */
    public boolean isArmed()
    {
        return this.armed;
    }

    /**
     * Arms and disarms the line builder. When armed, the line builder monitors user input and builds the polyline in
     * response to the actions mentioned in the overview above. When disarmed, the line builder ignores all user input.
     *
     * @param armed true to arm the line builder, false to disarm it.
     */
    public void setArmed(boolean armed)
    {
        this.armed = armed;
    }

    /**
     * Takes in the positions when you click on the map.
     * Determiens which point it is and what to do with it.
     * Does the haversine calculations using Calculations.java
     * 
     */
    private void addPosition()
    {
        Position curPos = this.wwd.getCurrentPosition();
        
        System.out.println(curPos);// put out the lat/long/alt for debugging purposes
        
        
        double pySort[] = new double[3];// used when sorting the high, med, low points
        
        
        switch(numPoints){
        
        case 1:
        	// reset the arrays
        	point1 = new double[3];
            point2 = new double[3];
            point3 = new double[3];
        	
        	point1[0] = curPos.latitude.degrees;
        	point1[1] = curPos.longitude.degrees;
        	point1[2] = curPos.elevation;
        	
        	plotPoint(curPos, false, curPos, null, 0.0);
        	break;
        case 2:
        	point2[0] = curPos.latitude.degrees;
        	point2[1] = curPos.longitude.degrees;
        	point2[2] = curPos.elevation;
        	
        	point2Pos = curPos;
        	
        	plotPoint(curPos, false, point2Pos, null, 0.0);
        	break;
        case 3:
        	point3[0] = curPos.latitude.degrees;
        	point3[1] = curPos.longitude.degrees;
        	point3[2] = curPos.elevation;
        	
        	pySort[0]  = point1[2];
        	pySort[1]  = point2[2];
        	pySort[2]  = point3[2];
        	
        	Arrays.sort(pySort);// sort the elevation values
        	
        	double l = pySort[0];
        	double m = pySort[1];
        	double h = pySort[2];
        	
        	
        	Calculations calc = new Calculations();// create calc object to do haversine math
        	
        	double hmAzimuth = 0, hlAzimuth = 0, hmDist, hlDist = 0, hmPlunge = 0, hlPlunge = 0;
        	
        	//Logic to see which point is the high, medium, low
			if(point1[2] == h && point2[2] == m && point3[2] == l)
			{
				hmAzimuth = calc.calcBearing(point1, point2);
				hlAzimuth = calc.calcBearing(point1, point3);

				hmDist = calc.calcDistance(point1, point2);
				hlDist = calc.calcDistance(point1, point3);

				hmPlunge = calc.calcPlunge(point1, point2, hmDist);
				hlPlunge = calc.calcPlunge(point1, point3, hlDist);
			}
			else if(point1[2] == h && point2[2] == l && point3[2] == m)
			{
				hmAzimuth = calc.calcBearing(point1, point3);
				hlAzimuth = calc.calcBearing(point1, point2);

				hmDist = calc.calcDistance(point1, point3);
				hlDist = calc.calcDistance(point1, point2);

				hmPlunge = calc.calcPlunge(point1, point3, hmDist);
				hlPlunge = calc.calcPlunge(point1, point2, hlDist);
			}
			else if(point1[2] == m && point2[2] == h && point3[2] == l)
			{
				hmAzimuth = calc.calcBearing(point2, point1);
				hlAzimuth = calc.calcBearing(point2, point3);

				hmDist = calc.calcDistance(point2, point1);
				hlDist = calc.calcDistance(point2, point3);

				hmPlunge = calc.calcPlunge(point2, point1, hmDist);
				hlPlunge = calc.calcPlunge(point2, point3, hlDist);

			}
			else if(point1[2] == l && point2[2] == h && point3[2] == m)
			{
				hmAzimuth = calc.calcBearing(point2, point3);
				hlAzimuth = calc.calcBearing(point2, point1);

				hmDist = calc.calcDistance(point2, point3);
				hlDist = calc.calcDistance(point2, point1);

				hmPlunge = calc.calcPlunge(point2, point3, hmDist);
				hlPlunge = calc.calcPlunge(point2, point1, hlDist);
			}
			else if(point1[2] == m && point2[2] == l && point3[2] == h)
			{
				hmAzimuth = calc.calcBearing(point3, point1);
				hlAzimuth = calc.calcBearing(point3, point2);

				hmDist = calc.calcDistance(point3, point1);
				hlDist = calc.calcDistance(point3, point2);

				hmPlunge = calc.calcPlunge(point3, point1, hmDist);
				hlPlunge = calc.calcPlunge(point3, point2, hlDist);
			}
			else if(point1[2] == l && point2[2] == m && point3[2] == h)
			{
				hmAzimuth = calc.calcBearing(point3, point2);
				hlAzimuth = calc.calcBearing(point3, point1);

				hmDist = calc.calcDistance(point3, point2);
				hlDist = calc.calcDistance(point3, point1);

				hmPlunge = calc.calcPlunge(point3, point2, hmDist);
				hlPlunge = calc.calcPlunge(point3, point1, hlDist);
			}
        	
        	
        	StrikeDipQuad results = calc.calcStrikeDip(hmAzimuth, hlAzimuth, hmPlunge, hlPlunge);
        	
        	plotPoint(curPos, true, point2Pos, results, hlDist/10);// plot the point on the globe
        	// hlDist/10 is arbitrary but the size seems good, if you want to change the default size of the objects change the 10
        	
        	this.dip = results.getDip();
        	this.dipaz = results.getDipAzimuth();
        	this.strike = results.getStrike();
        	this.quad = results.getQuad();

        	break;
        
        }
        
        if (curPos == null)
            return;

        this.positions.add(curPos);
        this.line.setPositions(this.positions);
        this.wwd.redraw();
        
        numPoints++;
        
        if(numPoints > 3)
        	numPoints = 1;
        
    }
    
    /**
     * 
     * @param curPos
     * @param isThirdPoint
     * @param point2
     * @param results
     * @param hldist
     * Plots a point on the globe.
     */
    private void plotPoint(Position curPos, boolean isThirdPoint, Position point2, StrikeDipQuad results, double hldist){
    	
    	
    	PointPlacemark pmStandard = new PointPlacemark(curPos);// the placemark
        
        pmStandard.setLabelText("P" + Integer.toString(numPoints));
        
        pmStandard.setLineEnabled(false);
        
        // important must be clamped to ground
        pmStandard.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        
        // connection to the dae file
        ColladaRoot r = null;
        ColladaController colladaController = null;
        
        // third point is special because it plots the symbols
        if(isThirdPoint){
	        try {

				r = ColladaRoot.createAndParse(shape);// shape is the URL for the dae file
				r.setPosition(point2);// put the shape at the second point (arbitrary)
				
				r.setField("url", shape);// save the URL field so we can export it later
				
				
				// 3d symbols seem to be off for heading but export correctly.  this makes it correct in World wind
				double newHeading = 0;
				if(results.getDipAzimuth()-180 < 0){
					newHeading = results.getDipAzimuth()+180;
				}
				else newHeading = results.getDipAzimuth()-180;
				
				r.setHeading(Angle.fromDegrees(newHeading));
				
				if(is3D)
				{
					r.setPitch(Angle.fromDegrees(results.getDip()));
				}
				else 
				{
					r.setPitch(Angle.fromDegrees(0));
					
					
					// the heading for 2d symbols was off by 180, this fixes it.
					r.setHeading(Angle.fromDegrees(results.getDipAzimuth()-180)); 
					r.setField("2d", "yes");

				}

				r.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
				r.setModelScale(new Vec4(hldist));
				
				colladaController = new ColladaController(r);
				
				// make a new layer for the model to be added
				RenderableLayer layer2 = new RenderableLayer();
		        layer2.addRenderable(colladaController);
		        
		        insertBeforeCompass(this.wwd, layer2);// add the model layer to the globe
		        
		        // this is the point that holds the 00/000 dip values
		        PointPlacemark pmValues = new PointPlacemark(point2);
		        
		        pmValues.setLabelText("" + Math.round(results.getDip()) + "/" + Math.round(results.getDipAzimuth()));
		        
		        // create a new layer for the dip values point
		        RenderableLayer pointsLayer = new RenderableLayer();

	            pointsLayer.addRenderable(pmValues);
	            pointsLayer.addRenderable(pmStandard);

	            insertBeforeCompass(this.wwd, pointsLayer);
	            
	            pointsLayers.add(pointsLayer);
	            pointsLayers.add(layer2);

	            
	        	placemarks.add(pmStandard);
	        	placemarks.add(pmValues);
	        	models.add(r);
	        	scopeIsThirdPoint = true;

	            
			} catch (IOException | XMLStreamException e) {
				e.printStackTrace();
			}
        }
        else {
        	scopeIsThirdPoint = false;

        	
        	// must add the placemark to the list before we add the attributes, the exporter does not like them
        	placemarks.add(pmStandard);
        	
            RenderableLayer pointsLayer = new RenderableLayer();
            pointsLayers.add(pointsLayer);

            pointsLayer.addRenderable(pmStandard);
            insertBeforeCompass(this.wwd, pointsLayer);
           
        }
        
    }

    private void replacePosition()
    {
        Position curPos = this.wwd.getCurrentPosition();
        if (curPos == null)
            return;

        int index = this.positions.size() - 1;
        if (index < 0)
            index = 0;

        //Position currentLastPosition = this.positions.get(index);
        this.positions.set(index, curPos);
        this.line.setPositions(this.positions);
        //this.firePropertyChange("LineBuilder.ReplacePosition", currentLastPosition, curPos);
        this.wwd.redraw();
    }

    private void removePosition()
    {
        if (this.positions.size() == 0)
            return;

        //Position currentLastPosition = this.positions.get(this.positions.size() - 1);
        this.positions.remove(this.positions.size() - 1);
        this.line.setPositions(this.positions);
       // this.firePropertyChange("LineBuilder.RemovePosition", currentLastPosition, null);
        this.wwd.redraw();
    }

    // ===================== Control Panel ======================= //
    // The following code is an example program illustrating LineBuilder usage. It is not required by the
    // LineBuilder class, itself.

    
    public void clearPoints(){
    	
    	for(RenderableLayer r : pointsLayers)
    	{
    		
    		r.removeAllRenderables();
        	r.dispose();
        	placemarks.clear();
        	models.clear();
    	}
    	
    	numPoints = 1;
    	
    	point1 = new double[3];
        point2 = new double[3];
        point3 = new double[3];
    	
    }
    
    public void clearLastPoint(){
    	
    	// this is actually when the plotting is done for the third point
    	if(numPoints == 1) {
    		
    		RenderableLayer r = pointsLayers.get(pointsLayers.size()-1);
	    	pointsLayers.remove(pointsLayers.size()-1);
	    	
	    	r.dispose();

	    	
	    	RenderableLayer r2 = pointsLayers.get(pointsLayers.size()-1);
	    	pointsLayers.remove(pointsLayers.size()-1);
	    	
	    	r2.dispose();
	    	
	    	numPoints = 3;
	    	
	    	placemarks.remove(placemarks.size()-1);
	    	models.remove(models.size()-1);

	    	
	    	this.wwd.redraw();
    	}
    	
    	/*else if(numPoints == 2)
    	{
    		RenderableLayer r = pointsLayers.get(pointsLayers.size()-1);
	    	pointsLayers.remove(pointsLayers.size()-1);
	    	
	    	r.dispose();

	    	placemarks.remove(placemarks.size()-1);
	    	
	    	numPoints = 1;
	    	
	    	point1 = new double[3];
	        point2 = new double[3];
	        point3 = new double[3];
	    	
    	}*/
    	
    	
    	// when the 1st and 2nd points are plotted
    	else if(numPoints == 3){
			RenderableLayer r = pointsLayers.get(pointsLayers.size()-1);
	    	pointsLayers.remove(pointsLayers.size()-1);
	    	
	    	r.dispose();

	    	placemarks.remove(placemarks.size()-1);
	    	placemarks.remove(placemarks.size()-1);


	    	
	      
	    	if(numPoints - 1 > 0)
	    		numPoints--;
	    	
	    	
	    	
	    	this.wwd.redraw();
    	}
    	
    	
    	
    }
    
    public Position getPoint2(){
    	return point2Pos;
    }
    
    
	public void exportKML(String path) {
		try{
	    	
		    // Create a StringWriter to collect KML in a string buffer
           // Writer stringWriter = new StringWriter();

            // Create a document builder that will write KML to the StringWriter
           // KMLDocumentBuilder kmlBuilder = new KMLDocumentBuilder(stringWriter);

            String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
								"<kml xmlns=\"http://www.opengis.net/kml/2.2\">" +
								"<Document>" +
									"<name>3PP Geo Symbols</name>" +
									"<Folder>" +
										"<name>Annotations and Models</name>";

         // Export the placemarks
            for(PointPlacemark p : placemarks){
            	
            	String placemarkString = "<Placemark>";
            	
            	placemarkString += "<name>" + p.getLabelText() + "</name>";
            	placemarkString += "<Point>" + 
            		"<coordinates>" + p.getPosition().getLongitude().getDegrees() + "," + p.getPosition().getLatitude().getDegrees() + "," + 0 + "</coordinates>";
    			placemarkString += "</Point>";
    			placemarkString += "</Placemark>";
    			
    			xmlString += placemarkString;
                        
            }
            
            // get all collada files and export them
            for(ColladaRoot m : models){
            	
            	String modelString = "<Placemark>";
            	
            	modelString += "<name>Symbol</name>";
            	modelString += "<Model id=\"model_1\">";
            	
            	double newHeading = 0;
            	if(!m.hasField("2d")){
					if(m.getHeading().getDegrees()+180 < 360){
						newHeading = m.getHeading().getDegrees()+180;
					}
					else newHeading = m.getHeading().getDegrees()-180;
            	}
            	else newHeading = m.getHeading().getDegrees();
				
            	modelString += "<Location>" + 
            					"<longitude>" + m.getPosition().getLongitude().getDegrees() + "</longitude>" +
            					"<latitude>" + m.getPosition().getLatitude().getDegrees() + "</latitude>" +
            					"<altitude>0</altitude>";
            	modelString += "</Location>";
            	modelString += "<Orientation>";
            	modelString +=	"<heading>" + newHeading + "</heading>";
            	modelString +=	"<tilt>" + m.getPitch().getDegrees() + "</tilt>";
            	modelString +=	"<roll>0</roll>";
            	modelString +=	"</Orientation>";
            	modelString +=	"<Scale>";
            	modelString +=		"<x>" + m.getModelScale().x + "</x>";
            	modelString +=		"<y>" + m.getModelScale().y + "</y>";
            	modelString +=		"<z>1</z>";
            	modelString +=	"</Scale>";
            	modelString +=	"<Link>";
            	modelString +=		"<href>" + m.getField("url") + "</href>";
    			modelString +=	"</Link>";
            	modelString +=	"<ResourceMap>";
            	modelString +=	"</ResourceMap>";
            	modelString +=	"</Model>";
            	
            	modelString += "</Placemark>";
            	
            	xmlString += modelString;
            }
            
            
            xmlString += "</Folder>" +
				         "</Document>" +
				         "</kml>";
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

           // String results = "";
            
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new StreamSource(new StringReader(xmlString)), new StreamResult(stringWriter));
            
            
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "utf-8"))) {
            	writer.write(stringWriter.toString());
            }
            catch (Exception e){
            	System.err.println("Writing error.");
      }
            
		} catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionAttemptingToWriteXml", e.toString());
            Logging.logger().severe(message);
            e.printStackTrace();
        }
    }

    /**
     * Marked as deprecated to keep it out of the javadoc.
     *
     * @deprecated
     */
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = 937100948174364269L;

		public AppFrame()
        {
            super(true, false, false);

           // Globe lineBuilder = new Globe(this.getWwd(), null, null);
            //this.getContentPane().add(new LinePanel(this.getWwd(), lineBuilder), BorderLayout.WEST);
            
        }
    }

    /**
     * Marked as deprecated to keep it out of the javadoc.
     *
     * @param args the arguments passed to the program.
     * @deprecated
     */
    public static void main(String[] args)
    {
        //noinspection deprecation
        ApplicationTemplate.start("World Wind Line Builder", Globe.AppFrame.class);
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

	public double getDipaz() {
		return dipaz;
	}

	public void setDipaz(double dipaz) {
		this.dipaz = dipaz;
	}

	public String getQuad() {
		return quad;
	}

	public void setQuad(String quad) {
		this.quad = quad;
	}
	
	public void setShape(String shape) {
		
		
		if(shape.equals("White Circle (3D)"))
		{
			this.shape = url + "5-m-Wcircle.dae";
			is3D = true;
		}
		else if(shape.equals("Blue Circle (3D)"))
		{
			this.shape = url + "5-m-Bcircle.dae";
			is3D = true;

		}
		else if(shape.equals("Pink Circle (3D)"))
		{
			this.shape = url + "5-m-Pcircle.dae";
			is3D = true;

		}
		else if(shape.equals("Orange Circle (3D)"))
		{
			this.shape = url + "5-m-Ocircle.dae";
			is3D = true;

		}
		else if(shape.equals("Light Blue Circle (3D)"))
		{
			this.shape = url + "5-m-LBcircle.dae";
			is3D = true;

		}
		else if(shape.equals("Green Circle (3D)"))
		{
			this.shape = url + "5-m-Gcircle.dae";
			is3D = true;

		}
		else if(shape.equals("Black Circle (3D)"))
		{
			this.shape = url + "5-m-BLcircle.dae";
			is3D = true;

		}
		else if(shape.equals("Arrow"))
		{
			this.shape = url + "arrow.dae";
			is3D = false;

		}
		else if(shape.equals("Bed"))
		{
			this.shape = url + "bed.dae";
			is3D = false;

		}
		else if(shape.equals("Cleavage"))
		{
			this.shape = url + "cleavage.dae";
			is3D = false;

		}
		else if(shape.equals("Joint"))
		{
			this.shape = url + "joint.dae";
			is3D = false;

		}
		else if(shape.equals("Layer"))
		{
			this.shape = url + "layer.dae";
			is3D = false;

		}
		else if(shape.equals("Slip"))
		{
			this.shape = url + "slip.dae";
			is3D = false;

		}
		else if(shape.equals("Fault"))
		{
			this.shape = url + "fault.dae";
			is3D = false;

		}
		else if(shape.equals("Strike-Dip"))
		{
			this.shape = url + "Strike-dip.dae";
			is3D = false;

		}
		else if(shape.equals("Bed White"))
		{
			this.shape = url + "bed-white.dae";
			is3D = false;

		}
		else if(shape.equals("Cleavage White"))
		{
			this.shape = url + "cleavage-white.dae";
			is3D = false;

		}
		else if(shape.equals("Joint White"))
		{
			this.shape = url + "joint-white.dae";
			is3D = false;

		}
		else if(shape.equals("Layer White"))
		{
			this.shape = url + "layer-white.dae";
			is3D = false;

		}
		else if(shape.equals("Fault White"))
		{
			this.shape = url + "fault-white.dae";
			is3D = false;

		}
		
		
	}
	
	public boolean getIsThirdPoint(){
		return scopeIsThirdPoint;
	}
}
