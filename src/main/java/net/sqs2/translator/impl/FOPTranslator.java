package net.sqs2.translator.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import net.sqs2.translator.DOMTranslatorSource;
import net.sqs2.translator.ParamEntry;
import net.sqs2.translator.SAXTranslatorSource;
import net.sqs2.translator.StreamTranslatorSource;
import net.sqs2.translator.StreamTranslatorSourceBean;
import net.sqs2.translator.Translator;
import net.sqs2.translator.TranslatorException;
import net.sqs2.translator.XSLTranslator;
import net.sqs2.xml.XMLUtil;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serializer.DOM3Serializer;
import org.apache.xml.serializer.dom3.DOM3SerializerImpl;
import org.apache.xml.serializer.dom3.LSSerializerImpl;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

public class FOPTranslator implements Translator {

	private XSLTranslator xslTranslator;

	private Locale selectedLocale;
	private String selectedLocaleSuffix;

	private FopFactory fopFactory;
	private FOUserAgent userAgent;
	private ExecutorService executorServiceForXSLT;
	private ExecutorService executorServiceForFOP;
	private  Exception[] exceptions;
	
	static class FileAttachmentBean{
		String title;
		String filename;
		byte[] bytes;
		FileAttachmentBean(	String title,
				String filename,
				byte[] bytes){
			this.title = title;
			this.filename = filename;
			this.bytes = bytes;
		}
		public String getTitle() {
			return title;
		}
		public String getFilename() {
			return filename;
		}
		public byte[] getBytes() {
			return bytes;
		}
	}

	public FOPTranslator(String groupID, String appID, String fopURL, String xsltClassBaseURL, URIResolver uriResolver, PageSetting pageSetting) throws TranslatorException{
		this(groupID, appID, fopURL, xsltClassBaseURL, null, uriResolver, pageSetting);
	}
	
	/**
	 * 
	 * @param groupID
	 * @param appID
	 * @param fopURL <em>required</em>
	 * @param xsltClassBaseURL <em>required</em>
	 * @param locale optional
	 * @param uriResolver <em>required</em>
	 * @param pageSetting <em>required</em>
	 * @throws TranslatorException
	 */
	public FOPTranslator(String groupID, String appID, String fopURL, String xsltClassBaseURL, Locale locale, URIResolver uriResolver, PageSetting pageSetting)
	throws TranslatorException {
		super();
		// If no page setting NullPointer later on
		if (pageSetting==null)
            throw new IllegalArgumentException("PageSetting is required.");
		// Not sure about this, probably resolver in translator-source should be enough..
		if (uriResolver==null)
		    throw new IllegalArgumentException("URIResolver is required.");
		// If not xslt or fop URL no result without errors..
		if (xsltClassBaseURL==null)
            throw new IllegalArgumentException("xsltBaseURL is required.");
		if (fopURL==null)
            throw new IllegalArgumentException("fopURL is required.");

		try {
			this.fopFactory = FopFactory.newInstance();
			this.fopFactory.setURIResolver(uriResolver);
			this.fopFactory.setBaseURL(TranslatorJarURIContext.getXSLTBaseURI());
			this.fopFactory.getFontManager().setFontBaseURL(TranslatorJarURIContext.getFontBaseURI());
			this.fopFactory.setUserConfig(selectFOPUserConfiguration(fopURL, "userconfig", "xml", (locale==null ? Locale.getDefault() : locale), uriResolver));
			this.userAgent = createFOUserAgent(uriResolver);
			this.userAgent.getRendererOptions().put("pageSetting", pageSetting);

			String xsltFileBaseURI = XSLTFileBaseUtil.userCustomizedURI(groupID, appID);
			this.xslTranslator = new XSLTranslator(new String[] { xsltFileBaseURI, xsltClassBaseURL},
					getXSLTScripts(), createParameterArrayMap(pageSetting, selectedLocale, selectedLocaleSuffix));

			this.executorServiceForXSLT = Executors.newSingleThreadExecutor();
			this.executorServiceForFOP = Executors.newSingleThreadExecutor();
			this.exceptions = new Exception[]{null, null};
		} catch (ConfigurationException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		} catch (TransformerException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		} catch (SAXException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		}
	}
	
	public void cancel(){
		if (this.userAgent != null) {
			try{
				this.userAgent.getRendererOverride().stopRenderer();
				shutdown();
			}catch(IOException ignore){
				ignore.printStackTrace();
			}
		}
	}
	
	public void shutdown(){
		if(! this.executorServiceForFOP.isTerminated() && ! this.executorServiceForFOP.isShutdown()){
			this.executorServiceForFOP.shutdown();
		}
		if(! this.executorServiceForXSLT.isTerminated() && ! this.executorServiceForXSLT.isShutdown()){
			this.executorServiceForXSLT.shutdown();
		}
	}

	private Configuration selectFOPUserConfiguration(String fopBaseURL, String basename, String suffix, Locale locale, URIResolver uriResolver)
			throws IOException, SAXException, ConfigurationException, TransformerException {
	
		DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
		Configuration cfg = null;
	
		for(Locale loc : new Locale[]{locale, Locale.ENGLISH}){
			if (loc!=null)
			{
			    BundleNames names = BundleNames.calculateBundleNames("", loc);
			    for(String name : names.names) {
			        String fopConfigFilePath = basename+name+"."+suffix;
			        try{
			        	cfg = createFOPUserConfiguration(cfgBuilder, fopBaseURL, fopConfigFilePath, uriResolver);
	                    selectedLocale = loc;
	                    selectedLocaleSuffix = name;
	                    return cfg;
	                }catch(Exception ignore){}
			    }
			}
		    
		}
		throw new ConfigurationException("not found: userconfig file");
	}

	private Configuration createFOPUserConfiguration(DefaultConfigurationBuilder cfgBuilder, String fopBaseURL, String fopConfigFilePath, URIResolver uriResolver) throws IOException, SAXException, ConfigurationException, TransformerException {
	    Source source = uriResolver.resolve(fopConfigFilePath, fopBaseURL);
	    InputStream in = source instanceof StreamSource ? ((StreamSource) source).getInputStream() : null ;
	    if (in == null) {
	    	URL fopConfigURL = new URL(fopBaseURL+fopConfigFilePath);
	        in = fopConfigURL.openStream();
		} 
		Configuration cfg = cfgBuilder.build(in);
		in.close();
		return cfg;
	}

	private FOUserAgent createFOUserAgent(URIResolver uriResolver) {
		FOUserAgent userAgent;
		userAgent = this.fopFactory.newFOUserAgent();
		userAgent.setProducer("SQS Translator");
		userAgent.setCreator("SQS Translator");// TODO FOP: CreatorInfo from sqs document xpath /html/head/meta
		userAgent.setAuthor("SQS User");// TODO FOP: AuthorInfo from sqs document xpath /html/head/meta
		userAgent.setCreationDate(new Date());
		userAgent.setTitle("SQS OMR From"); // TODO FOP: Title from sqs document xpath /html/head/title
		userAgent.setKeywords("SQS XML XSL-FO");
		userAgent.setURIResolver(uriResolver);
		userAgent.setBaseURL(TranslatorJarURIContext.getXSLTBaseURI());
		return userAgent;
	}

	private Fop createFop(FOUserAgent userAgent, String outputFormat) throws FOPException {
		return this.fopFactory.newFop(outputFormat, userAgent);
	}

	private Fop createFop(OutputStream pdfOutputStream) throws FOPException {
		return this.fopFactory.newFop(MimeConstants.MIME_PDF, this.userAgent, pdfOutputStream);
	}

	private Fop createFopAreaTree(OutputStream areaTreeOutputStream) throws FOPException {
		return this.fopFactory.newFop(MimeConstants.MIME_FOP_AREA_TREE, this.userAgent,
				areaTreeOutputStream);
	}

	private void xslTranslate(StreamTranslatorSource sqsSource, OutputStream foOutputStream) throws TranslatorException, IOException {
		// Note: called by FOPTranslator#createFOBytes
	    this.xslTranslator.translate(sqsSource, foOutputStream);
	}

	private Document xslTranslate(DOMTranslatorSource sqsSource) throws TranslatorException, IOException {
	    return this.xslTranslator.translate(sqsSource);
	}

	public String[] getXSLTScripts(){
		return new String[]{};
	}

	Map<String, ParamEntry[]> createParameterArrayMap(PageSetting pageSetting, Locale selectedLocale, String selectedLocaleSuffix){
		Map<String, ParamEntry[]> ret = new HashMap<String, ParamEntry[]>();
		return ret;
	}
	
	private void translateToPDF(final StreamTranslatorSource xmlSource, final OutputStream pdfOutputStream) throws TranslatorException,IOException {
		try {
            final byte[] xmlSourceBytes = IOUtils.toByteArray(xmlSource.getInputStream());
            IOUtils.closeQuietly(xmlSource.getInputStream());
            
            final StreamTranslatorSourceBean cachedXMLSource = new StreamTranslatorSourceBean(xmlSource.getUriResolver());
            cachedXMLSource.setInputStream(new ByteArrayInputStream(xmlSourceBytes));
            cachedXMLSource.setSystemId(xmlSource.getSystemId());

            final PipedOutputStream foOutputStream = new PipedOutputStream();
            final PipedInputStream foInputStream = new PipedInputStream(foOutputStream);
            
            executorServiceForFOP.submit(new Runnable(){
            	public void run(){
            		try{
            			translateFOtoPDF(xmlSourceBytes, foInputStream, xmlSource.getSystemId(), pdfOutputStream);
            		}catch(Exception ex){
            			exceptions[1] = ex;
            		}finally{
            			IOUtils.closeQuietly(foInputStream);
            			IOUtils.closeQuietly(pdfOutputStream);
            			executorServiceForFOP.shutdown();
            		}
            	}
            });

            executorServiceForXSLT.execute(new Runnable(){
            	public void run(){
            		try{
            			xslTranslate(cachedXMLSource, foOutputStream);
            		}catch(Exception ex){
            			exceptions[0] = ex;
            		}finally{
            			IOUtils.closeQuietly(cachedXMLSource.getInputStream());
                        IOUtils.closeQuietly(foOutputStream);
                        executorServiceForXSLT.shutdown();
            		}
            	}
            });
            
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		}
	}

	public void awaitTermination()throws TranslatorException{
		try{
			this.executorServiceForXSLT.awaitTermination(120, TimeUnit.SECONDS);
			this.executorServiceForFOP.awaitTermination(120, TimeUnit.SECONDS);
			if(exceptions[0] == null && exceptions[1] == null){
				return;
			}else{
				throw new TranslatorException("ERROR");
			}
		}catch(InterruptedException ignore){
			ignore.printStackTrace();
			throw new TranslatorException("TIMEOUT");
		}
	}

	@Override
	public void translate(final StreamTranslatorSource xmlSource, final OutputStream pdfOutputStream) throws TranslatorException,IOException {
		translateToPDF(xmlSource, pdfOutputStream);
		awaitTermination();
	}

	@Override
	public InputStream translate(final StreamTranslatorSource xmlSource) throws TranslatorException, IOException {
		final PipedOutputStream pdfOutputStream = new PipedOutputStream();
		final PipedInputStream pdfInputStream = new PipedInputStream(pdfOutputStream);
		translateToPDF(xmlSource, pdfOutputStream);
		return pdfInputStream;
	}

	private void translateFOtoPDF(byte[] sqsSourceBytes, InputStream foInputStream, String systemId, OutputStream pdfOutputStream) throws TranslatorException {
		try {
			this.userAgent.setBaseURL(systemId);

			ByteArrayOutputStream pdfRawDataOutputStream = new ByteArrayOutputStream(65536);
			Fop fop = createFop(pdfRawDataOutputStream);

			// Setup JAXP using identity transformer
			TransformerFactory xslTransformerFactory = TransformerFactory.newInstance();
			xslTransformerFactory.setURIResolver(this.userAgent.getURIResolver());
			Transformer xslTransformer = xslTransformerFactory.newTransformer();
			
			// Resulting SAX events (the generated FO) must be piped through to
			// FOP
			Result fopResult = new SAXResult(fop.getDefaultHandler());
			// Start XSLT transformation and FO processing
			
			StreamSource foInputSource = new StreamSource(foInputStream, systemId); 

			xslTransformer.transform(foInputSource, fopResult);
			
			pdfRawDataOutputStream.flush();
			byte[] pdfRawDataBytes = pdfRawDataOutputStream.toByteArray();

			foInputStream.close();
			foInputStream = null;
			
			combinePDFData(this.userAgent, sqsSourceBytes, pdfRawDataBytes, fop.getResults().getPageCount(), pdfOutputStream);

			pdfRawDataOutputStream.close();
			pdfRawDataOutputStream = null;
			pdfOutputStream.flush();

		} catch (TransformerException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		} catch (DocumentException ex) {
			ex.printStackTrace();
		} catch (FOPException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		}
	}

	private void translateFOtoPDF(byte[] sqsSourceBytes, SAXSource foSAXSource, OutputStream pdfOutputStream) throws TranslatorException {
		try {

			ByteArrayOutputStream pdfRawDataOutputStream = new ByteArrayOutputStream(65536);
			Fop fop = createFop(pdfRawDataOutputStream);

			// Setup JAXP using identity transformer
			TransformerFactory xslTransformerFactory = TransformerFactory.newInstance();
			xslTransformerFactory.setURIResolver(this.userAgent.getURIResolver());
			Transformer xslTransformer = xslTransformerFactory.newTransformer();
			
			// Setup input stream
			//Source foInputSource = new StreamSource(foInputStream, systemId);
			
			// Resulting SAX events (the generated FO) must be piped through to
			// FOP
			Result fopResult = new SAXResult(fop.getDefaultHandler());
			// Start XSLT transformation and FO processing
			
			//this.userAgent.setBaseURL(foSAXSource.getSystemId());
			xslTransformer.transform(foSAXSource, fopResult);

			pdfRawDataOutputStream.flush();
			byte[] pdfRawDataBytes = pdfRawDataOutputStream.toByteArray();

			combinePDFData(this.userAgent, sqsSourceBytes, pdfRawDataBytes, fop.getResults().getPageCount(), pdfOutputStream);

			pdfRawDataOutputStream.close();
			pdfRawDataOutputStream = null;
			pdfOutputStream.flush();

		} catch (TransformerException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		} catch (DocumentException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		} catch (FOPException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		} catch (IOException ex) {
			ex.printStackTrace();
			throw new TranslatorException(ex);
		}
	}
	
	protected void combinePDFData(FOUserAgent userAgent, byte[] pdfRawDataBytes, FileAttachmentBean[] fileAttachmentBeans, OutputStream pdfOutputStream) throws IOException,DocumentException{
		PdfReader reader = new PdfReader(pdfRawDataBytes);
		PdfStamper stamp = new PdfStamper(reader, pdfOutputStream);
		for(FileAttachmentBean fileAttachment: fileAttachmentBeans){
			stamp.addFileAttachment(fileAttachment.title, fileAttachment.bytes, null, fileAttachment.filename);
		}
		stamp.close();
		reader.close();
	}

	 protected void combinePDFData(FOUserAgent userAgent, byte[] sqsSourceBytes, byte[] pdfRawDataBytes, int numPages, OutputStream pdfOutputStream)throws IOException, DocumentException{
		 // do nothing by default
	 }

}
