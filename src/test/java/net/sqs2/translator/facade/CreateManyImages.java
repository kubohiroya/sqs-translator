/**
 * 
 */
package net.sqs2.translator.facade;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

/**
 * this program creates many images 
 * from three images present in a folder,
 * this is not a test.
 * 
 *
 */
public abstract class CreateManyImages 
{
    
    

    private static final int TOT = 30;

    public static void main(String[] args) throws IOException
    {
        File file = new File("C:\\mik2010\\Infordata2012\\SQS aLL\\test_final_1");
        final ArrayList list = new ArrayList();
        File img1 = new File(file, "xx_0000.jpg");
        File img2 = new File(file, "xx_0001.jpg");
        File img3 = new File(file, "xx_0003.jpg");
        list.add(img1);
        list.add(img2);
        list.add(img3);
        
        for (int i = 0;i<TOT;i++) {
            File out = new File(file, "xx_GEN" + i + ".jpg");
            int pos = (int) (Math.random() * 1000);
            FileUtils.copyFile((File) list.get(pos % list.size()), out);
        }
    }

}