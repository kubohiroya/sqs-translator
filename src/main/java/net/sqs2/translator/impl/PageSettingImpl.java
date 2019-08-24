/**
 * 
 */
package net.sqs2.translator.impl;

import java.awt.geom.Point2D;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.render.pdf.PageRectangle;
import org.apache.fop.render.pdf.SVGElementIDToPageRectangleMap;

public class PageSettingImpl implements PageSetting{
	private static final String VERSION = "2.1.0";
	private double width;
	private double height;
	private Point2D[] deskewGuideCenterPointArray = new Point2D[4];
	
	private PageSettingImpl(){
	    //this(595, 842);//A4 portlait size
		//this(284, 420);//hagaki size
	    // Avoid no params creation
        throw new Error();
	}
	/**
     * Returns a new PageSetting with dimensions for  <em>A4 595x842</em>
     * @return A {@link PageSetting}, never <code>null</code>.
     */
    public static final PageSetting getA4()
    {
        return new PageSettingImpl(595, 842);
    }
	/**
	 * Returns a new PageSetting with dimensions for  <em>Hagaki 284x420</em>
	 * @return A {@link PageSetting}, never <code>null</code>.
	 */
	public static final PageSetting getHagaki()
    {
        return new PageSettingImpl(284, 420);
    }
    /**
     * Constructor for a {@link PageSettingImpl}
     * See also {@link #getA4()} and {@link #getHagaki()}
     * as convenient defaults.
     * @param width
     * @param height
     */
	public PageSettingImpl(double width, double height){
		this.width = width;
		this.height = height;
	}
	
	@Override
	public void init(SVGElementIDToPageRectangleMap map, FOUserAgent ua)throws SQMSchemeException{
		if(true){
			if(! "auto".equals(ua.getPageWidth())){
				width = Double.parseDouble(ua.getPageWidth());
			}
			if(! "auto".equals(ua.getPageHeight())){
				height = Double.parseDouble(ua.getPageHeight());
			}
			try{
				PageRectangle e0 = (PageRectangle)map.get(ua, 0).get("SQSDeskewGuideNorthWest");
				PageRectangle e1 = (PageRectangle)map.get(ua, 0).get("SQSDeskewGuideNorthEast");
				PageRectangle e2 = (PageRectangle)map.get(ua, 0).get("SQSDeskewGuideSouthWest");
				PageRectangle e3 = (PageRectangle)map.get(ua, 0).get("SQSDeskewGuideSouthEast");
			
				if(e0 != null && e1 != null && e2 != null && e3 != null){
					deskewGuideCenterPointArray[0] = createCenterPointOfRectElement(e0);
					deskewGuideCenterPointArray[1] = createCenterPointOfRectElement(e1);
					deskewGuideCenterPointArray[2] = createCenterPointOfRectElement(e2);
					deskewGuideCenterPointArray[3] = createCenterPointOfRectElement(e3);
				}else{
					throw new IllegalArgumentException("page master scheme is invalid.");
				}
			}catch(NullPointerException ex){
				//throw new SQMSchemeException();
			}
		}
	}
	
	private Point2D.Double createCenterPointOfRectElement(PageRectangle rect)throws SQMSchemeException{
		if(rect == null){
			throw new SQMSchemeException();
		}
		return new Point2D.Double(rect.getX() + rect.getWidth()/ 2, rect.getY()
				+ rect.getHeight() / 2);
	}
	
	@Override
	public String getVersion(){
		return VERSION;
	}
	
	@Override
	public double getWidth(){
		return width;
	}
	
	@Override
	public double getHeight(){
		return height;
	}
	@Override
	public Point2D[] getDeskewGuideCenterPointArray() {
		return this.deskewGuideCenterPointArray;
	}
}