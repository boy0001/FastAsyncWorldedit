package net.jpountz.lz4;

import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class LZ4StreamTest {

    private long seed;
    private Random rand;

    private byte randomContent[];
    private byte compressedOutput[];

    @Before
    public void setUp() throws IOException {
        seed = System.currentTimeMillis();
        rand = new Random(seed);

        int randomContentLength = rand.nextInt(10000000) + 10000000;

        randomContent = new byte[randomContentLength];
        rand.nextBytes(randomContent);

        compressContent();
    }
    
    private void compressContent() throws IOException {
        ByteArrayOutputStream compressedOutputStream = new ByteArrayOutputStream();

        LZ4OutputStream os = new LZ4OutputStream(compressedOutputStream);
        int currentContentPosition = 0;
        
        while(currentContentPosition < randomContent.length) {
            int testBlockSize = rand.nextInt(500000);
            
            if(testBlockSize > randomContent.length - currentContentPosition)
                testBlockSize = randomContent.length - currentContentPosition;
            
            boolean writeByteByByte = true; //rand.nextBoolean();
            
            if(writeByteByByte) {
                for(int i=0;i<testBlockSize;i++) {
                    os.write(randomContent[currentContentPosition++]);
                }
            } else {
                boolean writeDirectlyFromContent = rand.nextBoolean();
                
                if(writeDirectlyFromContent) {
                    os.write(randomContent, currentContentPosition, testBlockSize);
                } else {
                    byte b[] = new byte[testBlockSize];
                    System.arraycopy(randomContent, currentContentPosition, b, 0, testBlockSize);
                    os.write(b);
                }
                currentContentPosition += testBlockSize;
            }
            
        }
        
        os.close();

        compressedOutput = compressedOutputStream.toByteArray();
    }

    @Test
    public void randomizedTest() throws IOException {
        try {
            InputStream is = new LZ4InputStream(new ByteArrayInputStream(compressedOutput));

            int currentContentPosition = 0;

            while(currentContentPosition < randomContent.length) {
                int testBlockSize = rand.nextInt(500000);

                boolean shouldTestBytes = rand.nextBoolean();

                if(shouldTestBytes) {
                    boolean shouldReadOneByOne = rand.nextBoolean();

                    if(shouldReadOneByOne) {
                        currentContentPosition += assertContentByteByByte(is, currentContentPosition, testBlockSize);
                    } else {
                        currentContentPosition += assertContentInSingleBlock(is, currentContentPosition, testBlockSize);
                    }
                } else {
                    currentContentPosition += skipContent(is, testBlockSize);
                }
            }

            assertEquals(-1, is.read(new byte[100]));
            assertEquals(-1, is.read());
        } catch(Throwable t) {
            t.printStackTrace();
            Assert.fail("Exception was thrown.  Seed value was " + seed);
        }

    }

    private int assertContentByteByByte(InputStream is, int currentContentPosition, int testBlockSize) throws IOException {
        int readContentLength = 0;

        while(readContentLength < testBlockSize) {
            int readContent = is.read();

            if(readContent == -1)
                break;

            assertEquals(randomContent[currentContentPosition + readContentLength], (byte)readContent);
            readContentLength++;
        }

        return readContentLength;
    }

    private int assertContentInSingleBlock(InputStream is, int currentContentPosition, int testBlockSize) throws IOException {
        int readContentLength;
        byte readContent[] = new byte[testBlockSize];

        readContentLength = is.read(readContent);

        assertEqualContent(readContent, currentContentPosition, readContentLength);

        return readContentLength;
    }

    private int skipContent(InputStream is, int testBlockSize) throws IOException {
        return (int) is.skip(testBlockSize);
    }

    private void assertEqualContent(byte readContent[], int uncompressedContentOffset, int readContentLength) {
        if(readContentLength < 0 && uncompressedContentOffset < randomContent.length)
            Assert.fail("Decompressed content was incomplete.  Index " + uncompressedContentOffset + ".  Seed was " + seed);
        
        for(int i=0;i<readContentLength;i++) {
            String message = "Bytes differed! Seed value was " + seed;
            Assert.assertEquals(message, randomContent[uncompressedContentOffset + i], readContent[i]);
        }
    }

}
