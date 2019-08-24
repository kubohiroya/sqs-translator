/**
 * 
 */
package net.sqs2.translator.facade;

import java.io.File;
import java.util.Locale;

import org.testng.annotations.Test;

public class TestSQS2PDF_XML extends AbstractTestSQS2PDF
{
    
    @Test
    public void testSQS2PDF_XML() throws Exception
    {
        final SQS2PDF sqs2pdf = getSQS2PDF("DomandeXML.sqs.xml", Locale.ITALY);
        final File pdfFile = createTemporaryFile(".pdf", false);
        
        System.out.println("Output file is " + pdfFile);
        final SQSOutput out = new SQSOutput(pdfFile);
        sqs2pdf.setLocale(java.util.Locale.ITALIAN);
        out.setParameter("qr-code-text", "Questo e' il valore del mio qrcode..");
        sqs2pdf.translate(out);
        showDocument(pdfFile);
        
    }

    

}