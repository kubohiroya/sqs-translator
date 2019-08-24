/**
 * 
 */
package net.sqs2.translator.facade;

import java.io.File;
import java.util.Locale;

import net.sqs2.browser.Browser;

import org.testng.annotations.Test;

public class TestSQS2PDF extends AbstractTestSQS2PDF
{
    @Test
    public void testSQS2PDF() throws Exception
    {
        //ImageImplRegistry reg = ImageImplRegistry.getDefaultInstance();
        //reg.registerPreloader(new PreloaderBMP());
        //reg.registerPreloader(new PreloaderJPEG());
        //reg.registerPreloader(new PreloaderImageIO());
       
        final SQS2PDF sqs2pdf = getSQS2PDF("QUESTIONARY_withfreetext2_img.sqs", Locale.ENGLISH);
        long l0 = System.currentTimeMillis();
        int TOT = 2 ;
        for (int i = 0; i < TOT; i++)
        {
            final boolean last = i==TOT-1;
            final File pdfFile = createTemporaryFile("." + i + ".pdf", !last);
            System.out.println("Output file is " + pdfFile);
            final SQSOutput out = new SQSOutput(pdfFile);
            out.setParameter("qr-code-text", l0 + i + " Renzo Tramaglino Solari Srl.");
            sqs2pdf.translate(out);
            if (last) {
                showDocument(pdfFile);
            }
        }
        long l1 = System.currentTimeMillis();
        System.out.println("Done " + TOT + " times in " + (l1 - l0) + " msecs.");

    }
    public void testSQS2PDF_FreeTextNewWay() throws Exception
    {
        final SQS2PDF sqs2pdf = getSQS2PDF("QUESTIONARY_withfreetext_newway.sqs", Locale.ENGLISH);
        final File pdfFile = createTemporaryFile(".pdf", false);
        System.out.println("Output file is " + pdfFile);
        final SQSOutput out = new SQSOutput(pdfFile);
        sqs2pdf.translate(out);
        showDocument(pdfFile);
        
    }

    

}