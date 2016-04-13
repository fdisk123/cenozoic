package com.freepig.cenozoic.code.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@Component
public class QueryFactory implements QueryType {

	private final Map<String, String> HQL = new ConcurrentHashMap<String, String>();
	private final Map<String, String> SQL = new ConcurrentHashMap<String, String>();

	private String files;

	private volatile boolean isScan = false;

	public void put(String name, String XQL, boolean isHQL) {
		if (null == name || null == XQL)
			return;
		if (isHQL)
			HQL.put(name, XQL);
		else
			SQL.put(name, XQL);
	}

	public String getXQL(String name, boolean isHQL) {
		if (!isScan)
			scan();
		if (null == name)
			return null;
		if (isHQL)
			return HQL.get(name);
		return SQL.get(name);
	}

	private void scan() {
		Set<String> o = null;
		try {
			o = Scan.doScan(files);
			x(o);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		isScan = true;
	}

	public String getFiles() {
		return files;
	}

	public void setFiles(String files) {
		this.files = files;
	}

	public void x(Set<String> urls) throws ParserConfigurationException, SAXException, IOException {
		Iterator<String> it = urls.iterator();
		SAXParserFactory factory = SAXParserFactory.newInstance();
		while (it.hasNext()) {
			SAXParser parser = factory.newSAXParser();
			xmlHandler handler = new xmlHandler();
			String str = it.next();
			System.err.println(str);
			//		String x = null;
			//		ByteArrayInputStream in = new ByteArrayInputStream(x.getBytes());
			InputSource is = new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(str));
			//			InputSource is = new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(it.next()));
			is.setEncoding("utf-8");
			parser.parse(is, handler);
		}
	}

	class xmlHandler extends DefaultHandler {
		private boolean isHQL = false;
		private String packageName = null;
		private String name = null;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equals(QUERY_LIST)) {
				packageName = attributes.getValue(PACKAGE);
			}
			else if (qName.equals(QUERY)) {
				isHQL = attributes.getValue(TYPE).equals("HQL");
				name = attributes.getValue(NAME);
			}
			super.startElement(uri, localName, qName, attributes);
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (length > 0) {
				put(String.format("%s.%s", packageName, name), new String(ch, start, length), isHQL);
			}
			super.characters(ch, start, length);
		}

	}

}