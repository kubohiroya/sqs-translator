/*

 SQSToPDFTranslatorTest.java

 
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

import static org.testng.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Locale;

import javax.xml.transform.URIResolver;

import net.sqs2.net.ClassURLStreamHandler;
import net.sqs2.net.ClassURLStreamHandlerFactory;
import net.sqs2.translator.StreamTranslatorSourceBean;
import net.sqs2.translator.TranslatorException;
import net.sqs2.xml.XMLUtil;
import net.sqs2.xml.XPathUtil;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

public class SQSToPDFTranslatorTest{

	@BeforeClass
	public void setUp(){
		// FIXME: ad-hoc URL.setURLStreamHandlerFactory to prevent throwing MalformedURLException. at /fop-formgenerator/src/java/org/apache/fop/fo/extensions/svg/SVGElement.java 
		try{
			URL.setURLStreamHandlerFactory(ClassURLStreamHandlerFactory.getSingleton());
		}catch(Error ignore){}
	}
	
	private static SQSToPDFTranslator createSQSToPDFTranslator(URIResolver uriResolver, PageSetting pageSetting, Locale locale)throws TranslatorException{
		return createSQSToPDFTranslator("sqs","SourceEditor", locale, uriResolver, pageSetting);
	}
		
	private static SQSToPDFTranslator createSQSToPDFTranslator(String groupID, String appID, Locale locale, URIResolver uriResolver, PageSetting pageSetting)throws TranslatorException{
		return new SQSToPDFTranslator(groupID, appID, 
				TranslatorJarURIContext.getFOPBaseURI(), 
				TranslatorJarURIContext.getXSLTBaseURI(), 
				locale,
				"test",
				uriResolver,
				pageSetting);
	}
	
	static URL createFileURL(String path, URLStreamHandler urlStreamHandler)throws MalformedURLException{
		return new URL("file", "", -1, path, urlStreamHandler);
	}
	
	static URL createResourceURL(Class<?> clazz, String path, URLStreamHandler urlStreamHandler)throws MalformedURLException{
		return new URL("class", clazz.getName(), -1, path, urlStreamHandler);
	}
	
	static abstract class SQStoPDFtoSQMDocumentScenario{
		SQSToPDFTranslator sqsToPdfTranslator;
		StreamTranslatorSourceBean streamTranslatorSource;
				
		SQStoPDFtoSQMDocumentScenario(URIResolver uriResolver, PageSetting pageSetting, Locale locale, URL url)throws Exception{
			this.sqsToPdfTranslator = createSQSToPDFTranslator(uriResolver, pageSetting, locale);
			InputStream sqsSourceInputStream = new BufferedInputStream(url.openStream());
			final String systemId = url.toString();
			this.streamTranslatorSource = new StreamTranslatorSourceBean(uriResolver);
			this.streamTranslatorSource.setInputStream(sqsSourceInputStream);
			this.streamTranslatorSource.setSystemId(systemId);
		}

		public abstract Document createResultSQMDocument()throws Exception;
		
		public Document createResultSQMDocument(InputStream pdfInputStream)throws Exception{
			byte[] sqmBytes = new PDFAttachmentReader(pdfInputStream).extractAttachmentFiles(".sqm");
			return XMLUtil.createDocumentBuilder().parse(new ByteArrayInputStream(sqmBytes));
		}
	}

	private SQStoPDFtoSQMDocumentScenario createSQStoPDFtoSQMDocumentScenarioWithPredefinedOutputStream(
			URIResolver uriResolver, PageSetting pageSetting, Locale locale, URL url) throws Exception {
		SQStoPDFtoSQMDocumentScenario scenario = new SQStoPDFtoSQMDocumentScenario(uriResolver, pageSetting, locale, url){
			public Document createResultSQMDocument()throws Exception{
				File pdfFile = File.createTempFile("sqs", "pdf");
				pdfFile.deleteOnExit();
				BufferedOutputStream pdfOutputStream = new BufferedOutputStream(new FileOutputStream(pdfFile));
				sqsToPdfTranslator.translate(streamTranslatorSource, pdfOutputStream);
				InputStream pdfInputStream = new BufferedInputStream(new FileInputStream(pdfFile));
				return createResultSQMDocument(pdfInputStream);
			}
		};
		return scenario;
	}

	private SQStoPDFtoSQMDocumentScenario createSQStoPDFtoSQMDocumentScenarioWithNewPipedInputStream(
			URIResolver uriResolver, PageSetting pageSetting, Locale locale, URL url) throws Exception {
		SQStoPDFtoSQMDocumentScenario scenario = new SQStoPDFtoSQMDocumentScenario(uriResolver, pageSetting, locale, url){
			public Document createResultSQMDocument()throws Exception{
				InputStream pdfInputStream = sqsToPdfTranslator.translate(streamTranslatorSource);
				return createResultSQMDocument(pdfInputStream);
			}
		};
		return scenario;
	}
	
	private void assertSQMDocument(Document resultSQMDocument) {
		assertEquals("2.1.0", XPathUtil.getStringValue(resultSQMDocument.getDocumentElement(), 
				"/svg:svg/svg:pageSet/svg:masterPage/svg:metadata/master:master/@master:version"));
		assertEquals("5.0", XPathUtil.getStringValue(resultSQMDocument.getDocumentElement(),
				"/svg:svg/svg:pageSet/svg:page[1]/svg:g/svg:rect/@width"));
		assertEquals("16.0", XPathUtil.getStringValue(resultSQMDocument.getDocumentElement(),
				"/svg:svg/svg:pageSet/svg:page[1]/svg:g/svg:rect/@height"));
	}

	@Test
	public void testTranslateFileIntoNewPipedInputStream() throws Exception{
		SQStoPDFtoSQMDocumentScenario scenario = createSQStoPDFtoSQMDocumentScenarioWithNewPipedInputStream(
				SQSToPDFTranslator.createDefaultURIResolver(),
				PageSettingImpl.getA4(),
				Locale.JAPAN, 
				createFileURL("src/test/resources/simple_ja.sqs", new ClassURLStreamHandler())
				);

		assertSQMDocument(scenario.createResultSQMDocument());
	}

	@Test
	public void testTranslateFileIntoPredefinedOutputStream() throws Exception{
		SQStoPDFtoSQMDocumentScenario scenario = createSQStoPDFtoSQMDocumentScenarioWithPredefinedOutputStream(
				SQSToPDFTranslator.createDefaultURIResolver(),
				PageSettingImpl.getA4(),
				Locale.JAPAN, 
				createFileURL("src/test/resources/simple_ja.sqs", new ClassURLStreamHandler())
				);

		assertSQMDocument(scenario.createResultSQMDocument());
	}	

	@Test
	public void testTranslateResourceIntoNewPipedInputStream() throws Exception{
		
		SQStoPDFtoSQMDocumentScenario scenario = createSQStoPDFtoSQMDocumentScenarioWithNewPipedInputStream(
				SQSToPDFTranslator.createDefaultURIResolver(),
				PageSettingImpl.getA4(),
				Locale.ENGLISH,
				createResourceURL(TranslatorJarURIContext.class, "/simple_en.sqs", new ClassURLStreamHandler())
				);

		assertSQMDocument(scenario.createResultSQMDocument());
	}

		
}
