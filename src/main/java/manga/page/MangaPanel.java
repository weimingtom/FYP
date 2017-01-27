package manga.page;

import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementPermission;
import java.util.ArrayList;

import pixelitor.Composition;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.painters.TranslatedMangaTextPainter;
import pixelitor.gui.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.MangaText;
import pixelitor.selection.Selection;
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

/**
 * @author PuiWa
 *
 */
public class MangaPanel {
	private MangaPage page;	// the page the panel belongs to
	private ImageLayer layer;	// a panel refers to a layer
	private MangaPanelImage panelImg;
	private Rectangle2D bound;	//bounding rectangle of panel
	private ArrayList<MangaBalloon> balloonList;
	
	private static int panelCount = 0;
	
	public MangaPanel(MangaPage page) {
		this.page = page;
		panelCount++;
		this.layer = page.getComp().addNewEmptyLayer("Panel "+panelCount, false);
		this.panelImg = new MangaPanelImage();
		this.bound = new Rectangle2D.Float();
		this.balloonList = new ArrayList<>();
	}
	
//	public MangaPanel(Composition comp, Rectangle2D bound) {
//		this.comp = comp;
//		this.layer = comp.addNewEmptyLayer("Panel "+panelCount, false);
//		panelCount++;
//		this.panelImg = new MangaPanelImage();
//		this.bound = bound;
//		this.balloonList = new ArrayList<>();
//	}

	public void setBound(Rectangle2D boundingRect) {
		this.bound = boundingRect;
	}
	
	public Rectangle2D getBound() {
		return this.bound;
	}
	
	public ImageLayer getLayer() {
		return this.layer;
	}
	
	/**
	 * for testing purpose
	 */
	public void printBound() {
		System.out.println(this.bound);
	}
	
	public void setImage(MangaPanelImage panelImg) {
		this.panelImg = panelImg;
	}
	
    /**
     * Image fit to panel bounding rectangle, starts from top-left corner.
     * The image will be put into a new layer, bounded by the area within selection.
     */
    public void fitImageToPanelBound() {
    	long frameTimestamp = VideoProcessor.getCurrTimestamp();
        BufferedImage frameImg = VideoProcessor.extractFrame();
        
        if (frameImg != null) {
        	// setup selection with the bounding rectangle of MangaPanel
        	Composition comp = page.getComp();
            Selection selection = new Selection(bound, comp.getIC());
            comp.setNewSelection(selection);
            
	        panelImg = new MangaPanelImage(comp, frameImg, frameTimestamp, bound);
	        
	        // image to new layer
	        ImageLayer layer = panelImg.getLayer();
	        
	        layer.setImageWithSelection(panelImg.getSubImage());
	        
	        // Deselect. Don't use selection.die() directly, which leads to error
	        comp.deselect(AddToHistory.NO);
        }
    }
    
    public void addMangaBalloon() {
		String linkedText = "";
		long frameTimestamp = panelImg.getFrameTimestamp();
//		System.out.println("Frame timestamp: "+SubtitleProcessor.timestampToTimeString(frameTimestamp));
//		System.out.println("Next Frame timestamp: "+SubtitleProcessor.timestampToTimeString(VideoProcessor.getNextKeyframeTimestamp(frameTimestamp)));
		ArrayList<String> subTextList = SubtitleProcessor.getSubTextList(frameTimestamp, VideoProcessor.getNextKeyframeTimestamp(frameTimestamp));
		
		if (subTextList.size() != 0) {
			for (String str: subTextList) {
				linkedText = linkedText + str +" ";
			}
	//		System.out.println(linkedText);
	//		System.out.println("subTextList.size(): "+subTextList.size());
	    	
	    	Composition comp = page.getComp();
	    	MangaText mangaTextLayer = new MangaText(comp, "Manga Text");
	    	mangaTextLayer.setDefaultSetting();
	//    	mangaTextLayer.printMangaText();

	    	
	    	// any change to textSettings need to use setSettings(), otherwise it will not be applied
	    	TextSettings oldSettings = mangaTextLayer.getSettings();
	        TextSettings newSettings = new TextSettings(oldSettings);
	        
	        newSettings.setText(linkedText);
	//        System.out.println("Timestamp1: "+frameTimestamp);
	//        System.out.println("Timestamp2: "+VideoProcessor.getNextKeyframeTimestamp(frameTimestamp));
	//        System.out.println("SubText: "+subTextList);
	        mangaTextLayer.setSettings(newSettings);
	    	
	        // set text translation within balloon bound, to be changed
	    	mangaTextLayer.setTranslation((int)bound.getX(), (int)bound.getY());
	    	
	    	WordBalloon balloonRef = new WordBalloon(bound.getX(), bound.getY(), 100, 100);
	    	MangaBalloon balloon = new MangaBalloon(this, mangaTextLayer, balloonRef);
	    	balloonList.add(balloon);
	    	
	    	TranslatedMangaTextPainter painter = (TranslatedMangaTextPainter) mangaTextLayer.getTranslatedTextPainter();
	    	painter.setBound(balloonRef.getBounds());
	    	
			//initialize shapes tool for drawing balloon
	        ShapesTool shapesTool = Tools.SHAPES;
	        shapesTool.setShapeType(ShapeType.WORDBALLOON);
	        shapesTool.setAction(ShapesAction.FILL_AND_STROKE);
	        shapesTool.setStrokFill(TwoPointBasedPaint.FOREGROUND);
	        shapesTool.setFill(TwoPointBasedPaint.BACKGROUND);
	        // reset stroke join and width to default
	        shapesTool.setStrokeJoinAndWidth(BasicStrokeJoin.ROUND, 5);
	        
	        // paint balloon relative to panel bound, to be changed
	    	shapesTool.paintShapeOnIC(balloon.getBallloonLayer(), new UserDrag(bound.getX(), bound.getY(), bound.getX()+200, bound.getY()+200));
        }
    }

	public ArrayList<MangaBalloon> getBalloons() {
		return balloonList;
	}
	
	public MangaPage getPage() {
		return page;
	}
}
