# Ideias futuras

Ideias levantadas durante o desenvolvimento que ficaram fora do escopo atual.

## Extração de entidades dos posts

**O que é:** identificar automaticamente, no texto de cada post, os itens técnicos que o analista copia e investiga, e exibi-los separados da mensagem no detalhe do post.

| Entidade | Exemplo | Uso para o analista |
|---|---|---|
| CVEs | `CVE-2025-24434` | Link para a ficha no NVD |
| Nota CVSS | `9.8` | Gravidade da vulnerabilidade citada |
| Domínios e URLs | `infected[.]io` | Investigar/bloquear (exibir em notação *defanged*) |
| IPs e hashes (MD5, SHA-1, SHA-256) | `185.220.101.47` | Copiar para ferramentas de threat intel |
| Hashtags | `#magento` | Buscar posts relacionados |
| Malware, atores, setores | `LockBit` | Exige NER, não dá para fazer só com regex |

**Por que no backend, e não no front:**

- A extração roda uma vez, na coleta ou na classificação, e fica salva junto do post. No front ela seria refeita a cada abertura.
- As entidades viram dado pesquisável: dá para filtrar a lista ("posts com CVE"), contar no dashboard ("CVEs mais citadas") e cruzar posts que citam o mesmo IP ou domínio.
- Permite usar técnicas melhores que regex (NER, listas de IoC conhecidas) e validar o resultado, o que reduz falsos positivos.
- Mantém um único critério entre telas, em vez de cada componente extrair do seu jeito.

**Histórico:** uma primeira versão foi feita no front, com regex, na seção "Entidades extraídas" do detalhe do post. Ela foi removida porque podia ser confundida com o campo `IoC` do classificador e porque o lugar certo para essa lógica é o backend.

## Tolerância a falhas por fonte

**Problema:** sem filtro de fonte, posts, estatísticas, nuvem de palavras e status de coleta consultam as 8 fontes do banco asgard em uma única consulta (`UNION ALL`, ver `PostsRepository`). Se uma fonte tiver problema (tabela ausente, coluna renomeada pelo coletor, falta de permissão), a consulta inteira falha e todas essas telas retornam erro 500, mesmo com as outras fontes funcionando.

**Por que ficou para depois:** hoje o asgard é um banco único e estável, então o risco é baixo.

**Proposta:**

- **Backend:** um componente guarda o conjunto de fontes saudáveis. Ele valida cada fonte rodando a própria consulta dela com `LIMIT 0` (o PostgreSQL só planeja, sem ler linhas), na subida e periodicamente (ex.: a cada 5 minutos), e registra um aviso no log quando uma fonte cai ou volta. Isso detecta tabela ausente, coluna inexistente e falta de permissão, o que checar só `information_schema.tables` não faria.
- As consultas passam a usar só as fontes saudáveis. Um filtro com apenas fontes indisponíveis retorna vazio, não 500.
- `GET /posts/collection-status` ganha um campo `available` por fonte, sem precisar de endpoint novo.
- **Frontend:** aviso no dashboard, algo como *"Fontes indisponíveis no momento: Niflheim, Deep. Os números não incluem esses dados."*. Sem esse aviso, os totais cairiam sem explicação, o que seria pior que o erro.

**Alternativa mais simples:** um teste de integração que rode a consulta de cada fonte com `LIMIT 0` contra o banco e falhe indicando a fonte quebrada. Ele não evita a queda em produção, mas aponta o problema antes do deploy.
