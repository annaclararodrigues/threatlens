package com.backend.threatlens.utils;

import java.util.Set;

public class StopWords {

    private StopWords() {}

    public static final Set<String> ALL = Set.of(
            // Português
            "a", "o", "as", "os", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
            "em", "no", "na", "nos", "nas", "por", "para", "com", "sem", "sob", "sobre",
            "e", "ou", "mas", "que", "se", "ao", "aos", "à", "às", "pelo", "pela", "pelos", "pelas",
            "este", "esta", "estes", "estas", "esse", "essa", "esses", "essas", "isso", "isto",
            "aquele", "aquela", "aqueles", "aquelas", "aquilo", "seu", "sua", "seus", "suas",
            "meu", "minha", "meus", "minhas", "nosso", "nossa", "nossos", "nossas",
            "eu", "tu", "ele", "ela", "vos", "eles", "elas", "voce", "voces",
            "é", "foi", "ser", "sao", "são", "está", "estão", "era", "eram", "foram", "ter", "tem",
            "tinha", "haver", "há", "como", "quando", "onde", "porque", "pois", "ja", "já",
            "mais", "menos", "muito", "muita", "muitos", "muitas", "pouco", "pouca", "poucos", "poucas",
            "todo", "toda", "todos", "todas", "algum", "alguma", "alguns", "algumas",
            "nao", "não", "sim", "tambem", "também", "ate", "até", "entre", "apos", "após", "durante",

            // English
            "the", "an", "and", "or", "but", "if", "then", "else", "of", "at", "by", "for",
            "with", "without", "about", "against", "between", "into", "through", "during",
            "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on",
            "off", "over", "under", "again", "further", "once", "here", "there", "when", "where",
            "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some",
            "such", "nor", "not", "only", "own", "same", "so", "than", "too", "very",
            "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having",
            "does", "did", "doing", "will", "would", "should", "could", "can",
            "this", "that", "these", "those", "it", "its", "i", "you", "he", "she", "we", "they",
            "what", "which", "who", "whom", "his", "her", "their", "our", "your", "my"
    );
}
