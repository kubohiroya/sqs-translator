/**
 * 
 */
package net.sqs2.translator.facade;

import java.io.File;
import java.util.Locale;

import org.testng.annotations.Test;

public class TestSQS2PDF_Unicode extends AbstractTestSQS2PDF
{
    
    @Test
    public void testSQS2PDF_Unicode() throws Exception
    {
        final SQS2PDF sqs2pdf = getSQS2PDF("unicode.sqs.xml", Locale.ITALY);
        final File pdfFile = createTemporaryFile(".pdf", false);
        System.out.println("Output file is " + pdfFile);
        final SQSOutput out = new SQSOutput(pdfFile);
        sqs2pdf.translate(out);
        showDocument(pdfFile);
        
    }

    

}