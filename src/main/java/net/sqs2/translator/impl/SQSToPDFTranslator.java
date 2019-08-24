/*

 SQSToPDFTranslator.java

 Copyright 2004 KUBO Hiroya (hiroya@cuc.ac.jp).

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 Created on 2007/09/04

 */
package net.sqs2.translator.impl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.transform.URIResolver;

import net.sqs2.barcode.support.BarcodeURIResolver;
import net.sqs2.net.ClassURIResolver;
import net.sqs2.translator.ParamEntry;
import net.sqs2.translator.TranslatorException;
import net.sqs2.util.FileUtil;
import net.sqs2.util.VersionTag;
import net.sqs2.xml.XMLUtil;
import net.sqs2.xmlns.SQSNamespaces;
import net.sqs2.xslt.CachedURIResolver;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.render.pdf.PageRectangle;
import org.apache.fop.render.pdf.SVGElementIDToPageRectangleMap;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.lowagie.text.DocumentException;
/**
 * <em>Main Entry Point</em> for SQS to PDF translation.
 * <p>Sample usage:
 * <pre>
 * SQSToPDFTranslator translator = 
 *      new SQSToPDFTranslator("mygroup", "myapp", "myfilename");
 * TranslatorSourceBean source = new TranslatorSourceBean();
 *  // configure SQS-xml source
 * source.setInputFile(file);
 *  // or
 *  // source.setSystemId(url.toString());
 *  // or
 *  // source.setInputStream(inputStream);
 *  
 * InputStream pdf =  t.translate(source);  
 * </pre>
 *
 */
public class SQSToPDFTranslator extends FOPTranslator {
	
	static ResourceBundle res = ResourceBundle.getBundle("form");
	static String questionPrefix  = res.getString("xhtml.h-attribute..sqs.prefix");
	static String example_blank_mark_label  = res.getString("example-blank-mark-label");
	static String example_filled_mark_label  = res.getString("example-filled-mark-label");
	static String example_incomplete_mark_abel = res.getString("example-incomplete-mark-label");
	static String characters_prohibit_line_break = res.getString("characters-prohibit-line-break");
	static String characters_prohibit_line_end = res.getString("characters-prohibit-line-end");
	static String fontFamily = res.getString("fontFamily");
	static String baseFontSizePt = res.getString("baseFontSizePt");
	
	static final String[] SQS2FO = { "cmpl-label.xsl", "cmpl-ref.xsl", "embed-counter.xsl",	"embed-link.xsl", "convert1.xsl", "convert2.xsl", "convert3.xsl" };

	private static final float SCALE = 1.0f;

    private static final String DEFAULT_FOP_URL = TranslatorJarURIContext.FOP_JAR_URI; //"class://net.sqs2.translator.impl.TranslatorJarURIContext/fop/";

    private static final String DEFAULT_XSLT_URL = TranslatorJarURIContext.SQSTRANS_JAR_URI; //  "class://net.sqs2.translator.impl.TranslatorJarURIContext/xslt/";

	private final String basename;


	/**
	 * Constructs a {@link SQSToPDFTranslator} with 
	 * default configuration for most properties.
	 * @param groupID
	 * @param appID
	 * @param fileName <em>required</em> file name
	 * @throws TranslatorException 
	 */
	public SQSToPDFTranslator(String groupID, String appID, String fileName) throws TranslatorException {
	    this(groupID, appID, null, null, null, fileName, null, null);
	}
	/**
	 * Constructs a {@link SQSToPDFTranslator}.
	 * @param groupID
	 * @param appID
	 * @param fopURL optional fop URL
	 * @param xsltURL optional xslt URL
	 * @param locale optional Locale
	 * @param filename <em>required</em> filename
	 * @param uriResolver optional {@link URIResolver}, when not passed {@link ClassURIResolver} is used.
	 * @param pageSetting <em>optional</em> {@link PageSetting}, 
	 * when not passed the A4 format is used (return value of {@link PageSettingImpl#getA4()}).
	 * @throws TranslatorException
	 */
	public SQSToPDFTranslator(String groupID, String appID, String fopURL, String xsltURL, Locale locale, String filename, 
			URIResolver uriResolver, PageSetting pageSetting)
	throws TranslatorException {
		super(groupID, appID, 
		    isEmpty(fopURL) ? DEFAULT_FOP_URL : fopURL, 
		    isEmpty(xsltURL) ? DEFAULT_XSLT_URL : xsltURL, 
		    locale == null ? Locale.getDefault() : locale, 
		    uriResolver==null ? createDefaultURIResolver() : uriResolver, 
		    pageSetting==null ? PageSettingImpl.getA4() : pageSetting);
        if (filename==null)
            throw new IllegalArgumentException("Null file name passed");
        this.basename = FileUtil.getBasename(filename);
	}
	
    private static boolean isEmpty(String s)
    {
        return s==null || s.trim().length()==0;
    }
    
	public static URIResolver createDefaultURIResolver(){
		return new BarcodeURIResolver(new CachedURIResolver(new ClassURIResolver()));
	}
    
    @Override
    public String[] getXSLTScripts(){
    	return SQS2FO;
    } 

    @Override
    Map<String, ParamEntry[]> createParameterArrayMap(PageSetting pageSetting, Locale selectedLocale, String selectedLocaleSuffix) {
    	Map<String, ParamEntry[]> ret = new HashMap<String, ParamEntry[]>();
		ret.put("embed-counter.xsl", new ParamEntry[] { new ParamEntry("xhtml.h-attribute..sqs.prefix", questionPrefix),
				new ParamEntry("xhtml.h-attribute..sqs.suffix", "."),
				new ParamEntry("xhtml.h-attribute..sqs.format", "1"),
				new ParamEntry("sqs.counter-attribute..sqs.prefix", "("),
				new ParamEntry("sqs.counter-attribute..sqs.suffix", ")"),
				new ParamEntry("sqs.counter-attribute..sqs.format", "1") });

		ret.put("convert2.xsl", new ParamEntry[] { new ParamEntry("xforms.hint-attribute..sqs.prefix", ""),
				new ParamEntry("xforms.hint-attribute..sqs.suffix", ""),
				new ParamEntry("xforms.hint-attribute..sqs.display", "inline"),
				new ParamEntry("xforms.help-attribute..sqs.prefix", "("),
				new ParamEntry("xforms.help-attribute..sqs.suffix", ")"),
				new ParamEntry("xforms.help-attribute..sqs.display", "inline"),
				new ParamEntry("xforms.alart-attribute..sqs.prefix", "*"),
				new ParamEntry("xforms.alart-attribute..sqs.suffix", ""),
				new ParamEntry("xforms.alart-attribute..sqs.display", "inline") });

		ret.put("convert3.xsl", new ParamEntry[] {
				new ParamEntry("language", selectedLocale.getLanguage()),
				new ParamEntry("localeSuffix",selectedLocaleSuffix),
				 new ParamEntry("example-blank-mark-label", example_blank_mark_label),
				new ParamEntry("example-filled-mark-label", example_filled_mark_label),
				new ParamEntry("example-incomplete-mark-label", example_incomplete_mark_abel),
				new ParamEntry("characters-prohibit-line-break", characters_prohibit_line_break),
				new ParamEntry("pageMasterPageWidth", Double.toString(pageSetting.getWidth())),//595
				new ParamEntry("pageMasterPageHeight", Double.toString(pageSetting.getHeight())),//842
				new ParamEntry("pageMasterMarginTop", "0"),
				new ParamEntry("pageMasterMarginBottom", "0"),
				new ParamEntry("pageMasterMarginLeft", "0"),
				new ParamEntry("pageMasterMarginRight", "0"),
				new ParamEntry("regionBodyMarginTop", "70"),
				new ParamEntry("regionBodyMarginBottom", "70"),
				new ParamEntry("regionBodyMarginLeft", "44"),
				new ParamEntry("regionBodyMarginRight", "44"),
				new ParamEntry("regionBeforeExtent", "10"),
				new ParamEntry("regionAfterExtent", "60"),
				new ParamEntry("regionStartExtent", "10"),
				new ParamEntry("regionEndExtent", "60"),
				
				new ParamEntry("deskewGuideAreaWidth","434"),
				new ParamEntry("deskewGuideAreaHeight","55"),
				new ParamEntry("deskewGuideBlockWidth","18"),
				new ParamEntry("deskewGuideBlockHeight","18"),

				new ParamEntry("pageSideStartingFrom", "left"),
				new ParamEntry("showStapleMark", "true"),
				new ParamEntry("showPageNumber", "true"),
				new ParamEntry("showEnqtitleBelowPagenum", "true"),
				new ParamEntry("showMarkingExample", "true"),
				new ParamEntry("sides", "duplex"),

				new ParamEntry("fontFamily", fontFamily),
				new ParamEntry("baseFontSizePt", baseFontSizePt)
		
		});
		return ret;
	}

    protected String getBasename() {
		return basename;
	}
    
	//byte[] sqsSourceBytes, byte[] pdfRawDataBytes, , String basename
	//byte[] svgBytes = createSVGPrint(userAgent, numPages);
	//new FileAttachment("SQS Source", sqsSourceBytes, basename + ".sqs")
	//new FileAttachment("SQS Master", svgBytes, basename + ".sqm")
	//int numPages = reader.getNumberOfPages();
	//userAgent.getRendererOptions().get("pageWidth");

    private byte[] createSVGPrint(FOUserAgent userAgent, int numPages) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.println("<svg:svg ");
			writer.println(" xmlns=\"" + SQSNamespaces.SVG_URI + "\" ");
			writer.println(" xmlns:svg=\"" + SQSNamespaces.SVG_URI + "\" ");
			writer.println(" xmlns:sqs=\"" + SQSNamespaces.SQS2004_URI + "\" ");
			writer.println(" xmlns:xforms=\"" + SQSNamespaces.XFORMS_URI + "\" ");
			writer.println(" xmlns:master=\"" + SQSNamespaces.SQS2007MASTER_URI + "\" ");
			writer.print("width=\"");
			PageSetting pageSetting = (PageSetting)userAgent.getRendererOptions().get("pageSetting");
			writer.print(pageSetting.getWidth());
			writer.print("\" height=\"");
			writer.print(pageSetting.getHeight());
			writer.println("\">");
			
			pageSetting.init(SVGElementIDToPageRectangleMap.getInstance(), userAgent);
			
			printPageSet(pageSetting, userAgent, numPages, writer);
			writer.println("</svg:svg>");
			writer.close();
			out.close();
			
			byte[] svgBytes = out.toByteArray();

			if (false) {
				ByteArrayInputStream svgInputStream = new ByteArrayInputStream(svgBytes);
				OutputStream sqmOutputStream = new BufferedOutputStream(new FileOutputStream("/tmp/sqs.sqm"));
				FileUtil.connect(svgInputStream, sqmOutputStream);
				svgInputStream.close();
				sqmOutputStream.close();
			}

			return svgBytes;
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (SQMSchemeException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void printPageSet(PageSetting pageSetting, FOUserAgent userAgent, int numPages, PrintWriter writer) {
		writer.println("<svg:pageSet>");

		printMasterPage(pageSetting, numPages, writer);

		SVGElementIDToPageRectangleMap svgElementIDToPageRectangleMap = SVGElementIDToPageRectangleMap
		.getInstance();

		if(new VersionTag(pageSetting.getVersion()).isSameOrOlderThan(new VersionTag("1.3.3"))){
			for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
				writer.println("  <svg:page>");
				Map<String, PageRectangle> map = svgElementIDToPageRectangleMap.remove(userAgent, pageIndex);
				if (map != null) {
					for (Map.Entry<String, PageRectangle> entry : map.entrySet()) {
						String id = entry.getKey();
						PageRectangle pageRectangle = entry.getValue();
						pageIndex = pageRectangle.getPageIndex();
						printGElement(pageSetting, id, pageRectangle, writer);
					}
				}
				writer.println("  </svg:page>");
			}
		}else{
			for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
				writer.println("  <svg:page>");
				Map<String, PageRectangle> map = svgElementIDToPageRectangleMap.remove(userAgent, pageIndex);
				if (map != null) {
					for (Map.Entry<String, PageRectangle> entry : map.entrySet()) {
						String id = entry.getKey();
						if(id.startsWith("mark") || id.startsWith("textarea") ){
							PageRectangle pageRectangle = entry.getValue();
							pageIndex = pageRectangle.getPageIndex();
							printGElement(pageSetting, id, pageRectangle, writer);
						}
					}
				}
				writer.println("  </svg:page>");
			}
		}		
		writer.println("</svg:pageSet>");
	}

	private void printGElement(PageSetting pageSetting, String id, PageRectangle area, PrintWriter writer) {
		writer.print("<svg:g id=\"");
		writer.print(id);
		writer.println("\">");
		writer.print("<svg:rect x=\"");
		writer.print(area.getX() / SCALE);
		writer.print("\" y=\"");
		writer.print(pageSetting.getHeight() - area.getY() / SCALE);
		writer.print("\" width=\"");
		writer.print(area.getWidth() / SCALE);
		writer.print("\" height=\"");
		writer.print(area.getHeight() / SCALE);
		writer.println("\">");
		writer.println(XMLUtil.createString((Element) area.getSVGMetadataNode()));
		writer.println("</svg:rect>");
		writer.println("</svg:g>");
	}

	private void printMasterPage(PageSetting pageSetting, int numPages, PrintWriter writer) {
		writer.println(" <svg:masterPage>");
		writer.println("  <svg:metadata>");
		writer.print("      <master:master master:version=\"" + pageSetting.getVersion()+ "\" master:numPages=\"");
		writer.print(numPages);
		writer.println("\" />");
		printDeskewGuideElement(pageSetting, writer);
		writer.println("  </svg:metadata>");
		writer.println(" </svg:masterPage>");
	}

	private void printDeskewGuideElement(PageSetting pageSetting, PrintWriter writer) {
		if(pageSetting.getDeskewGuideCenterPointArray()[0] == null){
			Logger.getLogger(getClass().getName()).fatal("deskewGuides are null.");
			return;
		}
		
		writer.print("      <master:deskewGuide master:x1=\"");
		writer.print(pageSetting.getDeskewGuideCenterPointArray()[0].getX());
		writer.print("\" master:y1=\"");
		writer.print(pageSetting.getHeight() - pageSetting.getDeskewGuideCenterPointArray()[0].getY());
		writer.print("\" master:x2=\"");
		writer.print(pageSetting.getDeskewGuideCenterPointArray()[1].getX());
		writer.print("\" master:y2=\"");
		writer.print(pageSetting.getHeight() - pageSetting.getDeskewGuideCenterPointArray()[1].getY());
		writer.print("\" master:x3=\"");
		writer.print(pageSetting.getDeskewGuideCenterPointArray()[2].getX());
		writer.print("\" master:y3=\"");
		writer.print(pageSetting.getHeight() - pageSetting.getDeskewGuideCenterPointArray()[2].getY());
		writer.print("\" master:x4=\"");
		writer.print(pageSetting.getDeskewGuideCenterPointArray()[3].getX());
		writer.print("\" master:y4=\"");
		writer.print(pageSetting.getHeight() - pageSetting.getDeskewGuideCenterPointArray()[3].getY());
		writer.println("\" />");
	}
	
	@Override
	protected void combinePDFData(FOUserAgent userAgent, byte[] sqsSourceBytes, byte[] pdfRawDataBytes, int numPages, OutputStream pdfOutputStream) throws IOException, DocumentException {
		combinePDFData(userAgent, pdfRawDataBytes, new FileAttachmentBean[]{
				new FileAttachmentBean("SQS Source", this.basename + ".sqs", sqsSourceBytes),
				new FileAttachmentBean("SQS Master", this.basename + ".sqm", createSVGPrint(userAgent, numPages))
		}, pdfOutputStream);
	}

	
}
