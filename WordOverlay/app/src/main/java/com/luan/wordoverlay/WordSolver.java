package com.luan.wordoverlay;

import java.text.Normalizer;
import java.util.*;

public final class WordSolver {
    private static final String WORDS = "" +
        "a ao aos o os as um uma uns umas de da do das dos em no na nos nas por para pra com sem sobre entre contra ante apos após ate até desde sobre sob e ou mas se que quem qual quais como quando onde porque porquê quanto muito pouco mais menos ja já ainda hoje ontem amanha amanhã" +
        "abafa abafar aberto aberta abrigo abril acaso achar agora agua agua afora ajuda algo alma alto amiga amigo amor andar ano anos antes ar arca arco area areia arma arte asno ator aula azul" +
        "bala banco barco base basta bater beijo bem boca bola bolo bom bonita bonito braço braco branco brava bravo breve brincar brilho" +
        "cada cair caixa cama campo canto cara caro carta casa caso causa cedo cento cerca certo chave cheio cheia chegar chuva cidade cinco claro clara clube coisa como corpo coracao coração correr costa custo" +
        "dar data dedo deusa dia dias dica dizer doce dois dona dono dor dormir duro" +
        "ela ele elas eles erro essa esse isso isto estar este esta estes estas eu" +
        "faca faca fácil face falar falta falso fama faro favor fazer feira feliz festa fogo fora forma forte foto fruto" +
        "gato gelado geral gente giro gol gosto grande grupo guerra guia" +
        "hora hoje homem homens ideia igual imagem ir jogo jogar jovem" +
        "lado lago legal letra leve livro lugar luz" +
        "mae mãe mais mal mano mapa mar marca medo meio melhor mesa mesmo mil minha meu minhas meus modo mundo" +
        "nada nao não noite nome novo nova nunca" +
        "obra oito olho onde ontem ordem outro outra" +
        "pai palavra papel parte passar passo paz pedra pegar peixe pela pelo pelos pena perto pessoa pequeno pequena pior poder ponto porta pouco prazer primeiro primeiro" +
        "quase quatro quando quem querer quero raiz raio rato real rede rei rio rir roda rosa rosto rua" +
        "saber sala salto sangue seco seguir sempre sendo senhor sete sim sinal sobre sonho sorte som sopa subir" +
        "tal talvez tarde te tempo ter terra texto tipo toda todo todos todas tomar trabalho tres três triste tudo" +
        "um uma usar" +
        "valor velha velho vez vida vidro vila vinho viver voce você" +
        "zero" +
        // Extra short/common words useful in word-game dictionaries
        "aba abo aca ago ai ala ale ali ama amo ano aro ato ave avo bar boa boa boa cao cao cal cor dar deu dia ela era eco elo era fez fio fim foi fora foi fui gas gel gol grau ira isso lar ler lhe lua mal mao mão meu mil nao nas nem net nos nua oca ora ovo pai pau pe pena por pra rei sao são seu sua sua sol som tao tão teu tia tio tom vai vem ver vez via voo" +
        // common inflections
        "abrem abriu abrimos aceite aceita aceitou acerta acertar acabam acabou acordo adora adoro adoram ajuda ajudam ajude ajudou alem alembrar alembrado alta altos andou andam andando aparece aparecem apareceu aprende aprendem aprendeu chegam chegou chamam chamou chama chamadas chamado chamares come comem comeu comigo começa comecar começou consegue conseguir contam conta contou correm correu criando criou cuidam cuida deu deixam deixou deixa dentro deve devem devia dizem disse digo digam entra entram entrou espero espera esperam estava estavam estou fazemos faz fez ficam ficou fica foram fomos fosse fossem ganha ganhar ganhou gostam gostou houve indica indicares levam levou leva lendo leu manda mandou manda marca marcou merecem merece mostrou mostra mostram nasce nasceu passa passam passou pega pegou pegam pensa pensam pensou pode podem podia põe poe podem querem queriam quero recebe recebeu recebe sabem sabia saiu sabe sabeis sejam segue seguem seguiu sente sentem sentiu será sera serão seram seja seja feito tinha tinham tive tiveram torna tornou torna-se traz trouxe usam usou usa veio vem vendo viu vivem viveu voltou volta voltam";

    private static Set<String> buildSet() {
        HashSet<String> s = new HashSet<>();
        for (String raw : WORDS.split("\\s+")) {
            String w = normalize(raw);
            if (w.length() >= 3 && w.length() <= 12 && w.matches("[a-z]+")) s.add(w);
        }
        return s;
    }
    private static final Set<String> DICT = buildSet();

    public static String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z]", "");
    }

    public static Map<Integer,List<String>> solve(String letters, Set<Integer> lengths) {
        String l = normalize(letters);
        if (l.length() < 3) return Collections.emptyMap();
        int[] available = counts(l);
        TreeMap<Integer,List<String>> out = new TreeMap<>(Collections.reverseOrder());
        for (String w : DICT) {
            if (lengths != null && !lengths.isEmpty() && !lengths.contains(w.length())) continue;
            if (canBuild(w, available)) out.computeIfAbsent(w.length(), k -> new ArrayList<>()).add(w.toUpperCase(Locale.ROOT));
        }
        for (List<String> a : out.values()) {
            Collections.sort(a, (x,y) -> { int c=Integer.compare(y.length(),x.length()); return c!=0?c:x.compareTo(y); });
        }
        return out;
    }

    private static int[] counts(String s) { int[] c=new int[26]; for(char ch:s.toCharArray()) if(ch>='a'&&ch<='z') c[ch-'a']++; return c; }
    private static boolean canBuild(String w,int[] c){ int[] n=new int[26]; for(char ch:w.toCharArray()){int i=ch-'a'; if(i<0||i>=26||++n[i]>c[i]) return false;} return true; }
}
