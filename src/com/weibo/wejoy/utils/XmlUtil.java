package com.weibo.wejoy.utils;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;

public class XmlUtil {

	public static Element getRootElement(Document document) {
		return document.getRootElement();
	}

	public static Element getElementByName(Document document, String name) {
		return document.getRootElement().element(name);
	}

	public static Element getElementByName(Element elem, String name) {
		return elem.element(name);
	}

	@SuppressWarnings("rawtypes")
	public static List getChildElements(Element elem) {
		return elem.elements();
	}

	@SuppressWarnings("rawtypes")
	public static List getChildElementsByName(Element elem, String name) {
		return elem.elements(name);
	}

	public static String getAttByName(Element element, String name) {
		return element.attributeValue(name);
	}
}
