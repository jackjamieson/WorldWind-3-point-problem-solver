/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package worldwind3pp;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ogc.kml.KMLAbstractFeature;
import gov.nasa.worldwind.ogc.kml.KMLRoot;
import gov.nasa.worldwind.ogc.kml.impl.KMLController;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.retrieve.RetrievalService;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.layertree.KMLLayerTreeNode;
import gov.nasa.worldwind.util.layertree.KMLNetworkLinkTreeNode;
import gov.nasa.worldwind.util.layertree.LayerTree;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import gov.nasa.worldwindx.examples.kml.KMLApplicationController;
import gov.nasa.worldwindx.examples.util.BalloonController;
import gov.nasa.worldwindx.examples.util.HotSpotController;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.stream.XMLStreamException;

/**
 * Built from an example application that allows the user to import a KML or KMZ file as a layer. The contents of the file are
 * displayed in a feature tree. Click on KML features in the tree to navigate the view to the feature. Clicking on
 * features on the globe will open an info balloon for the feature, if the feature provides a description. Use the File
 * menu to open a document from a local file or from a URL.
 *
 * @author tag
 * @version $Id: KMLViewer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class KMLViewer extends ApplicationTemplate
{
	
	
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = 421169486087652615L;
		protected LayerTree layerTree;
        protected RenderableLayer hiddenLayer;

        protected HotSpotController hotSpotController;
        protected KMLApplicationController kmlAppController;
        protected BalloonController balloonController;
        
        

        public AppFrame()
        {
            super(true, false, false); // Don't include the layer panel; we're using the on-screen layer tree.
            
            Globe lineBuilder = new Globe(this.getWwd(), null, null);
            this.getContentPane().add(new LinePanel(this.getWwd(), lineBuilder), BorderLayout.WEST);

            
            // Add the on-screen layer tree, refreshing model with the WorldWindow's current layer list. We
            // intentionally refresh the tree's model before adding the layer that contains the tree itself. This
            // prevents the tree's layer from being displayed in the tree itself.
            this.layerTree = new LayerTree(new Offset(20d, 160d, AVKey.PIXELS, AVKey.INSET_PIXELS));
            
            // default set the bing layer to ON so that we can zoom in much farther with detail
            this.getWwd().getModel().getLayers().getLayerByName("Bing Imagery").setEnabled(true);
            

          

            
            this.layerTree.getModel().refresh(this.getWwd().getModel().getLayers());
            

            // Set up a layer to display the on-screen layer tree in the WorldWindow. This layer is not displayed in
            // the layer tree's model. Doing so would enable the user to hide the layer tree display with no way of
            // bringing it back.
            this.hiddenLayer = new RenderableLayer();
            this.hiddenLayer.addRenderable(this.layerTree);
            this.getWwd().getModel().getLayers().add(this.hiddenLayer);

            // Add a controller to handle input events on the layer selector and on browser balloons.
            this.hotSpotController = new HotSpotController(this.getWwd());

            // Add a controller to handle common KML application events.
            this.kmlAppController = new KMLApplicationController(this.getWwd());

            // Add a controller to display balloons when placemarks are clicked. We override the method addDocumentLayer
            // so that loading a KML document by clicking a KML balloon link displays an entry in the on-screen layer
            // tree.
            this.balloonController = new BalloonController(this.getWwd())
            {
                @Override
                protected void addDocumentLayer(KMLRoot document)
                {
                    addKMLLayer(document);
                }
            };

            // Give the KML app controller a reference to the BalloonController so that the app controller can open
            // KML feature balloons when feature's are selected in the on-screen layer tree.
            this.kmlAppController.setBalloonController(balloonController);

            // Size the World Window to maximized
            this.setExtendedState(MAXIMIZED_BOTH);
            this.pack();
            WWUtil.alignComponent(null, this, AVKey.CENTER);

            makeMenu(this);

            // Set up to receive SSLHandshakeExceptions that occur during resource retrieval.
            WorldWind.getRetrievalService().setSSLExceptionListener(new RetrievalService.SSLExceptionListener()
            {
                public void onException(Throwable e, String path)
                {
                    System.out.println(path);
                    System.out.println(e);
                }
            });
        }

        /**
         * Adds the specified <code>kmlRoot</code> to this app frame's <code>WorldWindow</code> as a new
         * <code>Layer</code>, and adds a new <code>KMLLayerTreeNode</code> for the <code>kmlRoot</code> to this app
         * frame's on-screen layer tree.
         * <p/>
         * This expects the <code>kmlRoot</code>'s <code>AVKey.DISPLAY_NAME</code> field to contain a display name
         * suitable for use as a layer name.
         *
         * @param kmlRoot the KMLRoot to add a new layer for.
         */
        protected void addKMLLayer(KMLRoot kmlRoot)
        {
            // Create a KMLController to adapt the KMLRoot to the World Wind renderable interface.
            KMLController kmlController = new KMLController(kmlRoot);

            // Adds a new layer containing the KMLRoot to the end of the WorldWindow's layer list. This
            // retrieves the layer name from the KMLRoot's DISPLAY_NAME field.
            RenderableLayer layer = new RenderableLayer();
            layer.setName((String) kmlRoot.getField(AVKey.DISPLAY_NAME));
            layer.addRenderable(kmlController);
            this.getWwd().getModel().getLayers().add(layer);

            // Adds a new layer tree node for the KMLRoot to the on-screen layer tree, and makes the new node visible
            // in the tree. This also expands any tree paths that represent open KML containers or open KML network
            // links.
            KMLLayerTreeNode layerNode = new KMLLayerTreeNode(layer, kmlRoot);
            this.layerTree.getModel().addLayer(layerNode);
            this.layerTree.makeVisible(layerNode.getPath());
            layerNode.expandOpenContainers(this.layerTree);
            

            // Listens to refresh property change events from KML network link nodes. Upon receiving such an event this
            // expands any tree paths that represent open KML containers. When a KML network link refreshes, its tree
            // node replaces its children with new nodes created from the refreshed content, then sends a refresh
            // property change event through the layer tree. By expanding open containers after a network link refresh,
            // we ensure that the network link tree view appearance is consistent with the KML specification.
            layerNode.addPropertyChangeListener(AVKey.RETRIEVAL_STATE_SUCCESSFUL, new PropertyChangeListener()
            {
                public void propertyChange(final PropertyChangeEvent event)
                {
                    if (event.getSource() instanceof KMLNetworkLinkTreeNode)
                    {
                        // Manipulate the tree on the EDT.
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                ((KMLNetworkLinkTreeNode) event.getSource()).expandOpenContainers(layerTree);
                                getWwd().redraw();
                            }
                        });
                    }
                }
            });
        }
    }
    
    // ===================== Control Panel ======================= //
    // The following code is an example program illustrating LineBuilder usage. It is not required by the
    // LineBuilder class, itself.

    private static class LinePanel extends JPanel
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = -1863858957421121530L;
		private final WorldWindow wwd;
        private final Globe lineBuilder;
        private JButton newButton;
        private JButton pauseButton;
        private JButton endButton;
        private JLabel[] pointLabels;
        private JTextField latInputField = new JTextField ("40.0619");
        
        private JTextField lonInputField = new JTextField ("-74.5448");
        private JButton goNavButton;

        
        private JButton goToButton;
        private JButton exportKMLButton;
        private JButton clearPoints;
        private JButton clearLastPoint;

        
        String[] choices = { "White Circle (3D)","Blue Circle (3D)","Pink Circle (3D)","Orange Circle (3D)","Light Blue Circle (3D)","Green Circle (3D)","Black Circle (3D)", "Arrow","Bed","Cleavage","Joint","Layer","Slip","Fault","Bed White","Cleavage White","Joint White","Layer White","Fault White"};

        private JComboBox<String> cb = new JComboBox<String>(choices);

        

        
        JPanel topPanel = new JPanel(new GridBagLayout());
    	GridBagConstraints c = new GridBagConstraints();
        JPanel buttonPanel = new JPanel(new GridLayout(1, 1, 5, 0));
        JPanel lowerPanel = new JPanel(new GridLayout(1,1));
        JPanel navPanel = new JPanel(new GridLayout(1, 3, 2, 0));
        JPanel lowerButtPanel = new JPanel(new GridLayout(7,1, 0, 5));
        
        JLabel label2 = new JLabel("<html><b>3 Point Solution:</b><br>Strike:"
        		+ "<br>Dip:<br>Quad:<br>Dip-Azimuth:</html>");
    


        public LinePanel(WorldWindow wwd, final Globe lineBuilder)
        {
            super(new BorderLayout());
            this.wwd = wwd;
            this.lineBuilder = lineBuilder;
            this.makePanel(new Dimension(200, 400));
            
            // add the same mouse listener but use the globe methods to interact
            wwd.getInputHandler().addMouseListener(new MouseAdapter()
            {
                
                public void mouseReleased(MouseEvent mouseEvent)
                {
                    if (mouseEvent.getButton() == MouseEvent.BUTTON1)
                    {
                    	if(lineBuilder.getQuad() != null){
                    		label2.setText("<html><b>3 Point Solution:</b><br>Strike: " + lineBuilder.getStrike()
                            		+ "<br>Dip: " + lineBuilder.getDip() + "<br>Quad: " + lineBuilder.getQuad()
                            		+ "<br>Dip-Azimuth: " + lineBuilder.getDipaz() + "</html>");
                        	
                    		lineBuilder.setShape(cb.getSelectedItem().toString());
                    		
                    	}
                    }
                }
            });
        }

        private void makePanel(Dimension size)
        {
        	            
        	c.fill = GridBagConstraints.HORIZONTAL;
        	c.ipady = 10;
        	c.ipadx = 10;
            
            topPanel.add(buttonPanel, c);
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            c.gridy = 1;
            c.gridx = 0;
            
            topPanel.add(lowerButtPanel, c);
            lowerButtPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            c.gridy = 2;
            c.gridx = 0;
            topPanel.add(navPanel, c);
            navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
            
            c.gridy = 3;
            c.gridx = 0;
            topPanel.add(lowerPanel, c);
            lowerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            navPanel.add(latInputField);
            navPanel.add(lonInputField);
            goNavButton = new JButton("Go");
            goNavButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
					wwd.getView().goTo(Position.fromDegrees(Double.parseDouble(latInputField.getText()), Double.parseDouble(lonInputField.getText()), 200), 2000);

                }
            });
            navPanel.add(goNavButton);
         
            newButton = new JButton("New");
            newButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    //lineBuilder.clear();
                    lineBuilder.setArmed(true);
                    pauseButton.setText("Pause");
                    pauseButton.setEnabled(true);
                    endButton.setEnabled(true);
                    newButton.setEnabled(false);
                    ((Component) wwd).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                }
            });
            buttonPanel.add(newButton);
            newButton.setEnabled(true);

            pauseButton = new JButton("Pause");
            pauseButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    lineBuilder.setArmed(!lineBuilder.isArmed());
                    pauseButton.setText(!lineBuilder.isArmed() ? "Resume" : "Pause");
                    ((Component) wwd).setCursor(Cursor.getDefaultCursor());
                }
            });
            buttonPanel.add(pauseButton);
            pauseButton.setEnabled(false);
            
            goToButton = new JButton("Go to Delaware Water Gap");
            goToButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					
					wwd.getView().goTo(Position.fromDegrees(40.96741485595703, -75.12245178222656, 200), 1500);
					
				}
			});
            lowerButtPanel.add(goToButton);
            
            exportKMLButton = new JButton("Export KML");
            exportKMLButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setSelectedFile(new File("3ppGeoSymbols.kml"));
		            if (fileChooser.showSaveDialog(LinePanel.this) == JFileChooser.APPROVE_OPTION) {
		              File file = fileChooser.getSelectedFile();
		              lineBuilder.exportKML(file.getAbsolutePath());

		            }
					
				}
			});
            lowerButtPanel.add(exportKMLButton);
            
            
            clearPoints = new JButton("Clear All Points");
            clearPoints.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					
					
					lineBuilder.clearPoints();
					
				}
			});
            lowerButtPanel.add(clearPoints);
            
            clearLastPoint = new JButton("Clear Last Point");
            clearLastPoint.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					
					
					lineBuilder.clearLastPoint();
					
				}
			});
            lowerButtPanel.add(clearLastPoint);
            
            lowerButtPanel.add(new JLabel("Geologic symbol to plot: "));
            // the drop down menu
            lowerButtPanel.add(cb);
            lowerButtPanel.add(new JLabel("Fly to (Latitude, Longitude): "));

            
            // whenever the combo box is changed update it on the Globe
            cb.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent arg0) {
            		lineBuilder.setShape(cb.getSelectedItem().toString());
                }
            });


            endButton = new JButton("End");
            endButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    lineBuilder.setArmed(false);
                    newButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    pauseButton.setText("Pause");
                    endButton.setEnabled(false);
                    ((Component) wwd).setCursor(Cursor.getDefaultCursor());
                }
            });
            buttonPanel.add(endButton);
            endButton.setEnabled(false);

            JPanel pointPanel = new JPanel(new GridLayout(0, 1, 0, 10));
            pointPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            this.pointLabels = new JLabel[20];
            for (int i = 0; i < this.pointLabels.length; i++)
            {
                this.pointLabels[i] = new JLabel("");
                pointPanel.add(this.pointLabels[i]);
            }

            // Put the point panel in a container to prevent scroll panel from stretching the vertical spacing.
            JPanel dummyPanel = new JPanel(new BorderLayout());
            
            dummyPanel.add(pointPanel, BorderLayout.NORTH);
            lowerPanel.add(label2);

            // Put the point panel in a scroll bar.
            JScrollPane scrollPane = new JScrollPane(dummyPanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            if (size != null)
                scrollPane.setPreferredSize(size);

            
            // Add the buttons, scroll bar and inner panel to a titled panel that will resize with the main window.
            JPanel outerPanel = new JPanel(new BorderLayout());
            outerPanel.setBorder(
                new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("3 Point Problem Picker")));
            outerPanel.add(topPanel, BorderLayout.NORTH);
            this.add(outerPanel, BorderLayout.CENTER);
        }

    }


    /** A <code>Thread</code> that loads a KML file and displays it in an <code>AppFrame</code>. */
    public static class WorkerThread extends Thread
    {
        /** Indicates the source of the KML file loaded by this thread. Initialized during construction. */
        protected Object kmlSource;
        /** Indicates the <code>AppFrame</code> the KML file content is displayed in. Initialized during construction. */
        protected AppFrame appFrame;

        /**
         * Creates a new worker thread from a specified <code>kmlSource</code> and <code>appFrame</code>.
         *
         * @param kmlSource the source of the KML file to load. May be a {@link File}, a {@link URL}, or an {@link
         *                  java.io.InputStream}, or a {@link String} identifying a file path or URL.
         * @param appFrame  the <code>AppFrame</code> in which to display the KML source.
         */
        public WorkerThread(Object kmlSource, AppFrame appFrame)
        {
            this.kmlSource = kmlSource;
            this.appFrame = appFrame;
        }

        /**
         * Loads this worker thread's KML source into a new <code>{@link gov.nasa.worldwind.ogc.kml.KMLRoot}</code>,
         * then adds the new <code>KMLRoot</code> to this worker thread's <code>AppFrame</code>. The
         * <code>KMLRoot</code>'s <code>AVKey.DISPLAY_NAME</code> field contains a display name created from either the
         * KML source or the KML root feature name.
         * <p/>
         * If loading the KML source fails, this prints the exception and its stack trace to the standard error stream,
         * but otherwise does nothing.
         */
        public void run()
        {
            try
            {
                KMLRoot kmlRoot = this.parse();

                // Set the document's display name
                kmlRoot.setField(AVKey.DISPLAY_NAME, formName(this.kmlSource, kmlRoot));

                // Schedule a task on the EDT to add the parsed document to a layer
                final KMLRoot finalKMLRoot = kmlRoot;
                
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        appFrame.addKMLLayer(finalKMLRoot);
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        /**
         * Parse the KML document.
         *
         * @return The parsed document.
         *
         * @throws IOException        if the document cannot be read.
         * @throws XMLStreamException if document cannot be parsed.
         */
        protected KMLRoot parse() throws IOException, XMLStreamException
        {
            // KMLRoot.createAndParse will attempt to parse the document using a namespace aware parser, but if that
            // fails due to a parsing error it will try again using a namespace unaware parser. Note that this second
            // step may require the document to be read from the network again if the kmlSource is a stream.
            return KMLRoot.createAndParse(this.kmlSource);
        }
    }

    protected static String formName(Object kmlSource, KMLRoot kmlRoot)
    {
        KMLAbstractFeature rootFeature = kmlRoot.getFeature();

        if (rootFeature != null && !WWUtil.isEmpty(rootFeature.getName()))
            return rootFeature.getName();

        if (kmlSource instanceof File)
            return ((File) kmlSource).getName();

        if (kmlSource instanceof URL)
            return ((URL) kmlSource).getPath();

        if (kmlSource instanceof String && WWIO.makeURL((String) kmlSource) != null)
            return WWIO.makeURL((String) kmlSource).getPath();

        return "KML Layer";
    }

    /**
     * @wbp.parser.entryPoint
     */
    protected static void makeMenu(final AppFrame appFrame)
    {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("KML/KMZ File", "kml", "kmz"));

        JMenuBar menuBar = new JMenuBar();
        appFrame.setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        
        menuBar.add(fileMenu);

        JMenuItem openFileMenuItem = new JMenuItem(new AbstractAction("Open File...")
        {
            /**
			 * 
			 */
			private static final long serialVersionUID = 2592899248183147945L;

			public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    int status = fileChooser.showOpenDialog(appFrame);
                    if (status == JFileChooser.APPROVE_OPTION)
                    {
                        for (File file : fileChooser.getSelectedFiles())
                        {
                            new WorkerThread(file, appFrame).start();
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        fileMenu.add(openFileMenuItem);

        JMenuItem openURLMenuItem = new JMenuItem(new AbstractAction("Open URL...")
        {
            /**
			 * 
			 */
			private static final long serialVersionUID = -2826094156753749668L;

			public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    String status = JOptionPane.showInputDialog(appFrame, "URL");
                    if (!WWUtil.isEmpty(status))
                    {
                        new WorkerThread(status.trim(), appFrame).start();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        fileMenu.add(openURLMenuItem);
    }


    public static void main(String[] args)
    {
    	 try {
             // Set System L&F
         UIManager.setLookAndFeel(
             UIManager.getSystemLookAndFeelClassName());
     } 
     catch (UnsupportedLookAndFeelException e) {
        // handle exception
     }
     catch (ClassNotFoundException e) {
        // handle exception
     }
     catch (InstantiationException e) {
        // handle exception
     }
     catch (IllegalAccessException e) {
        // handle exception
     }
    	
        //noinspection UnusedDeclaration
        @SuppressWarnings("unused")
		final AppFrame af = (AppFrame) start("World Wind 3 Point Problem Solver", AppFrame.class);

    }
    
    
}
