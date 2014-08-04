/*
 * Copyright (c) 2011-2013, Dan McNulty
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the UDI project nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS AND CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourcecrumbs.refimpl.dwarf.sections;

import java.nio.ByteOrder;
import java.util.List;
import java.util.TreeMap;

import org.codehaus.preon.annotation.BoundList;

import net.sourcecrumbs.api.files.UnknownFormatException;
import net.sourcecrumbs.refimpl.dwarf.constants.AbbreviationTag;
import net.sourcecrumbs.refimpl.dwarf.constants.AttributeName;
import net.sourcecrumbs.refimpl.dwarf.entries.AbbreviationTable;
import net.sourcecrumbs.refimpl.dwarf.entries.AttributeValue;
import net.sourcecrumbs.refimpl.dwarf.entries.CompilationUnit;
import net.sourcecrumbs.refimpl.elf.spec.sections.SectionContent;

/**
 * Represents a .debug_info section in an ELF file
 *
 * @author mcnulty
 */
public class DebugInfo implements SectionContent {

    public static final String SECTION_NAME = ".debug_info";

    // This assumes that the data used to decode this section is limited to just this section
    @BoundList(type = CompilationUnit.class)
    private List<CompilationUnit> compilationUnits;

    private TreeMap<Long, CompilationUnit> unitsByStartingAddress = new TreeMap<>();

    /**
     * Build the DIEs, given information available in the DebugAbbrev section
     *
     * @param debugAbbrev the DebugAbbrev section
     * @param debugStr the DebugStr section
     * @param byteOrder the byte order of the target machine
     *
     * @throws UnknownFormatException when the data is not in the expected format
     */
    public void buildDIEs(DebugAbbrev debugAbbrev, DebugStr debugStr, ByteOrder byteOrder) throws UnknownFormatException {
        for (CompilationUnit compilationUnit : compilationUnits) {
            for (AbbreviationTable abbrevTable : debugAbbrev.getAbbreviationTables()) {
                if (abbrevTable.getSectionOffset() == compilationUnit.getHeader().getDebugAbbrevOffset()) {
                    compilationUnit.buildDIEs(abbrevTable, debugStr, byteOrder);
                }
            }
            if (compilationUnit.getRootDIE().getTag() == AbbreviationTag.DW_TAG_compile_unit) {
                for (AttributeValue value : compilationUnit.getRootDIE().getAttributeValues()) {
                    if (value.getName() == AttributeName.DW_AT_low_pc) {
                        unitsByStartingAddress.put(value.getDataAsLong(byteOrder), compilationUnit);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Retrieves the compilation unit that contains an address
     *
     * @param containingAddress the containing address
     *
     * @return the compilation unit
     */
    public CompilationUnit getCompilationUnit(Long containingAddress) {
        return unitsByStartingAddress.floorEntry(containingAddress).getValue();
    }
}
