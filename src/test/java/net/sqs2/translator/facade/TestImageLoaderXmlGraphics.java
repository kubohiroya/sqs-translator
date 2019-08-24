/**
 * 
 */
package net.sqs2.translator.facade;

import static junit.framework.Assert.assertEquals;

import java.util.Iterator;

import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.impl.DefaultImageContext;
import org.apache.xmlgraphics.image.loader.impl.DefaultImageSessionContext;
import org.apache.xmlgraphics.image.loader.spi.ImageImplRegistry;
import org.testng.annotations.Test;

/**
 * Tests for checking that image loader of XmlGraphics works
 * These tests work (at least on windows) only if there are the two files
 * <code>org.apache.xmlgraphics.image.loader.spi.ImageLoaderFactory</code>
 * and <code>org.apache.xmlgraphics.image.loader.spi.ImagePreloader</code>
 * in the META-INF/services in the class path, otherwise BMP and PNG 
 * are unknown image formats... not nice..
 */
public class TestImageLoaderXmlGraphics{
	@Test
    public void testRegistry() throws Exception
    {
        ImageImplRegistry reg = ImageImplRegistry.getDefaultInstance();
        // reg.registerPreloader(new PreloaderBMP());
        // reg.registerPreloader(new PreloaderJPEG());
        //reg.registerPreloader(new PreloaderImageIO());
        ImageManager imageManager = new ImageManager(new DefaultImageContext());

        
        Iterator iter = imageManager.getRegistry().getPreloaderIterator();
        while (iter.hasNext())
        {

            Object i = iter.next();
            
        }
    }
	
	@Test
    public void testContentType1() throws Exception
    {
        ImageImplRegistry reg = ImageImplRegistry.getDefaultInstance();
        ImageManager imageManager = new ImageManager(new DefaultImageContext());
        ImageSessionContext sessionContext = new DefaultImageSessionContext(imageManager.getImageContext(),
            null);

        // final String uri =
        // "file:/C:/eclipsework/sqs-translator/src/main/java/net/sqs2/translator/facade/test/qrcode.png";
        // final String uri =
        // "http://xmlgraphics.apache.org/fop/dev/svg/text.png";
        {
            final String uri = this.getClass().getResource("qrcode.png").toString();
            ImageInfo info = imageManager.getImageInfo(uri, sessionContext);
            assertEquals("image/png", info.getMimeType());
        }
    }
	
	@Test
    public void testContentType2() throws Exception
    {
        ImageImplRegistry reg = ImageImplRegistry.getDefaultInstance();
        ImageManager imageManager = new ImageManager(new DefaultImageContext());
        ImageSessionContext sessionContext = new DefaultImageSessionContext(imageManager.getImageContext(),
            null);

        {
            final String uri = this.getClass().getResource("qrcode.bmp").toString();
            ImageInfo info = imageManager.getImageInfo(uri, sessionContext);
            assertEquals("image/bmp", info.getMimeType());
        }

    }

}