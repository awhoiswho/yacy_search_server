// Status.java 
// -----------------------
// part of YaCy
// (C) by Michael Peter Christen; mc@anomic.de
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Using this software in any meaning (reading, learning, copying, compiling,
// running) means that you agree that the Author(s) is (are) not responsible
// for cost, loss of data or any harm that may be caused directly or indirectly
// by usage of this softare or this documentation. The usage of this software
// is on your own risk. The installation and usage (starting/running) of this
// software may allow other people or application to access your computer and
// any attached devices and is highly dependent on the configuration of the
// software which must be done by the user of the software; the author(s) is
// (are) also not responsible for proper configuration and usage of the
// software, even if provoked by documentation provided together with
// the software.
//
// Any changes to this file according to the GPL as documented in the file
// gpl.txt aside this file in the shipment you received can be done to the
// lines that follows this copyright notice here, but changes must not be
// done inside the copyright notive above. A re-distribution must contain
// the intact and unchanged copyright notice.
// Contributions and changes to the program code must be marked as such.

// You must compile this file with
// javac -classpath .:../Classes Status.java
// if the shell's current path is HTROOT

import java.text.DecimalFormat;
import java.util.Date;

import de.anomic.http.httpHeader;
import de.anomic.http.httpd;
import de.anomic.http.httpdByteCountInputStream;
import de.anomic.http.httpdByteCountOutputStream;
import de.anomic.plasma.plasmaSwitchboard;
import de.anomic.server.serverCore;
import de.anomic.server.serverDate;
import de.anomic.server.serverMemory;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.yacy.yacyCore;
import de.anomic.yacy.yacySeed;

public class Status {

    private static final String SEEDSERVER = "seedServer";
    private static final String PEERSTATUS = "peerStatus";

    public static serverObjects respond(httpHeader header, serverObjects post, serverSwitch env) {
        // return variable that accumulates replacements
        final serverObjects prop = new serverObjects();
        final plasmaSwitchboard sb = (plasmaSwitchboard) env;

        if ((post != null) && (post.containsKey("login"))) {
            if (sb.adminAuthenticated(header) < 2) {
                prop.put("AUTHENTICATE","admin log-in");
            } else {
                prop.put("LOCATION","");
            }
            return prop;
        } else if (post != null) {
        	boolean redirect = false;
        	if (post.containsKey("pauseCrawlJob")) {
        		String jobType = (String) post.get("jobType");
        		if (jobType.equals("localCrawl")) 
                    sb.pauseCrawlJob(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL);
        		else if (jobType.equals("remoteTriggeredCrawl")) 
                    sb.pauseCrawlJob(plasmaSwitchboard.CRAWLJOB_REMOTE_TRIGGERED_CRAWL);
        		else if (jobType.equals("globalCrawlTrigger")) 
                    sb.pauseCrawlJob(plasmaSwitchboard.CRAWLJOB_GLOBAL_CRAWL_TRIGGER);  
        		redirect = true;
        	} else if (post.containsKey("continueCrawlJob")) {
        		String jobType = (String) post.get("jobType");
        		if (jobType.equals("localCrawl")) 
                    sb.continueCrawlJob(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL);
        		else if (jobType.equals("remoteTriggeredCrawl")) 
                    sb.continueCrawlJob(plasmaSwitchboard.CRAWLJOB_REMOTE_TRIGGERED_CRAWL);
        		else if (jobType.equals("globalCrawlTrigger")) 
                    sb.continueCrawlJob(plasmaSwitchboard.CRAWLJOB_GLOBAL_CRAWL_TRIGGER);    
        		redirect = true;
        	} else if (post.containsKey("ResetTraffic")) {
        		httpdByteCountInputStream.resetCount();
        		httpdByteCountOutputStream.resetCount();
        		redirect = true;
        	} else if (post.containsKey("popup")) {
                String trigger_enabled = (String) post.get("popup");
                if (trigger_enabled.equals("false")) {
                    sb.setConfig("browserPopUpTrigger", "false");
                } else if (trigger_enabled.equals("true")){
                    sb.setConfig("browserPopUpTrigger", "true");
                }
                redirect = true;
        	}
        	
        	if (redirect) {
        		prop.put("LOCATION","");
        		return prop;
        	}
        }
        
        /*
          versionProbe=http://www.anomic.de/AnomicHTTPProxy/release.txt
          superseedFile=superseed.txt
         */
        // update seed info
        yacyCore.peerActions.updateMySeed();

        boolean adminaccess = sb.adminAuthenticated(header) >= 2;
        if (adminaccess) {
            prop.put("showPrivateTable",1);
            prop.put("privateStatusTable", "Status_p.inc");
        } else { 
            prop.put("showPrivateTable",0);
            prop.put("privateStatusTable", "");
        }

        // password protection
        if (sb.getConfig(httpd.ADMIN_ACCOUNT_B64MD5, "").length() == 0) {
            prop.put("protection", 0); // not protected
            prop.put("urgentSetPassword", 1);
        } else {
            prop.put("protection", 1); // protected
        }

        // if running on Windows or with updater/wrapper enable restart button
        if ((sb.updaterCallback != null) || (System.getProperty("os.name").toLowerCase().startsWith("win"))) {
        prop.put("restartEnabled", 1); }
		
        // version information
        prop.put("versionpp", yacy.combined2prettyVersion(sb.getConfig("version","0.1")));
        
        
        double thisVersion = Double.parseDouble(sb.getConfig("version","0.1"));
        // cut off the SVN Rev in the Version
        try {thisVersion = Math.round(thisVersion*1000.0)/1000.0;} catch (NumberFormatException e) {}
        
        if ((adminaccess) && (sb.updaterCallback != null) && (sb.updaterCallback.updateYaCyIsPossible())){
        	prop.put("hintVersionAvailable", 1);
            prop.put("hintVersionAvailable_latestVersion", sb.updaterCallback.getYaCyUpdateReleaseVersion());
            if ((post != null) && (post.containsKey("aquirerelease"))) {
                sb.updaterCallback.grantYaCyUpdate();
            }
        }
        
        /*
        if ((adminaccess) && (yacyVersion.latestRelease >= (thisVersion+0.01))) { // only new Versions(not new SVN)
            if ((yacyVersion.latestMainRelease != null) ||
                (yacyVersion.latestDevRelease != null)) {
                prop.put("hintVersionDownload", 1);
            } else if ((post != null) && (post.containsKey("aquirerelease"))) {
                yacyVersion.aquireLatestReleaseInfo();
                prop.put("hintVersionDownload", 1);
            } else {
                prop.put("hintVersionAvailable", 1);
            }
        }
        prop.putASIS("hintVersionDownload_versionResMain", (yacyVersion.latestMainRelease == null) ? "-" : yacyVersion.latestMainRelease.toAnchor());
        prop.putASIS("hintVersionDownload_versionResDev", (yacyVersion.latestDevRelease == null) ? "-" : yacyVersion.latestDevRelease.toAnchor());
        prop.put("hintVersionAvailable_latestVersion", Double.toString(yacyVersion.latestRelease));
		*/
        
        // place some more hints
        if ((adminaccess) && (sb.getThread(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL).getJobCount() == 0) && (sb.getThread(plasmaSwitchboard.INDEXER).getJobCount() == 0)) {
            prop.put("hintCrawlStart", 1);
        }
        
        if ((adminaccess) && (sb.getThread(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL).getJobCount() > 500)) {
            prop.put("hintCrawlMonitor", 1);
        }
        
        
        
        // hostname and port
        String extendedPortString = sb.getConfig("port", "8080");
        int pos = extendedPortString.indexOf(":"); 
        prop.put("port",serverCore.getPortNr(extendedPortString));
        if (pos!=-1) {
            prop.put("extPortFormat",1);
            prop.put("extPortFormat_extPort",extendedPortString);
        } else {
            prop.put("extPortFormat",0);
        }
        prop.put("host", serverCore.publicLocalIP().getHostAddress());
        
        // ssl support
        prop.put("sslSupport",sb.getConfig("keyStore", "").length() == 0 ? 0:1);

        // port forwarding: hostname and port
        if ((serverCore.portForwardingEnabled) && (serverCore.portForwarding != null)) {
            prop.put("portForwarding", 1);
            prop.put("portForwarding_host", serverCore.portForwarding.getHost());
            prop.put("portForwarding_port", Integer.toString(serverCore.portForwarding.getPort()));
            prop.put("portForwarding_status", serverCore.portForwarding.isConnected() ? 1:0);
        } else {
            prop.put("portForwarding", 0);
        }

        if (sb.getConfig("remoteProxyUse", "false").equals("true")) {
            prop.put("remoteProxy", 1);
            prop.put("remoteProxy_host", sb.getConfig("remoteProxyHost", "<unknown>"));
            prop.put("remoteProxy_port", sb.getConfig("remoteProxyPort", "<unknown>"));
            prop.put("remoteProxy_4Yacy", sb.getConfig("remoteProxyUse4Yacy", "true").equalsIgnoreCase("true")?0:1);
        } else {
            prop.put("remoteProxy", 0); // not used
        }

        // peer information
        String thisHash = "";
        final String thisName = sb.getConfig("peerName", "<nameless>");
        if (yacyCore.seedDB.mySeed == null)  {
            thisHash = "not assigned";
            prop.put("peerAddress", 0);    // not assigned
            prop.put("peerStatistics", 0); // unknown
        } else {
            final long uptime = 60000 * Long.parseLong(yacyCore.seedDB.mySeed.get(yacySeed.UPTIME, "0"));
            prop.put("peerStatistics", 1);
            prop.put("peerStatistics_uptime", serverDate.intervalToString(uptime));
            prop.put("peerStatistics_pagesperminute", yacyCore.seedDB.mySeed.get(yacySeed.ISPEED, "unknown"));
            prop.put("peerStatistics_queriesperhour", Double.toString(Math.round(6000d * yacyCore.seedDB.mySeed.getQPM()) / 100d));
            prop.put("peerStatistics_links", groupDigits(yacyCore.seedDB.mySeed.get(yacySeed.LCOUNT, "0")));
            prop.put("peerStatistics_words", groupDigits(yacyCore.seedDB.mySeed.get(yacySeed.ICOUNT, "0")));
            prop.put("peerStatistics_juniorConnects", yacyCore.peerActions.juniorConnects);
            prop.put("peerStatistics_seniorConnects", yacyCore.peerActions.seniorConnects);
            prop.put("peerStatistics_principalConnects", yacyCore.peerActions.principalConnects);
            prop.put("peerStatistics_disconnects", yacyCore.peerActions.disconnects);
            prop.put("peerStatistics_connects", yacyCore.seedDB.mySeed.get(yacySeed.CCOUNT, "0"));
            if (yacyCore.seedDB.mySeed.getPublicAddress() == null) {
                thisHash = yacyCore.seedDB.mySeed.hash;
                prop.put("peerAddress", 0); // not assigned + instructions
                prop.put("warningGoOnline", 1);
            } else {
                thisHash = yacyCore.seedDB.mySeed.hash;
                prop.put("peerAddress", 1); // Address
                prop.put("peerAddress_address", yacyCore.seedDB.mySeed.getPublicAddress());
                prop.put("peerAddress_peername", sb.getConfig("peerName", "<nameless>").toLowerCase());
            }
        }
        final String peerStatus = ((yacyCore.seedDB.mySeed == null) ? yacySeed.PEERTYPE_VIRGIN : yacyCore.seedDB.mySeed.get(yacySeed.PEERTYPE, yacySeed.PEERTYPE_VIRGIN));
        if (peerStatus.equals(yacySeed.PEERTYPE_VIRGIN)) {
            prop.put(PEERSTATUS, 0);
            prop.put("urgentStatusVirgin", 1);
        } else if (peerStatus.equals(yacySeed.PEERTYPE_JUNIOR)) {
            prop.put(PEERSTATUS, 1);
            prop.put("warningStatusJunior", 1);
        } else if (peerStatus.equals(yacySeed.PEERTYPE_SENIOR)) {
            prop.put(PEERSTATUS, 2);
            prop.put("hintStatusSenior", 1);
        } else if (peerStatus.equals(yacySeed.PEERTYPE_PRINCIPAL)) {
            prop.put(PEERSTATUS, 3);
            prop.put("hintStatusPrincipal", 1);
            prop.put("hintStatusPrincipal_seedURL", yacyCore.seedDB.mySeed.get("seedURL", "?"));
        }
        prop.put("peerName", thisName);
        prop.put("hash", thisHash);
        
        final String seedUploadMethod = sb.getConfig("seedUploadMethod", "");
        if (!seedUploadMethod.equalsIgnoreCase("none") || 
            (seedUploadMethod.equals("") && sb.getConfig("seedFTPPassword", "").length() > 0) ||
            (seedUploadMethod.equals("") && sb.getConfig("seedFilePath", "").length() > 0)) {
            if (seedUploadMethod.equals("")) {
                if (sb.getConfig("seedFTPPassword", "").length() > 0) {
                    sb.setConfig("seedUploadMethod","Ftp");
                }
                if (sb.getConfig("seedFilePath", "").length() > 0) {
                    sb.setConfig("seedUploadMethod","File");
                }
            }

            if (seedUploadMethod.equalsIgnoreCase("ftp")) {
                prop.put(SEEDSERVER, 1); // enabled
                prop.put("seedServer_seedServer", sb.getConfig("seedFTPServer", ""));
            } else if (seedUploadMethod.equalsIgnoreCase("scp")) {
                prop.put(SEEDSERVER, 1); // enabled
                prop.put("seedServer_seedServer", sb.getConfig("seedScpServer", ""));
            } else if (seedUploadMethod.equalsIgnoreCase("file")) {
                prop.put(SEEDSERVER, 2); // enabled
                prop.put("seedServer_seedFile", sb.getConfig("seedFilePath", ""));
            }
            prop.put("seedServer_lastUpload",
                    serverDate.intervalToString(System.currentTimeMillis() - sb.yc.lastSeedUpload_timeStamp));
        } else {
            prop.put(SEEDSERVER, 0); // disabled
        }
        
        if (yacyCore.seedDB != null && yacyCore.seedDB.sizeConnected() > 0){
            prop.put("otherPeers", 1);
            prop.put("otherPeers_num", yacyCore.seedDB.sizeConnected());
        }else{
            prop.put("otherPeers", 0); // not online
        }

        if (sb.getConfig("browserPopUpTrigger", "false").equals("false")) {
            prop.put("popup", 0);
        } else {
            prop.put("popup", 1);
        }

        if (sb.getConfig("onlineMode", "1").equals("0")) {
            prop.put("omode", 0);
        } else if (sb.getConfig("onlineMode", "1").equals("1")) {
                prop.put("omode", 1);
            } else {
            prop.put("omode", 2);
        }

        final Runtime rt = Runtime.getRuntime();

        // memory usage and system attributes
        prop.put("freeMemory", bytesToString(rt.freeMemory()));
        prop.put("totalMemory", bytesToString(rt.totalMemory()));
        prop.put("maxMemory", bytesToString(serverMemory.max));
        prop.put("processors", rt.availableProcessors());

        // proxy traffic
        //prop.put("trafficIn",bytesToString(httpdByteCountInputStream.getGlobalCount()));
        prop.put("trafficProxy",bytesToString(httpdByteCountOutputStream.getAccountCount("PROXY")));
        prop.put("trafficCrawler",bytesToString(httpdByteCountInputStream.getAccountCount("CRAWLER")));

        // connection information
        serverCore httpd = (serverCore) sb.getThread("10_httpd");
        int activeSessionCount = httpd.getActiveSessionCount();
        int idleSessionCount = httpd.getIdleSessionCount();
        int maxSessionCount = httpd.getMaxSessionCount();
        prop.put("connectionsActive",Integer.toString(activeSessionCount));
        prop.put("connectionsMax",Integer.toString(maxSessionCount));
        prop.put("connectionsIdle",Integer.toString(idleSessionCount));
        
        // Queue information
        int indexingJobCount = sb.getThread("80_indexing").getJobCount()+sb.indexingTasksInProcess.size();
        int indexingMaxCount = plasmaSwitchboard.indexingSlots;
        int indexingPercent = (indexingMaxCount==0)?0:indexingJobCount*100/indexingMaxCount;
        prop.put("indexingQueueSize", Integer.toString(indexingJobCount));
        prop.put("indexingQueueMax", Integer.toString(indexingMaxCount));
        prop.put("indexingQueuePercent",(indexingPercent>100)?"100":Integer.toString(indexingPercent));
        
        int loaderJobCount = sb.cacheLoader.size();
        int loaderMaxCount = plasmaSwitchboard.crawlSlots;
        int loaderPercent = (loaderMaxCount==0)?0:loaderJobCount*100/loaderMaxCount;
        prop.put("loaderQueueSize", Integer.toString(loaderJobCount));        
        prop.put("loaderQueueMax", Integer.toString(loaderMaxCount));        
        prop.put("loaderQueuePercent", (loaderPercent>100)?"100":Integer.toString(loaderPercent));
        
        prop.put("localCrawlQueueSize", Integer.toString(sb.getThread(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL).getJobCount()));
        prop.put("localCrawlPaused",sb.crawlJobIsPaused(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL)?1:0);

        prop.put("remoteTriggeredCrawlQueueSize", Integer.toString(sb.getThread(plasmaSwitchboard.CRAWLJOB_REMOTE_TRIGGERED_CRAWL).getJobCount()));
        prop.put("remoteTriggeredCrawlPaused",sb.crawlJobIsPaused(plasmaSwitchboard.CRAWLJOB_REMOTE_TRIGGERED_CRAWL)?1:0);        

        prop.put("globalCrawlTriggerQueueSize", Integer.toString(sb.getThread(plasmaSwitchboard.CRAWLJOB_GLOBAL_CRAWL_TRIGGER).getJobCount()));
        prop.put("globalCrawlTriggerPaused",sb.crawlJobIsPaused(plasmaSwitchboard.CRAWLJOB_GLOBAL_CRAWL_TRIGGER)?1:0);                
        
        prop.put("stackCrawlQueueSize", Integer.toString(sb.sbStackCrawlThread.size()));       

        // return rewrite properties
        prop.put("date",(new Date()).toString());
        return prop;
    }

    public static String bytesToString(long byteCount) {
        try {
            final StringBuffer byteString = new StringBuffer();

            final DecimalFormat df = new DecimalFormat( "0.00" );
            if (byteCount > 1073741824) {
                byteString.append(df.format((double)byteCount / (double)1073741824 ))
                          .append(" GB");
            } else if (byteCount > 1048576) {
                byteString.append(df.format((double)byteCount / (double)1048576))
                          .append(" MB");
            } else if (byteCount > 1024) {
                byteString.append(df.format((double)byteCount / (double)1024))
                          .append(" KB");
            } else {
                byteString.append(Long.toString(byteCount))
                .append(" Bytes");
            }

            return byteString.toString();
        } catch (Exception e) {
            return "unknown";
        }

    }
    
    
    //TODO: groupDigits-functions (Status.java & Network.java) should
    //      be referenced in a single class (now double-implemented)
    private static String groupDigits(String sValue) {
        long lValue;
        try {
            if (sValue.endsWith(".0")) { sValue = sValue.substring(0, sValue.length() - 2); } // for Connects per hour, why float ?
            lValue = Long.parseLong(sValue);
        } catch (Exception e) {lValue = 0;}
        if (lValue == 0) { return "-"; }
        return groupDigits(lValue);
    }
    
    private static String groupDigits(long Number) {
        final String s = Long.toString(Number);
        String t = "";
        for (int i = 0; i < s.length(); i++) t = s.charAt(s.length() - i - 1) + (((i % 3) == 0) ? "." : "") + t;
        return t.substring(0, t.length() - 1);
    }

}
