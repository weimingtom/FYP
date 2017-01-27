package manga.page;

import pixelitor.colors.FillType;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.ShapeType;
import pixelitor.tools.ShapesAction;
import pixelitor.tools.Tools;
import pixelitor.tools.UserDrag;
import pixelitor.tools.shapes.WordBalloon;
import pixelitor.tools.shapestool.BasicStrokeJoin;
import pixelitor.tools.shapestool.ShapesTool;
import pixelitor.tools.shapestool.TwoPointBasedPaint;
import subtitle.process.SubtitleProcessor;
import video.process.VideoProcessor;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.NewImage;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementPermission;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the main controller of generating manga
 * 
 * @author PuiWa
 *
 */

public final class MangaGenerator {
	private static ArrayList<MangaPage> pageList=new ArrayList<>();
	
	private MangaGenerator() {
		
	}
	
	public static void addNewMangaPage() {
		int keyFrameCount = VideoProcessor.getKeyFrameCount();
		System.out.println("keyframecount: "+keyFrameCount);
		int pageNum = keyFrameCount / 6;
		for (int i = 0; i<pageNum; i++) {
			MangaPage page = new MangaPage();
			pageList.add(page);
		}
		if (keyFrameCount % 6 != 0) {
			MangaPage page = new MangaPage();
			pageList.add(page);
		}
//		MangaPage page = new MangaPage();
//		pageList.add(page);
	}
	
	public static MangaPage getActivePage() {
		Composition comp = ImageComponents.getActiveCompOrNull();
		for (MangaPage p: pageList) {
			if (p.getComp().equals(comp)) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * Set active MangaPanel layer
	 * 
	 * @param i the index of layer of the current active composition
	 */
//	public static ImageLayer setActivePanelLayer(int i) {
//		MangaPanel selectedPanel = getActivePage().getPanels().get(i);
//		selectedPanel.getLayer().makeActive(AddToHistory.YES);
//		return selectedPanel.getLayer();
//	}
	
	/**
	 * Set active internal frame (i.e. MangaPage composition)
	 * 
	 * @param i the index of ArrayList of MangaPage
	 */
	public static void setActiveComp(int i) {
		List<ImageComponent> icList = ImageComponents.getICList();
		Composition selectedComp = pageList.get(i).getComp();
        for (ImageComponent ic : icList) {
        	if (ic.getComp() == selectedComp) {
        		ImageComponents.setActiveIC(ic, true);
        	}
        }
	}
	
	
	/**
	 * Draw 6 manga panels on different MangaPanel layers
	 */
	public static void drawMangaPanels() {
//		MangaPage activePage = getActivePage();
		
		for (MangaPage page: pageList) {

			// Declare 6 layers for 6 panels 
	    	for (int i=0; i<6; i++) {
	    		page.addNewMangaPanel();
	    	}

			ArrayList<MangaPanel> panels = page.getPanels();
	    	
	    	ImageLayer layer = panels.get(0).getLayer();	//Get 1st canvas for size reference
	        Canvas canvas = layer.getComp().getCanvas();
	        int canvasWidth = canvas.getWidth();
	        int canvasHeight = canvas.getHeight();
	        int canvasLeftRightMargin = (int)(canvas.getWidth()*0.05);
	        int canvasTopBottomMargin = (int)(canvas.getHeight()*0.05);
	        int mangaPanelWidth = (int)((canvas.getWidth()-canvasLeftRightMargin*3)/2);
	        int mangaPanelHeight = (int)((canvas.getHeight()-canvasTopBottomMargin*4)/3);
	        
	        ShapesTool shapesTool = Tools.SHAPES;
	        shapesTool.setShapeType(ShapeType.RECTANGLE);
	        shapesTool.setAction(ShapesAction.STROKE);
	        shapesTool.setStrokFill(TwoPointBasedPaint.FOREGROUND);
	        // set stroke join and width
	        shapesTool.setStrokeJoinAndWidth(BasicStrokeJoin.MITER, 10);
	        
	        // Add two panels (left and right) at a time
	        for (int i=0; i<3; i++) {
	            UserDrag leftPanelDrag = new UserDrag(canvasLeftRightMargin, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin, 
	            		canvasLeftRightMargin+mangaPanelWidth, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin+mangaPanelHeight);
	            UserDrag rightPanelDrag = new UserDrag(canvasLeftRightMargin*2+mangaPanelWidth, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin, 
	            		(canvasLeftRightMargin+mangaPanelWidth)*2, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin+mangaPanelHeight);
	            
	            // set bounding rectangle of the panels
	            panels.get(i*2).setBound(leftPanelDrag.createPossiblyEmptyRect());
	            panels.get(i*2+1).setBound(rightPanelDrag.createPossiblyEmptyRect());
	            
	            // Draw panels on different layers
	            shapesTool.paintShapeOnIC(panels.get(i*2).getLayer(), leftPanelDrag);
	            shapesTool.paintShapeOnIC(panels.get(i*2+1).getLayer(), rightPanelDrag);
	        }
		}
	}
	
	/**
	 * Draw word balloon on layer, similar to drawing panel.
	 */
	public static void drawWordBalloons() {
		for (MangaPage page: pageList) {
			for (MangaPanel panel: page.getPanels()) {
				panel.addMangaBalloon();
			}
		}
	}
	
	/**
	 * Fill panels with key frames
	 */
	public static void drawImgsToPanel() {
        for (int i = 0; i<pageList.size(); i++) {
        	MangaPage currentPage = pageList.get(i);
    		for (int j = 0; j<currentPage.getPanels().size(); j++) {
            	MangaPanel panel = currentPage.getPanels().get(j);
            	panel.fitImageToPanelBound();
            }
        }
	}
	
	/**
	 * Push balloon and text layers to the top.
	 * Otherwise they will be occluded by other layers
	 */
	public static void pushBalloonsAndTextToTop() {
        for (int i = 0; i<pageList.size(); i++) {
        	MangaPage currentPage = pageList.get(i);
        	// the active index of the top (latest created) layer
			int activeLayerIndex = currentPage.getComp().getActiveLayerIndex();
			for (MangaPanel panel: currentPage.getPanels()) {
				for (MangaBalloon balloon: panel.getBalloons()) {
					// update the layer with the new index
					// for some reason, teh two layers can work with same index, still under investigation
					balloon.getBallloonLayer().dragFinished(activeLayerIndex);
//					System.out.println("balloon index"+currentPage.getComp().getLayerIndex(balloon.getBallloonLayer()));
					balloon.getMangaTextLayer().dragFinished(activeLayerIndex);
//					System.out.println("text index"+currentPage.getComp().getLayerIndex(balloon.getMangaTextLayer()));
				}
			}
		}
	}
}
