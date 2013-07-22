package de.nrw.hbz.regal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.nrw.hbz.regal.sync.ingest.OpusDownloader;

public class TestOpusDownloader {
    Properties properties = new Properties();
    /*
     * 1637992 4676380 2258539 1638892 4628526
     */
    String pid = "1";// "3237397";//
    private final String piddownloaderServer;
    private final String piddownloaderDownloadLocation;

    public TestOpusDownloader() {
	try {
	    properties = new Properties();
	    properties.load(getClass().getResourceAsStream(
		    "/testOpusDownloader.properties"));
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	piddownloaderServer = properties.getProperty("piddownloader.server");
	piddownloaderDownloadLocation = properties
		.getProperty("piddownloader.downloadLocation");
    }

    @Before
    public void setUp() {
	try {

	    FileUtils.deleteDirectory(new File(piddownloaderDownloadLocation));

	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    @Test
    public void downloadPid() {

	System.out
		.println("de.nrw.hbz.dipp.downloader.TestOpusDownloader.java: To run this test please uncomment code.");

	OpusDownloader downloader = new OpusDownloader();
	downloader.init(piddownloaderServer, piddownloaderDownloadLocation);

	try {
	    downloader.download(pid);
	    File file = new File(piddownloaderDownloadLocation + File.separator
		    + pid);
	    Assert.assertTrue(file.exists());
	    FileUtils.deleteDirectory(file);
	    Assert.assertTrue(!file.exists());

	} catch (IOException e) {
	    e.printStackTrace();
	}

    }

}
