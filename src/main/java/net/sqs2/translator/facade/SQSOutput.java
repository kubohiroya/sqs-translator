/**
 * 
 */
package net.sqs2.translator.facade;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the output for {@link SQS2PDF}
 * and possibly other transformations, it contains
 * also extra parameters.
 * <p>
 * Usage:
 * 
 * <pre class='code'>
 * SQSOutput out = new SQSOutput(new File("file:/c:/out.pdf"));
 *  // or
 * SQSOutput out2 = new SQSOutput(outputStream, "MyPdf.pdf");
 * </pre>
 * 
 */
public class SQSOutput
{
    private File file;
    private OutputStream os;
    private String name;
    private final HashMap<String, String> parameters = new HashMap();

    public SQSOutput(File file)
    {
        super();
        this.file = file;
        this.name = file.getName();
    }
    public SQSOutput(OutputStream os, String name)
    {
        super();
        this.os = os;
        this.name= name;
    }
    
    
    /**
     * Returns the output stream to be used
     * to output the result of SQS transformation.
     * <p>This is:
     * <ol>
     * <li>If [OutputStream] property is set, its value.
     * <li>If [File] property is set, a FileInputStream for that file.
     * <li>A byte array output stream that is 
     * created and stored in the [OutputStreamProperty]
     * </ol>
     * 
     * @return A {@link OutputStream}, never <code>null</code>.
     */
    public final OutputStream getOutputStream() throws IOException
    {
        // Hmm... translate is closing the stream..should pass a NoCloseOutputStreamFilter  
        // in implementation of #translate(OutputStream ?)

        if (os!=null) return os;
        else if (file!=null) return new FileOutputStream(file);
        else os = new ByteArrayOutputStream();
        return os;
    }
    public final void setOutputStream(OutputStream os)
    {
        this.os = os;
    }
    public void setFile(File file)
    {
        this.file = file;
    }
    public final String getName()
    {
        return name;
    }
    public final void setName(String name)
    {
        this.name = name;
    }
    public Map<String, String> getParameters()
    {
        return new HashMap<String, String>(parameters);
    }
    public void clearParameters() {
        parameters.clear();
    }
    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }
    public void putParameters(Map<String,String> map) {
        parameters.putAll(map);
    }
    
    
    
    
}