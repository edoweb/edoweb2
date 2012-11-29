/*
 * Copyright 2012 hbz NRW (http://www.hbz-nrw.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.nrw.hbz.edoweb2.sync.extern;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class DigitalEntityBeanBuilder
 * 
 * <p>
 * <em>Title: </em>
 * </p>
 * <p>
 * Description:
 * </p>
 * 
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 */
public class DigitalEntityBuilder
{
	String baseDir = null;

	public DigitalEntityBuilder()
	{

	}

	public DigitalEntity buildComplexBean(String baseDir, String pid)
	{
		this.baseDir = baseDir;
		Element root = getDocument(pid);
		return buildComplexBean(root);
	}

	public DigitalEntity buildComplexBean(String baseDir, Element root)
	{
		this.baseDir = baseDir;
		return buildComplexBean(root);
	}

	private DigitalEntity buildComplexBean(Element root)
	{
		DigitalEntity dtlDe = null;

		String pid = root.getElementsByTagName("pid").item(0).getTextContent();

		dtlDe = buildSimpleBean(pid, "VIEW", root);

		NodeList list = root.getElementsByTagName("relation");
		for (int i = 0; i < list.getLength(); i++)
		{
			Node item = list.item(i);
			String relPid = ((Element) item).getElementsByTagName("pid")
					.item(0).getTextContent();
			String usageType = ((Element) item)
					.getElementsByTagName("usage_type").item(0)
					.getTextContent();
			String type = ((Element) item).getElementsByTagName("type").item(0)
					.getTextContent();

			Element relRoot = getDocument(relPid);

			if (type.compareTo(DigitalEntity.MANIFESTATION) == 0)
			{
				DigitalEntity b = buildSimpleBean(relPid, usageType, relRoot);

				if (usageType.compareTo(DigitalEntity.INDEX) == 0)
				{
					dtlDe.addIndexLink(b);
				}
				else if (usageType.compareTo(DigitalEntity.ARCHIVE) == 0)
				{
					dtlDe.addArchiveLink(b);
				}
				else if (usageType.compareTo(DigitalEntity.THUMBNAIL) == 0)
				{
					dtlDe.addThumbnailLink(b);
				}
				else if (usageType.compareTo(DigitalEntity.VIEW) == 0)
				{
					dtlDe.addViewLink(b);
				}
				else if (usageType.compareTo(DigitalEntity.VIEW_MAIN) == 0)
				{
					dtlDe.addViewMainLink(b);
				}
			}
			else if (type.compareTo(DigitalEntity.INCLUDE) == 0)
			{
				DigitalEntity b = buildComplexBean(baseDir, relPid);
				if (usageType.compareTo(DigitalEntity.VIEW_MAIN) == 0)
				{
					dtlDe.addViewMainLink(b);
				}
				else if (usageType.compareTo(DigitalEntity.INDEX) == 0)
				{
					dtlDe.addIndexLink(b);
				}
				else if (usageType.compareTo(DigitalEntity.ARCHIVE) == 0)
				{
					dtlDe.addArchiveLink(b);
				}
				else if (usageType.compareTo(DigitalEntity.THUMBNAIL) == 0)
				{
					dtlDe.addThumbnailLink(b);
				}
				else if (usageType.compareTo(DigitalEntity.VIEW) == 0)
				{
					dtlDe.addViewLink(b);
				}
			}
		}

		return dtlDe;
	}

	private Element getDocument(File digitalEntityFile)
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder docBuilder;

			docBuilder = factory.newDocumentBuilder();

			Document doc = docBuilder.parse(new BufferedInputStream(
					new FileInputStream(digitalEntityFile)));
			Element root = doc.getDocumentElement();
			root.normalize();
			return root;
		}
		catch (FileNotFoundException e)
		{

			e.printStackTrace();
		}
		catch (SAXException e)
		{

			e.printStackTrace();
		}
		catch (IOException e)
		{

			e.printStackTrace();
		}
		catch (ParserConfigurationException e)
		{

			e.printStackTrace();
		}
		return null;
	}

	/**
	 * <p>
	 * <em>Title: </em>
	 * </p>
	 * <p>
	 * Description:
	 * </p>
	 * 
	 * @param relPid
	 * @return
	 */
	private Element getDocument(String relPid)
	{
		File digitalEntityFile = new File(this.baseDir + File.separator
				+ relPid + ".xml");
		// System.out.println("DigitalEntityFile "
		// + digitalEntityFile.getAbsolutePath());

		return getDocument(digitalEntityFile);
	}

	public DigitalEntity buildSimpleBean(String pid, String usageType,
			Element root)
	{
		DigitalEntity dtlDe = new DigitalEntity(baseDir);
		// System.out.println("BaseDir "+baseDir);
		dtlDe.setPid(pid);

		dtlDe.setLabel(root.getElementsByTagName("label").item(0)
				.getTextContent());

		dtlDe.setControl(nodeToString(root.getElementsByTagName("control")
				.item(0)));

		NodeList list = root.getElementsByTagName("md");
		for (int i = 0; i < list.getLength(); i++)
		{
			Node item = list.item(i);
			String type = ((Element) item).getElementsByTagName("type").item(0)
					.getTextContent();

			if (type.compareTo("dc") == 0)
			{
				dtlDe.setDc(nodeToString(item));

			}
			else if (type.compareTo("preservation_md") == 0)
			{
				dtlDe.setPreservation(nodeToString(item));
			}
			else if (type.compareTo("text_md") == 0)
			{
				dtlDe.setText(nodeToString(item));
			}
			else if (type.compareTo("rights_md") == 0)
			{
				dtlDe.setRights(nodeToString(item));
			}
			else if (type.compareTo("jhove") == 0)
			{
				dtlDe.setJhove(nodeToString(item));
			}
			else if (type.compareTo("changehistory_md") == 0)
			{
				dtlDe.setHistory(nodeToString(item));
			}
			else if (type.compareTo("marc") == 0)
			{
				Element marc = (Element) ((Element) item).getElementsByTagName(
						"record").item(0);
				marc.setAttribute("xmlns", "http://www.loc.gov/MARC21/slim");
				String xmlStr = nodeToString(marc);
				dtlDe.setMarc(xmlStr);
			}
			else if (type.compareTo("metsHdr") == 0)
			{
				dtlDe.setMetsHdr(nodeToString(item));
			}
			else if (type.compareTo("structMap") == 0)
			{
				dtlDe.setStructMap(nodeToString(item));
			}
			else if (type.compareTo("fileSec") == 0)
			{
				dtlDe.setFileSec(nodeToString(item));
			}

		}
		Node streamRef = root.getElementsByTagName("stream_ref").item(0);
		String filename = ((Element) streamRef)
				.getElementsByTagName("file_name").item(0).getTextContent();
		File file = new File(baseDir + File.separator + pid + File.separator
				+ filename);
		dtlDe.setStream(file);
		dtlDe.setStreamMime(((Element) streamRef)
				.getElementsByTagName("mime_type").item(0).getTextContent());
		File xmlFile = new File(baseDir + File.separator + pid + ".xml");
		dtlDe.setXml(xmlFile);

		return dtlDe;
	}

	private String nodeToString(Node node)
	{
		try
		{
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			StringWriter buffer = new StringWriter(1024);
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes");

			transformer
					.transform(new DOMSource(node), new StreamResult(buffer));
			String str = buffer.toString();
			return str;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		catch (Error error)
		{
			error.printStackTrace();
		}
		return "";
	}
}
