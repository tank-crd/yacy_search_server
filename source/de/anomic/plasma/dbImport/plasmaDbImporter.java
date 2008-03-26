package de.anomic.plasma.dbImport;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import de.anomic.index.indexContainer;
import de.anomic.index.indexRWIEntry;
import de.anomic.index.indexRWIRowEntry;
import de.anomic.index.indexURLReference;
import de.anomic.plasma.plasmaSwitchboard;
import de.anomic.plasma.plasmaWordIndex;
import de.anomic.server.serverDate;

public class plasmaDbImporter extends AbstractImporter implements dbImporter {

	private File importPrimaryPath, importSecondaryPath;
	
	/**
	 * the source word index (the DB to import)
	 */
    private plasmaWordIndex importWordIndex;
    
    /**
     * the destination word index (the home DB)
     */
    protected plasmaWordIndex homeWordIndex;
    private int importStartSize;   

    private String wordHash = "------------";
    
    long wordChunkStart = System.currentTimeMillis(), wordChunkEnd = this.wordChunkStart;
    String wordChunkStartHash = "------------", wordChunkEndHash;
    private long urlCounter = 0, wordCounter = 0, entryCounter = 0, notBoundEntryCounter = 0;
    

    public plasmaDbImporter(plasmaSwitchboard sb, plasmaWordIndex homeWI, plasmaWordIndex importWI) {
    	super("PLASMADB",sb);
        this.homeWordIndex = homeWI;
        this.importWordIndex = importWI;
    }
    
    /**
     * @see dbImporter#getJobName()
     */
    public String getJobName() {
        return this.importPrimaryPath.toString();
    }

    /**
     * @see dbImporter#getStatus()
     */
    public String getStatus() {
        StringBuffer theStatus = new StringBuffer();
        
        theStatus.append("Hash=").append(this.wordHash).append("\n");
        theStatus.append("#URL=").append(this.urlCounter).append("\n");
        theStatus.append("#Word Entity=").append(this.wordCounter).append("\n");
        theStatus.append("#Word Entry={").append(this.entryCounter);
        theStatus.append(" ,NotBound=").append(this.notBoundEntryCounter).append("}");
        
        return theStatus.toString();
    }
    
    //public void init(File thePrimaryPath, File theSecondaryPath, int theCacheSize, long preloadTime) {
    /**
     * @throws ImporterException 
     * @see dbImporter#init(HashMap)
     */
    public void init(plasmaSwitchboard db, int cacheSize) throws ImporterException {
        super.init();
        
        // TODO: we need more errorhandling here
        this.importPrimaryPath = sb.indexPrimaryPath;
        this.importSecondaryPath = sb.indexSecondaryPath;

        this.cacheSize = cacheSize;
        if (this.cacheSize < 2*1024*1024) this.cacheSize = 8*1024*1024;
        
        // configure import DB
        String errorMsg = null;
        if (!this.importPrimaryPath.exists()) errorMsg = "Primary Import directory does not exist.";
        if (!this.importPrimaryPath.canRead()) errorMsg = "Primary Import directory is not readable.";
        if (!this.importPrimaryPath.canWrite()) errorMsg = "Primary Import directory is not writeable";
        if (!this.importPrimaryPath.isDirectory()) errorMsg = "Primary Import directory is not a directory.";
        if (errorMsg != null) {
            this.log.logSevere(errorMsg + "\nName: " + this.importPrimaryPath.getAbsolutePath());
            throw new IllegalArgumentException(errorMsg);
        }
        if (!this.importSecondaryPath.exists()) errorMsg = "Secondary Import directory does not exist.";
        if (!this.importSecondaryPath.canRead()) errorMsg = "Secondary Import directory is not readable.";
        if (!this.importSecondaryPath.canWrite()) errorMsg = "Secondary Import directory is not writeable";
        if (!this.importSecondaryPath.isDirectory()) errorMsg = "Secondary Import directory is not a directory.";
        if (errorMsg != null) {
            this.log.logSevere(errorMsg + "\nName: " + this.importSecondaryPath.getAbsolutePath());
            throw new IllegalArgumentException(errorMsg);
        }
        
        this.log.logFine("Initializing source word index db.");
        this.importWordIndex = new plasmaWordIndex(this.importPrimaryPath, this.importSecondaryPath, sb.getConfig("network.unit.name", ""), this.log);

        this.importStartSize = this.importWordIndex.size();
    }
    
    public void run() {
        try {
            importWordsDB();
        } finally {
            this.globalEnd = System.currentTimeMillis();
            //this.sb.dbImportManager.finishedJobs.add(this);
        }
    }

    /**
     * @see dbImporter#getProcessingStatusPercent()
     */
    public int getProcessingStatusPercent() {
        // thid seems to be better:
        // (this.importStartSize-this.importWordIndex.size())*100/((this.importStartSize==0)?1:this.importStartSize);
        // but maxint (2,147,483,647) could be exceeded when WordIndexes reach 20M entries
        //return (this.importStartSize-this.importWordIndex.size())/((this.importStartSize<100)?1:(this.importStartSize)/100);
        return (int)(this.wordCounter)/((this.importStartSize<100)?1:(this.importStartSize)/100);
    }

    /**
     * @see dbImporter#getElapsedTime()
     */
    public long getEstimatedTime() {
        return (this.wordCounter==0)?0:((this.importStartSize*getElapsedTime())/this.wordCounter)-getElapsedTime();
    }
    
    public void importWordsDB() {
        this.log.logInfo("STARTING DB-IMPORT");  
        
        try {
            this.log.logInfo("Importing DB from '" + this.importPrimaryPath.getAbsolutePath() + "'/'" + this.importSecondaryPath.getAbsolutePath() + "'");
            this.log.logInfo("Home word index contains " + homeWordIndex.size() + " words and " + homeWordIndex.countURL() + " URLs.");
            this.log.logInfo("Import word index contains " + this.importWordIndex.size() + " words and " + this.importWordIndex.countURL() + " URLs.");                        
            
            HashSet<String> unknownUrlBuffer = new HashSet<String>();
            HashSet<String> importedUrlBuffer = new HashSet<String>();
			
            // iterate over all words from import db
            //Iterator importWordHashIterator = this.importWordIndex.wordHashes(this.wordChunkStartHash, plasmaWordIndex.RL_WORDFILES, false);
            Iterator<indexContainer> indexContainerIterator = this.importWordIndex.indexContainerSet(this.wordChunkStartHash, false, false, 100).iterator();
            while (!isAborted() && indexContainerIterator.hasNext()) {
                
                TreeSet<String> entityUrls = new TreeSet<String>();
                indexContainer newContainer = null;
                try {
                    this.wordCounter++;
                    newContainer = (indexContainer) indexContainerIterator.next();
                    this.wordHash = newContainer.getWordHash();
                    
                    // loop throug the entities of the container and get the
                    // urlhash
                    Iterator<indexRWIRowEntry> importWordIdxEntries = newContainer.entries();
                    indexRWIEntry importWordIdxEntry;
                    while (importWordIdxEntries.hasNext()) {
                        // testing if import process was aborted
                        if (isAborted()) break;

                        // getting next word index entry
                        importWordIdxEntry = (indexRWIEntry) importWordIdxEntries.next();
                        String urlHash = importWordIdxEntry.urlHash();
                        entityUrls.add(urlHash);
                    }

                    Iterator<String> urlIter = entityUrls.iterator();
                    while (urlIter.hasNext()) {	
                        if (isAborted()) break;
                        String urlHash = urlIter.next();

                        if (importedUrlBuffer.contains(urlHash)) {
                            // already known url
                        } else if (unknownUrlBuffer.contains(urlHash)) {
                            // url known as unknown
                            unknownUrlBuffer.add(urlHash);
                            notBoundEntryCounter++;
                            newContainer.remove(urlHash);
                            continue;
                        } else {
                            // we need to import the url

                            // getting the url entry
                            indexURLReference urlEntry = this.importWordIndex.getURL(urlHash, null, 0);
                            if (urlEntry != null) {

                                /* write it into the home url db */
                                homeWordIndex.putURL(urlEntry);
                                importedUrlBuffer.add(urlHash);
                                this.urlCounter++;

                                if (this.urlCounter % 500 == 0) {
                                    this.log.logFine(this.urlCounter + " URLs processed so far.");
                                }

                            } else {
                                unknownUrlBuffer.add(urlHash);
                                notBoundEntryCounter++;
                                newContainer.remove(urlHash);
                                continue;
                            }
                        }
                        this.entryCounter++;
                    }
					
                    // testing if import process was aborted
                    if (isAborted()) break;
                    
                    // importing entity container to home db
                    if (newContainer.size() > 0) { homeWordIndex.addEntries(newContainer); }
                    
                    // delete complete index entity file
                    this.importWordIndex.deleteContainer(this.wordHash);                 
                    
                    // print out some statistical information
                    if (this.entryCounter % 500 == 0) {
                        this.log.logFine(this.entryCounter + " word entries and " + this.wordCounter + " word entities processed so far.");
                    }

                    if (this.wordCounter%500 == 0) {
                        this.wordChunkEndHash = this.wordHash;
                        this.wordChunkEnd = System.currentTimeMillis();
                        long duration = this.wordChunkEnd - this.wordChunkStart;
                        this.log.logInfo(this.wordCounter + " word entities imported " +
                                "[" + this.wordChunkStartHash + " .. " + this.wordChunkEndHash + "] " +
                                this.getProcessingStatusPercent() + "%\n" + 
                                "Speed: "+ 500*1000/duration + " word entities/s" +
                                " | Elapsed time: " + serverDate.formatInterval(getElapsedTime()) +
                                " | Estimated time: " + serverDate.formatInterval(getEstimatedTime()) + "\n" + 
                                "Home Words = " + homeWordIndex.size() + 
                                " | Import Words = " + this.importWordIndex.size());
                        this.wordChunkStart = this.wordChunkEnd;
                        this.wordChunkStartHash = this.wordChunkEndHash;
                    }                    
                    
                } catch (Exception e) {
                    this.log.logSevere("Import of word entity '" + this.wordHash + "' failed.",e);
                } finally {
                    if (newContainer != null) newContainer.clear();
                }

                if (!indexContainerIterator.hasNext()) {
                    // We may not be finished yet, try to get the next chunk of wordHashes
                    TreeSet<indexContainer> containers = this.importWordIndex.indexContainerSet(this.wordHash, false, false, 100);
                    indexContainerIterator = containers.iterator();
                    // Make sure we don't get the same wordhash twice, but don't skip a word
                    if ((indexContainerIterator.hasNext())&&(!this.wordHash.equals(((indexContainer) indexContainerIterator.next()).getWordHash()))) {
                        indexContainerIterator = containers.iterator();
                    }
                }
            }
            
            this.log.logInfo("Home word index contains " + homeWordIndex.size() + " words and " + homeWordIndex.countURL() + " URLs.");
            this.log.logInfo("Import word index contains " + this.importWordIndex.size() + " words and " + this.importWordIndex.countURL() + " URLs.");
        } catch (Exception e) {
            this.log.logSevere("Database import failed.",e);
            e.printStackTrace();
            this.error = e.toString();
        } finally {
            this.log.logInfo("Import process finished.");
            if (this.importWordIndex != null) try { this.importWordIndex.close(); } catch (Exception e){}
        }
    }    
    

    
}
