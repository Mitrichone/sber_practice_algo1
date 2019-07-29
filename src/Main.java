import StringMethods.StemmerPorterRU;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws IOException {

        long startTime = System.currentTimeMillis();

        File f = new File("Запросы.txt");
        final int length = (int) f.length();

        String str = "";

        if (length != 0) {

            char[] cbuf = new char[length];

            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF-8");

            final int read = isr.read(cbuf);

            str = new String(cbuf, 0, read);
            isr.close();
        }

        WordHash wordHash = startHashMap(str);
        System.out.println(wordHash.edit);
        System.out.println(wordHash.orig);
        HashMap<String, Integer> BL = removeSelectEntries(wordHash.edit);
        System.out.println("Черный список:");
        System.out.println(BL);
        System.out.println("\nСписок ключевых слов:");
        System.out.println(wordHash.edit);
        System.out.println("Время выполнения (сек): " + (double) (System.currentTimeMillis() - startTime)/1000);

        //region Запись базы
        try {
            FileOutputStream fileOut =
                    new FileOutputStream("hashmap2.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(wordHash.edit);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
        //endregion

        //region Запись черного списка
        try {
            FileOutputStream fileOut =
                    new FileOutputStream("hashmap2BL.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(BL);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
        //endregion

        //region Чтение
        HashMap<String, Integer> e = null;
        try {
            FileInputStream fileIn = new FileInputStream("hashmap2.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            e = (HashMap<String,Integer>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("HashMap<String, Integer> not found");
            c.printStackTrace();
        }
        return;
        //endregion

    }
    static class WordHash {
        HashMap<String, Integer> edit = new HashMap<String, Integer>();
        HashMap<String, String> orig = new HashMap<String, String>();

        public HashMap<String, Integer> getEdit() {
            return edit;
        }

        public HashMap<String, String> getOrig() {
            return orig;
        }

        WordHash(HashMap<String, Integer> edit, HashMap<String, String> orig) {
            this.edit = edit;
            this.orig = orig;
        }
    }

    public static HashMap<String, Integer> removeSelectEntries(HashMap<String, Integer> selects){
        System.out.println("Выберите слова, которые стоит добавить в Черный список:\n" +
                "\"1\" - ВЫБРАТЬ\n" +
                "\"2\" - ПРОПУСТИТЬ\n" +
                "\"0\" - остановить (пропустить все остальные)\n");
        HashMap<String, Integer> r = new HashMap<>();
        Scanner scanner = new Scanner(System.in);
        int size = selects.size();
        int i = 1;
        for(Map.Entry<String, Integer> entry : selects.entrySet()) {
            String key = entry.getKey();
            System.out.print("(" + i + "/" + size + ") "+ key + ": " );
            switch (scanner.nextInt()){
                case 1:
                    r.put(entry.getKey(), entry.getValue());
                    break;
                case 0:
                    for(Map.Entry<String, Integer> e : r.entrySet())
                        selects.remove(e.getKey());
                    return r;
            }
            i++;
        }
        for(Map.Entry<String, Integer> entry : r.entrySet())
            selects.remove(entry.getKey());

        return r;
    }

    public static WordHash startHashMap(String str) {//<T> HashMap<String, Integer>
        str = str.toLowerCase();
        Pattern reg = Pattern.compile("[a-zA-Zа-яА-Я]+");/*
                        + //region Удалить предлоги
                "(((и)|(да)|(не)|(но)|(так)|(а)|(однако)|(же)|(зато)|(или)|(либо)|(то)|(потому)|(оттого)|(как)|(в)|(виду)|(благодаря)|(тому)|(вследствие)|(того)|(тем)|(чтобы)|(чтоб)|(для)|(когда)|(лишь)|(только)|(пока)|(едва)|(если)|(бы)|(раз)|(ли)|(скоро)|(будто$)|(словно)|(точно)|(хотя)|(ни)|(по)|(при))|" //endregion
                        + //region Удалить обращения
                "((вам)|(ваше)|(вас)|(вы))|"
                //endregion
                        + //region И НИКАКОГО ДОБРОГО УТРА!1!!!!111
                "((доброе)|(утро)|(доброго)|(утра)|(хорошего)|(рабочего)|(дня))|"
                //endregion
                        + //region Без уважения тем более
                        "((с)|(уважением)))"
                //endregion
                );*/
        HashMap<String, Integer> list = new HashMap<String, Integer>();
        HashMap<String, String> list2 = new HashMap<String, String>();
        Matcher m = reg.matcher(str);
        while (m.find()) {
            if (m.group().length() > 2) {
                String wordbase = StemmerPorterRU.stem(m.group());
                if (!list.containsKey(wordbase)) {
                    list.put(wordbase, 1);
                    list2.put(wordbase, m.group());
                } else {
                    list.put(wordbase, list.get(wordbase) + 1);
                }
            }

        }

        return new WordHash(sortByValue(list), sortByValue(list2));
    }

    /**
     * Сортировка HashMap
     */
    static <K, V extends Comparable<? super V>> HashMap<K, V> sortByValue(HashMap<K, V> map) {
        List<HashMap.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(HashMap.Entry.comparingByValue());

        HashMap<K, V> result = new LinkedHashMap<>();
        for (HashMap.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Расширение базы ключевых слов
     * @param string строка с обрабатываемыми словами
     * @throws IOException
     */
    public static void updateDataBase(String string) throws IOException {
        Pattern reg = Pattern.compile("[a-zA-Zа-яА-Я]+");
        Matcher m = reg.matcher(string);

        //region Comment
        System.out.println("[Расширение базы ключевых слов]\n" +
                        "// 1 - добавить основу\n" +
                        "// 2 - изменить и добавить\n" +
                        "// 0 - не добавлять\n");
        //endregion

        FileWriter writer = new FileWriter("output.txt", true);
        Scanner scanner = new Scanner(System.in);
        while (m.find()) {
            String wordbase = StemmerPorterRU.stem(m.group());
            System.out.print(m.group() + " (" + wordbase + ") -> ");
            int p = scanner.nextInt();
            switch (p) {
                case 2:
                    scanner.nextLine();
                    wordbase = scanner.nextLine();
                case 1:
                    writer.write(wordbase + System.getProperty("line.separator"));
                    break;
            }
        }
        writer.close();
    }
}
