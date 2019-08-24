/*

 SQSToPDFTranslatorAPITest.java
 
 Copyright 2012 KUBO Hiroya (hiroya@cuc.ac.jp).
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package net.sqs2.translator.impl;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import javax.xml.transform.URIResolver;

import net.sqs2.barcode.support.BarcodeURIResolver;
import net.sqs2.net.ClassURIResolver;
import net.sqs2.net.ClassURLStreamHandlerFactory;
import net.sqs2.translator.StreamTranslatorSource;
import net.sqs2.translator.StreamTranslatorSourceBean;
import net.sqs2.translator.TranslatorException;
import net.sqs2.translator.facade.AbstractTestSQS2PDF;
import net.sqs2.xslt.CachedURIResolver;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SQSToPDFTranslatorAPITest {

	@BeforeClass
	public void setUp(){
		// FIXME: ad-hoc URL.setURLStreamHandlerFactory to prevent throwing MalformedURLException. at /fop-formgenerator/src/java/org/apache/fop/fo/extensions/svg/SVGElement.java 
		try{
			URL.setURLStreamHandlerFactory(ClassURLStreamHandlerFactory.getSingleton());
		}catch(Error ignore){}
	}
	
	
	@Test
    public void testAPI_Error01() throws TranslatorException
    {
        String fopURL = null;
        String xsltURL = null;
        // No name here, for this is the errror
        String name = null;
        URIResolver uriResolver = null;
        PageSetting pageSetting = new PageSettingImpl(400, 400);
        SQSToPDFTranslator t = null;
        try
        {
        t = new SQSToPDFTranslator("mygroup",
            "myapp",
            fopURL,
            xsltURL,
            Locale.getDefault(),
            name,
            uriResolver,
            pageSetting);
            fail("Should throw ill arg");
        }
        catch(IllegalArgumentException e) {
        	assertNull(t);
        }
    }

	@Test
    public void testAPI_ok02() throws TranslatorException
    {
        String fopURL = null;
        String xsltURL = null;
        // Here there is name
        String name = "myname";
        URIResolver uriResolver = null;
        // Here no page setting, but now is not anymore error
        PageSetting pageSetting = null;
        SQSToPDFTranslator t = new SQSToPDFTranslator("mygroup",
            "myapp",
            fopURL,
            xsltURL,
            Locale.getDefault(),
            name,
            uriResolver,
            pageSetting);
        
        assertNotNull(t);
    }
	
	@Test
    public void testAPI_Ok02() throws TranslatorException
    {
        String fopURL = null;
        String xsltURL = null;
        // Here there is name
        String name = "myname";
        URIResolver uriResolver = null;
        // Here also page setting, NO error
        PageSetting pageSetting = new PageSettingImpl(500, 400);
        SQSToPDFTranslator t = new SQSToPDFTranslator("mygroup",
            "myapp",
            fopURL,
            xsltURL,
            Locale.getDefault(),
            name,
            uriResolver,
            pageSetting);
            
        assertNotNull(t);
    }
	
	//@Test
    public void testAPI_ok_oldway() throws TranslatorException, IOException
    {
        // These are the minimal parameters for the previous impl:
        // fopURL, xsltURL, pageSetting, URIResolver,filename
        final String fopURL = "class://net.sqs2.translator.impl.TranslatorJarURIContext/fop/";
        final String xsltURL = "class://net.sqs2.translator.impl.TranslatorJarURIContext/xslt/";
        final PageSetting pageSetting = new PageSettingImpl(500, 400);
        final URIResolver uriResolver = new ClassURIResolver();
        final SQSToPDFTranslator t = new SQSToPDFTranslator("mygroup",
            "myapp",
            fopURL,
            xsltURL,
            Locale.getDefault(),
            "myfilename",
            uriResolver,
            pageSetting);
        
        StreamTranslatorSource source = createStreamTranslatorSource();
        InputStream is =  t.translate(source);  
        String s = IOUtils.toString(is);
        is.close();
        assertNotNull(s);
        assertTrue(s.length() > 0);//"No output content !"
        assertTrue(s.startsWith("%PDF-1.4"));
    }
	
	@Test
    public void testAPI_ok_newway() throws TranslatorException, IOException
    {
        // Now there are some defaults
        final String fopURL = null; // NOT NEEDED "class://net.sqs2.translator.impl.TranslatorJarURIContext/fop/";
        final String xsltURL = null; // NOT NEEDED "class://net.sqs2.translator.impl.TranslatorJarURIContext/xslt/";
        final PageSetting pageSetting = null;
        final URIResolver uriResolver = null; // NOT NEEDED new ClassURIResolver();
        final SQSToPDFTranslator t = new SQSToPDFTranslator("mygroup",
            "myapp",
            fopURL,
            xsltURL,
            Locale.ITALIAN,
            "myfilename",
            uriResolver,
            pageSetting);
        
        StreamTranslatorSource source = createStreamTranslatorSource();
        InputStream is =  t.translate(source);  
        String s = IOUtils.toString(is);
        is.close();
        assertNotNull(s);
        assertTrue(s.length() > 0);//"No output content !",
        assertTrue(s.startsWith("%PDF-1.4"));
    }
	
	@Test
    public void testAPI_ok_simplest_new() throws TranslatorException, IOException
    {
        final SQSToPDFTranslator t = new SQSToPDFTranslator("mygroup",
            "myapp",
            "myfilename");
        
        StreamTranslatorSource streamTranslatorSource = createStreamTranslatorSource();
        InputStream is =  t.translate(streamTranslatorSource);  
        String s = IOUtils.toString(is);
        is.close();
        assertNotNull(s);
        assertTrue(s.length() > 0);//"No output content !",
        assertTrue(s.startsWith("%PDF-1.4"));
    }
	
	private URIResolver createURIResolver(){
		return new BarcodeURIResolver(new CachedURIResolver(new ClassURIResolver()));
	}

    private StreamTranslatorSource createStreamTranslatorSource()
    {
        // Use class of other package..
    	String filename = "DomandeXML.sqs.xml";
        InputStream is = AbstractTestSQS2PDF.class.getResourceAsStream(filename);
        StreamTranslatorSourceBean source = new StreamTranslatorSourceBean();
        source.setInputStream(is);
        source.setUriResolver(createURIResolver());
        source.setSystemId("class://"+ AbstractTestSQS2PDF.class.getName()+"/net/sqs2/translator/facade/"+filename);
        return source;
    }

    private SQSToPDFTranslator createSQSToPDFTranslator() throws TranslatorException
    {
        String fopURL = null;
        String xsltURL = null;
        String name = null;
        URIResolver uriResolver = null;
        PageSetting pageSetting = null;
        SQSToPDFTranslator t = new SQSToPDFTranslator("mygroup",
            "myapp",
            fopURL,
            xsltURL,
            Locale.ITALIAN,
            "myfilename",
            uriResolver,
            pageSetting);
        return t;
    }
}
