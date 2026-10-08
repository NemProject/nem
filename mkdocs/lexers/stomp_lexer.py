from pygments.lexer import RegexLexer, bygroups
from pygments.token import *

__all__ = ['STOMPLexer']


class STOMPLexer(RegexLexer):
	name = 'STOMP'
	aliases = ['stomp']
	filenames = ['*.stomp']

	tokens = {
		'root': [
			(r'[A-Z][A-Z0-9_-]*', Keyword, 'headers'),
			(r'.+', Text, 'headers'),
		],
		'headers': [
			(r'\n\n', Text, 'body'),
			(r'\n', Text),
			(r'([^:\s]+)(:)([^\n]*)', bygroups(Name.Attribute, Punctuation, String)),
			(r'.+', Text),
		],
		'body': [
			(r'.+', Text),
			(r'\n', Text),
		],
	}
