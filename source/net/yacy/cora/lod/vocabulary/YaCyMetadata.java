/**
 *  YaCyMetadata
 *  Copyright 2011 by Michael Peter Christen, mc@yacy.net, Frankfurt am Main, Germany
 *  First released 16.12.2011 at http://yacy.net
 *
 *  $LastChangedDate: 2011-04-14 00:04:23 +0200 (Do, 14 Apr 2011) $
 *  $LastChangedRevision: 7653 $
 *  $LastChangedBy: orbiter $
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package net.yacy.cora.lod.vocabulary;

import java.util.Set;

import net.yacy.cora.lod.Literal;
import net.yacy.cora.lod.Vocabulary;

/**
 * this is the vocabulary of the 'classic' YaCy metadata database
 */
public enum YaCyMetadata implements Vocabulary {

    hash,     // the url's hash
    comp,     // components: the url, description, author and tags. As 5th element, an ETag is possible
    mod,      // last-modified from the httpd
    load,     // time when the url was loaded
    fresh,    // time until this url is fresh
    referrer, // (one of) the url's referrer hash(es)
    md5,      // the md5 of the url content (to identify changes)
    size,     // size of file in bytes
    wc,       // size of file by number of words; for video and audio: seconds
    dt,       // doctype, taken from extension or any other heuristic
    flags,    // flags; any stuff (see Word-Entity definition)
    lang,     // language
    llocal,   // # of outlinks to same domain; for video and image: width 
    lother,   // # of outlinks to outside domain; for video and image: height
    limage,   // # of embedded image links
    laudio,   // # of embedded audio links; for audio: track number; for video: number of audio tracks
    lvideo,   // # of embedded video links
    lapp;     // # of embedded links to applications
    
    /*
        "String hash-12, " +            // the url's hash
        "String comp-360, " +           // components: the url, description, author and tags. As 5th element, an ETag is possible
        "Cardinal mod-4 {b256}, " +     // last-modified from the httpd
        "Cardinal load-4 {b256}, " +    // time when the url was loaded
        "Cardinal fresh-4 {b256}, " +   // time until this url is fresh
        "String referrer-12, " +        // (one of) the url's referrer hash(es)
        "byte[] md5-8, " +              // the md5 of the url content (to identify changes)
        "Cardinal size-6 {b256}, " +    // size of file in bytes
        "Cardinal wc-3 {b256}, " +      // size of file by number of words; for video and audio: seconds
        "byte[] dt-1, " +               // doctype, taken from extension or any other heuristic
        "Bitfield flags-4, " +          // flags; any stuff (see Word-Entity definition)
        "String lang-2, " +             // language
        "Cardinal llocal-2 {b256}, " +  // # of outlinks to same domain; for video and image: width 
        "Cardinal lother-2 {b256}, " +  // # of outlinks to outside domain; for video and image: height
        "Cardinal limage-2 {b256}, " +  // # of embedded image links
        "Cardinal laudio-2 {b256}, " +  // # of embedded audio links; for audio: track number; for video: number of audio tracks
        "Cardinal lvideo-2 {b256}, " +  // # of embedded video links
        "Cardinal lapp-2 {b256}",       // # of embedded links to applications
     */

    public final static String IDENTIFIER = "http://yacy.net/vocabularies/yacymetadata#";
    public final static String PREFIX = "yacy";
    
    private final String predicate;
    
    private YaCyMetadata() {
        this.predicate = PREFIX + ":" +  this.name();
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
    
    @Override
    public Set<Literal> getLiterals() {
        return null;
    }

    @Override
    public String getPredicate() {
        return this.predicate;
    }

}