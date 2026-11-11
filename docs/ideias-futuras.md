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
