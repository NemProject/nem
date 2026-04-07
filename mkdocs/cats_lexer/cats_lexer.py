from pygments.lexer import RegexLexer
from pygments.token import *

__all__ = ['CATSLexer']

class CATSLexer(RegexLexer):
	name = 'CATS'
	aliases = ['cats']
	filenames = ['*.cats']

	tokens = {
		'root': [
			(r'\b(import|using|enum|struct|make_const|make_reserved|binary_fixed|sizeof|if|equals|not equals|has|not has|array|__FILL__|__value__|abstract|inline|pad_last|not pad_last)\b', Keyword),
			(r'\b(__FILL__|__value__)\b', Keyword.Reserved),
			(r'\b(int8|int16|int32|int64|uint8|uint16|uint32|uint64)\b', Keyword.Type),
			(r'@(is_bitwise|is_aligned|is_size_implicit|size|initializes|discriminator|comparer|alignment|is_byte_constrained|sort_key|sizeref)\b', Keyword),
			(r'(0x)?\d+', Number),
			(r'[a-zA-Z_][a-zA-Z0-9_]*', Name),
			(r'#.*', Comment.Single),
			(r'\s+', Text),
		],
	}
