from pygments.lexers._mapping import LEXERS

LEXERS['CATSLexer'] = (
	'cats_lexer.cats_lexer',    # module name
	'CATS',           # display name
	('cats',),        # aliases
	('*.cats',),      # file extensions
	('text/x-cats',)  # mimetypes
)
