/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.Locale;

import org.apache.commons.io.FileUtils;

import org.junit.After;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the Tika's cli
 */
public class TikaCLITest {

    /* Test members */
    private File profile = null;
    private ByteArrayOutputStream outContent = null;
    private PrintStream stdout = null;
    private URI testDataURI = new File("src/test/resources/test-data/").toURI();
    private String resourcePrefix;

    @Before
    public void setUp() throws Exception {
        profile = new File("welsh.ngp");
        outContent = new ByteArrayOutputStream();
        resourcePrefix = testDataURI.toString();
        stdout = System.out;
        System.setOut(new PrintStream(outContent, true, "UTF-8"));
    }

    /**
     * Creates a welsh language profile
     * 
     * @throws Exception
     */
    @Test
    public void testCreateProfile() throws Exception {
        String[] params = {"--create-profile=welsh", "-eUTF-8", resourcePrefix + "welsh_corpus.txt"};
        TikaCLI.main(params);
        assertTrue(profile.exists());
    }

    /**
     * Tests --list-parser-detail option of the cli
     * 
     * @throws Exception
     */
    @Test
    public void testListParserDetail() throws Exception{
        String[] params = {"--list-parser-detail"};
        TikaCLI.main(params);
        assertTrue(outContent.toString("UTF-8").contains("application/vnd.oasis.opendocument.text-web"));
    }

    /**
     * Tests --list-parser option of the cli
     * 
     * @throws Exception
     */
    @Test
    public void testListParsers() throws Exception{
        String[] params = {"--list-parser"};
        TikaCLI.main(params);
        //Assert was commented temporarily for finding the problem
        //		Assert.assertTrue(outContent != null && outContent.toString("UTF-8").contains("org.apache.tika.parser.iwork.IWorkPackageParser"));
    }

    /**
     * Tests -x option of the cli
     * 
     * @throws Exception
     */
    @Test
    public void testXMLOutput() throws Exception{
        String[] params = {"-x", resourcePrefix + "alice.cli.test"};
        TikaCLI.main(params);
        assertTrue(outContent.toString("UTF-8").contains("?xml version=\"1.0\" encoding=\"UTF-8\"?"));
    }

    /**
     * Tests a -h option of the cli
     * 
     * @throws Exception
     */
    @Test
    public void testHTMLOutput() throws Exception{
        String[] params = {"-h", resourcePrefix + "alice.cli.test"};
        TikaCLI.main(params);
        assertTrue(outContent.toString("UTF-8").contains("html xmlns=\"http://www.w3.org/1999/xhtml"));
        assertTrue("Expanded <title></title> element should be present",
                outContent.toString("UTF-8").contains("<title></title>"));
    }

    /**
     * Tests -t option of the cli
     * 
     * @throws Exception
     */
    @Test
    public void testTextOutput() throws Exception{
        String[] params = {"-t", resourcePrefix + "alice.cli.test"};
        TikaCLI.main(params);
        assertTrue(outContent.toString("UTF-8").contains("finished off the cake"));
    }

    /**
     * Tests -m option of the cli
     * @throws Exception
     */
    @Test
    public void testMetadataOutput() throws Exception{
        String[] params = {"-m", resourcePrefix + "alice.cli.test"};
        TikaCLI.main(params);
        assertTrue(outContent.toString("UTF-8").contains("text/plain"));
    }

    /**
     * Basic tests for -json option
     * 
     * @throws Exception
     */
    @Test
    public void testJsonMetadataOutput() throws Exception {
        String[] params = {"--json", resourcePrefix + "testJsonMultipleInts.html"};
        TikaCLI.main(params);
        String json = outContent.toString("UTF-8");
        //TIKA-1310
        assertTrue(json.contains("\"fb:admins\":\"1,2,3,4\","));
        
        //test legacy alphabetic sort of keys
        int enc = json.indexOf("\"Content-Encoding\"");
        int fb = json.indexOf("fb:admins");
        int title = json.indexOf("\"title\"");
        assertTrue(enc > -1 && fb > -1 && enc < fb);
        assertTrue (fb > -1 && title > -1 && fb < title);
    }

    /**
     * Tests -l option of the cli
     * 
     * @throws Exception
     */
    @Test
    public void testLanguageOutput() throws Exception{
        String[] params = {"-l", resourcePrefix + "alice.cli.test"};
        TikaCLI.main(params);
        assertTrue(outContent.toString("UTF-8").contains("en"));
    }

    /**
     * Tests -d option of the cli
     * 
     * @throws Exception
     */
    @Test
    public void testDetectOutput() throws Exception{
        String[] params = {"-d", resourcePrefix + "alice.cli.test"};
        TikaCLI.main(params);
        assertTrue(outContent.toString("UTF-8").contains("text/plain"));
    }

    /**
     * Tests --list-met-models option of the cli
     * 
     * @throws Exception
     */
    @Test
    public void testListMetModels() throws Exception{
        String[] params = {"--list-met-models", resourcePrefix + "alice.cli.test"};
        TikaCLI.main(params);
        assertTrue(outContent.toString("UTF-8").contains("text/plain"));
    }

    /**
     * Tests --list-supported-types option of the cli
     * 
     * @throws Exception
     */
    @Test
    public void testListSupportedTypes() throws Exception{
        String[] params = {"--list-supported-types", resourcePrefix + "alice.cli.test"};
        TikaCLI.main(params);
        assertTrue(outContent.toString("UTF-8").contains("supertype: application/octet-stream"));
    }

    /**
     * Tears down the test. Returns the System.out
     */
    @After
    public void tearDown() throws Exception {
        if(profile != null && profile.exists())
            profile.delete();
        System.setOut(stdout);
    }

    @Test
    public void testExtract() throws Exception {
        File tempFile = File.createTempFile("tika-test-", "");
        tempFile.delete();
        tempFile.mkdir(); // not really good method for production usage, but ok for tests
                          // google guava library has better solution

        try {
            String[] params = {"--extract-dir="+tempFile.getAbsolutePath(),"-z", resourcePrefix + "/coffee.xls"};
            
            TikaCLI.main(params);
            
            StringBuffer allFiles = new StringBuffer();
            for (String f : tempFile.list()) {
                if (allFiles.length() > 0) allFiles.append(" : ");
                allFiles.append(f);
            }
            
            // ChemDraw file
            File expectedCDX = new File(tempFile, "MBD002B040A.cdx");
            // Image of the ChemDraw molecule
            File expectedIMG = new File(tempFile, "file4.png");
            // OLE10Native
            File expectedOLE10 = new File(tempFile, "MBD002B0FA6_file5.bin");
            // Something that really isnt a text file... Not sure what it is???
            File expected262FE3 = new File(tempFile, "MBD00262FE3.txt");
            // Image of one of the embedded resources
            File expectedEMF = new File(tempFile, "file0.emf");
            
            assertExtracted(expectedCDX, allFiles.toString());
            assertExtracted(expectedIMG, allFiles.toString());
            assertExtracted(expectedOLE10, allFiles.toString());
            assertExtracted(expected262FE3, allFiles.toString());
            assertExtracted(expectedEMF, allFiles.toString());
        } finally {
            FileUtils.deleteDirectory(tempFile);
        }
    }
    protected static void assertExtracted(File f, String allFiles) {

        assertTrue(
                "File " + f.getName() + " not found in " + allFiles,
                f.exists()
        );

        assertFalse(
                "File " + f.getName() + " is a directory!", f.isDirectory()
        );

        assertTrue(
                "File " + f.getName() + " wasn't extracted with contents",
                f.length() > 0
        );
    }

    // TIKA-920
    @Test
    public void testMultiValuedMetadata() throws Exception {
        String[] params = {"-m", resourcePrefix + "testMultipleSheets.numbers"};
        TikaCLI.main(params);
        String content = outContent.toString("UTF-8");
        assertTrue(content.contains("sheetNames: Checking"));
        assertTrue(content.contains("sheetNames: Secon sheet"));
        assertTrue(content.contains("sheetNames: Logical Sheet 3"));
        assertTrue(content.contains("sheetNames: Sheet 4"));
    }

    // TIKA-1031
    @Test
    public void testZipWithSubdirs() throws Exception {
        String[] params = {"-z", "--extract-dir=target", resourcePrefix + "testWithSubdirs.zip"};
        new File("subdir/foo.txt").delete();
        new File("subdir").delete();
        TikaCLI.main(params);
        String content = outContent.toString("UTF-8");
        assertTrue(content.contains("Extracting 'subdir/foo.txt'"));
        // clean up. TODO: These should be in target.
        new File("target/subdir/foo.txt").delete();
        new File("target/subdir").delete();
    }
}
