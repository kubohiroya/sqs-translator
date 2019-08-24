package net.sqs2.translator.facade;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import javax.xml.transform.URIResolver;

import junit.framework.TestCase;
import net.sqs2.barcode.support.BarcodeURIResolver;
import net.sqs2.browser.Browser;
import net.sqs2.net.ClassURIResolver;
import net.sqs2.net.ClassURLStreamHandlerFactory;
import net.sqs2.xslt.CachedURIResolver;

import org.xml.sax.InputSource;

public abstract class AbstractTestSQS2PDF extends TestCase
{
	static{
		try{
			URL.setURLStreamHandlerFactory(ClassURLStreamHandlerFactory.getSingleton());
		}catch(Error ignore){}
	}
	
	
    protected static File createTemporaryFile(String suffix, boolean deleteOnExit) throws IOException
    {
        File targetFile = File.createTempFile("sqs-draft-", suffix);
        if (deleteOnExit) targetFile.deleteOnExit();
        return targetFile;
    }

    public AbstractTestSQS2PDF()
    {
        super();
    }

    protected SQS2PDF getSQS2PDF(String res, Locale locale) throws IOException
    {
        final SQS2PDF sqs2pdf = new SQS2PDF();
        final URIResolver resolver = new BarcodeURIResolver(
                new CachedURIResolver(new ClassURIResolver()));
        sqs2pdf.setUriResolver(resolver);
        final InputSource is = new InputSource(TestSQS2PDF.class.getResourceAsStream(res));
        sqs2pdf.setLocale(locale);
        sqs2pdf.init(is);
        return sqs2pdf;
    }

    public AbstractTestSQS2PDF(String name)
    {
        super(name);
    }
    
    protected final void showDocument(File file)
    {
    	if(! GraphicsEnvironment.isHeadless()){
    		Browser.showDocument(file);
    	}
    }

}