package org.xmlcml.norma;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.log.Log;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.xml.XMLUtil;

public class Norma {

	private static final Logger LOG = Logger.getLogger(Norma.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	private NormaArgProcessor argProcessor;
	private List<String> inputList;
	private List<HtmlElement> htmlElementOutputList;
	private List<InputWrapper> inputWrapperList;
	private Pubstyle pubstyle;

	public static void main(String[] args) {
		Norma norma = new Norma();
		norma.run(args);
	}

	public void run(String[] args) {
		argProcessor = new NormaArgProcessor(args);
		createInputList();
		findFileTypesAndExpandDirectories();
		normalizeInputs();
		writeOutput();
	}

	private void writeOutput() {
		String outputName = null;
		try {
			outputName = argProcessor.getOutput();
			if (outputName != null) {
				mkdirs(outputName);
	//			for (HtmlElement htmlElement : outputList) {
	//				XMLUtil.debug(htmlElement, new FileOutputStream(new File()));
	//			}
				if (htmlElementOutputList.size() == 1) {
					XMLUtil.debug(htmlElementOutputList.get(0), new FileOutputStream(new File(outputName)), 1);
					LOG.debug("Wrote XML File: "+outputName);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Cannot write "+outputName, e);
		}
	}

	private void mkdirs(String outputName) {
		File outputFile = new File(outputName);
		if (outputName.endsWith("/")) {
			outputFile.mkdirs();
		} else {
			outputFile.getParentFile().mkdirs();
		}
	}

	private void findFileTypesAndExpandDirectories() {
		inputWrapperList = new ArrayList<InputWrapper>();
		for (String inputName : inputList) {
			InputWrapper inputWrapper;
			File file = new File(inputName);
			if (file.exists()) {
				List<String> extensions = argProcessor.getExtensions();
				boolean recursive = argProcessor.isRecursive();
				addInputWrappersForFiles(inputName, file, extensions, recursive);
			} else{
				addInputWrapperForURL(inputName);
			}
		}
	}

	private void addInputWrapperForURL(String inputName) {
		InputWrapper inputWrapper;
		URL url = null;
		if (!inputName.startsWith("http")) {
			try {
				url = new URL(inputName = "http://"+inputName); 
			} catch (Exception e) {
				// fails
			}
		} else {
			try {
				url = new URL(inputName);
			} catch (Exception e) {
				// fails
			}
		}
		if (url != null) {
			inputWrapper = new InputWrapper(url, inputName);
			inputWrapperList.add(inputWrapper);
		}
	}

	private void addInputWrappersForFiles(String inputName, File file,
			List<String> extensions, boolean recursive) {
		InputWrapper inputWrapper;
		if (!file.isDirectory()) {
			inputWrapper = new InputWrapper(file, inputName);
			inputWrapperList.add(inputWrapper);
		} else {
			List<InputWrapper> inputWrappers = InputWrapper.expandDirectories(file, extensions, recursive);
			inputWrappers.addAll(inputWrappers);
		}
	}

	private void normalizeInputs() {
		this.pubstyle = argProcessor.getPubstyle();
		htmlElementOutputList = new ArrayList<HtmlElement>();
		for (InputWrapper inputWrapper : inputWrapperList) {
			try {
				HtmlElement element = inputWrapper.transform(pubstyle);
				htmlElementOutputList.add(element);
			} catch (Exception e) {
				e.printStackTrace();
				LOG.error("Failed to read/process : "+inputWrapper+ " ("+e.getMessage()+")");
			}
		}
	}


	private void createInputList() {
		argProcessor.expandWildcardsExhaustively();
		inputList = argProcessor.getInputList();
	}
}
