package csv;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CSV

{
    static final private int NUMMARK = 10;
    static final private char COMMA = ',';
    static final private char DQUOTE = '"';
    static final private char CRETURN = '\r';
    static final private char LFEED = '\n';
    static final private char SQUOTE = '\'';
    static final private char COMMENT = '#';

    private boolean stripMultipleNewlines;

    private char separator;
    private ArrayList<String> fields;
    private boolean eofSeen;
    private Reader in;

    static public Reader stripBom(InputStream in) throws java.io.IOException

    {
        PushbackInputStream pin = new PushbackInputStream(in, 3);
        byte[] b = new byte[3];
        int len = pin.read(b, 0, b.length);
        if ( (b[0] & 0xFF) == 0xEF && len == 3 ) {
            if ( (b[1] & 0xFF) == 0xBB &&
                    (b[2] & 0xFF) == 0xBF ) {
                return new InputStreamReader(pin, "UTF-8");
            } else {
                pin.unread(b, 0, len);
            }
        }
        else if ( len >= 2 ) {
            if ( (b[0] & 0xFF) == 0xFE &&
                    (b[1] & 0xFF) == 0xFF ) {
                return new InputStreamReader(pin, "UTF-16BE");
            } else if ( (b[0] & 0xFF) == 0xFF &&
                    (b[1] & 0xFF) == 0xFE ) {
                return new InputStreamReader(pin, "UTF-16LE");
            } else {
                pin.unread(b, 0, len);
            }
        } else if ( len > 0 ) {
            pin.unread(b, 0, len);
        }
        return new InputStreamReader(pin, "UTF-8");
    }

    public CSV(boolean stripMultipleNewlines,
               char separator,
               Reader input)
    {
        this.stripMultipleNewlines = stripMultipleNewlines;
        this.separator = separator;
        this.fields = new ArrayList<>();
        this.eofSeen = false;
        this.in = new BufferedReader(input);
    }

    public CSV(boolean stripMultipleNewlines,
               char separator,
               InputStream input)
            throws java.io.IOException,
            java.io.UnsupportedEncodingException
    {
        this.stripMultipleNewlines = stripMultipleNewlines;
        this.separator = separator;
        this.fields = new ArrayList<String>();
        this.eofSeen = false;
        this.in = new BufferedReader(stripBom(input));
    }

    public boolean hasNext() throws java.io.IOException
    {
        if ( eofSeen ) return false;
        fields.clear();
        eofSeen = split( in, fields );
        if ( eofSeen ) return ! fields.isEmpty();
        else return true;
    }

    public List<String> next()
    {
        return fields;
    }

    //zwraca prawdę jeśli koniec pliku (EOF)
    static private boolean discardLinefeed(Reader in,
                                           boolean stripMultiple)
            throws java.io.IOException
    {
        if ( stripMultiple ) {
            in.mark(NUMMARK);
            int value = in.read();
            while ( value != -1 ) {
                char c = (char)value;
                if ( c != CRETURN && c != LFEED ) {
                    in.reset();
                    return false;
                } else {
                    in.mark(NUMMARK);
                    value = in.read();
                }
            }
            return true;
        } else {
            in.mark(NUMMARK);
            int value = in.read();
            if ( value == -1 ) return true;
            else if ( (char)value != LFEED ) in.reset();
            return false;
        }
    }

    private boolean skipComment(Reader in)
            throws java.io.IOException
    {
        /* odrzuca linię */
        int value;
        while ( (value = in.read()) != -1 ) {
            char c = (char)value;
            if ( c == CRETURN )
                return discardLinefeed( in, stripMultipleNewlines );
        }
        return true;
    }


    // Zwraca wartość true, gdy EOF (koniec pliku)
    private boolean split(Reader in,ArrayList<String> fields)
            throws java.io.IOException
    {
        StringBuilder sbuf = new StringBuilder();
        int value;
        while ( (value = in.read()) != -1 ) {
            char c = (char)value;
            switch(c) {
                case CRETURN:
                    if ( sbuf.length() > 0 ) {
                        fields.add( sbuf.toString() );
                        sbuf.delete( 0, sbuf.length() );
                    }
                    return discardLinefeed( in, stripMultipleNewlines );

                case LFEED:
                    if ( sbuf.length() > 0 ) {
                        fields.add( sbuf.toString() );
                        sbuf.delete( 0, sbuf.length() );
                    }
                    if ( stripMultipleNewlines )
                        return discardLinefeed( in, stripMultipleNewlines );
                    else return false;

                case DQUOTE:
                {
                    // Procesowanie znaku cudzysłowia
                    while ( (value = in.read()) != -1 ) {
                        c = (char)value;
                        if ( c == DQUOTE ) {

                            in.mark(NUMMARK);
                            if ( (value = in.read()) == -1 ) {

                                if ( sbuf.length() > 0 ) {
                                    fields.add( sbuf.toString() );
                                    sbuf.delete( 0, sbuf.length() );
                                }
                                return true;
                            } else if ( (c = (char)value) == DQUOTE ) {

                                sbuf.append( DQUOTE );
                            } else if ( c == CRETURN ) {

                                if ( sbuf.length() > 0 ) {
                                    fields.add( sbuf.toString() );
                                    sbuf.delete( 0, sbuf.length() );
                                }

                                return discardLinefeed( in,
                                        stripMultipleNewlines );
                            } else if ( c == LFEED ) {

                                if ( sbuf.length() > 0 ) {
                                    fields.add( sbuf.toString() );
                                    sbuf.delete( 0, sbuf.length() );
                                }

                                if ( stripMultipleNewlines )
                                    return discardLinefeed( in, stripMultipleNewlines );
                                else return false;
                            } else {

                                in.reset();
                                break;
                            }
                        } else {

                            sbuf.append( c );
                        }
                    }

                    if ( value == -1 ) {

                        if ( sbuf.length() > 0 ) {
                            fields.add( sbuf.toString() );
                            sbuf.delete( 0, sbuf.length() );
                        }
                        return true;
                    }
                }
                break;

                default:
                    if ( c == separator ) {
                        fields.add( sbuf.toString() );
                        sbuf.delete(0, sbuf.length());
                    } else {
                        /* znak dla komentarza '#' */
                        if ( c == COMMENT && fields.isEmpty() &&
                                sbuf.toString().trim().isEmpty() ) {
                            boolean eof = skipComment(in);
                            if ( eof ) return eof;
                            else sbuf.delete(0, sbuf.length());
                        } else sbuf.append(c);
                    }
            }
        }
        if ( sbuf.length() > 0 ) {
            fields.add( sbuf.toString() );
            sbuf.delete( 0, sbuf.length() );
        }
        return true;
    }
}
