package org.xmlcml.nhtml.input;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.xmlcml.nhtml.InputType;
import org.xmlcml.nhtml.RawInput;
import org.xmlcml.nhtml.input.html.HtmlReader;

public class InputReader {

	private static final Logger LOG = Logger.getLogger(InputReader.class);

	public static InputReader createReader(InputType type) {
		InputReader reader = null;
		if (type == null) {
			LOG.debug("no input type");
		} else if (type.equals(InputType.HTML)) {
			reader = new HtmlReader();
		} else {
			throw new RuntimeException("Unknown/unsupported input type: "+type);
		}
		return reader;
	}

	public RawInput read(InputStream inputStream) throws IOException {
		byte[] rawBytes = IOUtils.toByteArray(inputStream);
		RawInput rawInput = new RawInput(rawBytes);
		return rawInput;
	}

}
