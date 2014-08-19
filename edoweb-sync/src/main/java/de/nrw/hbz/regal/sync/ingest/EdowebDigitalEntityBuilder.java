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
package de.nrw.hbz.regal.sync.ingest;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.nrw.hbz.regal.api.helper.ObjectType;
import de.nrw.hbz.regal.fedora.XmlUtils;
import de.nrw.hbz.regal.sync.extern.DigitalEntity;
import de.nrw.hbz.regal.sync.extern.DigitalEntityBuilderInterface;
import de.nrw.hbz.regal.sync.extern.DigitalEntityRelation;
import de.nrw.hbz.regal.sync.extern.Md5Checksum;
import de.nrw.hbz.regal.sync.extern.RelatedDigitalEntity;
import de.nrw.hbz.regal.sync.extern.StreamType;


/**
 * @author Jan Schnasse schnasse@hbz-nrw.de
 * 
 */
public class EdowebDigitalEntityBuilder implements
	DigitalEntityBuilderInterface {

    final static Logger logger = LoggerFactory
	    .getLogger(EdowebDigitalEntityBuilder.class);
    Map<String, DigitalEntity> filedIds2DigitalEntity = new HashMap<String, DigitalEntity>();
    Map<String, String> groupIds2FileIds = new HashMap<String, String>();

    @Override
    public DigitalEntity build(String location, String pid) {
	DigitalEntity dtlDe = buildSimpleBean(location, pid);
	if (dtlDe.getStream(StreamType.STRUCT_MAP) != null) {
	    dtlDe = prepareMetsStructure(dtlDe);
	    dtlDe = addSiblings(dtlDe);
	    dtlDe = addChildren(dtlDe);
	} else {
	    dtlDe = addSiblings(dtlDe);
	    dtlDe = addDigitoolChildren(dtlDe);
	}
	dtlDe = removeEmptyVolumes(dtlDe);
	return dtlDe;
    }

    private DigitalEntity removeEmptyVolumes(final DigitalEntity entity) {
	DigitalEntity dtlDe = entity;
	List<RelatedDigitalEntity> result = new Vector<RelatedDigitalEntity>();
	List<RelatedDigitalEntity> related = entity.getRelated();

	for (RelatedDigitalEntity d : related) {
	    if (DigitalEntityRelation.part_of.toString().equals(d.relation)) {
		if (ObjectType.volume.toString()
			.equals(d.entity.getUsageType())) {
		    if (d.entity.getRelated().isEmpty())
			continue;
		}
	    }
	    result.add(d);
	}
	dtlDe.setRelated(result);
	return dtlDe;
    }

    DigitalEntity buildSimpleBean(String location, String pid) {
	DigitalEntity dtlDe = new DigitalEntity(location, pid);
	dtlDe.setXml(new File(dtlDe.getLocation() + File.separator + pid
		+ ".xml"));

	Element root = getXmlRepresentation(dtlDe);
	dtlDe.setLabel(getLabel(root));
	loadMetadataStreams(dtlDe, root);
	setType(dtlDe);
	try {
	    setCatalogId(dtlDe);
	} catch (CatalogIdNotFoundException e) {
	    logger.debug("", e);
	}
	loadDataStream(dtlDe, root);
	linkToParent(dtlDe);

	return dtlDe;
    }

    /**
     * Tries to find the catalog id (aleph)
     * 
     * @param dtlDe
     *            the digital entity
     */
    void setCatalogId(DigitalEntity dtlDe) {
	try {
	    Element root = XmlUtils.getDocument(dtlDe
		    .getStream(StreamType.MARC).getFile());
	    XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
	    xpath.setNamespaceContext(new MarcNamespaceContext());
	    XPathExpression expr = xpath.compile("//controlfield[@tag='001']");
	    Object result = expr.evaluate(root, XPathConstants.NODESET);
	    NodeList nodes = (NodeList) result;
	    if (nodes.getLength() != 1) {
		throw new CatalogIdNotFoundException("Found "
			+ nodes.getLength() + " ids");
	    }
	    String id = nodes.item(0).getTextContent();
	    dtlDe.addIdentifier(id);
	    // logger.debug(dtlDe.getPid() + " add id " + id);
	} catch (Exception e) {
	    throw new CatalogIdNotFoundException(e);
	}

    }

    private void setType(DigitalEntity dtlBean) {
	Element root = XmlUtils.getDocument(dtlBean.getStream(
		StreamType.CONTROL).getFile());
	XPathFactory factory = XPathFactory.newInstance();
	XPath xpath = factory.newXPath();
	try {
	    XPathExpression expr = xpath.compile("//partition_c");
	    Object result = expr.evaluate(root, XPathConstants.NODESET);
	    NodeList nodes = (NodeList) result;
	    if (nodes.getLength() != 1) {
		throw new TypeNotFoundException("Found " + nodes.getLength()
			+ " types");
	    }
	    dtlBean.setType(nodes.item(0).getTextContent());
	    // logger.debug(dtlBean.getPid() + " setType to: " +
	    // dtlBean.getType());
	} catch (XPathExpressionException e) {
	    throw new XPathException(e);
	}
    }

    private DigitalEntity prepareMetsStructure(final DigitalEntity entity) {
	DigitalEntity dtlDe = entity;
	dtlDe = createTree(entity);
	mapGroupIdsToFileIds(entity);
	return dtlDe;
    }

    private void mapGroupIdsToFileIds(DigitalEntity entity) {
	try {
	    Element root = XmlUtils.getDocument(entity.getStream(
		    StreamType.FILE_SEC).getFile());
	    XPathFactory xpathFactory = XPathFactory.newInstance();
	    XPath xpath = xpathFactory.newXPath();
	    NodeList volumes = (NodeList) xpath.evaluate("/*/*/*/*/*", root,
		    XPathConstants.NODESET);
	    for (int i = 0; i < volumes.getLength(); i++) {
		Element item = (Element) volumes.item(i);
		String groupId = item.getAttribute("GROUPID");
		String fileId = item.getAttribute("ID");
		// logger.debug(groupId + " to " + fileId);
		groupIds2FileIds.put(groupId, fileId);
	    }
	} catch (XPathExpressionException e) {
	    logger.warn("", e);
	} catch (Exception e) {
	    logger.debug("", e);
	}

    }

    private DigitalEntity createTree(final DigitalEntity entity) {
	DigitalEntity dtlDe = entity;
	try {
	    Element root = XmlUtils.getDocument(entity.getStream(
		    StreamType.STRUCT_MAP).getFile());

	    List<Element> volumes = XmlUtils.getElements("/*/*/*/*/*", root,
		    null);
	    logger.debug("create volumes for: " + entity.getPid());
	    for (Element volume : volumes) {
		DigitalEntity v = createDigitalEntity(ObjectType.volume,
			volume.getAttribute("LABEL"), dtlDe.getPid(),
			dtlDe.getLocation());
		dtlDe.addRelated(v, DigitalEntityRelation.part_of.toString());
		logger.debug("Create volume " + v.getPid());
		List<Element> issues = XmlUtils.getElements("./div", volume,
			null);
		if (issues == null)
		    mapFileIdToDigitalEntity(v, volume);
		else {
		    for (Element issue : issues) {
			DigitalEntity i = createDigitalEntity(ObjectType.issue,
				issue.getAttribute("LABEL"), v.getPid(),
				dtlDe.getLocation());

			logger.debug("Create issue " + i.getPid());
			v.addRelated(i,
				DigitalEntityRelation.part_of.toString());
			mapFileIdToDigitalEntity(i, issue);
		    }
		}
	    }
	} catch (XPathException e) {
	    logger.warn(entity.getPid() + " no volumes found.");
	} catch (Exception e) {
	    logger.debug("", e);
	}
	return dtlDe;
    }

    private void mapFileIdToDigitalEntity(DigitalEntity de, Element root) {
	final String regex = ".//fptr";
	List<Element> files = XmlUtils.getElements(regex, root, null);
	for (Element f : files) {
	    String fileId = f.getAttribute("FILEID");
	    logger.debug("Key: " + fileId + " Value: " + de.getPid());
	    filedIds2DigitalEntity.put(fileId, de);
	}
    }

    private DigitalEntity createDigitalEntity(ObjectType type, String label,
	    String parentPid, String location) {
	String prefix = prefix(parentPid);
	String pid = prefix + "-" + uuid();
	DigitalEntity entity = new DigitalEntity(location + File.separator
		+ pid, pid);
	entity.setLabel(label);
	entity.setParentPid(parentPid);
	entity.setUsageType(type.toString());
	return entity;
    }

    private String prefix(String pid) {
	if (!pid.contains("-"))
	    return pid;
	else
	    return pid.split("-")[0];
    }

    private String uuid() {
	Long uuid = UUID.randomUUID().getMostSignificantBits();
	return Long.toHexString(uuid);
    }

    private void loadDataStream(DigitalEntity dtlDe, Element root) {
	Node streamRef = root.getElementsByTagName("stream_ref").item(0);
	String filename = ((Element) streamRef)
		.getElementsByTagName("file_name").item(0).getTextContent();
	File file = new File(dtlDe.getLocation() + File.separator
		+ dtlDe.getPid() + File.separator + filename);
	String mime = ((Element) streamRef).getElementsByTagName("mime_type")
		.item(0).getTextContent();
	String fileId = ((Element) streamRef).getElementsByTagName("file_id")
		.item(0).getTextContent();
	if (!file.exists()) {
	    logger.error("The file " + filename
		    + " found in DTL Xml does not exist.");
	    return;
	}
	logger.debug("found data stream " + file + "," + mime + ","
		+ StreamType.DATA + "," + fileId);

	dtlDe.addStream(file, mime, StreamType.DATA, fileId, getMd5(file));
    }

    private void loadMetadataStreams(DigitalEntity dtlDe, Element root) {
	setXmlStream(dtlDe, root.getElementsByTagName("control").item(0),
		StreamType.CONTROL);
	dtlDe.setUsageType(root.getElementsByTagName("usage_type").item(0)
		.getTextContent());
	NodeList list = root.getElementsByTagName("md");
	for (int i = 0; i < list.getLength(); i++) {
	    Node item = list.item(i);
	    String type = getItemType((Element) item);

	    if (type.compareTo("dc") == 0) {
		setXmlStream(dtlDe, item, StreamType.DC);
	    } else if (type.compareTo("preservation_md") == 0) {
		setXmlStream(dtlDe, item, StreamType.PREMIS);
	    } else if (type.compareTo("text_md") == 0) {
		setXmlStream(dtlDe, item, StreamType.TEXT);
	    } else if (type.compareTo("rights_md") == 0) {
		setXmlStream(dtlDe, item, StreamType.RIGHTS);
	    } else if (type.compareTo("jhove") == 0) {
		setXmlStream(dtlDe, item, StreamType.JHOVE);
	    } else if (type.compareTo("changehistory_md") == 0) {
		setXmlStream(dtlDe, item, StreamType.HIST);
	    } else if (type.compareTo("marc") == 0) {
		/*
		 * FIXME : Workaround for some bug.
		 */
		setMarcStream(dtlDe, item, StreamType.MARC);
	    } else if (type.compareTo("metsHdr") == 0) {
		setXmlStream(dtlDe, item, StreamType.METS_HDR);
	    } else if (type.compareTo("structMap") == 0) {
		setXmlStream(dtlDe, item, StreamType.STRUCT_MAP);
	    } else if (type.compareTo("fileSec") == 0) {
		setXmlStream(dtlDe, item, StreamType.FILE_SEC);
	    }

	}
    }

    private void setXmlStream(DigitalEntity dtlDe, Node item, StreamType type) {
	try {
	    File file = new File(dtlDe.getLocation() + File.separator + "."
		    + dtlDe.getPid() + "_" + type.toString() + ".xml");
	    File stream = XmlUtils.stringToFile(file,
		    XmlUtils.nodeToString(item));
	    String md5Hash = getMd5(stream);
	    dtlDe.addStream(stream, "application/xml", type, null, md5Hash);
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    /*
     * FIXME : Workaround for some bug.
     */
    private void setMarcStream(DigitalEntity dtlDe, Node item, StreamType type) {
	try {
	    File file = new File(dtlDe.getLocation() + File.separator + "."
		    + dtlDe.getPid() + "_" + type.toString() + ".xml");
	    File stream = XmlUtils.stringToFile(file, getMarc(item));
	    String md5Hash = getMd5(stream);
	    dtlDe.addStream(stream, "application/xml", type, null, md5Hash);
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    private String getMd5(File stream) {
	Md5Checksum md5 = new Md5Checksum();
	return md5.getMd5Checksum(stream);
    }

    private String getMarc(Node item) {
	Element marc = (Element) ((Element) item)
		.getElementsByTagName("record").item(0);
	marc.setAttribute("xmlns", "http://www.loc.gov/MARC21/slim");
	String xmlStr = XmlUtils.nodeToString(marc);
	/*
	 * FIXME : Workaround for some bug.
	 */
	xmlStr = xmlStr.replaceAll("nam  2200000 u 4500",
		"00000    a2200000   4500");
	return xmlStr;
    }

    private String getItemType(Element root) {
	return root.getElementsByTagName("type").item(0).getTextContent();
    }

    private String getLabel(Element root) {
	return root.getElementsByTagName("label").item(0).getTextContent();
    }

    private Element getXmlRepresentation(final DigitalEntity dtlDe) {
	File digitalEntityFile = new File(dtlDe.getLocation() + File.separator
		+ dtlDe.getPid() + ".xml");
	return XmlUtils.getDocument(digitalEntityFile);
    }

    private DigitalEntity addSiblings(final DigitalEntity entity) {
	DigitalEntity dtlDe = entity;
	Element root;
	try {
	    root = XmlUtils.getDocument(entity.getXml());
	    NodeList list = root.getElementsByTagName("relation");
	    for (int i = 0; i < list.getLength(); i++) {
		Node item = list.item(i);
		String relPid = ((Element) item).getElementsByTagName("pid")
			.item(0).getTextContent();
		String usageType = ((Element) item)
			.getElementsByTagName("usage_type").item(0)
			.getTextContent();
		String type = ((Element) item).getElementsByTagName("type")
			.item(0).getTextContent();
		if (type.compareTo(DigitalEntityRelation.manifestation
			.toString()) == 0) {
		    DigitalEntity b = buildSimpleBean(entity.getLocation(),
			    relPid);
		    logger.debug("Add sibling " + b.getPid() + " to "
			    + entity.getPid() + " utilizing relation "
			    + usageType);
		    dtlDe.addRelated(b, usageType);
		}
	    }
	} catch (Exception e) {
	    logger.warn("", e);
	}

	return dtlDe;
    }

    private DigitalEntity addChildren(final DigitalEntity entity) {
	DigitalEntity dtlDe = entity;

	try {
	    Element root = XmlUtils.getDocument(entity.getXml());
	    NodeList list = root.getElementsByTagName("relation");
	    for (int i = 0; i < list.getLength(); i++) {
		Node item = list.item(i);
		String relPid = ((Element) item).getElementsByTagName("pid")
			.item(0).getTextContent();
		String usageType = ((Element) item)
			.getElementsByTagName("usage_type").item(0)
			.getTextContent();
		String type = ((Element) item).getElementsByTagName("type")
			.item(0).getTextContent();
		String mimeType = ((Element) item)
			.getElementsByTagName("mime_type").item(0)
			.getTextContent();
		if (type.compareTo(DigitalEntityRelation.include.toString()) == 0
			&& mimeType.equals("application/pdf")
			&& (usageType.compareTo(DigitalEntityRelation.ARCHIVE
				.toString()) != 0)) {
		    try {
			DigitalEntity b = build(entity.getLocation(), relPid);
			b.setUsageType(usageType);
			addToTree(dtlDe, b);
		    } catch (Exception e) {
			// TODO
			e.printStackTrace();
		    }
		}
	    }
	} catch (Exception e) {
	    logger.warn("", e);
	}
	return dtlDe;
    }

    private DigitalEntity addDigitoolChildren(final DigitalEntity entity) {
	DigitalEntity dtlDe = entity;
	try {
	    Element root = XmlUtils.getDocument(entity.getXml());
	    NodeList list = root.getElementsByTagName("relation");
	    for (int i = 0; i < list.getLength(); i++) {
		Node item = list.item(i);
		String relPid = ((Element) item).getElementsByTagName("pid")
			.item(0).getTextContent();
		String usageType = ((Element) item)
			.getElementsByTagName("usage_type").item(0)
			.getTextContent();
		String type = ((Element) item).getElementsByTagName("type")
			.item(0).getTextContent();
		String mimeType = ((Element) item)
			.getElementsByTagName("mime_type").item(0)
			.getTextContent();
		if (type.compareTo(DigitalEntityRelation.include.toString()) == 0
			&& mimeType.equals("application/pdf")
			&& (usageType.compareTo(DigitalEntityRelation.ARCHIVE
				.toString()) != 0)) {
		    try {

			DigitalEntity b = build(entity.getLocation(), relPid);
			// logger.debug(b.getPid() + " is child of "
			// + dtlDe.getPid());
			b.setUsageType(usageType);
			dtlDe.setIsParent(true);
			b.setParentPid(dtlDe.getPid());
			dtlDe.addRelated(b,
				DigitalEntityRelation.part_of.toString());

		    } catch (Exception e) {
			// TODO
			e.printStackTrace();
		    }
		}
	    }
	} catch (Exception e) {
	    logger.warn("", e);
	}
	return dtlDe;
    }

    private void linkToParent(DigitalEntity dtlDe) {
	try {
	    Element root = XmlUtils.getDocument(dtlDe.getXml());
	    NodeList list = root.getElementsByTagName("relation");
	    for (int i = 0; i < list.getLength(); i++) {
		Node item = list.item(i);
		String relPid = ((Element) item).getElementsByTagName("pid")
			.item(0).getTextContent();
		String type = ((Element) item).getElementsByTagName("type")
			.item(0).getTextContent();
		if (type.compareTo(DigitalEntityRelation.part_of.toString()) == 0) {
		    dtlDe.setIsParent(false);
		    dtlDe.setParentPid(relPid);
		}
	    }
	} catch (Exception e) {
	    logger.warn("", e);
	}
    }

    private void addToTree(DigitalEntity dtlDe, DigitalEntity related) {
	DigitalEntity parent = findParent(dtlDe, related);
	parent.setIsParent(true);
	related.setParentPid(parent.getPid());
	parent.addRelated(related, DigitalEntityRelation.part_of.toString());
	logger.debug(related.getPid() + " is child of " + parent.getPid());
    }

    private DigitalEntity findParent(DigitalEntity dtlDe, DigitalEntity related) {
	if (!(related.getUsageType().compareTo("VIEW") == 0)
		&& !(related.getUsageType().compareTo("VIEW_MAIN") == 0)) {
	    System.out.println("Related of wrong type: " + related.getPid());
	    return dtlDe;
	}
	String groupId = related.getStream(StreamType.DATA).getFileId();
	String fileId = groupIds2FileIds.get(groupId);
	DigitalEntity parent = this.filedIds2DigitalEntity.get(fileId);
	if (parent == null) {
	    return dtlDe;
	} else {
	    related.setUsageType(ObjectType.file.toString());
	    return parent;
	}
    }

    @SuppressWarnings("javadoc")
    public class MarcNamespaceContext implements NamespaceContext {

	public String getNamespaceURI(String prefix) {
	    if (prefix == null)
		throw new NullPointerException("Null prefix");
	    else if ("marc".equals(prefix))
		return "http://www.loc.gov/MARC21/slim";
	    else if ("xml".equals(prefix))
		return XMLConstants.XML_NS_URI;
	    return XMLConstants.NULL_NS_URI;
	}

	// This method isn't necessary for XPath processing.
	public String getPrefix(String uri) {
	    throw new UnsupportedOperationException();
	}

	// This method isn't necessary for XPath processing either.
	@SuppressWarnings("rawtypes")
	public Iterator getPrefixes(String uri) {
	    throw new UnsupportedOperationException();
	}

    }

    @SuppressWarnings({ "javadoc", "serial" })
    public class TypeNotFoundException extends RuntimeException {

	public TypeNotFoundException(String message) {
	    super(message);
	}

    }

    @SuppressWarnings({ "javadoc", "serial" })
    public class CatalogIdNotFoundException extends RuntimeException {

	public CatalogIdNotFoundException(String message) {
	    super(message);
	}

	public CatalogIdNotFoundException(Throwable cause) {
	    super(cause);
	}

    }

    @SuppressWarnings({ "javadoc", "serial" })
    public class XPathException extends RuntimeException {
	public XPathException(Throwable cause) {
	    super(cause);
	}

	public XPathException(String message, Throwable cause) {
	    super(message, cause);
	}
    }
}

