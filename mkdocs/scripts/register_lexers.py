from pygments.lexers._mapping import LEXERS

LEXERS['CATSLexer'] = (
	'lexers.cats_lexer',   # module name
	'CATS',                # display name
	('cats',),             # aliases
	('*.cats',),           # file extensions
	('text/x-cats',)       # mimetypes
)

LEXERS['STOMPLexer'] = (
	'lexers.stomp_lexer',  # module name
	'STOMP',               # display name
	('stomp',),            # aliases
	('*.stomp',),          # file extensions
	('text/x-stomp',)      # mimetypes
)
