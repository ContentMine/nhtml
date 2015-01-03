package org.xmlcml.norma.document.hindawi;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.norma.InputType;
import org.xmlcml.norma.RawInput;
import org.xmlcml.norma.document.DocumentReader;
import org.xmlcml.norma.document.hindawi.HindawiReader;

public class URLReaderTest {

	@Test
	@Ignore // uses URL
	public void testReadURL() throws Exception {
		String urlString = "http://www.hindawi.com/journals/ija/2014/507405/";
		DocumentReader hindawiReader = new HindawiReader(InputType.HTML);
		hindawiReader.readURL(urlString);
		RawInput rawInput = hindawiReader.getRawInput();
		Assert.assertNotNull("raw input", rawInput);
		byte[] bytes = rawInput.getRawBytes();
		Assert.assertNotNull("read bytes", bytes);
		Assert.assertEquals("bytes read", 111681, bytes.length);
	}
	
}
