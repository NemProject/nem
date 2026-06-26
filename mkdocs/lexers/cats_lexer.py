from pygments.lexer import RegexLexer, words
from pygments.token import *

__all__ = ['CATSLexer']


class CATSLexer(RegexLexer):
	name = 'CATS'
	aliases = ['cats']
	filenames = ['*.cats']

	tokens = {
		'root': [
			# Whitespace and non-code literals.
			(r'\s+', Text),
			(r'#[^\n]*(?:\r?\n[\t ]*#[^\n]*)*', Comment.Multiline),
			(r'"(?:\\.|[^"\\\n])*"', String.Double),
			(r'<[^<>\n]+>', Name.Tag),
			(r'\[[^\n]+\]', Name.Tag),

			# Grammar keywords and built-in constructors.
			(words(('import', 'using', 'enum', 'struct', 'if'), prefix=r'\b', suffix=r'\b'), Keyword),
			(words(('abstract', 'inline'), prefix=r'\b', suffix=r'\b'), Keyword.Declaration),
			(words(('make_const', 'make_reserved', 'binary_fixed', 'sizeof', 'array'), prefix=r'\b', suffix=r'\b'), Name.Builtin),

			# Conditional and attribute argument operators.
			(r'\bnot\s+(?:equals|in)\b', Operator.Word),
			(words(('equals', 'in', 'not'), prefix=r'\b', suffix=r'\b'), Operator.Word),

			# Attributes, placeholders and transforms.
			(r'@(is_bitwise|is_byte_constrained|alignment|sort_key|sizeref|is_aligned|is_size_implicit|size|initializes|discriminator|comparer)\b', Name.Decorator),
			(words(('pad_last', '__FILL__', '__value__'), prefix=r'\b', suffix=r'\b'), Keyword.Pseudo),
			(r'\bripemd_keccak_256\b', Name.Function),

			# Primitive values.
			(r'\bu?int(?:8|16|32|64)\b', Keyword.Type),
			(r'\b0x[A-F0-9]+\b', Number.Hex),
			(r'\b\d+\b', Number.Integer),

			# CATS name classes.
			(r'\b[A-Z][A-Z0-9_]+\b', Name.Constant),
			(r'\b[A-Z][a-z][A-Za-z0-9]*\b', Name.Class),
			(r'\b[a-z][a-z0-9_]*\b', Name.Variable),
			(r'[=():,!<>]', Punctuation),
			(r'[A-Za-z_][A-Za-z0-9_]*', Name),
		],
	}
