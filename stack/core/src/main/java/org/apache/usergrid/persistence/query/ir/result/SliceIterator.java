/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.cassandra.CursorCache;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.exceptions.QueryIterationException;
import org.apache.usergrid.persistence.query.ir.QuerySlice;

import me.prettyprint.hector.api.beans.HColumn;


/**
 * An iterator that will take all slices and order them correctly
 *
 * @author tnine
 */
public class SliceIterator implements ResultIterator {


    protected final SliceParser parser;
    protected final IndexScanner scanner;
    private final int pageSize;
    protected final boolean isReversed;


    /**
     * Pointer to the uuid set until it's returned
     */
    private Set<ScanColumn> lastResult;

    /**
     * The pointer to the last set of parsed columns
     */
    private Set<ScanColumn> parsedCols;

    /**
     * counter that's incremented as we load pages. If pages loaded = 1 when reset, we don't have to reload from cass
     */
    private int pagesLoaded = 0;



    /**
     * @param scanner The scanner to use to read the cols
     * @param parser The parser for the scanner results
     */
    public SliceIterator(  IndexScanner scanner, SliceParser parser ) {
        this.parser = parser;
        this.scanner = scanner;
        this.pageSize = scanner.getPageSize();
        this.parsedCols = new LinkedHashSet<ScanColumn>( this.pageSize );
        this.isReversed = scanner.isReversed();
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Set<ScanColumn>> iterator() {
        return this;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        if ( lastResult == null ) {
            return load();
        }

        return true;
    }


    private boolean load() {
        if ( !scanner.hasNext() ) {
            return false;
        }

        Iterator<HColumn<ByteBuffer, ByteBuffer>> results = scanner.next().iterator();

        parsedCols.clear();

        while ( results.hasNext() ) {

            final HColumn<ByteBuffer, ByteBuffer> column = results.next();

            final ByteBuffer colName = column.getName().duplicate();

            final ScanColumn parsed = parser.parse( colName, isReversed );


            //skip this value, the parser has discarded it
            if ( parsed == null ) {
                continue;
            }

            parsedCols.add( parsed );
        }


        pagesLoaded++;

        lastResult = parsedCols;

        return lastResult != null && lastResult.size() > 0;
    }




    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    @Override
    public Set<ScanColumn> next() {
        Set<ScanColumn> temp = lastResult;
        lastResult = null;
        return temp;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is not supported" );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.result.ResultIterator#reset()
     */
    @Override
    public void reset() {
        // Do nothing, we'll just return the first page again
        if ( pagesLoaded == 1 ) {
            lastResult = parsedCols;
            return;
        }
        scanner.reset();
    }


}
