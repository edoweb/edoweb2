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

import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nrw.hbz.regal.api.helper.ObjectType;
import de.nrw.hbz.regal.datatypes.ContentModel;
import de.nrw.hbz.regal.sync.extern.DigitalEntity;
import de.nrw.hbz.regal.sync.extern.Stream;
import de.nrw.hbz.regal.sync.extern.StreamType;

/**
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 * 
 */
public class EdowebIngester implements IngestInterface {
    final static Logger logger = LoggerFactory.getLogger(EdowebIngester.class);

    private String namespace = "edoweb";

    Webclient webclient = null;
    String host = null;

    @Override
    public void init(String host, String user, String password, String ns) {
	this.namespace = ns;
	this.host = host;
	webclient = new Webclient(namespace, user, password, host);
    }

    @Override
    public ContentModel createContentModel() {
	// TODO implement
	return null;
    }

    @Override
    public void ingest(DigitalEntity dtlBean) {
	logger.info(dtlBean.getPid() + " " + "Start ingest: " + namespace + ":"
		+ dtlBean.getPid());

	String partitionC = null;
	String pid = null;
	pid = dtlBean.getPid();
	try {
	    ControlBean control = new ControlBean(dtlBean);
	    partitionC = control.getPartitionC().firstElement();
	} catch (Exception e) {
	    e.printStackTrace();
	}
	try {

	    if (partitionC.compareTo("EJO01") == 0) {
		if (dtlBean.isParent()) {
		    logger.info(pid + ": start ingesting eJournal");
		    ingestEJournalComplete(dtlBean);
		    logger.info(pid + ": end ingesting eJournal");
		} else {
		    logger.info(pid + ": start ingesting eJournal issue");
		    updatePart(dtlBean);
		    logger.info(pid + ": end ingesting eJournal issue");
		}
	    } else if (partitionC.compareTo("WPD01") == 0) {

		logger.info(pid + ": start updating monograph (wpd01)");
		updateMonographs(dtlBean);
		logger.info(pid + ": end updating monograph (wpd01)");
	    } else if (partitionC.compareTo("WPD02") == 0) {

		logger.info(pid + ": start updating monograph (wpd02)");
		updateMonographs(dtlBean);
		logger.info(pid + ": end updating monograph (wpd02)");
	    } else if (partitionC.compareTo("WSC01") == 0) {
		if (dtlBean.isParent()) {
		    logger.info(pid + ": start ingesting webpage (wsc01)");
		    ingestWebpageComplete(dtlBean);
		    logger.info(pid + ": end ingesting webpage (wsc01)");
		} else {
		    logger.info(pid
			    + ": start ingesting webpage version (wsc01)");
		    updateWebpagePart(dtlBean);
		    logger.info(pid + ": end ingesting webpage version (wsc01)");
		}
	    } else if (partitionC.compareTo("WSI01") == 0) {
		logger.info(pid + ": start updating webpage (wsi01)");
		updateSingleWebpage(dtlBean);
		logger.info(pid + ": end updating webpage (wsi01)");
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    logger.info(dtlBean.getPid() + " " + e.getMessage());
	}

	logger.info(dtlBean.getPid() + " " + "Thanx and goodbye!\n");
    }

    @Override
    public void update(DigitalEntity dtlBean) {
	logger.info(dtlBean.getPid() + " " + "Start update: " + namespace + ":"
		+ dtlBean.getPid());

	String partitionC = null;
	String pid = null;
	pid = dtlBean.getPid();
	try {
	    ControlBean control = new ControlBean(dtlBean);
	    partitionC = control.getPartitionC().firstElement();
	} catch (Exception e) {
	    e.printStackTrace();
	}
	try {

	    if (partitionC.compareTo("EJO01") == 0) {
		if (dtlBean.isParent()) {
		    logger.info(pid + ": start updating eJournal");
		    updateEJournalParent(dtlBean);
		    logger.info(pid + ": end updating eJournal");
		} else {
		    logger.info(pid + ": start updating eJournal issue");
		    updateVolume(dtlBean);
		    logger.info(pid + ": end updating eJournal issue");
		}
	    } else if (partitionC.compareTo("WPD01") == 0) {
		logger.info(pid + ": start updating monograph (wpd01)");
		updateMonographs(dtlBean);
		logger.info(pid + ": end updating monograph (wpd01)");
	    } else if (partitionC.compareTo("WPD02") == 0) {

		logger.info(pid + ": start updating monograph (wpd02)");
		updateMonographs(dtlBean);
		logger.info(pid + ": end updating monograph (wpd02)");
	    } else if (partitionC.compareTo("WSC01") == 0) {
		if (dtlBean.isParent()) {
		    logger.info(pid + ": start updating webpage (wsc01)");
		    updateWebpageParent(dtlBean);
		    logger.info(pid + ": end updating webpage (wsc01)");
		} else {
		    logger.info(pid
			    + ": start updating webpage version (wsc01)");
		    updateWebpagePart(dtlBean);
		    logger.info(pid + ": end updating webpage version (wsc01)");
		}
	    } else if (partitionC.compareTo("WSI01") == 0) {
		logger.info(pid + ": start updating webpage (wsi01)");
		updateSingleWebpage(dtlBean);
		logger.info(pid + ": end updating webpage (wsi01)");
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    logger.info(dtlBean.getPid() + " " + e.getMessage());
	}

    }

    @Override
    public void delete(String p) {
	webclient.delete(p);

    }

    private void updatePart(DigitalEntity dtlBean) {

	String usageType = dtlBean.getUsageType();
	if (usageType.compareTo(ObjectType.volume.toString()) == 0) {
	    updateVolume(dtlBean);
	} else // if (usageType.compareTo(ObjectType.issue.toString()) == 0)
	{
	    updateIssue(dtlBean);
	}
    }

    private void updateVolume(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	logger.info(pid + " " + "Found eJournal volume.");
	ObjectType t = ObjectType.volume;
	webclient.createResource(t, dtlBean);
	String metadata = "<" + pid
		+ "> <http://purl.org/ontology/bibo/volume> \""
		+ dtlBean.getLabel() + "\" .\n" + "<" + pid
		+ "> <http://iflastandards.info/ns/isbd/elements/P1004> \""
		+ dtlBean.getLabel() + "\" .\n";
	webclient.setMetadata(dtlBean, metadata);

	Vector<DigitalEntity> issues = dtlBean.getParts();
	int num = issues.size();
	int count = 1;
	logger.info(pid + " Found " + num + " issues.");
	for (DigitalEntity issue : issues) {
	    logger.info("Part: " + (count++) + "/" + num);
	    updateIssue(issue);
	}

	logger.info(pid + " " + "updated.\n");
    }

    private void updateIssue(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	logger.info(pid + " " + "Found eJournal issue.");
	ObjectType t = ObjectType.issue;
	webclient.createObject(dtlBean, "application/pdf", t);
	String metadata = "<" + pid
		+ "> <http://purl.org/ontology/bibo/volume> \""
		+ dtlBean.getLabel() + "\" .\n" + "<" + pid
		+ "> <http://iflastandards.info/ns/isbd/elements/P1004> \""
		+ dtlBean.getLabel() + "\" .\n";
	webclient.setMetadata(dtlBean, metadata);
	logger.info(pid + " " + "updated.\n");

    }

    private void updateWebpagePart(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	logger.info(pid + " Found webpage version.");

	webclient.createObject(dtlBean, "application/zip", ObjectType.version);
	logger.info(pid + " " + "updated.\n");
    }

    private void updateMonographs(DigitalEntity dtlBean) {

	String pid = namespace + ":" + dtlBean.getPid();
	logger.info(pid + " Found monograph.");
	webclient
		.createObject(dtlBean, "application/pdf", ObjectType.monograph);
	webclient.autoGenerateMetdata(dtlBean);
	logger.info(pid + " " + "updated.\n");
    }

    private void updateEJournalParent(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	try {
	    logger.info(pid + " Found ejournal.");
	    webclient.createResource(ObjectType.journal, dtlBean);
	    webclient.autoGenerateMetdata(dtlBean);
	    Vector<DigitalEntity> viewMainLinks = dtlBean.getViewMainLinks();
	    int numOfVols = viewMainLinks.size();
	    logger.info(pid + " " + "Found " + numOfVols + " volumes.");
	    logger.info(pid + " " + "Will not update volumes.");
	    logger.info(pid + " " + "updated.\n");
	} catch (Exception e) {
	    logger.error(pid + " " + e.getMessage());
	}

    }

    private void updateWebpageParent(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	try {
	    logger.info(pid + " Found webpage.");
	    webclient.createResource(ObjectType.webpage, dtlBean);
	    webclient.autoGenerateMetdata(dtlBean);
	    Vector<DigitalEntity> viewLinks = dtlBean.getViewLinks();
	    int numOfVersions = viewLinks.size();
	    logger.info(pid + " " + "Found " + numOfVersions + " versions.");
	    logger.info(pid + " " + "Will not update versions.");
	    logger.info(pid + " " + "updated.\n");
	} catch (Exception e) {
	    logger.info(pid + " " + e.getMessage());
	}

    }

    private void updateSingleWebpage(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	try {
	    logger.info(pid + " Found webpage.");
	    webclient.createResource(ObjectType.webpage, dtlBean);
	    webclient.autoGenerateMetdata(dtlBean);
	    for (DigitalEntity b : dtlBean.getArchiveLinks()) {
		b.setParentPid(dtlBean.getPid());
		Stream dataStream = b.getStream(StreamType.DATA);
		if (dataStream.getMimeType().compareTo("application/zip") == 0) {
		    webclient.createObject(b, "application/zip",
			    ObjectType.version);
		    break;
		}
	    }
	    logger.info(pid + " " + "updated.\n");
	} catch (Exception e) {
	    logger.error(pid + " " + e.getMessage());
	}

    }

    private void ingestEJournalComplete(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	try {
	    logger.info(pid + " Found ejournal.");
	    logger.info(dtlBean.toString());
	    webclient.createResource(ObjectType.journal, dtlBean);
	    webclient.autoGenerateMetdata(dtlBean);
	    Vector<DigitalEntity> volumes = dtlBean.getVolumes();
	    int numOfVols = volumes.size();
	    int count = 1;
	    logger.info(pid + " Found " + numOfVols + " parts.");
	    for (DigitalEntity b : volumes) {
		logger.info("Part: " + (count++) + "/" + numOfVols);
		updatePart(b);
	    }
	    logger.info(pid + " " + "and all volumes updated.\n");
	} catch (Exception e) {
	    logger.error(pid + " " + e.getMessage());
	}
    }

    private void ingestWebpageComplete(DigitalEntity dtlBean) {
	String pid = namespace + ":" + dtlBean.getPid();
	try {
	    logger.info(pid + " Found webpage.");

	    webclient.createResource(ObjectType.webpage, dtlBean);
	    webclient.autoGenerateMetdata(dtlBean);
	    Vector<DigitalEntity> viewLinks = dtlBean.getViewLinks();
	    int numOfVersions = viewLinks.size();
	    logger.info(pid + " Found " + numOfVersions + " versions.");
	    int count = 1;
	    for (DigitalEntity b : viewLinks) {
		logger.info("Part: " + (count++) + "/" + numOfVersions);
		updateWebpagePart(b);
	    }
	    logger.info(pid + " " + "and all versions updated.\n");
	} catch (Exception e) {
	    logger.info(pid + " " + e.getMessage());
	}

    }

    @Override
    public void setNamespace(String namespace) {
	this.namespace = namespace;

    }
}
