/**
 * 
 */
package net.sqs2.translator.facade;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Locale;

import javax.xml.transform.URIResolver;

import net.sqs2.barcode.support.BarcodeURIResolver;
import net.sqs2.net.ClassURIResolver;
import net.sqs2.translator.StreamTranslatorSourceBean;
import net.sqs2.translator.Translator;
import net.sqs2.translator.TranslatorException;
import net.sqs2.translator.impl.PageSetting;
import net.sqs2.translator.impl.PageSettingImpl;
import net.sqs2.translator.impl.SQSToHTMLTranslator;
import net.sqs2.translator.impl.SQSToPDFTranslator;
import net.sqs2.translator.impl.TranslatorJarURIContext;
import net.sqs2.util.FileUtil;
import net.sqs2.xslt.CachedURIResolver;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;

/**
 * A Facade for printing a PDF from a SQS file.
 * <p>
 * Usage:
 * 
 * <pre class='code'>
 * InputSource is = new InputSource("file:/c:/questionary.sqs");
 * SQS2PDF sqs2pdf = new SQS2PDF();
 * sqs2pdf.init(is);
 * SQSOutput out = new SQSOutput(new File("file:/c:/out.pdf"));
 * sqs2pdf.translate(out);          
 * </pre>
 * <p>In order to add parameters for each print:
 * <pre class='code'>
 * InputSource is = new InputSource("file:/c:/questionary.sqs");
 * SQS2PDF sqs2pdf = new SQS2PDF();
 * sqs2pdf.init(is);
 * for (File file : files) {
 *  SQSOutput out = new SQSOutput(file);
 *  sqs2pdf.setParameter("questionary-identifier", file.getName());
 *  sqs2pdf.translate(out);          
 * }
 * </pre>
 * <p>
 */
public class SQS2PDF
{
    static
    {
        // URL.setURLStreamHandlerFactory(ClassURLStreamHandlerFactory.getSingleton());

    }

    static abstract class TranslatorFactory
    {
        /**
         * 
         * @param groupID
         * @param appID
         * @param fopURL
         * @param xsltURL
         * @param name
         * @param uriResolver
         * @param pageSetting
         * @param language a 2 chars language code, never null or an empty string.
         * @return
         * @throws TranslatorException
         */
        public abstract Translator createTranslator(String groupID,
            String appID,
            String fopURL,
            String xsltURL,
            Locale locale,
            String name,
            URIResolver uriResolver,
            PageSetting pageSetting, 
            String language) throws TranslatorException;

    }

    static TranslatorFactory HTML = new TranslatorFactory()
    {

        public Translator createTranslator(String groupID,
            String appID,
            String fopURL,
            String xsltURL,
            Locale locale,
            String name,
            URIResolver uriResolver,
            PageSetting pageSetting, String language) throws TranslatorException
        {
            return new SQSToHTMLTranslator(TranslatorJarURIContext.getXSLTBaseURI());
        }

    };
    static TranslatorFactory PDF = new TranslatorFactory()
    {

        public Translator createTranslator(String groupID,
            String appID,
            String fopURL,
            String xsltURL,
            Locale locale,
            String name,
            URIResolver uriResolver,
            PageSetting pageSetting, String language) throws TranslatorException
        {
            return new SQSToPDFTranslator(groupID,
                appID,
                fopURL,
                xsltURL,
                locale,
                name,
                uriResolver,
                pageSetting);
        }

    };

    protected final TranslatorFactory getTranslatorFactory()
    {
        return PDF;
    }

    private PageSetting pageSetting = PageSettingImpl.getA4();
    private URIResolver uriResolver = null;
    private String groupID = "sqs";
    private String appID = "SQS2PDF_1.0";
    private String fopURL = "class://net.sqs2.translator.impl.TranslatorJarURIContext/fop/";
    private String xsltURL = "class://net.sqs2.translator.impl.TranslatorJarURIContext/xslt/";
    private boolean initialized = false;
    private byte[] sqs;
    private String systemId;
    private Locale locale;

    // private TranslatorFactory translatorFactory = new
    // HTMLTranslatorFactory();
    /**
     * No-args constructor.
     */
    public SQS2PDF()
    {
        super();
    }

    /**
     * Initializes this {@link SQS2PDF} instance with an SQS {@link InputSource}
     * .
     * 
     * @param sqsInputSource A {@link InputSource}, never <code>null</code>.
     * @throws IOException if something goes wrong retrieving data from the
     *             source.
     */
    public void init(final InputSource sqsInputSource) throws IOException
    {
        if (sqsInputSource == null)
            throw new IllegalArgumentException("null InputSource (.sqs)");
        if (!initialized)
        {
            if (uriResolver == null) uriResolver = new ClassURIResolver();
            initialized = true;
        }
        final InputStream byteStream;
        final InputStream sourceStream = sqsInputSource.getByteStream();
        this.systemId = sqsInputSource.getSystemId();
        if (sourceStream == null)
        {
            if (systemId == null) {
                final Reader reader = sqsInputSource.getCharacterStream();
                if (reader!=null) {
                    this.sqs = IOUtils.toString(reader).getBytes("UTF-8");
                    // Not nice but..
                    return;
                }
                else throw new IllegalArgumentException("no stream and no system-id in input source");
            }
            else
                byteStream = new URL(systemId).openStream();
        }
        else
        {
            byteStream = sourceStream;
        }

        try
        {
            this.sqs = IOUtils.toByteArray(byteStream);
        }
        finally
        {
            IOUtils.closeQuietly(byteStream);
        }
    }

    /**
     * Main translation method, outputs the PDF result of translation to the
     * passed {@link SQSOutput}.
     * 
     * @param os A {@link SQSOutput}, never <code>null</code>.
     * @param name A {@link String}, never <code>null</code>.
     * @throws TranslatorException
     * @throws IOException
     */
    public void translate(final SQSOutput out) throws TranslatorException,
                                              IOException
    {
        if (!initialized)
            throw new IllegalStateException("Must call init() before.");

        final String name = out.getName();
        if (locale==null) locale = Locale.ENGLISH;// Locale.getDefault();
        final String language = locale.getLanguage();
        final Translator translator = getTranslatorFactory().createTranslator(groupID,
            appID,
            fopURL,
            xsltURL,
            locale,
            name,
            uriResolver,
            pageSetting, 
            language);

        final InputStream sqsInputStream = new ByteArrayInputStream(sqs);
        final StreamTranslatorSourceBean sourceBean = new StreamTranslatorSourceBean(this.uriResolver);
        sourceBean.setInputStream(sqsInputStream);
        sourceBean.setSystemId(this.systemId);
        sourceBean.putParameters(out.getParameters());
        OutputStream os = null;
        try
        {
            os = out.getOutputStream();
            translator.translate(sourceBean,
                os); // TODO FIXME HIGH out.getParameters()
        }
        finally
        {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(sqsInputStream);
        }

    }

    // //////////////////// CONFIGURATION

    /**
     * Configuration setter, use this method to optionally configure a
     * {@link PageSetting}. The default one is 595x842
     * (PageSettingImpl(595,842))
     * 
     * @param pageSetting A {@link String}, never <code>null</code>.
     */
    public final void setPageSetting(PageSetting pageSetting)
    {
        if (pageSetting == null)
            throw new IllegalArgumentException("null pageSetting");
        this.pageSetting = pageSetting;
    }
    
    /**
     * Configuration setter, use this method to optionally configure a
     * printing language, the default is the system default locale.
     */
    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    /**
     * Configuration setter, use this method to optionally configure a
     * {@link URIResolver}. The default one is {@link ClassURIResolver}.
     * 
     * @param uriResolver A {@link URIResolver}, never <code>null</code>.
     * 
     */
    public final void setUriResolver(URIResolver uriResolver)
    {
        if (uriResolver == null)
            throw new IllegalArgumentException("null uriResolver");
        this.uriResolver = uriResolver;
    }

    /**
     * Configuration setter, use this method to optionally configure the
     * Group-Id. The default is <code>sqs</code>.
     * 
     * @param groupID A {@link String}, never <code>null</code>.
     */
    public final void setGroupID(String groupID)
    {
        if (groupID == null)
            throw new IllegalArgumentException("null groupId");
        this.groupID = groupID;
    }

    public final void setFopURL(String fopURL)
    {

        if (fopURL == null) throw new IllegalArgumentException("null url");
        this.fopURL = fopURL;
    }

    public final void setXsltURL(String xsltURL)
    {
        if (xsltURL == null) throw new IllegalArgumentException("null url");
        this.xsltURL = xsltURL;
    }
    
    public static final Option COMMAND_LINE_OPTION_OF_NO_GUI = new Option("t", "translator", true, "translate source to PDF in no gui mode");
    public static final Option COMMAND_LINE_OPTION_OF_XML_INPUT = new Option("i", "xml", true, "xml input file");
    public static final Option COMMAND_LINE_OPTION_OF_PDF_OUTPUT = new Option("o", "pdf", true, "pdf output file");
    public static final Option COMMAND_LINE_OPTION_OF_XSLT = new Option("t", "xsl", true, "xslt script file translating xml into fo");
    
    public static void main(String args[])throws Exception{

		String sqsFilename = null;
		String pdfFilename = null;
    	
        	Options options = new Options();
        	options.addOption(COMMAND_LINE_OPTION_OF_NO_GUI);
        	options.addOption(COMMAND_LINE_OPTION_OF_XML_INPUT);
        	options.addOption(COMMAND_LINE_OPTION_OF_PDF_OUTPUT) ;
        	CommandLineParser parser = new PosixParser();
        	CommandLine commandLine = parser.parse(options, args);

        	if(commandLine.hasOption(COMMAND_LINE_OPTION_OF_XML_INPUT.getArgName())){
        		sqsFilename = commandLine.getOptionValue(COMMAND_LINE_OPTION_OF_XML_INPUT.getArgName());	
        	}
        	if(commandLine.hasOption(COMMAND_LINE_OPTION_OF_PDF_OUTPUT.getArgName())){
        		pdfFilename = commandLine.getOptionValue(COMMAND_LINE_OPTION_OF_PDF_OUTPUT.getArgName());
        	}
        	
        	if(commandLine.getArgs().length == 1){
        		if(sqsFilename == null && pdfFilename != null){
        			sqsFilename = commandLine.getArgs()[0];
        		}else if(sqsFilename != null && pdfFilename == null){
        			pdfFilename = commandLine.getArgs()[0];
        		}else if(sqsFilename == null && pdfFilename == null){
        			sqsFilename = commandLine.getArgs()[0];
        			pdfFilename = FileUtil.getBasepath(sqsFilename)+".pdf";
        		}
        	}
        	
        	if(sqsFilename == null || pdfFilename == null){
        		System.out.println("Usage: <input sqs source file> <output pdf file>");
        		return;
        	}
        	
        	InputSource sqsInputSource = new InputSource(new File(sqsFilename).toURI().toString()); 

        	SQS2PDF sqs2pdf = new SQS2PDF() ;
        	sqs2pdf.init(sqsInputSource);
            URIResolver uriResolver = new BarcodeURIResolver(new CachedURIResolver(new ClassURIResolver()));
            sqs2pdf.setUriResolver(uriResolver);
        	SQSOutput out = new SQSOutput(new File(pdfFilename));
       	 	out.setParameter("questionary-identifier", pdfFilename);
        	sqs2pdf.translate(out);   
    }
}
