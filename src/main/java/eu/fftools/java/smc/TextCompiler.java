package eu.fftools.java.smc;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * <p>
 * Created by Clemens on 16.01.2017.
 */

public
class TextCompiler
    {
        public static final String version   = "smc 0.0.1";
        public static final String copyright = "";

        public static final String STR_CONTENT = "content";
        public static final String STR_LENGTH  = "length";
        public static final String STR_NR      = "nr";

        private String sourceFilename;
        private String msgFormat = "1";
        private String msgID     = "MSG";
        private String msgLang   = "en";
        private String header;
        private Boolean allowDEBUG     = new Boolean( false );
        private Boolean allowINFO      = new Boolean( false );
        private Boolean allowWARN      = new Boolean( false );
        private Boolean allowERROR     = new Boolean( false );
        private Boolean allowFATAL     = new Boolean( false );
        private Boolean allowParameter = new Boolean( false );
        private Boolean allowSpatial   = new Boolean( false );
        private Integer msgMax         = new Integer( 999 );
        private Integer msgCount       = new Integer( 0 );
        private Integer msgMaxLength   = new Integer( 0 );
        private Integer msgMaxNr       = new Integer( 0 );

        public
        TextCompiler( String[] args )
            {
                sourceFilename = args[ 0 ];
            }

        public static
        void main( String[] args )
            {
                System.out.println( version );
                System.out.println( copyright + "\n" );

                if ( args.length == 0 )
                    {
                        System.out.println( "Usage: java -jar scm.jar sourceFile\nsourceFile must be readable by current user" );
                        System.exit( 1 );
                    }

                TextCompiler cliapp = new TextCompiler( args );
                cliapp.compileFile();
                cliapp = null;

                System.exit( 0 );

            }

        public
        void compileFile()
            {
                File sourceFile    = new File( sourceFilename );
                int  warnings      = 0;
                int  lastMsgNumber = 0;

                if ( true == sourceFile.canRead() && sourceFile.isFile() )
                    {
                        System.out.print( "using input file " + sourceFilename );
                        int extension = sourceFilename.indexOf(".txt");

                        if ( extension == 0 )
                            {
                                System.out.println( "ERROR: malformed sourceName " + sourceFilename + ": extension .txt missing" );
                                System.exit( 1 );
                            }
                        String compileFilename = sourceFilename.substring(0,extension) + ".msg";

                        if ( compileFilename.equals( sourceFilename ) )
                            {
                                System.out.println( "ERROR: compile name " + compileFilename + " must be different from sourceName " + sourceFilename );
                                System.exit( 1 );
                            }
                        System.out.println( ", will create " + compileFilename );

                        ArrayList < LinkedHashMap < String, Object > > fseeks = new ArrayList <>();

                        // add empty Linked hashamp to occupy index 0
                        fseeks.add( new LinkedHashMap() );

                        try ( BufferedReader br = new BufferedReader( new FileReader( sourceFilename ) ) )
                            {
                                boolean textParsingRunning = false;
                                String  line, parsedLine;
                                int     lineNum            = 0;

                                while ( ( line = br.readLine() ) != null )
                                    {
                                        parsedLine = line.trim();
                                        if ( parsedLine.startsWith( "*" ) )
                                            { continue; }

                                        lineNum++;


                                        if ( textParsingRunning )
                                            {
                                                if ( parsedLine.contains( "=" ) )
                                                    {
                                                        // expect header setting
                                                        String[] splits = parsedLine.split( "=" );
                                                        String   key    = splits[ 0 ];
                                                        String   value  = splits[ 1 ];

                                                        if ( key.startsWith( msgID ) )
                                                            {
                                                                if ( key.endsWith( "D" ) && allowDEBUG.booleanValue() == false )
                                                                    {
                                                                        System.out.println( sourceFilename + "(" + lineNum + "): using class DEBUG but not allowed it, ignored." );
                                                                        warnings++;
                                                                    }
                                                                if ( key.endsWith( "I" ) && allowINFO.booleanValue() == false )
                                                                    {
                                                                        System.out.println( sourceFilename + "(" + lineNum + "): using class INFO but not allowed it, ignored." );
                                                                        warnings++;
                                                                    }
                                                                if ( key.endsWith( "W" ) && allowWARN.booleanValue() == false )
                                                                    {
                                                                        System.out.println( sourceFilename + "(" + lineNum + "): using class WARN but not allowed it, ignored." );
                                                                        warnings++;
                                                                    }
                                                                if ( key.endsWith( "E" ) && allowERROR.booleanValue() == false )
                                                                    {
                                                                        System.out.println( sourceFilename + "(" + lineNum + "): using class ERROR but not allowed it, ignored." );
                                                                        warnings++;
                                                                    }
                                                                if ( key.endsWith( "F" ) && allowFATAL.booleanValue() == false )
                                                                    {
                                                                        System.out.println( sourceFilename + "(" + lineNum + "): using class FATAL but not allowed it, ignored." );
                                                                        warnings++;
                                                                    }

                                                                if ( !key.endsWith( "D" ) && !key.endsWith( "I" ) && !key.endsWith( "W" ) && !key.endsWith( "E" ) && !key.endsWith( "F" ) )
                                                                    {
                                                                        System.out.println( sourceFilename + "(" + lineNum + "): mail formed msgid (class missing)." );
                                                                        warnings++;
                                                                    }


                                                                int msgNumber = Integer.parseInt( key.substring( msgID.length(), key.length() - 1 ) );

                                                                if ( fseeks.size() > msgNumber && fseeks.get( msgNumber ) != null )
                                                                    {
                                                                        System.out.println( sourceFilename + "(" + lineNum + "): duplicate message " + msgNumber + ", ignored." );
                                                                        warnings++;
                                                                    }
                                                                else if ( msgNumber != ( lastMsgNumber + 1 ) )
                                                                    {
                                                                        if ( msgNumber > ( lastMsgNumber + 1 ) )
                                                                            {
                                                                                System.out.println( sourceFilename
                                                                                                    + "("
                                                                                                    + lineNum
                                                                                                    + "): numbers missing (current "
                                                                                                    + msgNumber
                                                                                                    + ", expected "
                                                                                                    + ( lastMsgNumber + 1 )
                                                                                                    + ")"
                                                                                                  );
                                                                                warnings++;
                                                                                while ( msgNumber != ( lastMsgNumber + 1 ) )
                                                                                    {
                                                                                        LinkedHashMap < String, Object > stringEntry = new LinkedHashMap <>();

                                                                                        stringEntry.put( STR_NR, new Integer( lastMsgNumber ) );
                                                                                        stringEntry.put( STR_CONTENT, "" );
                                                                                        stringEntry.put( STR_LENGTH, new Integer( 0 ) );

                                                                                        fseeks.add( lastMsgNumber, stringEntry );
                                                                                        lastMsgNumber++;
                                                                                    }
                                                                            }
                                                                    }

                                                                LinkedHashMap < String, Object > stringEntry = new LinkedHashMap <>();

                                                                stringEntry.put( STR_NR, new Integer( msgNumber ) );
                                                                stringEntry.put( STR_CONTENT, value );
                                                                stringEntry.put( STR_LENGTH, new Integer( value.length() ) );

                                                                fseeks.add( msgNumber, stringEntry );

                                                                msgCount = new Integer( msgCount.intValue() + 1 );
                                                                lastMsgNumber = msgNumber;
                                                                if ( msgNumber > msgMaxNr.intValue() )
                                                                    { msgMaxNr = new Integer( msgNumber ); }
                                                            }
                                                    }

                                            }
                                        else
                                            {
                                                // gather header information
                                                if ( parsedLine.contains( "=" ) )
                                                    {
                                                        // expect header setting
                                                        String[] splits = parsedLine.split( "=" );
                                                        String   key    = splits[ 0 ];
                                                        String   value  = splits[ 1 ];

                                                        switch ( key )
                                                            {
                                                                case "msgFormat":
                                                                    msgFormat = value;
                                                                    break;
                                                                case "msgID":
                                                                    msgID = value;
                                                                    break;
                                                                case "msgMax":
                                                                    if ( Integer.parseInt( value ) < 99999 && Integer.parseInt( value ) > 0 )
                                                                        { msgMax = new Integer( value ); }
                                                                    break;
                                                                case "msgAllowDEBUG":
                                                                    allowDEBUG = new Boolean( value );
                                                                    break;
                                                                case "msgAllowINFO":
                                                                    allowINFO = new Boolean( value );
                                                                    break;
                                                                case "msgAllowWARN":
                                                                    allowWARN = new Boolean( value );
                                                                    break;
                                                                case "msgAllowERROR":
                                                                    allowERROR = new Boolean( value );
                                                                    break;
                                                                case "msgAllowFATAL":
                                                                    allowFATAL = new Boolean( value );
                                                                    break;
                                                                case "msgAllowParameter":
                                                                    allowParameter = new Boolean( value );
                                                                    break;
                                                                case "msgAllowSpatial":
                                                                    allowSpatial = new Boolean( value );
                                                                    break;
                                                                case "msgLang":
                                                                    msgLang = value;
                                                                    break;
                                                                default:
                                                                    System.out.println( sourceFilename + "(" + lineNum + "): unknown header \"" + key + "\" setting, ignored." );
                                                                    warnings++;
                                                            }
                                                    }
                                                else
                                                    {
                                                        // check für initial message (like MSG000)
                                                        String maxZeros = "00000".substring( 0, msgMax.toString().length() );

                                                        if ( parsedLine.startsWith( msgID + maxZeros ) )
                                                            {
                                                                textParsingRunning = true;
                                                                System.out.println( "header parsing finished in line " + lineNum );
                                                            }
                                                    }
                                            }
                                    }

                                br.close();
                            }
                        catch ( IOException exc )
                            {
                                System.out.println( "Exception during reading of " + sourceFilename + ":" + exc.getMessage() );
                                System.exit( 1 );
                            }

                        // at this point, the input is fully parsed and we got no problems so far
                        // so write the compiled message file

                        if ( allowSpatial.booleanValue() == false )
                            {
                                if ( msgMaxNr.intValue() != fseeks.size() )
                                    {
                                        System.out.println( "ERROR: compile name " + compileFilename + " logical error detected (wrong number of index entries)" );
                                        System.exit( 1 );
                                    }
                            }

                        // build compiled header
                        int bitfield = 0;
                        if ( allowDEBUG.booleanValue() )
                            { bitfield += 1; }
                        if ( allowINFO.booleanValue() )
                            { bitfield += 2; }
                        if ( allowWARN.booleanValue() )
                            { bitfield += 4; }
                        if ( allowERROR.booleanValue() )
                            { bitfield += 8; }
                        if ( allowFATAL.booleanValue() )
                            { bitfield += 16; }
                        if ( allowParameter.booleanValue() )
                            { bitfield += 32; }
                        if ( allowSpatial.booleanValue() )
                            { bitfield += 64; }

                        String headerMsgId     = ( msgID + "_____" ).substring( 0, 5 );
                        String headerMsgCount  = String.format( "%05d", msgCount.intValue() );
                        String headerMaxNr     = String.format( "%05d", msgMaxNr.intValue() );
                        String headerMaxLength = String.format( "%05d", msgMaxLength.intValue() );
                        String headerBitfield  = String.format( "%05d", bitfield );
                        String reserved        = "_______";

                        header = msgFormat
                                        + msgLang
                                        + headerMsgId
                                        + headerMaxNr
                                        + headerMsgCount
                                        + headerMaxLength
                                        + headerBitfield
                                        + reserved;

                        RandomAccessFile compileFile = null;

                        try
                            {
                                compileFile = new RandomAccessFile( compileFilename, "rw" );
                            }
                        catch ( FileNotFoundException exc )
                            {
                                System.out.println( "ERROR: cant create file " + compileFilename );
                                System.exit( 1 );
                            }

                        try
                            {
                                compileFile.seek(0L);
                            }
                        catch ( IOException exc )
                            {
                                System.out.println( "ERROR: exception occurred during file write:" + exc.getMessage() );
                                System.exit( 1 );
                            }

                        int offset     = header.length() + msgCount.intValue() * 4;
                        int nextNumber = 0;

                        for ( int counter = 0; counter < msgCount.intValue(); counter++ )
                            {
                                if ( counter == 0 )
                                    {
                                        try
                                            {
                                                compileFile.writeUTF( header );
                                            }
                                        catch ( IOException exc )
                                            {
                                                System.out.println( "ERROR: exception occurred during file write:" + exc.getMessage() );
                                                System.exit( 1 );
                                            }
                                    }
                                else
                                    {
                                        LinkedHashMap < String, Object > currentMsg = fseeks.get( counter );

                                        offset = addToFile( compileFile, currentMsg, counter, offset );
                                    }
                            }
                        try
                            {
                                compileFile.close();
                            }
                        catch ( IOException exc )
                            {
                                System.out.println( "ERROR: exception occurred during file write:" + exc.getMessage() );
                                System.exit( 1 );
                            }
                        System.out.println( msgCount + " messages written to " + compileFilename );

                        if ( warnings != 0 )
                            { System.out.println( warnings + " warnings encountered , please check messages" ); }
                    }
                else
                    {
                        System.out.println( "ERROR: can not access " + sourceFilename );
                        System.exit( 1 );
                    }

            }

        private
        int addToFile( RandomAccessFile compileFile, LinkedHashMap < String, Object > currentEntry, int indexNumber, int offset )
            {
                try
                    {
                        compileFile.seek( ( long ) header.length() + indexNumber * 4 );
                    }
                catch ( IOException exc )
                    {
                        System.out.println( "ERROR: exception occurred during file write:" + exc.getMessage() );
                        System.exit( 1 );
                    }
                if ( currentEntry != null )
                    {
                        byte wordOffsetLow = ( byte ) ( offset & 0xFF );
                        byte wordOffsetHi  = ( byte ) ( ( offset << 8 ) & 0xFF );
                        byte wordLengthLow = ( byte ) ( ( ( Integer ) currentEntry.get( STR_LENGTH ) ).intValue() & 0xFF );
                        byte wordLengthHi  = ( byte ) ( ( ( ( Integer ) currentEntry.get( STR_LENGTH ) ).intValue() << 8 ) & 0xFF );

                        try
                            {
                                compileFile.write( wordOffsetLow );
                                compileFile.write( wordOffsetHi );
                                compileFile.write( wordLengthLow );
                                compileFile.write( wordLengthHi );

                                compileFile.seek( ( long ) offset );
                                compileFile.writeUTF( ( String ) currentEntry.get( STR_CONTENT ) );
                            }
                        catch ( IOException exc )
                            {
                                System.out.println( "ERROR: exception occurred during file write:" + exc.getMessage() );
                                System.exit( 1 );
                            }

                        return offset + ( ( Integer ) currentEntry.get( STR_LENGTH ) ).intValue();
                    }
                else
                    {
                        try
                            {
                                compileFile.write( ( byte ) 0 );
                                compileFile.write( ( byte ) 0 );
                                compileFile.write( ( byte ) 0 );
                                compileFile.write( ( byte ) 0 );
                            }
                        catch ( IOException exc )
                            {
                                System.out.println( "ERROR: exception occurred during file write:" + exc.getMessage() );
                                System.exit( 1 );
                            }
                        return offset;
                    }
            }
    }
