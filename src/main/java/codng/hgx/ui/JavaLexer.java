package codng.hgx.ui;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;


public class JavaLexer
{
    public CharSequence text;
    private int startOffset;
    public int offset;
    private int endOffset;
    private int column;
    private int line = 1;
    private int lastColumn = 1;

    //Current token
    private int tokenStart;


    public JavaLexer(CharSequence text, int startOffset, int length)
    {
        this.text = text;
        this.startOffset = startOffset;
        this.endOffset = startOffset + length;
        this.offset = startOffset;
    }

    public JavaToken next() throws ParseException
    {
        skipWhiteSpace();

        JavaToken token;
        tokenStart = offset;
        final char c = read();


        if(c == EOF) {
            token = JavaToken.EOF;
        } else if(isDigit(c,10)) {
            unread();
            token = parseNumber();
        } else if(read() == '{' && c == '$') { //Special case
            token = makeToken(TokenType.DOLLAR_LCURLY);
        } else {
            unread();
            if(isIdStart(c)) {
                token = parseId();
			} else if(c == '#') {
				token = parseAnnotation(TokenType.DIRECTIVE);
			} else if(c == '@') {
				if( read() == '"' ) {
					token = parseString(TokenType.AT_STRING);
				} else {
					token = parseAnnotation(TokenType.ANNOTATION);
				}
            } else if(c == '"') {
                token = parseString(TokenType.STRING);
            } else {
                TokenType ttype = parseMultichar(c);
                if(ttype == null) {
                    token = makeToken(TokenType.ERROR);
                } else {
                    token = makeToken(ttype);
                }
            }
        }

        return token;
    }

	private JavaToken parseAnnotation(TokenType ttype) {
		JavaToken token;
		read();
		char c = read();
		while(isIdPart(c)) {
			c = read();
		}
		unread();
		token = makeToken(ttype);
		return token;
	}

	private JavaToken parseString(TokenType ttype) throws ParseException
    {
        char c = read();
        loop: while(c != '"') {
            switch(c) {
            case '\\':
                c = read();
                switch(c) {
                case '\\':
                case '"':
                case '\'':
                case 'b':
                case 't':
                case 'n':
                case 'f':
                case 'r':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case 'u': // TODO: these are not properly parsed, but it's unnecessary for syntax highlighting
                    break;
                default:
                    throw new ParseException("Unrecognized escape sequence", -1);
                }
                break;
            case '\n':
            case '\r':
            case EOF:
//                throw new ParseException("Unterminated string constant", -1);
				// Just terminate it
                break loop;
            }
			c = read();
        }
        if(c != '"') unread();
        return makeToken(ttype);
    }

    private TokenType parseMultichar(char c)
    {
        final TokenType ttype;
        switch(c) {
        case '.': ttype = TokenType.DOT; break;
        case ',': ttype = TokenType.COMMA; break;
        case ';': ttype = TokenType.SEMICOLON; break;
        case ':': ttype = TokenType.COLON; break;
        case '^': ttype = TokenType.BXOR; break;
        case '?': ttype = TokenType.QMARK; break;
        case '[': ttype = TokenType.LBRACKET; break;
        case ']': ttype = TokenType.RBRACKET; break;
        case '{': ttype = TokenType.LCURLY; break;
        case '}': ttype = TokenType.RCURLY; break;
        case '(': ttype = TokenType.LPAREN; break;
        case ')': ttype = TokenType.RPAREN; break;
        case '<':
            switch(read()) {
            case '=': ttype = TokenType.LE; break;
            case '<': ttype = TokenType.LSHIFT; break;
            default: unread();ttype = TokenType.LT;
            }
            break;
        case '>':
            switch(read()) {
            case '=': ttype = TokenType.GE; break;
            case '>': ttype = TokenType.RSHIFT; break;
            default: unread();ttype = TokenType.GT;
            }
            break;
        case '=':
            switch(read()) {
            case '=': ttype = TokenType.EQ; break;
            default: unread();ttype = TokenType.ASSIGN;
            }
            break;
        case '*':
            switch(read()) {
            case '=': ttype = TokenType.MUL_ASSIGN; break;
            default: unread();ttype = TokenType.MUL;
            }
            break;
        case '/':
            switch(read()) {
            case '=': ttype = TokenType.DIV_ASSIGN; break;
            case '/':
                ttype = TokenType.SL_COMMENT;
                for(c = read(); c != '\n' && c != EOF; c = read());
				if(c == EOF) unread();
                break;
            case '*':
                ttype = TokenType.ML_COMMENT;
                for(c = read(); c != EOF; c = read()) {
                    if(c == '*') {
                        if(read() == '/') {
                            break;
                        } else {
                            unread();
                        }
                    }
                }
				if(c == EOF) unread(); // This should fail for Java
                break;
            default: unread();ttype = TokenType.DIV;
            }
            break;
        case '%':
            switch(read()) {
            case '=': ttype = TokenType.MOD_ASSIGN; break;
            default: unread();ttype = TokenType.MOD;
            }
            break;

        case '&':
            switch(read()) {
            case '&': ttype = TokenType.LAND; break;
            default: unread();ttype = TokenType.BAND;
            }
            break;

        case '|':
            switch(read()) {
            case '|': ttype = TokenType.LOR; break;
            default: unread();ttype = TokenType.BOR;
            }
            break;


        case '+':
            switch(read()) {
            case '=': ttype = TokenType.PLUS_ASSIGN; break;
            case '+': ttype = TokenType.INC; break;
            default: unread();ttype = TokenType.PLUS;
            }
            break;
        case '-':
            switch(read()) {
            case '=': ttype = TokenType.MINUS_ASSIGN; break;
            case '-': ttype = TokenType.DEC; break;
            default: unread();ttype = TokenType.MINUS;
            }
            break;
        case '!':
            switch(read()) {
            case '=': ttype = TokenType.NEQ; break;
            default: unread();ttype = TokenType.NOT;
            }
            break;
        default:
            ttype = null;
        }
        return ttype;
    }

    private JavaToken parseId()
    {
        char c = read();
        while(isIdPart(c)) {
            c = read();
        }
        unread();
		return makeToken(TokenType.ID);
    }

    private boolean isIdPart(char c)
    {
        return c != EOF && Character.isJavaIdentifierPart(c);
    }

    private boolean isIdStart(char c)
    {
        return c != EOF && Character.isJavaIdentifierStart(c);
    }

    JavaToken parseNumber()
    {
        TokenType ttype = TokenType.INT_LITERAL;
        char c = read();
        int radix = 10;


        if(c == '0') {
            c = read();
            if(c == 'x' || c == 'X') {
                radix = 16;
                c = read();
            } else if(c == '.') {
                radix = 10;
            } else {
                radix = 8;
            }
        }

        while(isDigit(c, radix)) {
            c = read();
        }

        if(radix == 10) {
            if(c == '.') {
                c = read();
                while(isDigit(c, radix)) {
                    c = read();
                }
                ttype = TokenType.FLOAT_LITERAL;
            }

            if (c == 'E' || c == 'e') {
                c = read();
                if(c == '+' || c == '-') {
                    c = read();
                }
                while(isDigit(c, radix)) {
                    c = read();
                }
                ttype = TokenType.FLOAT_LITERAL;
            }
        }
        if(ttype == TokenType.INT_LITERAL && (c == 'l' || c == 'L')) {
            ttype = TokenType.LONG_LITERAL;
        } else if(c == 'f' || c == 'F') {
            ttype = TokenType.FLOAT_LITERAL;
        } else if(c == 'd' || c == 'D') {
            ttype = TokenType.DOUBLE_LITERAL;
        } else {
            unread();
        }
        return makeToken(ttype);
    }

    private JavaToken makeToken(TokenType ttype)
    {
        return new JavaToken(ttype, text, tokenStart, offset, line, column - offset + tokenStart);
    }

    private boolean isDigit(char c, int radix)
    {
        return c != EOF && Character.digit(c, radix) != -1;
    }

    void skipWhiteSpace()
    {
        for(char c = read(); c != EOF && Character.isWhitespace(c); c = read());
        unread();
    }

    char read()
    {
        char read;
        if(offset >= endOffset) {
            read = EOF;
            ++offset;
        } else {
            read = text.charAt(offset++);
        }

        if(read == '\n') {
            lastColumn = column;
            column = 1;
            line++;
        } else {
            ++column;
        }
        return read;
    }

    void unread()
    {
        if(--offset < startOffset) {
            throw new IllegalStateException("too many unreads");
        }
        if(--column == 0) {
            column = lastColumn;
            if(line > 1) {
                --line;
            }
        }
    }

    private static final char EOF = '\uFFFF';
}