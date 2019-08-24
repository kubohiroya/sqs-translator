/**
 * 
 */
package net.sqs2.translator.impl;

import java.awt.geom.Point2D;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.render.pdf.SVGElementIDToPageRectangleMap;

public interface PageSetting{
	public abstract void init(SVGElementIDToPageRectangleMap map, FOUserAgent userAgent)throws SQMSchemeException;

	public abstract String getVersion();
	public abstract double getWidth();
	public abstract double getHeight();	
	public abstract Point2D[] getDeskewGuideCenterPointArray();
}