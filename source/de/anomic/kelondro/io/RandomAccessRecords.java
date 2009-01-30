// kelondroRecords.java
// (C) 2007 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 03.08.2007 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate: 2006-04-02 22:40:07 +0200 (So, 02 Apr 2006) $
// $LastChangedRevision: 1986 $
// $LastChangedBy: orbiter $
//
// LICENSE
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

package de.anomic.kelondro.io;

import java.io.IOException;

import de.anomic.kelondro.table.RecordHandle;
import de.anomic.kelondro.table.Node;

public interface RandomAccessRecords {
    
    // this is now implemented by kelondroTray
    // the newNode method is used to define a enumeration in kelondroTray, but is still there abstract
    // the real implementation is done in kelondroEcoRecords and kelondroCachedRecords
    
    public Node newNode(RecordHandle handle, byte[] bulk, int offset) throws IOException;
    
}
