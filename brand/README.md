# Identidade Aluguéis Nani

Símbolo de casa integrado ao N de Nani, desenhado em vetor, em marfim (#F7F3EC)
sobre índigo (#6870D9 → #4851AF → #303771). O telhado identifica o contexto
de imóveis; as duas hastes e a diagonal formam o monograma. Terminações
arredondadas mantêm a marca acolhedora e legível em tamanhos pequenos.

- `nani-symbol.svg`: mestre vetorial com máscara arredondada para apresentação.
- `nani-icon.png`: prévia de 512 px.
- `nani-icon-preview.png`: revisão em máscaras circular/arredondada, tamanhos pequenos e monocromático.
- `generate_icons.py`: fonte geométrica e geração reproduzível de SVG, vetores Android e WebP (`python brand/generate_icons.py`; requer Pillow e resvg-py apenas no computador).
- Android: foreground vetorial, background em gradiente, camada monocromática e alternativas WebP por densidade.
- `ic_nani_notification.xml`: monograma anterior preservado para notificações; esta alteração abrange somente o ícone de lançamento.

A máscara final do launcher pertence ao Android; a marca fica dentro da região segura adaptativa.
Sem fonte externa, imagem de banco ou dependência de rede. O SVG e os vetores Android são os arquivos mestres.
