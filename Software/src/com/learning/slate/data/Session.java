package com.learning.slate.data;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author cbryant
 */
public class Session
{
	private Long timeStamp;
	
	public Session(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public Long getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(Long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getXmlStart(boolean singleLine, boolean includeXmlHeader)
	{
		StringBuffer start = new StringBuffer(includeXmlHeader ? "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" : "");
		start.append(singleLine ? "" : "\n");
		start.append("<session timestamp=\"");
		start.append(timeStamp);
		start.append("\">");
		start.append(singleLine ? "" : "\n");
		return start.toString();
	}
	
	public String getXmlEnd(boolean singleLine)
	{
		StringBuffer end = new StringBuffer((singleLine ? "" : "\n") + "</session>");
		return end.toString();
	}
	
	public static Session fromXml(Node sessionNode, XPath xpath, XPathExpression xpathExpr) throws XPathExpressionException
	{
		NamedNodeMap sessionMap = sessionNode.getAttributes();
		long timeStamp = Long.valueOf(sessionMap.getNamedItem("timestamp").getTextContent());
		return new Session(timeStamp);
	}
}